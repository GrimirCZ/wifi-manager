package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.captive.application.devicefingerprint.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.security.mvc.ClientIdentityUnavailableException
import cz.grimir.wifimanager.shared.security.mvc.MissingClientMacException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CurrentClientResolver(
    private val cache: CurrentClientIdentityCache,
    private val routerAgentPort: RouterAgentPort,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val fingerprintingProperties: CaptiveFingerprintingProperties,
) {
    fun resolve(request: HttpServletRequest): ClientInfo =
        cache.getOrLoad(request) {
            val ip = request.remoteAddr

            val clientInfo = routerAgentPort.getClientInfo(ip)
            if (clientInfo == null) {
                logger.warn { "Could not find client info for $ip" }
                throw ClientIdentityUnavailableException(ip)
            }
            val macAddress = MacAddressNormalizer.normalize(clientInfo.macAddress)
            if (macAddress.isBlank()) {
                logger.warn { "Router agent returned blank mac address for ip=$ip" }
                throw MissingClientMacException(ip)
            }

            val tlsFingerprint =
                deviceFingerprintService.trustedTlsFingerprint(
                    request.getHeader(fingerprintingProperties.trustedTlsHeaderName),
                )
            val fingerprintProfile =
                deviceFingerprintService.createHttpObservation(
                    routerHostname = clientInfo.hostname,
                    dhcpHostname = clientInfo.dhcpHostname,
                    dhcpVendorClass = clientInfo.dhcpVendorClass,
                    dhcpPrlHash = clientInfo.dhcpPrlHash,
                    tlsFingerprint = tlsFingerprint,
                    userAgent = request.getHeader("User-Agent"),
                )

            ClientInfo(
                ipAddress = ip,
                macAddress = macAddress,
                hostname = clientInfo.hostname,
                dhcpVendorClass = clientInfo.dhcpVendorClass,
                dhcpPrlHash = clientInfo.dhcpPrlHash,
                dhcpHostname = clientInfo.dhcpHostname,
                fingerprintProfile = fingerprintProfile,
            )
        }
}
