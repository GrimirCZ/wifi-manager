package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdminDeviceDisconnectWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `disconnect removes device from admin devices list`() {
        authorizeDeviceAsUserInIsolatedContext(deviceName = "E2E Laptop")

        loginAsStaff()
        openDevicesFromAccountMenu()

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
            deviceRow
                .getByRole(
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

        waitUntilDeviceNotVisible(DEFAULT_DEVICE_MAC)
    }
}
