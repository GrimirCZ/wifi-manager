package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.authorization.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.authorization.handler.command.AuthorizeDeviceWithCodeUsecase
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.identity.port.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command.TouchNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveAccessCodeForm
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import java.time.Instant
import java.util.UUID

class CaptivePortalControllerTest {
    private val authorizeDeviceWithCodeUsecase: AuthorizeDeviceWithCodeUsecase = mock()
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort = mock()
    private val findNetworkUserDeviceByMacUsecase: FindNetworkUserDeviceByMacUsecase = mock()
    private val touchNetworkUserDeviceUsecase: TouchNetworkUserDeviceUsecase = mock()
    private val userIdentityPort: CaptiveUserIdentityPort = mock()
    private val htmxRequest: HtmxRequest = mock()

    private val controller =
        CaptivePortalController(
            authorizeDeviceWithCodeUsecase = authorizeDeviceWithCodeUsecase,
            accessCodeFormatter = AccessCodeFormatter(),
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            findNetworkUserDeviceByMacUsecase = findNetworkUserDeviceByMacUsecase,
            touchNetworkUserDeviceUsecase = touchNetworkUserDeviceUsecase,
            userIdentityPort = userIdentityPort,
        )

    private val clientInfo =
        ClientInfo(
            ipAddress = "192.168.0.10",
            macAddress = "AA:BB:CC:DD:EE:FF",
            hostname = "guest-phone",
        )

    @Test
    fun `required name ticket returns htmx name step when name is missing`() {
        val form = CaptiveAccessCodeForm(accessCode = "abc-def-gh", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        val token = token(requireUserNameOnLogin = true)
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEFGH")).willReturn(token)
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(clientInfo.macAddress)).willReturn(null)

        val view = controller.submit(clientInfo, form, bindingResult, model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        assertEquals(true, model["requireUserNameStep"])
        assertFalse(bindingResult.hasFieldErrors("name"))
        verifyNoInteractions(authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `name shorter than three characters is rejected after trim`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEFGH", name = "  ab  ", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEFGH")).willReturn(token(requireUserNameOnLogin = true))
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(clientInfo.macAddress)).willReturn(null)

        val view = controller.submit(clientInfo, form, bindingResult, model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        assertEquals(true, model["requireUserNameStep"])
        assertTrue(bindingResult.hasFieldErrors("name"))
        assertEquals("captive.error.name.min-length", bindingResult.getFieldError("name")?.code)
        verifyNoInteractions(authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `valid three character name authorizes required name ticket`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEFGH", name = "  abc  ", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEFGH")).willReturn(token(requireUserNameOnLogin = true))
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(clientInfo.macAddress)).willReturn(null)

        val view = controller.submit(clientInfo, form, bindingResult, model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        val commandCaptor = argumentCaptor<AuthorizeDeviceWithCodeCommand>()
        verify(authorizeDeviceWithCodeUsecase).authorize(commandCaptor.capture())
        assertEquals("ABCDEFGH", commandCaptor.firstValue.accessCode)
        assertEquals("abc", commandCaptor.firstValue.device.displayName)
        assertEquals("guest-phone", commandCaptor.firstValue.device.deviceName)
        assertEquals(false, model["requireUserNameStep"])
        assertNull((model["form"] as CaptiveAccessCodeForm).name)
    }

    @Test
    fun `unflagged ticket authorizes immediately without requiring a name`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEFGH", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEFGH")).willReturn(token(requireUserNameOnLogin = false))
        given(findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)).willReturn(null)
        given(findAuthorizationTokenPort.findByAuthorizedDeviceMac(clientInfo.macAddress)).willReturn(null)

        controller.submit(clientInfo, form, bindingResult, model, htmxRequest)

        val commandCaptor = argumentCaptor<AuthorizeDeviceWithCodeCommand>()
        verify(authorizeDeviceWithCodeUsecase).authorize(commandCaptor.capture())
        assertNull(commandCaptor.firstValue.device.displayName)
        assertEquals("guest-phone", commandCaptor.firstValue.device.deviceName)
    }

    private fun token(requireUserNameOnLogin: Boolean): AuthorizationToken =
        AuthorizationToken(
            id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000300")),
            accessCode = "ABCDEFGH",
            validUntil = Instant.parse("2025-01-01T10:00:00Z"),
            requireUserNameOnLogin = requireUserNameOnLogin,
            authorizedDevices = mutableListOf(),
            kickedMacAddresses = mutableSetOf(),
        )
}
