package cz.grimir.wifimanager.web.captive.security.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentClientArgumentResolver(
    private val cache: CurrentClientIdentityCache,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentClient::class.java)
        val isClientInfo = ClientInfo::class.java.isAssignableFrom(parameter.parameterType)
        return hasAnnotation && isClientInfo
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

        return cache.getOrLoad(request) {
            // TODO: fetch from router agent

            ClientInfo(
                ipAddress = "0.0.0.0",
                macAddress = "00:00:00:00:00:00",
                hostname = "Localhost",
            )
        }
    }
}
