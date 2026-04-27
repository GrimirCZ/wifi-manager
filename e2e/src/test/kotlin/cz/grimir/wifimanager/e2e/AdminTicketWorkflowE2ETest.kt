package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject

class AdminTicketWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `staff can create and end ticket and duplicate creation is blocked`() {
        loginAsStaff()
        openTicketsFromAccountMenu()

        val csrfToken = createTicketAndReturnCsrfToken()
        val ticketCode = extractActiveTicketCode()
        assertTrue(ticketCode.isNotBlank())
        assertEquals(1, jdbcTemplate.queryForObject<Int>("select count(*) from admin.ticket"))

        submitDuplicateTicketRequest(csrfToken)
        assertEquals(1, jdbcTemplate.queryForObject<Int>("select count(*) from admin.ticket"))

        page.waitForFunction("() => window.htmx !== undefined")
        val activeTicketCard = page.locator("#ticket-panel .ticket-card").first()
        val endButton =
            activeTicketCard.getByRole(
                AriaRole.BUTTON,
                Locator
                    .GetByRoleOptions()
                    .setName("End early"),
            )
        val deleteResponse =
            clickAndConfirmAndWaitForResponse(
                trigger = endButton,
                expectedMethod = "DELETE",
                pathPredicate = { path ->
                    path.startsWith("/admin/ticket/") && path.split("/").size >= 4
                },
            )
        assertTrue(
            deleteResponse.status() in 200..299,
            "Unexpected end-ticket response status=${deleteResponse.status()} url=${deleteResponse.url()}",
        )

        val deadline = System.currentTimeMillis() + 8_000
        while (System.currentTimeMillis() < deadline) {
            val canceledCount =
                jdbcTemplate.queryForObject<Int>(
                    "select count(*) from admin.ticket where was_canceled = true",
                )
            if (canceledCount == 1) {
                return
            }
            page.waitForTimeout(200.0)
        }
        throw AssertionError("Ticket was not canceled after End early action")
    }
}
