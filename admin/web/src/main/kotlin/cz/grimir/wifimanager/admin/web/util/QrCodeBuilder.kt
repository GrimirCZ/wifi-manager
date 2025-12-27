package cz.grimir.wifimanager.admin.web.util

import org.springframework.stereotype.Component

@Component("QrCodeBuilder")
class QrCodeBuilder {
    fun createWifiQrCode(ssid: String): String {
        val escapedSsid =
            ssid
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
        return "WIFI:T:nopass;S:$escapedSsid;;"
    }
}
