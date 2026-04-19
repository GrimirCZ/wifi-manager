package cz.grimir.wifimanager.shared.security.mvc

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RequestMdcInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        RequestMdc.clear()
        val sessionUser =
            request.getSession(false)
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
}
