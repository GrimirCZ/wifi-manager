package cz.grimir.wifimanager.captive.web.mvc.dto

data class CaptiveAccessCodeForm(
    var accessCode: String? = null,
    var acceptTerms: Boolean = false,
)
