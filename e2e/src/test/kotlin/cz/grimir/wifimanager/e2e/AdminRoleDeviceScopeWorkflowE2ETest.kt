package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject

class AdminRoleDeviceScopeWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `staff cannot access all devices scope`() {
        loginAsStaff()
        openDevicesFromAccountMenu()

        assertThat(page.getByText("Manage your authorized devices.")).isVisible()
        assertEquals(
            0,
            page
                .getByRole(
                    AriaRole.LINK,
                    com.microsoft.playwright.Page
                        .GetByRoleOptions()
                        .setName("All devices"),
                ).count(),
        )

        val forbiddenResponse = page.request().get("$baseUrl/admin/devices?scope=all")
        assertEquals(403, forbiddenResponse.status())
    }

    @Test
    fun `admin can view all devices and disconnect other users device`() {
        authorizeDeviceAsUserInIsolatedContext(deviceName = "E2E Shared Laptop")

        loginAsAdmin()
        openDevicesFromAccountMenu()
        page
            .getByRole(
                AriaRole.LINK,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("All devices"),
            ).click()
        page.waitForURL("**/admin/devices?scope=all")
        assertThat(page.getByText("Owner user:")).isVisible()

        val deviceRow =
            page
                .locator("#user-device-list li")
                .filter(
                    Locator
                        .FilterOptions()
                        .setHasText(DEFAULT_DEVICE_MAC),
                ).first()
        assertThat(deviceRow).isVisible()

        val disconnectButton =
            deviceRow.getByRole(
                AriaRole.BUTTON,
                Locator
                    .GetByRoleOptions()
                    .setName("Disconnect"),
            )
        val response =
            clickAndConfirmAndWaitForResponse(
                trigger = disconnectButton,
                expectedMethod = "POST",
                pathPredicate = { path ->
                    path.startsWith("/admin/devices/") && path.endsWith("/_deauthorize")
                },
            )
        assertTrue(response.status() in 200..299, "Unexpected disconnect status=${response.status()}")

        waitUntilDeviceNotVisible(DEFAULT_DEVICE_MAC, scope = "all")
        assertEquals(
            0,
            jdbcTemplate.queryForObject<Int>(
                "select count(*) from admin.user_device where device_mac = ?",
                DEFAULT_DEVICE_MAC,
            ),
        )
    }
}
