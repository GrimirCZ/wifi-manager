package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject

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

    @Test
    fun `admin can view all tickets and reveal owner name without replacing uuid`() {
        val (staffTicketOwnerId, staffTicketOwnerDisplayName) =
            withIsolatedPage { staffPage ->
                loginAdminOnPage(targetPage = staffPage, username = "user", password = "user")
                createTicketAndReturnCsrfToken(staffPage)
                val ownerId =
                    jdbcTemplate.queryForObject<String>(
                        "select author_id::text from admin.ticket order by created_at desc limit 1",
                    )
                val displayName =
                    jdbcTemplate.queryForObject<String>(
                        "select display_name from admin.user_identity where user_id = cast(? as uuid)",
                        ownerId,
                    )
                ownerId to displayName
            }

        loginAsAdmin()
        openTicketsFromAccountMenu()

        val scopeSwitch = page.locator("#ticket-scope-switch")
        assertThat(scopeSwitch).isVisible()
        assertThat(scopeSwitch.getByText("Mine")).isVisible()
        assertThat(scopeSwitch.getByText("All")).isVisible()

        scopeSwitch.getByText("All").click()
        page.waitForURL("**/admin?scope=all")

        val ticketCard =
            page
                .locator("#ticket-panel .ticket-card")
                .filter(
                    Locator
                        .FilterOptions()
                        .setHasText(staffTicketOwnerId),
                ).first()
        assertThat(ticketCard).isVisible()

        val ownerLookup =
            ticketCard
                .locator(".owner-lookup")
                .filter(
                    Locator
                        .FilterOptions()
                        .setHasText(staffTicketOwnerId),
                ).first()
        assertThat(ownerLookup).isVisible()
        assertThat(ownerLookup).containsText(staffTicketOwnerId)

        ownerLookup.hover()

        assertThat(ownerLookup.locator(".owner-lookup-popup-content")).containsText(staffTicketOwnerDisplayName)
        assertThat(ownerLookup).containsText(staffTicketOwnerId)
    }
}
