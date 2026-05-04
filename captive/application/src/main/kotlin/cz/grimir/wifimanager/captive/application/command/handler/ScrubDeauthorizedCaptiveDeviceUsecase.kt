package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.port.CaptiveDevicePrivacyPort
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class ScrubDeauthorizedCaptiveDeviceUsecase(
    private val clientAccessAuthorizationResolver: ClientAccessAuthorizationResolver,
    private val captiveDevicePrivacyPort: CaptiveDevicePrivacyPort,
) {
    @Transactional
    fun scrubIfEligible(macAddress: String) {
        val mac = MacAddressNormalizer.normalize(macAddress)
        if (!clientAccessAuthorizationResolver.isDevicePrivacyCleanupEligible(mac)) {
            logger.trace { "Skipping captive device PII scrub because authorization record remains mac=$mac" }
            return
        }

        val updated = captiveDevicePrivacyPort.scrubPiiByMac(mac)
        logger.info { "Scrubbed captive device PII mac=$mac updatedRows=$updated" }
    }
}
