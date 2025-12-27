package cz.grimir.wifimanager.web.captive.mvc.dto

data class CaptiveAccessCodeForm(
    var accessCode: String? = null,
    var acceptTerms: Boolean = false,
)
