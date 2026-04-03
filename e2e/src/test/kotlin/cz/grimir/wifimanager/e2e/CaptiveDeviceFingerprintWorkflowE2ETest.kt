package cz.grimir.wifimanager.e2e

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignal
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintSignalStrength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.queryForObject

class CaptiveDeviceFingerprintWorkflowE2ETest : BaseWorkflowE2ETest() {
    @Autowired
    lateinit var authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard

    @Autowired
    lateinit var deviceFingerprintService: DeviceFingerprintService

    @Test
    fun `no change keeps account device connected without audit or reauth`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")

        page.navigate("$baseUrl/captive")

        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()
        assertEquals(0, countFingerprintMismatchRows())
        assertNull(networkUserReauthRequiredAt())
    }

    @Test
    fun `alarm only mismatch keeps access active and records audit row`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")
        seedNetworkUserFingerprint(alarmOnlyAcceptedFingerprint())

        page.navigate("$baseUrl/captive")

        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()
        assertThat(page.getByText("Please sign in again")).isHidden()
        assertEquals(1, countFingerprintMismatchRows())
        assertNull(networkUserReauthRequiredAt())
    }

    @Test
    fun `http fingerprint breach redirects account device to login and sets reauth state`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")
        seedNetworkUserFingerprint(breachedAcceptedFingerprint())

        page.navigate("$baseUrl/captive")
        page.waitForURL("**/captive/login")

        assertThat(page.getByText("Please sign in again")).isVisible()
        assertThat(page.getByText("We noticed this device no longer matches its last verified sign-in")).isVisible()
        assertNotNull(networkUserReauthRequiredAt())
        assertEquals(1, countFingerprintMismatchRows())
        assertEquals(true, latestMismatchBreached())
    }

    @Test
    fun `server side deauth shows account login banner on next captive visit`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")

        authorizedClientFingerprintGuard.processAuthorizedClientObservation(DEFAULT_DEVICE_MAC, breachedCurrentFingerprint())

        page.navigate("$baseUrl/captive")
        page.waitForURL("**/captive/login")

        assertThat(page.getByText("Please sign in again")).isVisible()
        assertNotNull(networkUserReauthRequiredAt())
    }

    @Test
    fun `account fingerprint reauth login clears reauth state and returns to connected screen`() {
        loginCaptiveAndAuthorizeDevice(username = "user", password = "user", deviceName = "E2E Phone")

        authorizedClientFingerprintGuard.processAuthorizedClientObservation(DEFAULT_DEVICE_MAC, breachedCurrentFingerprint())

        page.navigate("$baseUrl/captive")
        page.waitForURL("**/captive/login")

        val form = page.locator("form").first()
        form.getByLabel("Username").fill("user")
        form.getByLabel("Password").fill("user")
        form.getByRole(AriaRole.BUTTON, com.microsoft.playwright.Locator.GetByRoleOptions().setName("Sign in")).click()
        page.waitForURL("**/captive")

        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()
        assertNull(networkUserReauthRequiredAt())
    }

    @Test
    fun `server side deauth keeps ticket devices on access code flow with warning banner`() {
        loginAsStaff()
        createTicketAndReturnCsrfToken()
        val accessCode = extractActiveTicketCode()

        submitCaptiveAccessCode(accessCode)
        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                com.microsoft.playwright.Page.GetByRoleOptions().setName("You're connected"),
            ),
        ).isVisible()

        authorizedClientFingerprintGuard.processAuthorizedClientObservation(DEFAULT_DEVICE_MAC, breachedCurrentFingerprint())

        page.navigate("$baseUrl/captive")

        assertThat(page.getByText("Please sign in again")).isVisible()
        assertThat(page.getByLabel("Access code")).isVisible()
        assertEquals("/captive", java.net.URI(page.url()).path)
    }

    private fun seedNetworkUserFingerprint(profile: DeviceFingerprintProfile) {
        jdbcTemplate.update(
            """
            update captive.network_user_device
            set fingerprint_profile = ?::jsonb,
                fingerprint_status = ?,
                fingerprint_verified_at = now(),
                reauth_required_at = null
            where device_mac = ?
            """.trimIndent(),
            deviceFingerprintService.toJson(profile),
            deviceFingerprintService.status(profile).name,
            DEFAULT_DEVICE_MAC,
        )
    }

    private fun countFingerprintMismatchRows(): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from admin.device_fingerprint_mismatch where device_mac = ?",
            Int::class.java,
            DEFAULT_DEVICE_MAC,
        ) ?: 0

    private fun latestMismatchBreached(): Boolean =
        jdbcTemplate.queryForObject(
            """
            select breached
            from admin.device_fingerprint_mismatch
            where device_mac = ?
            order by detected_at desc
            limit 1
            """.trimIndent(),
            Boolean::class.java,
            DEFAULT_DEVICE_MAC,
        ) ?: false

    private fun networkUserReauthRequiredAt(): java.time.Instant? =
        jdbcTemplate.queryForObject(
            "select reauth_required_at from captive.network_user_device where device_mac = ?",
            java.time.Instant::class.java,
            DEFAULT_DEVICE_MAC,
        )

    private fun alarmOnlyAcceptedFingerprint(): DeviceFingerprintProfile =
        DeviceFingerprintProfile(
            signals =
                linkedMapOf(
                    DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-a", DeviceFingerprintSignalStrength.STRONG),
                    DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-99", DeviceFingerprintSignalStrength.MEDIUM),
                    DeviceFingerprintService.SIGNAL_EXACT_HOSTNAME to signal("other-device.local", DeviceFingerprintSignalStrength.WEAK),
                    DeviceFingerprintService.SIGNAL_HOSTNAME_STEM to signal("other-device", DeviceFingerprintSignalStrength.WEAK),
                ),
        )

    private fun breachedAcceptedFingerprint(): DeviceFingerprintProfile =
        DeviceFingerprintProfile(
            signals =
                linkedMapOf(
                    DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-b", DeviceFingerprintSignalStrength.STRONG),
                    DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-99", DeviceFingerprintSignalStrength.MEDIUM),
                ),
        )

    private fun breachedCurrentFingerprint(): DeviceFingerprintProfile =
        DeviceFingerprintProfile(
            signals =
                linkedMapOf(
                    DeviceFingerprintService.SIGNAL_DHCP_PRL_HASH to signal("hash-b", DeviceFingerprintSignalStrength.STRONG),
                    DeviceFingerprintService.SIGNAL_DHCP_VENDOR_CLASS to signal("android-dhcp-99", DeviceFingerprintSignalStrength.MEDIUM),
                ),
        )

    private fun signal(
        value: String,
        strength: DeviceFingerprintSignalStrength,
    ) = DeviceFingerprintSignal(value = value, source = "router-agent:dhcp-log", strength = strength)
}
