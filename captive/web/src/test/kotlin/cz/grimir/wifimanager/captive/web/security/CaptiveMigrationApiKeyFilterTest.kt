package cz.grimir.wifimanager.captive.web.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CaptiveMigrationApiKeyFilterTest {
    private val properties = CaptiveMigrationApiProperties(apiKey = "secret-key")
    private val filter = CaptiveMigrationApiKeyFilter("/captive/api/migration/import-user", properties)

    @Test
    fun `missing api key returns unauthorized`() {
        val request: HttpServletRequest = mock()
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()
        whenever(request.servletPath).thenReturn("/captive/api/migration/import-user")
        whenever(request.getHeader("X-API-Key")).thenReturn(null)

        filter.doFilter(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `wrong api key returns unauthorized`() {
        val request: HttpServletRequest = mock()
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()
        whenever(request.servletPath).thenReturn("/captive/api/migration/import-user")
        whenever(request.getHeader("X-API-Key")).thenReturn("wrong")

        filter.doFilter(request, response, chain)

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `valid api key continues filter chain`() {
        val request: HttpServletRequest = mock()
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()
        whenever(request.servletPath).thenReturn("/captive/api/migration/import-user")
        whenever(request.getHeader("X-API-Key")).thenReturn("secret-key")

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(response, never()).sendError(HttpServletResponse.SC_UNAUTHORIZED)
    }
}
