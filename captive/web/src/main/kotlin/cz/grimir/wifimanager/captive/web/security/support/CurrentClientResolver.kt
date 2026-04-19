package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.captive.application.devicefingerprint.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
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

            // TODO: maybe cache
            val clientInfo = routerAgentPort.getClientInfo(ip)
            if (clientInfo == null) {
                logger.warn { "Could not find client info for $ip" }
                error("Unable to identify client based on provided IP address")
            }
            if (clientInfo.macAddress.isBlank()) {
                logger.warn { "Mac address is blank" }
                error("Mac address is blank")
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
                macAddress = clientInfo.macAddress,
                hostname = clientInfo.hostname,
                dhcpVendorClass = clientInfo.dhcpVendorClass,
                dhcpPrlHash = clientInfo.dhcpPrlHash,
                dhcpHostname = clientInfo.dhcpHostname,
                fingerprintProfile = fingerprintProfile,
            )
        }
}
