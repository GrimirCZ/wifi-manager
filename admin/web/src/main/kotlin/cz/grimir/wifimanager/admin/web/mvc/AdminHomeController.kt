package cz.grimir.wifimanager.admin.web.mvc

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminHomeController {
    @GetMapping("/admin", "/admin/")
    fun index(): String = "admin/index"
}

