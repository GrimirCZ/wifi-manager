package cz.grimir.wifimanager.captive.application.deviceprivacy.handler

import cz.grimir.wifimanager.captive.application.command.handler.ScrubDeauthorizedCaptiveDeviceUsecase
import cz.grimir.wifimanager.captive.application.port.CaptiveDevicePrivacyPort
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ScrubDeauthorizedCaptiveDeviceUsecaseTest {
    private val clientAccessAuthorizationResolver: ClientAccessAuthorizationResolver = mock()
    private val captiveDevicePrivacyPort: CaptiveDevicePrivacyPort = mock()

    private val usecase =
        ScrubDeauthorizedCaptiveDeviceUsecase(
            clientAccessAuthorizationResolver = clientAccessAuthorizationResolver,
            captiveDevicePrivacyPort = captiveDevicePrivacyPort,
        )

    @Test
    fun `scrubs normalized mac when privacy cleanup is eligible`() {
        val mac = "aa:bb:cc:dd:ee:01"
        given(clientAccessAuthorizationResolver.isDevicePrivacyCleanupEligible(mac)).willReturn(true)

        usecase.scrubIfEligible("AA-BB-CC-DD-EE-01")

        verify(captiveDevicePrivacyPort).scrubPiiByMac(mac)
    }

    @Test
    fun `skips scrub when authorization or pending reauth record remains`() {
        val mac = "aa:bb:cc:dd:ee:01"
        given(clientAccessAuthorizationResolver.isDevicePrivacyCleanupEligible(mac)).willReturn(false)

        usecase.scrubIfEligible(mac)

        verify(captiveDevicePrivacyPort, never()).scrubPiiByMac(mac)
    }
}
