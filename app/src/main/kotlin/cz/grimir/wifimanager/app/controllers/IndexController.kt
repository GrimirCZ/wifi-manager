package cz.grimir.wifimanager.app.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/")
class IndexController {
    // TODO: later decide where to redirect based on ip
    @GetMapping
    fun index(): String = "redirect:/admin"
}