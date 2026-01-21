package cz.grimir.wifimanager.web.captive.mvc

import cz.grimir.wifimanager.captive.application.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.ports.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.usecase.AuthorizeDeviceWithCodeUsecase
import cz.grimir.wifimanager.captive.application.usecase.commands.TouchNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.core.exceptions.InvalidAccessCodeException
import cz.grimir.wifimanager.captive.core.exceptions.KickedAddressAttemptedLoginException
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import cz.grimir.wifimanager.web.captive.mvc.dto.CaptiveAccessCodeForm
import cz.grimir.wifimanager.web.captive.security.support.ClientInfo
import cz.grimir.wifimanager.web.captive.security.support.CurrentClient
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
    @GetMapping("/captive", "/captive/")
    fun index(
        @CurrentClient
        clientInfo: ClientInfo,
        model: Model,
    ): String {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveAccessCodeForm())
        }
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

        if (accessCode.isBlank()) {
            bindingResult.rejectValue("accessCode", "captive.error.code.required")
        }
        if (!form.acceptTerms) {
            bindingResult.rejectValue("acceptTerms", "captive.error.terms.required")
        }

        if (bindingResult.hasErrors()) {
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
                            name = clientInfo.hostname,
                        ),
                ),
            )
        } catch (_: InvalidAccessCodeException) {
            bindingResult.reject("captive.error.code.invalid")
            form.acceptTerms = false
            applyClientAuthorization(clientInfo, model)
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        } catch (ex: KickedAddressAttemptedLoginException) {
            logger.warn(ex) { "Kicked device attempted login mac=${ex.macAddress}" }
            form.acceptTerms = false
            model.addAttribute("deviceLoggedIn", false)
            model.addAttribute("deviceKicked", true)
            model.addAttribute(
                "usedTicketValidUntil",
                findAuthorizationTokenPort.findByAccessCode(accessCode)?.validUntil,
            )
            model.addAttribute("usedTicketDevice", clientInfo.hostname ?: clientInfo.macAddress)
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return if (htmxRequest.isHtmxRequest) "captive/index :: captiveContent" else "captive/index"
        }

        model.addAttribute("form", CaptiveAccessCodeForm())
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
            model.addAttribute("usedTicketDevice", "")
            model.addAttribute("clientMacAddress", clientInfo.macAddress)
            model.addAttribute("clientIpAddress", clientInfo.ipAddress)
            return
        }

        val kicked = token.kickedMacAddresses.contains(clientInfo.macAddress)

        model.addAttribute("accountAuthorized", false)
        model.addAttribute("deviceLoggedIn", !kicked)
        model.addAttribute("deviceKicked", kicked)
        model.addAttribute("usedTicketValidUntil", token.validUntil)
        model.addAttribute("usedTicketDevice", clientInfo.hostname ?: clientInfo.macAddress)
        model.addAttribute("clientMacAddress", clientInfo.macAddress)
        model.addAttribute("clientIpAddress", clientInfo.ipAddress)
    }
}
