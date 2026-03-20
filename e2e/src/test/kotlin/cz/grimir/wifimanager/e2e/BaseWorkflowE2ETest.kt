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

    protected val baseUrl: String
        get() = "http://localhost:8080"

    @BeforeEach
    fun setupTest() {
        truncateDomainTables()
        context = browser().newContext(Browser.NewContextOptions().setIgnoreHTTPSErrors(true))
        page = context.newPage()
        page.onDialog { dialog -> dialog.accept() }
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
        page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Sign in with SSO")).click()

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
        openAccountMenu()
        page.getByRole(AriaRole.MENUITEM, Page.GetByRoleOptions().setName("Tickets")).click()
        page.waitForURL("**/admin")
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Create ticket"))).isVisible()
    }

    protected fun openDevicesFromAccountMenu() {
        openAccountMenu()
        page.getByRole(AriaRole.MENUITEM, Page.GetByRoleOptions().setName("Devices")).click()
        page.waitForURL("**/admin/devices**")
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("My devices"))).isVisible()
    }

    protected fun openAllowedMacFromAccountMenu() {
        openAccountMenu()
        page.getByRole(AriaRole.MENUITEM, Page.GetByRoleOptions().setName("Network clients")).click()
        page.waitForURL("**/admin/allowed-mac**")
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                Page.GetByRoleOptions().setName("Allowed MAC addresses")
            )
        ).isVisible()
        assertThat(page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Network clients"))).isVisible()
    }

    protected fun assertAccountMenuItemVisible(
        label: String,
        shouldBeVisible: Boolean,
    ) {
        openAccountMenu()
        val item = page.getByRole(AriaRole.MENUITEM, Page.GetByRoleOptions().setName(label))
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
        val isolatedContext = browser().newContext(Browser.NewContextOptions().setIgnoreHTTPSErrors(true))
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
        page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("User menu")).click()
        assertThat(page.locator("[role='menu']")).isVisible()
    }

    private fun assertAccountIdentityVisible(email: String) {
        openAccountMenu()
        assertThat(page.getByText(email)).isVisible()
    }
}
