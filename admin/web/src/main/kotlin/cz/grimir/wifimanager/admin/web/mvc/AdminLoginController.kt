package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.web.security.AdminSecurityProperties
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminLoginController(
    private val securityProperties: AdminSecurityProperties,
) {
    @GetMapping("/admin/login")
    fun login(model: Model): String {
        model.addAttribute("oidcRegistrationId", securityProperties.oidcRegistrationId)
        return "admin/login"
    }
}
