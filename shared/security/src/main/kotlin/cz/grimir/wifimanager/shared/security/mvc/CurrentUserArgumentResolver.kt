package cz.grimir.wifimanager.shared.security.mvc

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentUser::class.java)
        val isSnapshot = UserIdentitySnapshot::class.java.isAssignableFrom(parameter.parameterType)
        return hasAnnotation && isSnapshot
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: error("No HttpServletRequest")

        val session =
            request.getSession(false)
                ?: error("No HTTP session found for current user but one was expected")
        val sessionUser =
            session.getAttribute(SessionUserIdentity.SESSION_KEY) as? SessionUserIdentity
                ?: error("No current user snapshot in session")
        return sessionUser.toSnapshot()
    }
}
