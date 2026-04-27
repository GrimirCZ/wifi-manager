package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `ticket without required name connects directly without intermediate name step`() {
        loginAdmin(username = "user", password = "user")
        createTicketAndReturnCsrfToken(requireUserNameOnLogin = false)
        val ticketCode = extractActiveTicketCode()

        submitCaptiveAccessCode(accessCode = ticketCode, acceptTerms = true)

        assertEquals(0, page.getByLabel("Your name").count())
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("You're connected"),
            ),
        ).isVisible()
    }

    @Test
    fun `ticket requiring name enforces minimum length and shows entered name in admin`() {
        loginAdmin(username = "user", password = "user")
        createTicketAndReturnCsrfToken(requireUserNameOnLogin = true)
        val ticketCode = extractActiveTicketCode()

        submitCaptiveAccessCode(accessCode = ticketCode, acceptTerms = true)
        assertThat(page.getByLabel("Your name")).isVisible()

        page.getByLabel("Your name").fill("ab")
        page
            .getByRole(
                AriaRole.BUTTON,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("Authorize"),
            ).click()
        assertThat(page.getByText("Please enter at least 3 characters.")).isVisible()

        page.getByLabel("Your name").fill("Alice Guest")
        page
            .getByRole(
                AriaRole.BUTTON,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("Authorize"),
            ).click()
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("You're connected"),
            ),
        ).isVisible()
        assertThat(page.getByText("Name")).isVisible()
        assertThat(page.getByText("Alice Guest")).isVisible()

        page.navigate("$baseUrl/admin")
        assertThat(page.getByText("Alice Guest")).isVisible()
    }
}
