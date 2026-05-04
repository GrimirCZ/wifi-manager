package cz.grimir.wifimanager.captive.application.support.devicefingerprint

fun interface UserAgentClassifier {
    fun classify(userAgent: String?): String?
}
