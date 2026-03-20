package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.authorization.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.identity.port.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.handler.command.AuthorizeDeviceWithCodeUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command.TouchNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.core.exceptions.InvalidAccessCodeException
import cz.grimir.wifimanager.captive.core.exceptions.KickedAddressAttemptedLoginException
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveAccessCodeForm
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
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
    private val findNetworkUserDeviceByMacUsecase: FindNetworkUserDeviceByMacUsecase,
    private val touchNetworkUserDeviceUsecase: TouchNetworkUserDeviceUsecase,
    private val userIdentityPort: CaptiveUserIdentityPort,
) {
    companion object {
        private const val MIN_REQUIRED_NAME_LENGTH = 3
    }

    @GetMapping("/captive", "/captive/")
    fun index(
        @CurrentClient
        clientInfo: ClientInfo,
        model: Model,
    ): String {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveAccessCodeForm())
        }
        model.addAttribute("requireUserNameStep", false)
        applyClientAuthorization(clientInfo, model)
        return "captive/index"
    }

    @PostMapping("/captive")
    fun submit(
        @CurrentClient
        clientInfo: ClientInfo,
        @ModelAttribute("form")
        form: CaptiveAccessCodeForm,
        bindingResult: BindingResult,
        model: Model,
        htmxRequest: HtmxRequest,
    ): String {
        val accessCode = form.accessCode?.let(accessCodeFormatter::normalize).orEmpty()
        val trimmedName = form.name?.trim().orEmpty()
        form.accessCode = accessCode
        form.name = trimmedName.ifBlank { null }

        if (accessCode.isBlank()) {
            bindingResult.rejectValue("accessCode", "captive.error.code.required")
        }
        if (!form.acceptTerms) {
            bindingResult.rejectValue("acceptTerms", "captive.error.terms.required")
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("requireUserNameStep", false)
            applyClientAuthorization(clientInfo, model)
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        val token =
            findAuthorizationTokenPort.findByAccessCode(accessCode)
                ?: run {
                    bindingResult.reject("captive.error.code.invalid")
                    form.acceptTerms = false
                    model.addAttribute("requireUserNameStep", false)
                    applyClientAuthorization(clientInfo, model)
                    return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
                }

        if (token.requireUserNameOnLogin && trimmedName.isBlank()) {
            model.addAttribute("requireUserNameStep", true)
            applyClientAuthorization(clientInfo, model)
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        if (trimmedName.isNotBlank() && trimmedName.length < MIN_REQUIRED_NAME_LENGTH) {
            bindingResult.rejectValue("name", "captive.error.name.min-length")
            model.addAttribute("requireUserNameStep", token.requireUserNameOnLogin)
            applyClientAuthorization(clientInfo, model)
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
                        ),
                ),
            )
        } catch (_: InvalidAccessCodeException) {
            bindingResult.reject("captive.error.code.invalid")
            form.acceptTerms = false
            model.addAttribute("requireUserNameStep", false)
            applyClientAuthorization(clientInfo, model)
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
        applyClientAuthorization(clientInfo, model)
        return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
    }

    @GetMapping("/captive/components")
    fun components(): String = "captive/components"

    private fun applyClientAuthorization(
        clientInfo: ClientInfo,
        model: Model,
    ) {
        val networkDevice = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        if (networkDevice != null) {
            touchNetworkUserDeviceUsecase.touch(networkDevice.userId, networkDevice.mac)
            val identity = userIdentityPort.findByUserId(networkDevice.userId)
            model.addAttribute("accountAuthorized", true)
            model.addAttribute("deviceLoggedIn", true)
            model.addAttribute("deviceKicked", false)
            model.addAttribute("authorizedByEmail", identity?.email ?: "")
            model.addAttribute("authorizedByName", identity?.displayName ?: "")
            model.addAttribute(
                "authorizedDeviceName",
                networkDevice.name ?: networkDevice.hostname ?: clientInfo.hostname ?: clientInfo.macAddress,
            )
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return
        }

        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(clientInfo.macAddress)
        if (token == null) {
            model.addAttribute("accountAuthorized", false)
            model.addAttribute("deviceLoggedIn", false)
            model.addAttribute("deviceKicked", false)
            model.addAttribute("usedTicketValidUntil", null)
            model.addAttribute("usedTicketName", null)
            model.addAttribute("usedTicketDevice", "")
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return
        }

        val kicked = token.kickedMacAddresses.contains(clientInfo.macAddress)
        val authorizedDevice = token.authorizedDevices.firstOrNull { it.mac == clientInfo.macAddress }

        model.addAttribute("accountAuthorized", false)
        model.addAttribute("deviceLoggedIn", !kicked)
        model.addAttribute("deviceKicked", kicked)
        model.addAttribute("usedTicketValidUntil", token.validUntil)
        model.addAttribute("usedTicketName", authorizedDevice?.displayName)
        model.addAttribute(
            "usedTicketDevice",
            authorizedDevice?.deviceName ?: clientInfo.hostname ?: clientInfo.macAddress,
        )
        model.addAttribute("clientMacAddress", clientInfo.macAddress)
        model.addAttribute("clientIpAddress", clientInfo.ipAddress)
    }
}
