package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminRoleTicketWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `admin can hold multiple active tickets`() {
        loginAsAdmin()
        openTicketsFromAccountMenu()

        createTicketAndReturnCsrfToken(validityMinutes = "45")
        assertThat(page.getByText("Active ticket")).isVisible()

        createTicketAndReturnCsrfToken(validityMinutes = "10080")

        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (page.locator("#ticket-panel .ticket-card").count() == 2) {
                assertEquals(2, page.getByText("Access code:").count())
                return
            }
            page.waitForTimeout(200.0)
        }

        throw AssertionError("Expected 2 active ticket cards in UI, found ${page.locator("#ticket-panel .ticket-card").count()}")
    }
}
