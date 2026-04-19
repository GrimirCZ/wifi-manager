package cz.grimir.wifimanager.shared.security.mvc

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class RequestMdcInterceptorTest {
    private val interceptor = RequestMdcInterceptor()

    @AfterEach
    fun tearDown() {
        RequestMdc.clear()
    }

    @Test
    fun `adds user identity fields to mdc from session`() {
        val request = MockHttpServletRequest()
        request.getSession(true)!!.setAttribute(
            SessionUserIdentity.SESSION_KEY,
            SessionUserIdentity(
                userId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000202"),
                displayName = "Alice Doe",
                email = "alice@example.com",
                pictureUrl = null,
                roles = emptySet(),
            ),
        )

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals("alice@example.com", org.slf4j.MDC.get("userEmail"))
        assertEquals("00000000-0000-0000-0000-000000000101", org.slf4j.MDC.get("userId"))
        assertEquals(
            "email=alice@example.com userId=00000000-0000-0000-0000-000000000101",
            org.slf4j.MDC.get("principal"),
        )
    }

    @Test
    fun `clears mdc when request finishes`() {
        val request = MockHttpServletRequest()
        request.getSession(true)!!.setAttribute(
            SessionUserIdentity.SESSION_KEY,
            SessionUserIdentity(
                userId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000202"),
                displayName = "Alice Doe",
                email = "alice@example.com",
                pictureUrl = null,
                roles = emptySet(),
            ),
        )

        interceptor.preHandle(request, MockHttpServletResponse(), Any())
        interceptor.afterCompletion(request, MockHttpServletResponse(), Any(), null)

        assertNull(org.slf4j.MDC.get("userEmail"))
        assertNull(org.slf4j.MDC.get("userId"))
        assertNull(org.slf4j.MDC.get("principal"))
        assertNull(org.slf4j.MDC.get("clientMac"))
    }
}
