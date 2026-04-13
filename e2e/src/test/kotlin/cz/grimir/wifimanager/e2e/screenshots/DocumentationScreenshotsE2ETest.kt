package cz.grimir.wifimanager.e2e.screenshots

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import cz.grimir.wifimanager.e2e.BaseWorkflowE2ETest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject
import java.util.UUID

@Tag("screenshots")
class DocumentationScreenshotsE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `captures admin ticket creation screen`() {
        useRenderMode(RenderMode.DESKTOP)

        loginAsStaff()
        openTicketsFromAccountMenu()
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

        loginAsStaff()
        openTicketsFromAccountMenu()
        createTicketAndReturnCsrfToken()
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

        authorizeDeviceAsUserInIsolatedContext(deviceName = "Teacher Tablet")

        loginAsAdmin()
        openAllowedMacFromAccountMenu()
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
    fun `captures captive access code entry screen on phone`() {
        useRenderMode(RenderMode.PHONE)

        page.navigate("$baseUrl/captive")
        assertThat(page.getByLabel("Access code")).isVisible()

        captureLocalizedScreenshots(
            ScreenshotSpec(
                fileName = "captive-code-entry-phone.png",
                mode = RenderMode.PHONE,
                fullPage = true,
            ),
        )
    }

    @Test
    fun `captures captive connected screen on phone`() {
        useRenderMode(RenderMode.PHONE)

        val ticketCode = createTicketAsStaffAndGetCode()
        submitCaptiveAccessCode(ticketCode, acceptTerms = true)
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()

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
        submitCaptiveAccessCode(ticketCode, acceptTerms = true)
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()

        kickTicketDeviceAsStaff()

        page.navigate("$baseUrl/captive")
        assertThat(page.getByText("You were disconnected by the administrator.")).isVisible()

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

    private fun submitCaptiveAccessCodeInIsolatedContext(ticketCode: String) {
        withIsolatedPage(RenderMode.PHONE) { isolatedPage ->
            isolatedPage.navigate("$baseUrl/captive")
            val form = isolatedPage.locator("form:has(input[name='accessCode'])").first()
            assertThat(form).isVisible()
            form.getByLabel("Access code").fill(ticketCode)
            form.getByLabel("I agree to the terms and conditions.").check()
            form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Continue")).click()
            assertThat(
                isolatedPage.getByRole(
                    AriaRole.HEADING,
                    Page.GetByRoleOptions().setName("You're connected"),
                ),
            ).isVisible()
        }
    }

    private fun waitForTicketDeviceRow(timeoutMs: Long = 15_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            page.navigate("$baseUrl/admin")
            val deviceRow = page.locator("#ticket-panel .device-row").filter(Locator.FilterOptions().setHasText(DEFAULT_DEVICE_MAC)).first()
            if (deviceRow.count() > 0 && deviceRow.isVisible) {
                return
            }
            page.waitForTimeout(250.0)
        }
        throw AssertionError("Ticket device row was not visible for $DEFAULT_DEVICE_MAC")
    }

    private fun addAllowedMacEntry() {
        val macAddress = "AA:11:22:33:44:55"
        val note = "Diploma thesis screenshot"

        val form = page.locator("form:has(#allowed-mac-input)").first()
        assertThat(form).isVisible()
        form.getByLabel("MAC address").fill(macAddress)
        form.getByLabel("Duration").selectOption("60")
        form.getByLabel("Note").fill(note)
        form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Allow")).click()

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
                val row = isolatedPage.locator("#ticket-device-list-$ticketId .device-row")
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
                            row.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Disconnect")).click()
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

    private fun loginAsStaff(targetPage: Page) {
        loginAdmin(targetPage, username = "user", password = "user")
        assertAccountIdentityVisible(targetPage, "user@wifimanager.local")
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
        openAccountMenu(targetPage)
        targetPage.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Tickets")).click()
        targetPage.waitForURL("**/admin")
        assertThat(targetPage.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Create ticket"))).isVisible()
    }

    private fun createTicketAndReturnCode(targetPage: Page): String {
        targetPage.navigate("$baseUrl/admin")
        val form = targetPage.locator("form:has(select[name='validityMinutes'])").first()
        val createTicketToggler = targetPage.getByText("Create ticket").first()
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (form.isVisible) {
                break
            }
            if (createTicketToggler.isVisible) {
                createTicketToggler.click()
            }
            targetPage.waitForTimeout(200.0)
        }
        check(form.isVisible) { "Ticket form not visible. url=${targetPage.url()}" }

        form.locator("select[name='validityMinutes']").selectOption("45")
        form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Create")).click()
        val ticketCard = targetPage.locator("#ticket-panel .ticket-card").first()
        assertThat(ticketCard).isVisible()
        val formatted = ticketCard.locator(".code-card span").first().innerText()
        return formatted.replace(Regex("[^0-9A-Za-z]"), "")
    }

    private fun assertAccountIdentityVisible(targetPage: Page, email: String) {
        openAccountMenu(targetPage)
        val profileButton = targetPage.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Profile")).first()
        val accountMenu = profileButton.locator("xpath=following-sibling::*[@role='menu'][1]")
        assertThat(accountMenu.getByText(email)).isVisible()
    }

    private fun openAccountMenu(targetPage: Page) {
        val profileButton = targetPage.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Profile")).first()
        profileButton.click()
        val accountMenu = profileButton.locator("xpath=following-sibling::*[@role='menu'][1]")
        assertThat(accountMenu).isVisible()
    }
}
