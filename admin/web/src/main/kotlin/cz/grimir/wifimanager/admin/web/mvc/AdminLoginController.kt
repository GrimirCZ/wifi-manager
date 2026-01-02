package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminLoginController(
    private val securityProperties: UiSecurityProperties,
) {
    @GetMapping("/admin/login")
    fun login(model: Model): String {
        model.addAttribute("oidcRegistrationId", securityProperties.oidcRegistrationId)
        return "admin/login"
    }
}
