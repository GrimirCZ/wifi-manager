package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Response
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.FormData
import com.microsoft.playwright.options.RequestOptions
import com.microsoft.playwright.options.ViewportSize
import cz.grimir.wifimanager.app.Application
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class BaseWorkflowE2ETest {
    companion object {
        const val DEFAULT_DEVICE_MAC = "02:00:00:00:00:01"
        private val containersStarted = AtomicBoolean(false)

        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("wifimanager")
                .withUsername("wifimanager")
                .withPassword("wifimanager")!!

        val keycloak: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("quay.io/keycloak/keycloak:26.3.0"))
                .withCopyFileToContainer(
                    MountableFile.forHostPath(realmImportPath()),
                    "/opt/keycloak/data/import/wifimanager-realm.json",
                ).withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                .withEnv("KC_HTTP_ENABLED", "true")
                .withEnv("KC_HOSTNAME_STRICT", "false")
                .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
                .withCommand("start-dev", "--import-realm")
                .withExposedPorts(8080)
                .waitingFor(
                    Wait
                        .forHttp("/realms/wifimanager/.well-known/openid-configuration"),
                )

        private lateinit var playwright: Playwright
        private lateinit var browser: Browser

        @JvmStatic
        @BeforeAll
        fun startBrowser() {
            ensureContainersStarted()
            playwright = Playwright.create()
            browser =
                playwright.chromium().launch(
                    BrowserType
                        .LaunchOptions()
                        .setHeadless(true),
                )
        }

        @JvmStatic
        @AfterAll
        fun stopBrowser() {
            browser.close()
            playwright.close()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            ensureContainersStarted()
            val issuer = "http://${keycloak.host}:${keycloak.getMappedPort(8080)}/realms/wifimanager"

            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("server.port") { "8080" }

            registry.add("spring.docker.compose.enabled") { "false" }

            registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri") { issuer }
            registry.add("spring.security.oauth2.client.registration.admin.provider") { "keycloak" }
            registry.add("spring.security.oauth2.client.registration.admin.client-id") { "admin-web" }
            registry.add("spring.security.oauth2.client.registration.admin.client-secret") { "admin-web-secret" }
            registry.add("spring.security.oauth2.client.registration.admin.scope") { "openid,profile,email,roles" }

            registry.add("wifimanager.security.oidc-registration-id") { "admin" }
            registry.add("wifimanager.security.authorities-provider-by-registration.admin") { "keycloak-realm-roles" }

            registry.add("wifimanager.captive.auth.google.enabled") { "false" }
            registry.add("wifimanager.captive.auth.keycloak.enabled") { "true" }
            registry.add("wifimanager.captive.auth.keycloak.issuer-uri") { issuer }
            registry.add("wifimanager.captive.auth.keycloak.client-id") { "captive-portal" }
            registry.add("wifimanager.captive.auth.keycloak.client-secret") { "captive-portal-secret" }
            registry.add("wifimanager.captive.auth.keycloak.allowed-devices-by-role") { "WIFI_STAFF=1;WIFI_ADMIN=5" }

            registry.add("wifimanager.captive.router-agent.type") { "dummy" }
            registry.add("wifimanager.captive.router-agent.dummy.default-client-mac-address") { DEFAULT_DEVICE_MAC }
            registry.add("wifimanager.captive.router-agent.dummy.default-client-hostname") { "e2e-device" }
            registry.add("wifimanager.captive.router-agent.dummy.default-dhcp-vendor-class") { "android-dhcp-14" }
            registry.add("wifimanager.captive.router-agent.dummy.default-dhcp-prl-hash") { "hash-a" }
            registry.add("wifimanager.captive.router-agent.dummy.default-dhcp-hostname") { "e2e-device.local" }

            // Avoid binding the application.yml fallback literal "[]" as a single invalid CIDR string.
            registry.add("wifimanager.captive.request-routing.ipv4-subnets[0]") { "198.51.100.0/24" }
            registry.add("wifimanager.captive.request-routing.ipv6-subnets[0]") { "2001:db8:ffff::/64" }
        }

        private fun realmImportPath(): String {
            val rootDir = System.getProperty("wifimanager.root-dir") ?: error("Missing wifimanager.root-dir")
            return Paths.get(rootDir, "app", "keycloak", "import", "wifimanager-realm.json").toString()
        }

        @Synchronized
        private fun ensureContainersStarted() {
            if (containersStarted.get()) {
                return
            }
            postgres.start()
            keycloak.start()
            containersStarted.set(true)
        }

        protected fun browser(): Browser = browser
    }

    private lateinit var context: BrowserContext
    protected lateinit var page: Page

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    protected enum class RenderMode {
        DESKTOP,
        PHONE,
    }

    protected data class ScreenshotSpec(
        val fileName: String,
        val mode: RenderMode,
        val fullPage: Boolean = false,
    )

    protected enum class LanguageVariant(
        val code: String,
    ) {
        EN("en"),
        CS("cs"),
    }

    protected val baseUrl: String
        get() = "http://localhost:8080"

    @BeforeEach
    fun setupTest() {
        truncateDomainTables()
        resetBrowserContext()
    }

    @AfterEach
    fun teardownTest() {
        if (::context.isInitialized) {
            context.close()
        }
    }

    protected fun loginAdmin(
        username: String,
        password: String,
    ) {
        page.navigate("$baseUrl/admin/login")
        page.locator("a[href^='/oauth2/authorization/']").first().click()

        val usernameInput = page.locator("#username, input[name='username']").first()
        val passwordInput = page.locator("#password, input[name='password']").first()

        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            if (isAdminHome(page.url())) {
                return
            }
            if (usernameInput.isVisible) {
                usernameInput.fill(username)
                passwordInput.fill(password)
                page.locator("#kc-login, button[type='submit']").first().click()
                waitForAdminHome()
                return
            }
            page.waitForTimeout(200.0)
        }

        throw AssertionError("Admin login did not reach /admin. finalUrl=${page.url()}")
    }

    protected fun loginAsStaff() {
        loginAdmin(username = "user", password = "user")
        assertAccountIdentityVisible("user@wifimanager.local")
    }

    protected fun loginAsAdmin() {
        loginAdmin(username = "admin", password = "admin")
        assertAccountIdentityVisible("admin@wifimanager.local")
    }

    protected fun openTicketsFromAccountMenu() {
        page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Tickets")).click()
        page.waitForURL("**/admin")
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Create ticket"))).isVisible()
    }

    protected fun openDevicesFromAccountMenu() {
        page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Devices")).click()
        page.waitForURL("**/admin/devices**")
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("My devices"))).isVisible()
    }

    protected fun openAllowedMacFromAccountMenu() {
        page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Admin")).click()
        page.waitForURL("**/admin/allowed-mac**")
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                Page.GetByRoleOptions().setName("Network operations")
            )
        ).isVisible()
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Allowed MAC addresses"))).isVisible()
        assertThat(page.getByText("Network clients").first()).isVisible()
    }

    protected fun assertTopNavItemVisible(
        label: String,
        shouldBeVisible: Boolean,
    ) {
        val item = page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName(label))
        if (shouldBeVisible) {
            assertThat(item).isVisible()
        } else {
            assertEquals(0, item.count())
        }
    }

    protected fun createTicketAndReturnCsrfToken(
        validityMinutes: String = "45",
        requireUserNameOnLogin: Boolean = false,
    ): String {
        page.navigate("$baseUrl/admin")

        val form = page.locator("form:has(select[name='validityMinutes'])").first()
        val createTicketToggler = page.getByText("Create ticket").first()
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (form.isVisible) {
                break
            }
            if (createTicketToggler.isVisible) {
                createTicketToggler.click()
            }
            page.waitForTimeout(200.0)
        }
        if (!form.isVisible) {
            val pageSummary = page.locator("body").innerText().replace(Regex("\\s+"), " ").take(400)
            throw AssertionError("Ticket form not visible. url=${page.url()} body=$pageSummary")
        }

        val csrfToken = form.locator("input[name='_csrf']").first().inputValue()
        form.locator("select[name='validityMinutes']").selectOption(validityMinutes)
        if (requireUserNameOnLogin) {
            form.locator("input[name='requireUserNameOnLogin']").check()
        } else {
            form.locator("input[name='requireUserNameOnLogin']").uncheck()
        }
        form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Create")).click()

        assertThat(page.locator("#ticket-panel .ticket-card")).isVisible()
        return csrfToken
    }

    protected fun extractActiveTicketCode(): String {
        val formatted = page.locator("#ticket-panel .ticket-card .code-card span").first().innerText()
        return formatted.replace(Regex("[^0-9A-Za-z]"), "")
    }

    protected fun submitDuplicateTicketRequest(
        csrfToken: String,
        validityMinutes: String = "45",
    ) {
        val response =
            page
                .request()
                .post(
                    "$baseUrl/admin/ticket",
                    RequestOptions
                        .create()
                        .setForm(
                            FormData
                                .create()
                                .set("_csrf", csrfToken)
                                .set("validityMinutes", validityMinutes),
                        ),
                )

        if (response.status() !in setOf(200, 302, 303)) {
            throw AssertionError("Unexpected duplicate ticket response status=${response.status()}")
        }
    }

    protected fun submitCaptiveAccessCode(
        accessCode: String,
        acceptTerms: Boolean = true,
        name: String? = null,
    ) {
        page.navigate("$baseUrl/captive")

        val form = page.locator("form:has(input[name='accessCode'])").first()
        assertThat(form).isVisible()

        form.getByLabel("Access code").fill(accessCode)
        val terms = form.getByLabel("I agree to the terms and conditions.")
        if (acceptTerms) {
            terms.check()
        } else {
            terms.uncheck()
        }

        form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Continue")).click()

        if (name != null) {
            assertThat(page.getByLabel("Your name")).isVisible()
            page.getByLabel("Your name").fill(name)
            page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Authorize")).click()
        }
    }

    protected fun loginCaptiveAndAuthorizeDevice(
        username: String,
        password: String,
        deviceName: String = "E2E Device",
    ) {
        page.navigate("$baseUrl/captive/login")

        val form = page.locator("form:has(input[name='username']):has(input[name='password'])").first()
        assertThat(form).isVisible()

        form.getByLabel("Username").fill(username)
        form.getByLabel("Password").fill(password)
        form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Sign in")).click()

        page.waitForURL("**/captive/device")

        page.getByLabel("Device name").fill(deviceName)
        page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Authorize device")).click()

        page.waitForURL("**/captive")
    }

    protected fun waitUntilDeviceNotVisible(
        mac: String,
        scope: String = "mine",
        timeoutMs: Long = 30_000,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            page.navigate("$baseUrl/admin/devices?scope=$scope")
            val listText = page.locator("#user-device-list").innerText()
            if (!listText.contains(mac)) {
                return
            }
            page.waitForTimeout(350.0)
        }

        throw AssertionError("Device $mac remained visible in /admin/devices for ${timeoutMs}ms")
    }

    protected fun clickAndConfirmAndWaitForResponse(
        trigger: Locator,
        expectedMethod: String,
        pathPredicate: (String) -> Boolean,
    ): Response {
        val confirmButton = page.locator("wm-confirm-dialog [data-role='confirm']")
        trigger.click()
        assertThat(confirmButton).isVisible()
        return page.waitForResponse(
            { candidate ->
                candidate.request().method() == expectedMethod && pathPredicate(URI.create(candidate.url()).path)
            },
        ) {
            confirmButton.click()
        }
    }

    protected fun authorizeDeviceAsUserInIsolatedContext(deviceName: String = "E2E Device") {
        val isolatedContext = browser().newContext(browserContextOptions(RenderMode.DESKTOP))
        val isolatedPage = isolatedContext.newPage()
        isolatedContext.use {
            isolatedPage.navigate("$baseUrl/captive/login")
            val form = isolatedPage.locator("form:has(input[name='username']):has(input[name='password'])").first()
            assertThat(form).isVisible()
            form.getByLabel("Username").fill("user")
            form.getByLabel("Password").fill("user")
            form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Sign in")).click()
            isolatedPage.waitForURL("**/captive/device")
            isolatedPage.getByLabel("Device name").fill(deviceName)
            isolatedPage.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Authorize device")).click()
            isolatedPage.waitForURL("**/captive")
            assertThat(
                isolatedPage.getByRole(
                    AriaRole.HEADING,
                    Page.GetByRoleOptions().setName("You're connected"),
                ),
            ).isVisible()
        }
    }

    protected fun <T> withIsolatedPage(
        mode: RenderMode = RenderMode.DESKTOP,
        block: (Page) -> T,
    ): T {
        val isolatedContext = browser().newContext(browserContextOptions(mode))
        val isolatedPage = isolatedContext.newPage()
        isolatedPage.onDialog { dialog -> dialog.accept() }
        isolatedContext.use {
            return block(isolatedPage)
        }
    }

    protected fun useRenderMode(mode: RenderMode) {
        resetBrowserContext(mode)
    }

    protected fun captureScreenshot(
        spec: ScreenshotSpec,
        target: Locator? = null,
    ): Path {
        if (spec.mode != activeRenderMode) {
            useRenderMode(spec.mode)
        }
        preparePageForScreenshot()
        waitForPageToSettle()

        val outputPath = screenshotOutputDir().resolve(spec.fileName)
        Files.createDirectories(outputPath.parent)
        if (target != null) {
            target.screenshot(
                Locator.ScreenshotOptions()
                    .setAnimations(com.microsoft.playwright.options.ScreenshotAnimations.DISABLED)
                    .setPath(outputPath),
            )
        } else {
            page.screenshot(
                Page.ScreenshotOptions()
                    .setAnimations(com.microsoft.playwright.options.ScreenshotAnimations.DISABLED)
                    .setFullPage(spec.fullPage)
                    .setPath(outputPath),
            )
        }
        return outputPath
    }

    protected fun captureLocalizedScreenshots(
        spec: ScreenshotSpec,
        target: Locator? = null,
        languages: List<LanguageVariant> = listOf(LanguageVariant.EN, LanguageVariant.CS),
    ): List<Path> {
        val outputs = mutableListOf<Path>()
        for (language in languages) {
            setLanguage(language)
            outputs.add(captureScreenshot(spec.withLanguage(language), target))
            if (target != null) {
                outputs.add(captureScreenshot(spec.withLanguage(language).asFullPageVariant(), target = null))
            }
        }
        return outputs
    }

    protected fun waitForPageToSettle() {
        page.waitForLoadState()
        page.waitForFunction(
            """
            () => {
              const htmxReady = window.htmx === undefined || document.querySelector('.htmx-request') === null;
              return htmxReady && document.fonts.status === 'loaded';
            }
            """.trimIndent(),
        )
        page.waitForTimeout(150.0)
    }

    private fun truncateDomainTables() {
        jdbcTemplate.execute(
            """
            DO $$
            DECLARE
              row_record RECORD;
            BEGIN
              FOR row_record IN
                SELECT schemaname, tablename
                FROM pg_tables
                WHERE schemaname IN ('admin', 'captive', 'auth')
                  AND tablename <> 'flyway_schema_history'
              LOOP
                EXECUTE format('TRUNCATE TABLE %I.%I RESTART IDENTITY CASCADE', row_record.schemaname, row_record.tablename);
              END LOOP;
            END $$;
            """.trimIndent(),
        )
    }

    private fun waitForAdminHome(timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isAdminHome(page.url())) {
                return
            }
            page.waitForTimeout(200.0)
        }
        throw AssertionError("Admin login did not reach /admin after submit. finalUrl=${page.url()}")
    }

    private fun isAdminHome(url: String): Boolean {
        val path = URI(url).path
        return path == "/admin" || path == "/admin/"
    }

    private fun openAccountMenu() {
        val profileButton = page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Profile")).first()
        profileButton.click()
        val accountMenu = profileButton.locator("xpath=following-sibling::*[@role='menu'][1]")
        assertThat(accountMenu).isVisible()
    }

    private fun assertAccountIdentityVisible(email: String) {
        val profileButton = page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Profile")).first()
        openAccountMenu()
        val accountMenu = profileButton.locator("xpath=following-sibling::*[@role='menu'][1]")
        assertThat(accountMenu.getByText(email)).isVisible()
    }

    private var activeRenderMode: RenderMode = RenderMode.DESKTOP

    private fun resetBrowserContext(mode: RenderMode = RenderMode.DESKTOP) {
        if (::context.isInitialized) {
            context.close()
        }
        activeRenderMode = mode
        context = browser().newContext(browserContextOptions(mode))
        page = context.newPage()
        page.onDialog { dialog -> dialog.accept() }
    }

    private fun browserContextOptions(mode: RenderMode): Browser.NewContextOptions {
        val options = Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
        return when (mode) {
            RenderMode.DESKTOP ->
                options.setViewportSize(1440, 1200)

            RenderMode.PHONE ->
                options
                    .setViewportSize(ViewportSize(375, 667))
                    .setScreenSize(375, 667)
                    .setDeviceScaleFactor(2.0)
                    .setIsMobile(true)
                    .setHasTouch(true)
                    .setUserAgent(
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
                            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 " +
                            "Mobile/15E148 Safari/604.1"
                    )
        }
    }

    private fun preparePageForScreenshot() {
        page.evaluate(
            """
            () => {
              if (document.activeElement instanceof HTMLElement) {
                document.activeElement.blur();
              }
            }
            """.trimIndent(),
        )
        page.addStyleTag(
            Page.AddStyleTagOptions().setContent(
                """
                *,
                *::before,
                *::after {
                  animation: none !important;
                  transition: none !important;
                  caret-color: transparent !important;
                }
                [data-testid='debug'],
                #ticket-copy-toast {
                  display: none !important;
                }
                *:focus,
                *:focus-visible,
                *:active {
                  outline: none !important;
                  box-shadow: none !important;
                }
                .group:hover [role='menu'],
                .group:focus-within [role='menu'] {
                  display: none !important;
                }
                """
                    .trimIndent(),
            ),
        )
    }

    private fun screenshotOutputDir(): Path {
        val explicit = System.getProperty("wifimanager.screenshots.output-dir")
        val rootDir = System.getProperty("wifimanager.root-dir") ?: error("Missing wifimanager.root-dir")
        val base =
            if (!explicit.isNullOrBlank()) {
                Paths.get(explicit)
            } else {
                Paths.get(rootDir, "build", "reports", "screenshots")
            }
        return Files.createDirectories(base)
    }

    private fun setLanguage(language: LanguageVariant) {
        val current = URI(page.url())
        val query = current.rawQuery.orEmpty()
        val preserved =
            query
                .split("&")
                .filter { it.isNotBlank() && !it.startsWith("lang=") }
                .toMutableList()
        preserved += "lang=${language.code}"
        val querySuffix = if (preserved.isEmpty()) "" else "?${preserved.joinToString("&")}"
        page.navigate("$baseUrl${current.path}$querySuffix")
        waitForPageToSettle()
    }

    private fun ScreenshotSpec.withLanguage(language: LanguageVariant): ScreenshotSpec {
        val dotIndex = fileName.lastIndexOf('.')
        val withSuffix =
            if (dotIndex >= 0) {
                "${fileName.substring(0, dotIndex)}-${language.code}${fileName.substring(dotIndex)}"
            } else {
                "$fileName-${language.code}"
            }
        return copy(fileName = withSuffix)
    }

    private fun ScreenshotSpec.asFullPageVariant(): ScreenshotSpec {
        val dotIndex = fileName.lastIndexOf('.')
        val withSuffix =
            if (dotIndex >= 0) {
                "${fileName.substring(0, dotIndex)}-full${fileName.substring(dotIndex)}"
            } else {
                "$fileName-full"
            }
        return copy(fileName = withSuffix, fullPage = true)
    }
}
