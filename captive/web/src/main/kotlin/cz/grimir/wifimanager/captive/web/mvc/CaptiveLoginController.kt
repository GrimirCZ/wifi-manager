package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.usecase.queries.FindNetworkUserDeviceByMacUsecase
import cz.grimir.wifimanager.captive.application.user.UserCredentials
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveLdapLoginForm
import cz.grimir.wifimanager.captive.web.security.CaptiveAuthSessionLoginHandler
import cz.grimir.wifimanager.captive.web.security.LdapLoginFailureReason
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import jakarta.servlet.http.HttpServletRequest
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
) {
    @GetMapping("/captive/login")
    fun login(
        @CurrentClient
        clientInfo: ClientInfo,
        model: Model,
        htmxRequest: HtmxRequest,
    ): String {
        val existingDevice = findNetworkUserDeviceByMacUsecase.find(clientInfo.macAddress)
        if (existingDevice != null) {
            return if (htmxRequest.isHtmxRequest) "redirect:htmx:/captive" else "redirect:/captive"
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CaptiveLdapLoginForm())
        }
        return "captive/login"
    }

    @PostMapping("/captive/login")
    fun submit(
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

        val result =
            captiveAuthSessionLoginHandler.login(
                UserCredentials(
                    username = username,
                    password = password,
                ),
                request,
            )

        if (!result.success) {
            when (result.failureReason) {
                LdapLoginFailureReason.NO_DEVICE_ACCESS -> bindingResult.reject("captive.error.login.no-access")
                else -> bindingResult.reject("captive.error.login.invalid")
            }
            form.password = null
            return "captive/login"
        }

        return if (htmxRequest.isHtmxRequest) {
            "redirect:htmx:/captive/device"
        } else {
            "redirect:/captive/device"
        }
    }
}
