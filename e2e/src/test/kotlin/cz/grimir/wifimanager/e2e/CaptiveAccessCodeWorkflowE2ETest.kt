package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Test

class CaptiveAccessCodeWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `invalid code is rejected and valid code is accepted`() {
        loginAdmin(username = "user", password = "user")
        createTicketAndReturnCsrfToken()
        val ticketCode = extractActiveTicketCode()

        submitCaptiveAccessCode(accessCode = "111111", acceptTerms = true)
        assertThat(page.getByText("Invalid access code.")).isVisible()

        submitCaptiveAccessCode(accessCode = ticketCode, acceptTerms = true)
        assertThat(
            page.getByRole(
                com.microsoft.playwright.options.AriaRole.HEADING,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("You're connected"),
            ),
        ).isVisible()
    }
}
