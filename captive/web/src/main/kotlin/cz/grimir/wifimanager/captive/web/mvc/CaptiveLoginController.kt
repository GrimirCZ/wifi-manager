package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command.CompleteNetworkUserDeviceReauthUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.application.auth.model.UserCredentials
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveLdapLoginForm
import cz.grimir.wifimanager.captive.web.security.CaptiveAuthSessionLoginHandler
import cz.grimir.wifimanager.captive.web.security.LdapLoginFailureReason
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping

@Controller
class CaptiveLoginController(
    private val captiveAuthSessionLoginHandler: CaptiveAuthSessionLoginHandler,
    private val findNetworkUserDeviceByMacUsecase: FindNetworkUserDeviceByMacUsecase,
    private val completeNetworkUserDeviceReauthUsecase: CompleteNetworkUserDeviceReauthUsecase,
) {
    @GetMapping("/captive/login")
    fun login(
        @CurrentClient
        clientInfo: ClientInfo,
        session: HttpSession,
        model: Model,
        htmxRequest: HtmxRequest,
    ): String {
        val existingDevice = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        if (existingDevice != null && existingDevice.reauthRequiredAt == null) {
            session.removeAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)
            return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive" else "redirect:/captive"
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveLdapLoginForm())
        }
        model.addAttribute(
            "deviceVerificationRequired",
            session.getAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY) == true,
        )
        return "captive/login"
    }

    @PostMapping("/captive/login")
    fun submit(
        @CurrentClient
        clientInfo: ClientInfo,
        @ModelAttribute("form")
        form: CaptiveLdapLoginForm,
        bindingResult: BindingResult,
        request: HttpServletRequest,
        htmxRequest: HtmxRequest,
    ): String {
        val username = form.username?.trim().orEmpty()
        val password = form.password.orEmpty()

        if (username.isBlank()) {
            bindingResult.rejectValue("username", "captive.error.login.username.required")
        }
        if (password.isBlank()) {
            bindingResult.rejectValue("password", "captive.error.login.password.required")
        }
        if (bindingResult.hasErrors()) {
            form.password = null
            return "captive/login"
        }

        val existingDevice = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        val expectedUserId = existingDevice?.takeIf { it.reauthRequiredAt != null }?.userId
        val result =
            captiveAuthSessionLoginHandler.login(
                UserCredentials(
                    username = username,
                    password = password,
                ),
                request,
                expectedUserId = expectedUserId,
            )

        if (!result.success) {
            when (result.failureReason) {
                LdapLoginFailureReason.NO_DEVICE_ACCESS -> bindingResult.reject("captive.error.login.no-access")
                else -> bindingResult.reject("captive.error.login.invalid")
            }
            form.password = null
            return "captive/login"
        }

        request.session.removeAttribute(CaptivePortalController.ACCOUNT_REAUTH_SESSION_KEY)
        if (existingDevice?.reauthRequiredAt != null && result.userId != null) {
            completeNetworkUserDeviceReauthUsecase.complete(
                userId = result.userId,
                mac = clientInfo.macAddress,
                currentFingerprint = clientInfo.fingerprintProfile,
            )
            return if (htmxRequest.isHtmxRequest) {
                "redirect:htmx:/captive"
            } else {
                "redirect:/captive"
            }
        }

        return if (htmxRequest.isHtmxRequest) {
            "redirect:htmx:/captive/device"
        } else {
            "redirect:/captive/device"
        }
    }
}
