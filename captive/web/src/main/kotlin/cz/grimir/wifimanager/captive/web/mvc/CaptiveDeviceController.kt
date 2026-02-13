package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.network.MacAddressUtils
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.captive.application.usecase.commands.AuthorizeNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.usecase.commands.DeviceOwnershipException
import cz.grimir.wifimanager.captive.application.usecase.commands.RemoveNetworkUserDeviceUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.CountNetworkUserDevicesByUserIdUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.FindNetworkUserByUserIdUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.FindNetworkUserDevicesByUserIdUsecase
import cz.grimir.wifimanager.captive.application.usecase.queries.ResolveNetworkUserLimitUsecase
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveDeviceNameForm
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.security.mvc.CurrentUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class CaptiveDeviceController(
    private val findNetworkUserDeviceByMacUsecase: FindNetworkUserDeviceByMacUsecase,
    private val findNetworkUserByUserIdUsecase: FindNetworkUserByUserIdUsecase,
    private val resolveNetworkUserLimitUsecase: ResolveNetworkUserLimitUsecase,
    private val findNetworkUserDevicesByUserIdUsecase: FindNetworkUserDevicesByUserIdUsecase,
    private val countNetworkUserDevicesByUserIdUsecase: CountNetworkUserDevicesByUserIdUsecase,
    private val authorizeNetworkUserDeviceUsecase: AuthorizeNetworkUserDeviceUsecase,
    private val removeNetworkUserDeviceUsecase: RemoveNetworkUserDeviceUsecase,
    private val routerAgentPort: RouterAgentPort,
) {
    @GetMapping("/captive/device")
    fun wizard(
        @CurrentUser
        user: UserIdentitySnapshot,
        @CurrentClient
        clientInfo: ClientInfo,
        model: Model,
    ): String {
        val device = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        if (device != null) {
            return "redirect:/captive"
        }

        val networkUser = findNetworkUserByUserIdUsecase.find(user.userId)
        if (networkUser == null) {
            model.addAttribute("noDeviceAccess", true)
            return "captive/device"
        }

        val effectiveLimit = resolveNetworkUserLimitUsecase.resolve(networkUser)
        val devices = findNetworkUserDevicesByUserIdUsecase.find(user.userId)
        val deviceCount = devices.size

        model.addAttribute("deviceLimit", effectiveLimit)
        model.addAttribute("deviceCount", deviceCount)
        model.addAttribute("devices", devices)
        model.addAttribute("noDeviceAccess", effectiveLimit <= 0)
        model.addAttribute("limitReached", effectiveLimit in 1..deviceCount)
        model.addAttribute("deviceMac", clientInfo.macAddress)
        model.addAttribute("deviceHostname", clientInfo.hostname)
        model.addAttribute("deviceRandomized", MacAddressUtils.isLocallyAdministered(clientInfo.macAddress))

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveDeviceNameForm(name = clientInfo.hostname))
        }

        return "captive/device"
    }

    @PostMapping("/captive/device/authorize")
    fun authorize(
        @CurrentUser
        user: UserIdentitySnapshot,
        @CurrentClient
        clientInfo: ClientInfo,
        @ModelAttribute("form")
        form: CaptiveDeviceNameForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        val existing = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        if (existing != null) {
            bindingResult.reject("captive.device.error.already-authorized")
            return wizard(user, clientInfo, model)
        }

        val networkUser = findNetworkUserByUserIdUsecase.find(user.userId)
        if (networkUser == null) {
            bindingResult.reject("captive.device.error.no-access")
            return wizard(user, clientInfo, model)
        }

        val effectiveLimit = resolveNetworkUserLimitUsecase.resolve(networkUser)
        val deviceCount = countNetworkUserDevicesByUserIdUsecase.count(user.userId)
        if (effectiveLimit <= 0) {
            bindingResult.reject("captive.device.error.no-access")
            return wizard(user, clientInfo, model)
        }
        if (deviceCount >= effectiveLimit) {
            bindingResult.reject("captive.device.error.limit-reached")
            return wizard(user, clientInfo, model)
        }

        val name = form.name?.trim().orEmpty()
        val resolvedName = if (name.isNotBlank()) name else clientInfo.hostname
        if (resolvedName.isNullOrBlank()) {
            bindingResult.rejectValue("name", "captive.device.error.name.required")
            return wizard(user, clientInfo, model)
        }

        try {
            authorizeNetworkUserDeviceUsecase.authorize(
                userId = user.userId,
                mac = clientInfo.macAddress,
                name = resolvedName,
                hostname = clientInfo.hostname,
                isRandomized = MacAddressUtils.isLocallyAdministered(clientInfo.macAddress),
            )
        } catch (ex: DeviceOwnershipException) {
            bindingResult.reject("captive.device.error.owned")
            return wizard(user, clientInfo, model)
        }
        routerAgentPort.allowClientAccess(listOf(clientInfo.macAddress))

        return "redirect:/captive"
    }

    @PostMapping("/captive/device/remove")
    fun remove(
        @CurrentUser
        user: UserIdentitySnapshot,
        @RequestParam("mac")
        mac: String,
    ): String {
        val device =
            findNetworkUserDevicesByUserIdUsecase.find(user.userId).firstOrNull { it.mac == mac }
                ?: return "redirect:/captive/device"

        removeNetworkUserDeviceUsecase.remove(user.userId, mac)
        routerAgentPort.revokeClientAccess(listOf(mac))

        return "redirect:/captive/device"
    }
}
