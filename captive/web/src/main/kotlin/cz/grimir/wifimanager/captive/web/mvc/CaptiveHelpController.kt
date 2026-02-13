package cz.grimir.wifimanager.captive.web.mvc

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class CaptiveHelpController {
    @GetMapping("/captive/help/randomized-mac")
    fun randomizedMac(): String = "captive/help-randomized-mac"
}
