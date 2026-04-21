package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command.CompleteNetworkUserDeviceReauthUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.captive.web.CaptiveLoginProperties
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveLdapLoginForm
import cz.grimir.wifimanager.captive.web.security.CaptiveAuthSessionLoginHandler
import cz.grimir.wifimanager.captive.web.security.LdapLoginResult
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import java.time.Instant
import java.util.UUID

class CaptiveLoginControllerTest {
    private val captiveAuthSessionLoginHandler: CaptiveAuthSessionLoginHandler = mock()
    private val findNetworkUserDeviceByMacUsecase: FindNetworkUserDeviceByMacUsecase = mock()
    private val completeNetworkUserDeviceReauthUsecase: CompleteNetworkUserDeviceReauthUsecase = mock()
    private val htmxRequest: HtmxRequest = mock()

    private val controller =
        CaptiveLoginController(
            captiveAuthSessionLoginHandler = captiveAuthSessionLoginHandler,
            findNetworkUserDeviceByMacUsecase = findNetworkUserDeviceByMacUsecase,
            completeNetworkUserDeviceReauthUsecase = completeNetworkUserDeviceReauthUsecase,
            captiveLoginProperties = CaptiveLoginProperties(accountNameFormat = "me@example.com"),
        )

    private val clientInfo =
        ClientInfo(
            ipAddress = "192.168.0.10",
            macAddress = "AA:BB:CC:DD:EE:FF",
            hostname = "guest-phone",
            dhcpVendorClass = null,
            dhcpPrlHash = null,
            dhcpHostname = null,
            fingerprintProfile = null,
        )

    @Test
    fun `login page renders when client identity is unavailable`() {
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(session.getAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)).willReturn(false)

        val view = controller.login(null, session, model, htmxRequest)

        assertEquals("captive/login", view)
        assertEquals(false, model["deviceVerificationRequired"])
        assertEquals("me@example.com", model["usernamePlaceholder"])
        assertTrue(model["form"] is CaptiveLdapLoginForm)
        verifyNoInteractions(findNetworkUserDeviceByMacUsecase)
    }

    @Test
    fun `login page omits username placeholder override when account name format is blank`() {
        val controller =
            CaptiveLoginController(
                captiveAuthSessionLoginHandler = captiveAuthSessionLoginHandler,
                findNetworkUserDeviceByMacUsecase = findNetworkUserDeviceByMacUsecase,
                completeNetworkUserDeviceReauthUsecase = completeNetworkUserDeviceReauthUsecase,
                captiveLoginProperties = CaptiveLoginProperties(accountNameFormat = " "),
            )
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(session.getAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)).willReturn(false)

        val view = controller.login(null, session, model, htmxRequest)

        assertEquals("captive/login", view)
        assertFalse(model.containsAttribute("usernamePlaceholder"))
    }

    @Test
    fun `login page shows verification banner when session marker is present`() {
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(session.getAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)).willReturn(true)

        val view = controller.login(clientInfo, session, model, htmxRequest)

        assertEquals("captive/login", view)
        assertEquals(true, model["deviceVerificationRequired"])
        assertTrue(model["form"] is CaptiveLdapLoginForm)
    }

    @Test
    fun `successful login clears account reauth session marker`() {
        val form = CaptiveLdapLoginForm(username = "user", password = "password")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val request = mock<HttpServletRequest>()
        val session = mock<HttpSession>()
        given(request.session).willReturn(session)
        given(htmxRequest.isHtmxRequest).willReturn(false)
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(
            captiveAuthSessionLoginHandler.login(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(request),
                org.mockito.kotlin.isNull(),
            ),
        )
            .willReturn(LdapLoginResult(success = true, userId = cz.grimir.wifimanager.shared.core.UserId(UUID.fromString("00000000-0000-0000-0000-000000000111"))))

        val view = controller.submit(clientInfo, form, bindingResult, ExtendedModelMap(), request, htmxRequest)

        assertEquals("redirect:/captive/device", view)
        assertFalse(bindingResult.hasErrors())
        verify(session).removeAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)
    }

    @Test
    fun `existing device redirects back to captive and clears marker`() {
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(existingDevice())
        given(htmxRequest.isHtmxRequest).willReturn(false)

        val view = controller.login(clientInfo, session, model, htmxRequest)

        assertEquals("redirect:/captive", view)
        verify(session).removeAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)
    }

    @Test
    fun `reauth required device stays on login page with warning banner`() {
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(existingDevice(reauthRequiredAt = Instant.parse("2025-01-01T10:06:00Z")))
        given(session.getAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)).willReturn(true)

        val view = controller.login(clientInfo, session, model, htmxRequest)

        assertEquals("captive/login", view)
        assertEquals(true, model["deviceVerificationRequired"])
    }

    @Test
    fun `successful login completes account reauth and redirects to captive`() {
        val form = CaptiveLdapLoginForm(username = "user", password = "password")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val request = mock<HttpServletRequest>()
        val session = mock<HttpSession>()
        val existingDevice = existingDevice(reauthRequiredAt = Instant.parse("2025-01-01T10:06:00Z"))
        given(request.session).willReturn(session)
        given(htmxRequest.isHtmxRequest).willReturn(false)
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(existingDevice)
        given(
            captiveAuthSessionLoginHandler.login(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(request),
                org.mockito.kotlin.any(),
            ),
        )
            .willReturn(LdapLoginResult(success = true, userId = existingDevice.userId))

        val view = controller.submit(clientInfo, form, bindingResult, ExtendedModelMap(), request, htmxRequest)

        assertEquals("redirect:/captive", view)
        verify(session).removeAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)
        verify(completeNetworkUserDeviceReauthUsecase).complete(existingDevice.userId, clientInfo.macAddress, clientInfo.fingerprintProfile)
    }

    private fun existingDevice(reauthRequiredAt: Instant? = null): NetworkUserDevice =
        NetworkUserDevice(
            userId = cz.grimir.wifimanager.shared.core.UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            mac = clientInfo.macAddress,
            name = "Work Laptop",
            hostname = clientInfo.hostname,
            isRandomized = false,
            authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
            lastSeenAt = Instant.parse("2025-01-01T10:05:00Z"),
            fingerprintProfile = null,
            fingerprintStatus = DeviceFingerprintStatus.NONE,
            fingerprintVerifiedAt = null,
            reauthRequiredAt = reauthRequiredAt,
        )
}
