package cz.grimir.wifimanager.e2e.screenshots

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import cz.grimir.wifimanager.e2e.BaseWorkflowE2ETest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject
import java.util.UUID

@Tag("screenshots")
class DocumentationScreenshotsE2ETest : BaseWorkflowE2ETest() {
    private companion object {
        const val CZECH_DEVICE_NAME = "Ucitelsky tablet"
        const val CZECH_ALLOWED_MAC_NOTE = "Snimek k diplomove praci"
        const val CZECH_REQUIRED_NAME = "Jan Novak"
    }

    @Test
    fun `captures admin ticket creation screen`() {
        useRenderMode(RenderMode.DESKTOP)

        loginAsStaffForScreenshots()
        openTicketsPage()
        waitForPageToSettle()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "admin-ticket-create-desktop.png",
                mode = RenderMode.DESKTOP,
            ),
            target = page.locator("#ticket-panel > .hero-shell").first(),
        )
    }

    @Test
    fun `captures admin ticket detail with authorized device`() {
        useRenderMode(RenderMode.DESKTOP)

        loginAsStaffForScreenshots()
        openTicketsPage()
        createTicketAndReturnCsrfTokenForScreenshots()
        val ticketCode = extractActiveTicketCode()

        submitCaptiveAccessCodeInIsolatedContext(ticketCode)
        waitForTicketDeviceRow()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "admin-ticket-detail-desktop.png",
                mode = RenderMode.DESKTOP,
            ),
            target = page.locator("#ticket-panel .ticket-card").first(),
        )
    }

    @Test
    fun `captures admin allowed mac screen`() {
        useRenderMode(RenderMode.DESKTOP)

        authorizeDeviceAsUserInIsolatedContextForScreenshots(deviceName = CZECH_DEVICE_NAME)

        loginAsAdminForScreenshots()
        openAllowedMacPage()
        addAllowedMacEntry()
        waitForPageToSettle()

        val path = java.net.URI(page.url()).path
        val networkClientSummary = page.locator("details:has(#network-client-list) summary").first()
        val networkClientList = page.locator("#network-client-list")
        val refreshButton = networkClientList.locator("button[hx-target='#network-client-list']").first()
        val networkClientRow = networkClientList.locator("li[data-mac]").first()
        val target = page.locator("main.app-main").first()

        for (language in listOf("en", "cs")) {
            page.navigate("$baseUrl$path?lang=$language")
            waitForPageToSettle()

            networkClientSummary.click()
            assertThat(networkClientList).isVisible()
            refreshButton.click()
            assertThat(networkClientRow).isVisible()

            captureScreenshot(
                ScreenshotSpec(
                    fileName = "admin-allowed-mac-desktop-$language.png",
                    mode = RenderMode.DESKTOP,
                ),
                target = target,
            )
            captureScreenshot(
                ScreenshotSpec(
                    fileName = "admin-allowed-mac-desktop-$language-full.png",
                    mode = RenderMode.DESKTOP,
                    fullPage = true,
                ),
            )
        }
    }

    @Test
    fun `captures user device management screen`() {
        useRenderMode(RenderMode.DESKTOP)

        authorizeDeviceAsUserInIsolatedContextForScreenshots(deviceName = CZECH_DEVICE_NAME)

        loginAsStaffForScreenshots()
        openUserDeviceManagementPage()
        assertThat(page.locator("#user-device-list li").filter(Locator.FilterOptions().setHasText(CZECH_DEVICE_NAME))).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "admin-user-device-management-desktop.png",
                mode = RenderMode.DESKTOP,
            ),
            target = page.locator("#user-device-list").first(),
        )
    }

    @Test
    fun `captures admin ticket help dialog`() {
        useRenderMode(RenderMode.DESKTOP)

        loginAsStaffForScreenshots()
        openTicketsPage()
        createTicketAndReturnCsrfTokenForScreenshots()
        waitForPageToSettle()

        val dialog = page.locator("wm-captive-help-dialog [role='dialog']").first()
        val path = java.net.URI(page.url()).path
        for (language in listOf("en", "cs")) {
            page.navigate("$baseUrl$path?lang=$language")
            waitForPageToSettle()
            page.evaluate("document.querySelector('wm-captive-help-dialog')?.open()")
            assertThat(dialog).isVisible()

            captureScreenshot(
                ScreenshotSpec(
                    fileName = "admin-ticket-help-dialog-desktop-$language.png",
                    mode = RenderMode.DESKTOP,
                    fullPage = true,
                ),
            )
        }
    }

    @Test
    fun `captures captive access code entry screen on phone`() {
        useRenderMode(RenderMode.PHONE)

        page.navigate("$baseUrl/captive")
        assertThat(page.locator("#access-code")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-code-entry-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive account login form on phone`() {
        useRenderMode(RenderMode.PHONE)

        page.navigate("$baseUrl/captive/login")
        assertThat(page.locator("form:has(#username):has(#password)")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-account-login-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive name input screen on phone`() {
        val ticketCode = createTicketAsStaffAndGetCode(requireUserNameOnLogin = true)

        for (language in listOf(LanguageVariant.EN, LanguageVariant.CS)) {
            useRenderMode(RenderMode.PHONE)

            submitCaptiveAccessCodeForRequiredNameStep(ticketCode, language)
            page.locator("#required-name").fill(CZECH_REQUIRED_NAME)
            assertThat(page.locator("#required-name")).hasValue(CZECH_REQUIRED_NAME)

            captureScreenshot(
                ScreenshotSpec(
                    fileName = "captive-name-input-phone-${language.code}.png",
                    mode = RenderMode.PHONE,
                    fullPage = true,
                ),
            )
        }
    }

    @Test
    fun `captures captive randomized mac warning on phone`() {
        useRenderMode(RenderMode.PHONE)

        openCaptiveDeviceSetupAsUser()
        assertThat(page.locator(".alert.alert-warning")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-randomized-mac-warning-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive device authorized by account on phone`() {
        useRenderMode(RenderMode.PHONE)

        authorizeDeviceAsUserForScreenshots(page, deviceName = CZECH_DEVICE_NAME)
        assertThat(page.locator(".captive-card-hero .alert.alert-success")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-account-device-authorized-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive connected screen on phone`() {
        useRenderMode(RenderMode.PHONE)

        val ticketCode = createTicketAsStaffAndGetCode()
        submitCaptiveAccessCodeForScreenshots(ticketCode, acceptTerms = true)
        assertThat(page.locator(".captive-details")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-connected-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive kicked screen on phone`() {
        useRenderMode(RenderMode.PHONE)

        val ticketCode = createTicketAsStaffAndGetCode()
        submitCaptiveAccessCodeForScreenshots(ticketCode, acceptTerms = true)
        assertThat(page.locator(".captive-details")).isVisible()

        kickTicketDeviceAsStaff()

        page.navigate("$baseUrl/captive")
        assertThat(page.locator(".captive-card-hero .alert.alert-danger")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-kicked-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    private fun createTicketAsStaffAndGetCode(): String =
        withIsolatedPage(RenderMode.DESKTOP) { isolatedPage ->
            loginAsStaff(isolatedPage)
            openTicketsFromAccountMenu(isolatedPage)
            createTicketAndReturnCode(isolatedPage)
        }

    private fun loginAsStaffForScreenshots() {
        loginAdmin(page, username = "user", password = "user")
    }

    private fun loginAsAdminForScreenshots() {
        loginAdmin(page, username = "admin", password = "admin")
    }

    private fun openTicketsPage() {
        page.navigate("$baseUrl/admin")
        assertThat(page.locator("#ticket-panel")).isVisible()
    }

    private fun openAllowedMacPage() {
        page.navigate("$baseUrl/admin/allowed-mac")
        assertThat(page.locator("form:has(#allowed-mac-input)")).isVisible()
    }

    private fun openUserDeviceManagementPage() {
        page.navigate("$baseUrl/admin/devices?scope=mine")
        assertThat(page.locator("#user-device-list")).isVisible()
    }

    private fun createTicketAndReturnCsrfTokenForScreenshots(requireUserNameOnLogin: Boolean = false): String {
        page.navigate("$baseUrl/admin")

        val form = page.locator("form:has(select[name='validityMinutes'])").first()
        assertThat(form).isVisible()

        val csrfToken = form.locator("input[name='_csrf']").first().inputValue()
        form.locator("select[name='validityMinutes']").selectOption("45")
        if (requireUserNameOnLogin) {
            form.locator("input[name='requireUserNameOnLogin']").check()
        } else {
            form.locator("input[name='requireUserNameOnLogin']").uncheck()
        }
        form.locator("button[type='submit']").click()

        assertThat(page.locator("#ticket-panel .ticket-card")).isVisible()
        return csrfToken
    }

    private fun createTicketAsStaffAndGetCode(requireUserNameOnLogin: Boolean): String =
        withIsolatedPage(RenderMode.DESKTOP) { isolatedPage ->
            loginAsStaff(isolatedPage)
            openTicketsFromAccountMenu(isolatedPage)
            isolatedPage.navigate("$baseUrl/admin")
            val form = isolatedPage.locator("form:has(select[name='validityMinutes'])").first()
            assertThat(form).isVisible()

            form.locator("select[name='validityMinutes']").selectOption("45")
            if (requireUserNameOnLogin) {
                form.locator("input[name='requireUserNameOnLogin']").check()
            } else {
                form.locator("input[name='requireUserNameOnLogin']").uncheck()
            }
            form.locator("button[type='submit']").click()
            val ticketCard = isolatedPage.locator("#ticket-panel .ticket-card").first()
            assertThat(ticketCard).isVisible()
            val formatted = ticketCard.locator(".code-card span").first().innerText()
            formatted.replace(Regex("[^0-9A-Za-z]"), "")
        }

    private fun submitCaptiveAccessCodeInIsolatedContext(ticketCode: String) {
        withIsolatedPage(RenderMode.PHONE) { isolatedPage ->
            isolatedPage.navigate("$baseUrl/captive")
            val form = isolatedPage.locator("form:has(input[name='accessCode'])").first()
            assertThat(form).isVisible()
            form.locator("#access-code").fill(ticketCode)
            form.locator("input[name='acceptTerms']").check()
            form.locator("button[type='submit']").click()
            assertThat(isolatedPage.locator(".captive-details")).isVisible()
        }
    }

    private fun waitForTicketDeviceRow(timeoutMs: Long = 15_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            page.navigate("$baseUrl/admin")
            val deviceRow =
                page.locator("[id^='ticket-device-list-'] > div")
                    .filter(Locator.FilterOptions().setHasText(DEFAULT_DEVICE_MAC))
                    .first()
            if (deviceRow.count() > 0 && deviceRow.isVisible) {
                return
            }
            page.waitForTimeout(250.0)
        }
        throw AssertionError("Ticket device row was not visible for $DEFAULT_DEVICE_MAC")
    }

    private fun submitCaptiveAccessCodeForRequiredNameStep(
        ticketCode: String,
        language: LanguageVariant,
    ) {
        page.navigate("$baseUrl/captive?lang=${language.code}")
        val form = page.locator("form:has(input[name='accessCode'])").first()
        assertThat(form).isVisible()
        form.locator("#access-code").fill(ticketCode)
        form.locator("input[name='acceptTerms']").check()
        form.locator("button[type='submit']").click()
        assertThat(page.locator("#required-name")).isVisible()
    }

    private fun addAllowedMacEntry() {
        val macAddress = "AA:11:22:33:44:55"
        val note = CZECH_ALLOWED_MAC_NOTE

        val form = page.locator("form:has(#allowed-mac-input)").first()
        assertThat(form).isVisible()
        form.locator("#allowed-mac-input").fill(macAddress)
        form.locator("select[name='validityMinutes']").selectOption("60")
        form.locator("#allowed-mac-note").fill(note)
        form.locator("button[type='submit']").click()

        val row = page.locator("#allowed-mac-list li").filter(Locator.FilterOptions().setHasText(macAddress)).first()
        assertThat(row).isVisible()
    }

    private fun kickTicketDeviceAsStaff() {
        val ticketId = jdbcTemplate.queryForObject<UUID>("select id from admin.ticket limit 1")
            ?: error("Missing admin ticket")

        withIsolatedPage(RenderMode.DESKTOP) { isolatedPage ->
            loginAsStaff(isolatedPage)
            isolatedPage.navigate("$baseUrl/admin")
            val deadline = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < deadline) {
                val row = isolatedPage.locator("#ticket-device-list-$ticketId > div")
                    .filter(Locator.FilterOptions().setHasText(DEFAULT_DEVICE_MAC))
                    .first()
                if (row.count() > 0 && row.isVisible) {
                    val response =
                        isolatedPage.waitForResponse(
                            { candidate ->
                                candidate.request().method() == "POST" &&
                                    java.net.URI(candidate.url()).path == "/admin/ticket/$ticketId/device/_kick"
                            },
                        ) {
                            row.locator("button[hx-post*='/_kick']").click()
                            isolatedPage.locator("wm-confirm-dialog [data-role='confirm']").click()
                        }
                    check(response.status() in 200..299) { "Unexpected kick response status=${response.status()}" }
                    return@withIsolatedPage
                }
                isolatedPage.navigate("$baseUrl/admin")
                isolatedPage.waitForTimeout(250.0)
            }
            error("Ticket device row not found for kick action")
        }
    }

    private fun authorizeDeviceAsUserInIsolatedContextForScreenshots(deviceName: String) {
        withIsolatedPage(RenderMode.DESKTOP) { isolatedPage ->
            authorizeDeviceAsUserForScreenshots(isolatedPage, deviceName)
        }
    }

    private fun authorizeDeviceAsUserForScreenshots(
        targetPage: Page,
        deviceName: String,
    ) {
        openCaptiveDeviceSetupAsUser(targetPage)
        targetPage.locator("#device-name").fill(deviceName)
        targetPage.locator("button[type='submit']").click()
        targetPage.waitForURL("**/captive")
        assertThat(targetPage.locator(".captive-card-hero")).isVisible()
    }

    private fun openCaptiveDeviceSetupAsUser(targetPage: Page = page) {
        targetPage.navigate("$baseUrl/captive/login")
        val form = targetPage.locator("form:has(input[name='username']):has(input[name='password'])").first()
        assertThat(form).isVisible()
        form.locator("#username").fill("user")
        form.locator("#password").fill("user")
        form.locator("button[type='submit']").click()
        targetPage.waitForURL("**/captive/device")
    }

    private fun loginAsStaff(targetPage: Page) {
        loginAdmin(targetPage, username = "user", password = "user")
    }

    private fun loginAdmin(
        targetPage: Page,
        username: String,
        password: String,
    ) {
        targetPage.navigate("$baseUrl/admin/login")
        targetPage.locator("a[href^='/oauth2/authorization/']").first().click()

        val usernameInput = targetPage.locator("#username, input[name='username']").first()
        val passwordInput = targetPage.locator("#password, input[name='password']").first()
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            val path = java.net.URI(targetPage.url()).path
            if (path == "/admin" || path == "/admin/") {
                return
            }
            if (usernameInput.isVisible) {
                usernameInput.fill(username)
                passwordInput.fill(password)
                targetPage.locator("#kc-login, button[type='submit']").first().click()
            }
            targetPage.waitForTimeout(200.0)
        }
        error("Admin login did not reach /admin. finalUrl=${targetPage.url()}")
    }

    private fun openTicketsFromAccountMenu(targetPage: Page) {
        targetPage.navigate("$baseUrl/admin")
        assertThat(targetPage.locator("#ticket-panel")).isVisible()
    }

    private fun createTicketAndReturnCode(targetPage: Page): String {
        targetPage.navigate("$baseUrl/admin")
        val form = targetPage.locator("form:has(select[name='validityMinutes'])").first()
        assertThat(form).isVisible()

        form.locator("select[name='validityMinutes']").selectOption("45")
        form.locator("button[type='submit']").click()
        val ticketCard = targetPage.locator("#ticket-panel .ticket-card").first()
        assertThat(ticketCard).isVisible()
        val formatted = ticketCard.locator(".code-card span").first().innerText()
        return formatted.replace(Regex("[^0-9A-Za-z]"), "")
    }

    private fun submitCaptiveAccessCodeForScreenshots(
        accessCode: String,
        acceptTerms: Boolean = true,
        name: String? = null,
    ) {
        page.navigate("$baseUrl/captive")

        val form = page.locator("form:has(input[name='accessCode'])").first()
        assertThat(form).isVisible()

        form.locator("#access-code").fill(accessCode)
        val terms = form.locator("input[name='acceptTerms']")
        if (acceptTerms) {
            terms.check()
        } else {
            terms.uncheck()
        }

        form.locator("button[type='submit']").click()

        if (name != null) {
            assertThat(page.locator("#required-name")).isVisible()
            page.locator("#required-name").fill(name)
            page.locator("button[type='submit']").click()
        }
    }
}
