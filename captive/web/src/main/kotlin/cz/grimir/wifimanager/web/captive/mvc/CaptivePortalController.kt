package cz.grimir.wifimanager.web.captive.mvc

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class CaptivePortalController {
    @GetMapping("/captive", "/captive/")
    fun index(): String = "captive/index"

    @GetMapping("/captive/components")
    fun components(): String = "captive/components"
}
