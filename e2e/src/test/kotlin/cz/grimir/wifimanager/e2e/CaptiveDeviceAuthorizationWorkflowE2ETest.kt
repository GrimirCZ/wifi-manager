package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.queryForObject

class CaptiveDeviceAuthorizationWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `captive login and device authorization succeeds`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")

        assertThat(
            page.getByRole(
                com.microsoft.playwright.options.AriaRole.HEADING,
                com.microsoft.playwright.Page
                    .GetByRoleOptions()
                    .setName("You're connected"),
            ),
        ).isVisible()
        assertEquals(
            1,
            jdbcTemplate.queryForObject<Int>(
                "select count(*) from captive.network_user_device where device_mac = ?",
                DEFAULT_DEVICE_MAC,
            ),
        )
    }
}
