package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.command.handler.AuthorizeDeviceWithCodeUsecase
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.application.command.handler.TouchNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.core.exceptions.InvalidAccessCodeException
import cz.grimir.wifimanager.captive.core.exceptions.KickedAddressAttemptedLoginException
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveAccessCodeForm
import cz.grimir.wifimanager.captive.web.portal.CaptiveClientAccessState
import cz.grimir.wifimanager.captive.web.portal.CaptiveClientAccessStatusService
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import cz.grimir.wifimanager.captive.web.security.support.MaybeCurrentClient
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping

private val logger = KotlinLogging.logger {}

@Controller
class CaptivePortalController(
    private val authorizeDeviceWithCodeUsecase: AuthorizeDeviceWithCodeUsecase,
    private val accessCodeFormatter: AccessCodeFormatter,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val touchNetworkUserDeviceUsecase: TouchNetworkUserDeviceUsecase,
    private val userIdentityPort: CaptiveUserIdentityPort,
    private val clientAccessStatusService: CaptiveClientAccessStatusService,
) {
    companion object {
        private const val MIN_REQUIRED_NAME_LENGTH = 3
        const val ACCOUNT_REAUTH_SESSION_KEY = "captive.accountReauthRequired"
    }

    @GetMapping("/captive", "/captive/")
    fun index(
        @MaybeCurrentClient
        clientInfo: ClientInfo?,
        request: HttpServletRequest,
        model: Model,
    ): String {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveAccessCodeForm())
        }
        model.addAttribute("requireUserNameStep", false)

        if (clientInfo != null && applyClientAuthorization(clientInfo, model, request)) {
            return "redirect:/captive/login"
        }

        return "captive/index"
    }

    @GetMapping("/captive/info")
    fun info(
        @CurrentClient
        clientInfo: ClientInfo,
    ): ResponseEntity<String> = ResponseEntity.ok(clientInfo.toString())

    @PostMapping("/captive")
    fun submit(
        @CurrentClient
        clientInfo: ClientInfo,
        @ModelAttribute("form")
        form: CaptiveAccessCodeForm,
        bindingResult: BindingResult,
        request: HttpServletRequest,
        model: Model,
        htmxRequest: HtmxRequest,
    ): String {
        logger.debug {
            "Started processing of ticket code submit for mac=${clientInfo.macAddress} code=${form.accessCode}"
        }

        val rawAccessCode = form.accessCode.orEmpty()
        val accessCode = accessCodeFormatter.normalize(rawAccessCode)
        val trimmedName = form.name?.trim().orEmpty()
        form.accessCode = accessCodeFormatter.formatForInput(rawAccessCode)
        form.name = trimmedName.ifBlank { null }

        if (accessCode.isBlank()) {
            bindingResult.rejectValue("accessCode", "captive.error.code.required")
        } else if (!accessCodeFormatter.isValid(rawAccessCode)) {
            bindingResult.rejectValue("accessCode", "captive.error.code.format")
        }
        if (!form.acceptTerms) {
            bindingResult.rejectValue("acceptTerms", "captive.error.terms.required")
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("requireUserNameStep", false)
            if (applyClientAuthorization(clientInfo, model, request)) {
                return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
            }
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        val token =
            findAuthorizationTokenPort.findByAccessCode(accessCode)
                ?: run {
                    bindingResult.reject("captive.error.code.invalid")
                    form.acceptTerms = false
                    model.addAttribute("requireUserNameStep", false)
                    if (applyClientAuthorization(clientInfo, model, request)) {
                        return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
                    }
                    return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
                }

        if (token.requireUserNameOnLogin && trimmedName.isBlank()) {
            model.addAttribute("requireUserNameStep", true)
            if (applyClientAuthorization(clientInfo, model, request)) {
                return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
            }
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        if (trimmedName.isNotBlank() && trimmedName.length < MIN_REQUIRED_NAME_LENGTH) {
            bindingResult.rejectValue("name", "captive.error.name.min-length")
            model.addAttribute("requireUserNameStep", token.requireUserNameOnLogin)
            if (applyClientAuthorization(clientInfo, model, request)) {
                return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
            }
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        try {
            authorizeDeviceWithCodeUsecase.authorize(
                AuthorizeDeviceWithCodeCommand(
                    accessCode = accessCode,
                    device =
                        Device(
                            mac = clientInfo.macAddress,
                            displayName = trimmedName.ifBlank { null },
                            deviceName = clientInfo.hostname,
                            fingerprintProfile = clientInfo.fingerprintProfile,
                        ),
                ),
            )
        } catch (_: InvalidAccessCodeException) {
            logger.debug {
                "Wrong code submitted for mac=${clientInfo.macAddress}"
            }
            bindingResult.reject("captive.error.code.invalid")
            form.acceptTerms = false
            model.addAttribute("requireUserNameStep", false)
            if (applyClientAuthorization(clientInfo, model, request)) {
                return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
            }
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        } catch (ex: KickedAddressAttemptedLoginException) {
            logger.warn(ex) { "Kicked device attempted login mac=${ex.macAddress}" }
            form.acceptTerms = false
            model.addAttribute("requireUserNameStep", false)
            model.addAttribute("deviceLoggedIn", false)
            model.addAttribute("deviceKicked", true)
            model.addAttribute(
                "usedTicketValidUntil",
                token.validUntil,
            )
            model.addAttribute("usedTicketDevice", clientInfo.hostname ?: clientInfo.macAddress)
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        model.addAttribute("form", CaptiveAccessCodeForm())
        model.addAttribute("requireUserNameStep", false)
        logger.debug {
            "Completed authorization for mac=${clientInfo.macAddress}"
        }
        if (applyClientAuthorization(clientInfo, model, request)) {
            return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive/login" else "redirect:/captive/login"
        }
        return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
    }

    @GetMapping("/captive/components")
    fun components(): String = "captive/components"

    private fun applyClientAuthorization(
        clientInfo: ClientInfo,
        model: Model,
        request: HttpServletRequest,
    ): Boolean {
        val status = clientAccessStatusService.resolve(clientInfo)
        if (status.state == CaptiveClientAccessState.REAUTH_REQUIRED_NETWORK_USER_DEVICE) {
            if (status.networkUserDevice != null) {
                request.session.setAttribute(ACCOUNT_REAUTH_SESSION_KEY, true)
                return true
            }
        }
        if (status.state == CaptiveClientAccessState.REAUTH_REQUIRED_TICKET_DEVICE) {
            applyTicketLikeModel(
                model = model,
                clientInfo = clientInfo,
                tokenValidUntil = status.token?.validUntil,
                displayName = status.ticketDevice?.displayName,
                deviceName = status.ticketDevice?.deviceName,
                deviceLoggedIn = false,
                deviceKicked = false,
                deviceVerificationRequired = true,
            )
            return false
        }

        request.session.removeAttribute(ACCOUNT_REAUTH_SESSION_KEY)
        val networkDevice = status.networkUserDevice
        if (status.state == CaptiveClientAccessState.ACTIVE_NETWORK_USER_DEVICE && networkDevice != null) {
            touchNetworkUserDeviceUsecase.touch(networkDevice.userId, networkDevice.mac)
            val identity = userIdentityPort.findByUserId(networkDevice.userId)
            model.addAttribute("accountAuthorized", true)
            model.addAttribute("deviceLoggedIn", true)
            model.addAttribute("deviceKicked", false)
            model.addAttribute("deviceVerificationRequired", false)
            model.addAttribute("authorizedByEmail", identity?.email ?: "")
            model.addAttribute("authorizedByName", identity?.displayName ?: "")
            model.addAttribute(
                "authorizedDeviceName",
                networkDevice.name ?: networkDevice.hostname ?: clientInfo.hostname ?: clientInfo.macAddress,
            )
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return false
        }

        if (status.state == CaptiveClientAccessState.ACTIVE_TICKET_DEVICE ||
            status.state == CaptiveClientAccessState.KICKED_TICKET_DEVICE
        ) {
            applyTicketLikeModel(
                model = model,
                clientInfo = clientInfo,
                tokenValidUntil = status.token?.validUntil,
                displayName = status.ticketDevice?.displayName,
                deviceName = status.ticketDevice?.deviceName,
                deviceLoggedIn = status.state == CaptiveClientAccessState.ACTIVE_TICKET_DEVICE,
                deviceKicked = status.state == CaptiveClientAccessState.KICKED_TICKET_DEVICE,
                deviceVerificationRequired = false,
            )
            return false
        }

        if (status.state == CaptiveClientAccessState.ACTIVE_ALLOWED_MAC) {
            applyTicketLikeModel(
                model = model,
                clientInfo = clientInfo,
                tokenValidUntil = status.allowedMac?.validUntil,
                displayName = null,
                deviceName = clientInfo.hostname,
                deviceLoggedIn = true,
                deviceKicked = false,
                deviceVerificationRequired = false,
            )
            return false
        }

        if (status.state == CaptiveClientAccessState.UNAUTHORIZED ||
            status.state == CaptiveClientAccessState.EXPIRED_ALLOWED_MAC
        ) {
            model.addAttribute("accountAuthorized", false)
            model.addAttribute("deviceLoggedIn", false)
            model.addAttribute("deviceKicked", false)
            model.addAttribute("deviceVerificationRequired", false)
            model.addAttribute("usedTicketValidUntil", null)
            model.addAttribute("usedTicketName", null)
            model.addAttribute("usedTicketDevice", "")
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return false
        }

        return false
    }

    private fun applyTicketLikeModel(
        model: Model,
        clientInfo: ClientInfo,
        tokenValidUntil: java.time.Instant?,
        displayName: String?,
        deviceName: String?,
        deviceLoggedIn: Boolean,
        deviceKicked: Boolean,
        deviceVerificationRequired: Boolean,
    ) {
        model.addAttribute("accountAuthorized", false)
        model.addAttribute("deviceLoggedIn", deviceLoggedIn)
        model.addAttribute("deviceKicked", deviceKicked)
        model.addAttribute("deviceVerificationRequired", deviceVerificationRequired)
        model.addAttribute("usedTicketValidUntil", tokenValidUntil)
        model.addAttribute("usedTicketName", displayName)
        model.addAttribute(
            "usedTicketDevice",
            deviceName ?: clientInfo.hostname ?: clientInfo.macAddress,
        )
        model.addAttribute("clientMacAddress", clientInfo.macAddress)
        model.addAttribute("clientIpAddress", clientInfo.ipAddress)
    }
}
