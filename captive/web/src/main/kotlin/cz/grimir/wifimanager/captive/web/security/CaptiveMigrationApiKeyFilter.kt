package cz.grimir.wifimanager.captive.web.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class CaptiveMigrationApiKeyFilter(
    private val endpointPath: String,
    private val properties: CaptiveMigrationApiProperties,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.servletPath != endpointPath

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val configuredApiKey = properties.apiKey
        val receivedApiKey = request.getHeader(properties.headerName)
        if (configuredApiKey.isBlank() || receivedApiKey != configuredApiKey) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        filterChain.doFilter(request, response)
    }
}
