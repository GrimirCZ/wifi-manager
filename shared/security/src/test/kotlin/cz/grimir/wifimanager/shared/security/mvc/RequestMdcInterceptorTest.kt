package cz.grimir.wifimanager.shared.security.mvc

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
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
        val request = MockHttpServletRequest("GET", "/admin/users")
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

        assertEquals("/admin/users", MDC.get("requestPath"))
        assertEquals("alice@example.com", MDC.get("userEmail"))
        assertEquals("00000000-0000-0000-0000-000000000101", MDC.get("userId"))
        assertEquals(
            "email=alice@example.com userId=00000000-0000-0000-0000-000000000101",
            MDC.get("principal"),
        )
        requireNotNull(MDC.get("cid")) {
            "Expected request correlation id to be added to MDC"
        }
    }

    @Test
    fun `uses request cid header when present`() {
        val request = MockHttpServletRequest("GET", "/admin/users")
        request.addHeader("X-CID", "edge-cid-123")

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals("edge-cid-123", MDC.get("cid"))
    }

    @Test
    fun `clears mdc when request finishes`() {
        val request = MockHttpServletRequest("GET", "/captive/login")
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

        assertNull(MDC.get("userEmail"))
        assertNull(MDC.get("userId"))
        assertNull(MDC.get("principal"))
        assertNull(MDC.get("clientMac"))
        assertNull(MDC.get("requestPath"))
        assertNull(MDC.get("cid"))
    }
}
