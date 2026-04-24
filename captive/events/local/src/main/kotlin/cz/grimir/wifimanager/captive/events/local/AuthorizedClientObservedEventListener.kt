package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AuthorizedClientObservedEventListener(
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard,
    private val deviceFingerprintService: DeviceFingerprintService,
) {
    @Async
    @EventListener
    fun on(event: AuthorizedClientObservedEvent) {
        authorizedClientFingerprintGuard.processAuthorizedClientObservation(
            mac = MacAddressNormalizer.normalize(event.macAddress),
            currentFingerprint =
                deviceFingerprintService.createRouterObservation(
                    hostname = event.hostname,
                    dhcpHostname = event.dhcpHostname,
                    dhcpVendorClass = event.dhcpVendorClass,
                    dhcpPrlHash = event.dhcpPrlHash,
                ),
        )
    }
}
