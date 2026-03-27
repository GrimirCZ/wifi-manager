package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject

class AdminAllowedMacRoleWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `staff is blocked from allowed mac management`() {
        loginAsStaff()
        assertTopNavItemVisible("Admin", shouldBeVisible = false)

        val forbiddenResponse = page.request().get("$baseUrl/admin/allowed-mac")
        assertEquals(403, forbiddenResponse.status())
    }

    @Test
    fun `admin can add and remove allowed mac`() {
        val macAddress = "AA:11:22:33:44:55"
        val note = "E2E allowlist device"

        loginAsAdmin()
        assertTopNavItemVisible("Admin", shouldBeVisible = true)
        openAllowedMacFromAccountMenu()

        page.locator("#allowed-mac-form-details summary").click()
        val form = page.locator("form:has(#allowed-mac-input)")
        assertThat(form).isVisible()
        form.getByLabel("MAC address").fill(macAddress)
        form.getByLabel("Duration").selectOption("60")
        form.getByLabel("Note").fill(note)

        val allowResponse =
            page.waitForResponse(
                { candidate ->
                    candidate.request().method() == "POST" && candidate.url().contains("/admin/allowed-mac")
                },
            ) {
                form.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Allow")).click()
            }
        assertTrue(allowResponse.status() in 200..299, "Unexpected allow-mac status=${allowResponse.status()}")

        val macRow = page.locator("#allowed-mac-list li").filter(Locator.FilterOptions().setHasText(macAddress)).first()
        assertThat(macRow).isVisible()
        assertThat(macRow.getByText(note)).isVisible()
        assertEquals(1, jdbcTemplate.queryForObject<Int>("select count(*) from admin.allowed_mac where lower(mac) = lower(?)", macAddress))

        val removeButton = macRow.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Remove"))
        val deleteResponse =
            clickAndConfirmAndWaitForResponse(
                trigger = removeButton,
                expectedMethod = "DELETE",
                pathPredicate = { path -> path.startsWith("/admin/allowed-mac/") },
            )
        assertTrue(deleteResponse.status() in 200..299, "Unexpected remove-mac status=${deleteResponse.status()}")

        assertEquals(0, page.locator("#allowed-mac-list li").filter(Locator.FilterOptions().setHasText(macAddress)).count())
        assertEquals(0, jdbcTemplate.queryForObject<Int>("select count(*) from admin.allowed_mac where lower(mac) = lower(?)", macAddress))
    }
}
