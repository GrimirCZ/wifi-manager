package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.port.RouterAgentPort
import cz.grimir.wifimanager.captive.application.port.ClientInfo
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

class CurrentClientResolverTest {
    private val cache = CurrentClientIdentityCache()
    private val routerAgentPort: RouterAgentPort = mock()
    private val deviceFingerprintService: DeviceFingerprintService = mock()
    private val resolver =
        CurrentClientResolver(
            cache = cache,
            routerAgentPort = routerAgentPort,
            deviceFingerprintService = deviceFingerprintService,
            fingerprintingProperties = CaptiveFingerprintingProperties(),
        )

    @Test
    fun `normalizes router agent mac before returning client info`() {
        val request = mock<HttpServletRequest>()
        given(request.remoteAddr).willReturn("192.0.2.10")
        given(routerAgentPort.getClientInfo("192.0.2.10"))
            .willReturn(
                ClientInfo(
                    macAddress = "  AA-BB-CC-DD-EE-FF  ",
                    hostname = "phone",
                ),
            )

        val clientInfo = resolver.resolve(request)

        assertEquals("aa:bb:cc:dd:ee:ff", clientInfo.macAddress)
    }
}
