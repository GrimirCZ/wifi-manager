package cz.grimir.wifimanager.captive.application.deviceprivacy.port

interface CaptiveDevicePrivacyPort {
    fun scrubPiiByMac(mac: String): Int
}
