package cz.grimir.wifimanager.captive.application.devicefingerprint

fun interface UserAgentClassifier {
    fun classify(userAgent: String?): String?
}
