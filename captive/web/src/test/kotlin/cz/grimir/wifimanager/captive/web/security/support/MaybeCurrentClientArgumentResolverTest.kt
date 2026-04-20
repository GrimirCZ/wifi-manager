package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.shared.security.mvc.ClientIdentityUnavailableException
import cz.grimir.wifimanager.shared.security.mvc.MissingClientMacException
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class MaybeCurrentClientArgumentResolverTest {
    private val currentClientResolver: CurrentClientResolver = mock()
    private val argumentResolver = MaybeCurrentClientArgumentResolver(currentClientResolver)

    @Test
    fun `returns null when client identity is unavailable`() {
        val request = mock<HttpServletRequest>()
        val webRequest = mock<NativeWebRequest>()
        given(webRequest.getNativeRequest(HttpServletRequest::class.java)).willReturn(request)
        given(currentClientResolver.resolve(request)).willThrow(ClientIdentityUnavailableException("222"))

        val result = argumentResolver.resolveArgument(parameter(), null, webRequest, null)

        assertNull(result)
    }

    @Test
    fun `returns null when client mac is blank`() {
        val request = mock<HttpServletRequest>()
        val webRequest = mock<NativeWebRequest>()
        given(webRequest.getNativeRequest(HttpServletRequest::class.java)).willReturn(request)
        given(currentClientResolver.resolve(request)).willThrow(MissingClientMacException("aaa"))

        val result = argumentResolver.resolveArgument(parameter(), null, webRequest, null)

        assertNull(result)
    }

    private fun parameter(): MethodParameter =
        MethodParameter(TestController::class.java.getDeclaredMethod("optionalClient", ClientInfo::class.java), 0)

    private class TestController {
        @Suppress("unused")
        fun optionalClient(
            @MaybeCurrentClient clientInfo: ClientInfo?,
        ) {
        }
    }
}
