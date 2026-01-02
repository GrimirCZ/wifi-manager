package cz.grimir.wifimanager.web.captive.mvc

import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class CaptiveLoginController(
    private val securityProperties: UiSecurityProperties,
) {
    @GetMapping("/captive/login")
    fun login(model: Model): String {
        model.addAttribute("oidcRegistrationId", securityProperties.oidcRegistrationId)
        return "captive/login"
    }
}
