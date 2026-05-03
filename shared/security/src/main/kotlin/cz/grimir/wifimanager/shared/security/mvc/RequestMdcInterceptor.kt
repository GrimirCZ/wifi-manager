package cz.grimir.wifimanager.shared.security.mvc

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID

@Component
class RequestMdcInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        RequestMdc.clear()
        RequestMdc.putCid(request.getHeader(CID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
        RequestMdc.putRequestPath(request.requestURI)
        val sessionUser =
            request
                .getSession(false)
                ?.getAttribute(SessionUserIdentity.SESSION_KEY) as? SessionUserIdentity
        if (sessionUser != null) {
            RequestMdc.putUser(sessionUser)
        }
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        RequestMdc.clear()
    }

    companion object {
        const val CID_HEADER = "X-CID"
    }
}
