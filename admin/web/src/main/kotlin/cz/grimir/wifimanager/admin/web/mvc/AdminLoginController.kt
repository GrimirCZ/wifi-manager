package cz.grimir.wifimanager.admin.web.mvc

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminLoginController {
    @GetMapping("/admin/login")
    fun login(): String = "admin/login"
}
