package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.port.CaptiveDevicePrivacyPort
import org.springframework.stereotype.Repository

@Repository
class CaptiveJpaDevicePrivacyAdapter(
    private val deviceRepository: CaptiveDeviceJpaRepository,
) : CaptiveDevicePrivacyPort {
    override fun scrubPiiByMac(mac: String): Int = deviceRepository.scrubPiiByMac(mac)
}
