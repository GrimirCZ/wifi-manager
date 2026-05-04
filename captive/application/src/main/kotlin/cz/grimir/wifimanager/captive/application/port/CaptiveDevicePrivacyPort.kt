package cz.grimir.wifimanager.captive.application.port

interface CaptiveDevicePrivacyPort {
    fun scrubPiiByMac(mac: String): Int
}
