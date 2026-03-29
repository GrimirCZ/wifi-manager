package cz.grimir.wifimanager.captive.web.security

import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.web.security.support.CurrentClientResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class CaptiveDeviceSessionFingerprintInterceptor(
    private val currentClientResolver: CurrentClientResolver,
    private val authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val clientInfo = currentClientResolver.resolve(request)
        authorizedClientFingerprintGuard.refreshAuthenticatedClientFingerprint(clientInfo.macAddress, clientInfo.fingerprintProfile)
        return true
    }
}
