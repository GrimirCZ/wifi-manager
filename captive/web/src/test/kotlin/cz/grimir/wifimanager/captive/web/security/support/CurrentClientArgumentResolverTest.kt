package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.shared.security.mvc.ClientIdentityUnavailableException
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class CurrentClientArgumentResolverTest {
    private val currentClientResolver: CurrentClientResolver = mock()
    private val argumentResolver = CurrentClientArgumentResolver(currentClientResolver)

    @Test
    fun `required current client propagates unavailable identity failure`() {
        val request = mock<HttpServletRequest>()
        val webRequest = mock<NativeWebRequest>()
        given(webRequest.getNativeRequest(HttpServletRequest::class.java)).willReturn(request)
        given(currentClientResolver.resolve(request)).willThrow(ClientIdentityUnavailableException("111"))

        assertThrows(ClientIdentityUnavailableException::class.java) {
            argumentResolver.resolveArgument(parameter(), null, webRequest, null)
        }
    }

    private fun parameter(): MethodParameter =
        MethodParameter(TestController::class.java.getDeclaredMethod("requiredClient", ClientInfo::class.java), 0)

    private class TestController {
        @Suppress("unused")
        fun requiredClient(
            @CurrentClient clientInfo: ClientInfo,
        ) {
        }
    }
}
