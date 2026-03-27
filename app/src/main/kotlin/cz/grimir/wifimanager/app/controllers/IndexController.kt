package cz.grimir.wifimanager.app.controllers

import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/")
class IndexController(
    private val securityProperties: UiSecurityProperties,
) {
    @GetMapping
    fun index(
        model: Model,
        authentication: Authentication?,
    ): String {
        if (authentication != null && authentication.isAuthenticated) {
            return "redirect:/admin"
        }
        model.addAttribute("oidcRegistrationId", securityProperties.oidcRegistrationId)
        return "admin/landing"
    }
}
