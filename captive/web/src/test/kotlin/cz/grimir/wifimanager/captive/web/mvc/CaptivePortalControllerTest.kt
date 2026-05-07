package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.command.handler.AuthorizeDeviceWithCodeUsecase
import cz.grimir.wifimanager.captive.application.port.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveAccessCodeForm
import cz.grimir.wifimanager.captive.web.portal.CaptiveClientAccessState
import cz.grimir.wifimanager.captive.web.portal.CaptiveClientAccessStatus
import cz.grimir.wifimanager.captive.web.portal.CaptiveClientAccessStatusService
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
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
    private val userIdentityPort: CaptiveUserIdentityPort = mock()
    private val clientAccessStatusService: CaptiveClientAccessStatusService = mock()
    private val htmxRequest: HtmxRequest = mock()

    private val controller =
        CaptivePortalController(
            authorizeDeviceWithCodeUsecase = authorizeDeviceWithCodeUsecase,
            accessCodeFormatter = AccessCodeFormatter(),
            findAuthorizationTokenPort = findAuthorizationTokenPort,
            userIdentityPort = userIdentityPort,
            clientAccessStatusService = clientAccessStatusService,
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
    fun `index renders captive page when client identity is unavailable`() {
        val request = request()
        val model = ExtendedModelMap()

        val view = controller.index(null, request, model)

        assertEquals("captive/index", view)
        assertEquals(false, model["requireUserNameStep"])
        assertTrue(model["form"] is CaptiveAccessCodeForm)
        verifyNoInteractions(clientAccessStatusService, userIdentityPort)
    }

    @Test
    fun `index renders connected state for active account device`() {
        val request = request()
        val model = ExtendedModelMap()
        val device =
            NetworkUserDevice(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
                mac = clientInfo.macAddress,
                name = "Work Laptop",
                hostname = clientInfo.hostname,
                isRandomized = false,
                authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
                lastSeenAt = Instant.parse("2025-01-01T10:05:00Z"),
                fingerprintProfile = null,
                fingerprintStatus = DeviceFingerprintStatus.NONE,
                fingerprintVerifiedAt = null,
                reauthRequiredAt = null,
            )
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(
                CaptiveClientAccessStatus(
                    state = CaptiveClientAccessState.ACTIVE_NETWORK_USER_DEVICE,
                    networkUserDevice = device,
                ),
            )

        val view = controller.index(clientInfo, request, model)

        assertEquals("captive/index", view)
        assertEquals(true, model["accountAuthorized"])
        assertEquals(true, model["deviceLoggedIn"])
        assertEquals(false, model["deviceVerificationRequired"])
        assertEquals("Work Laptop", model["authorizedDeviceName"])
        verify(userIdentityPort).findByUserId(device.userId)
    }

    @Test
    fun `required name ticket returns htmx name step when name is missing`() {
        val form = CaptiveAccessCodeForm(accessCode = "abc-def", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        val token = token(requireUserNameOnLogin = true)
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token)

        val view = controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        assertEquals(true, model["requireUserNameStep"])
        assertFalse(bindingResult.hasFieldErrors("name"))
        verifyNoInteractions(authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `required name ticket returns full page name step without htmx when name is missing`() {
        val form = CaptiveAccessCodeForm(accessCode = "abc-def", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        val token = token(requireUserNameOnLogin = true)
        given(htmxRequest.isHtmxRequest).willReturn(false)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token)

        val view = controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        assertEquals("captive/index", view)
        assertEquals(true, model["requireUserNameStep"])
        assertFalse(bindingResult.hasFieldErrors("name"))
        verifyNoInteractions(authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `captive template includes no js access code and scanner hooks`() {
        val template =
            requireNotNull(javaClass.classLoader.getResource("templates/captive/index.html"))
                .readText()

        assertTrue(template.contains("""<html lang="en" class="no-js""""))
        assertTrue(template.contains("""pattern="[A-Za-z0-9]{3}-?[A-Za-z0-9]{3}""""))
        assertTrue(template.contains("""maxlength="7""""))
        assertTrue(template.contains("""class="captive-scanner-section require-js""""))
    }

    @Test
    fun `name shorter than three characters is rejected after trim`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEF", name = "  ab  ", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token(requireUserNameOnLogin = true))

        val view = controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        assertEquals(true, model["requireUserNameStep"])
        assertTrue(bindingResult.hasFieldErrors("name"))
        assertEquals("captive.error.name.min-length", bindingResult.getFieldError("name")?.code)
        verifyNoInteractions(authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `access code with invalid format is rejected before lookup`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABC-DE", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))

        val view = controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        assertEquals("ABC-DE", form.accessCode)
        assertTrue(bindingResult.hasFieldErrors("accessCode"))
        assertEquals("captive.error.code.format", bindingResult.getFieldError("accessCode")?.code)
        verifyNoInteractions(findAuthorizationTokenPort, authorizeDeviceWithCodeUsecase)
    }

    @Test
    fun `dashed access code is normalized for lookup`() {
        val form = CaptiveAccessCodeForm(accessCode = "abc-def", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token(requireUserNameOnLogin = false))

        controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        verify(findAuthorizationTokenPort).findByAccessCode("ABCDEF")
        verify(authorizeDeviceWithCodeUsecase).authorize(org.mockito.kotlin.any())
        assertEquals("ABC-DEF", form.accessCode)
    }

    @Test
    fun `valid three character name authorizes required name ticket`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEF", name = "  abc  ", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token(requireUserNameOnLogin = true))

        val view = controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        assertEquals("captive/index :: captiveContent", view)
        val commandCaptor = argumentCaptor<AuthorizeDeviceWithCodeCommand>()
        verify(authorizeDeviceWithCodeUsecase).authorize(commandCaptor.capture())
        assertEquals("ABCDEF", commandCaptor.firstValue.accessCode)
        assertEquals("abc", commandCaptor.firstValue.device.displayName)
        assertEquals("guest-phone", commandCaptor.firstValue.device.deviceName)
        assertEquals(false, model["requireUserNameStep"])
        assertNull((model["form"] as CaptiveAccessCodeForm).name)
    }

    @Test
    fun `unflagged ticket authorizes immediately without requiring a name`() {
        val form = CaptiveAccessCodeForm(accessCode = "ABCDEF", acceptTerms = true)
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        given(htmxRequest.isHtmxRequest).willReturn(true)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(CaptiveClientAccessStatus(CaptiveClientAccessState.UNAUTHORIZED))
        given(findAuthorizationTokenPort.findByAccessCode("ABCDEF")).willReturn(token(requireUserNameOnLogin = false))

        controller.submit(clientInfo, form, bindingResult, request(), model, htmxRequest)

        val commandCaptor = argumentCaptor<AuthorizeDeviceWithCodeCommand>()
        verify(authorizeDeviceWithCodeUsecase).authorize(commandCaptor.capture())
        assertNull(commandCaptor.firstValue.device.displayName)
        assertEquals("guest-phone", commandCaptor.firstValue.device.deviceName)
    }

    @Test
    fun `account fingerprint reauth redirects to login and stores session marker`() {
        val request = mock<HttpServletRequest>()
        val session = mock<HttpSession>()
        val model = ExtendedModelMap()
        given(request.session).willReturn(session)
        given(clientAccessStatusService.resolve(clientInfo))
            .willReturn(
                CaptiveClientAccessStatus(
                    state = CaptiveClientAccessState.REAUTH_REQUIRED_NETWORK_USER_DEVICE,
                    networkUserDevice =
                        NetworkUserDevice(
                            userId =
                                UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
                            mac = clientInfo.macAddress,
                            name = "Work Laptop",
                            hostname = clientInfo.hostname,
                            isRandomized = false,
                            authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
                            lastSeenAt = Instant.parse("2025-01-01T10:05:00Z"),
                            fingerprintProfile = null,
                            fingerprintStatus = DeviceFingerprintStatus.NONE,
                            fingerprintVerifiedAt = null,
                            reauthRequiredAt = Instant.parse("2025-01-01T10:06:00Z"),
                        ),
                ),
            )

        val view = controller.index(clientInfo, request, model)

        assertEquals("redirect:/captive/login", view)
        verify(session).setAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY, true)
    }

    private fun token(requireUserNameOnLogin: Boolean): AuthorizationToken =
        AuthorizationToken(
            id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000300")),
            accessCode = "ABCDEF",
            validUntil = Instant.parse("2025-01-01T10:00:00Z"),
            requireUserNameOnLogin = requireUserNameOnLogin,
            authorizedDevices = mutableListOf(),
            kickedMacAddresses = mutableSetOf(),
        )

    private fun request(): HttpServletRequest {
        val request = mock<HttpServletRequest>()
        val session = mock<HttpSession>()
        given(request.session).willReturn(session)
        return request
    }
}
