package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

private val logger = KotlinLogging.logger {}

@Component
class CurrentClientArgumentResolver(
    private val cache: CurrentClientIdentityCache,
    private val routerAgentPort: RouterAgentPort,
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
            val ip = request.remoteAddr

            val clientInfo = routerAgentPort.getClientInfo(ip)
            if (clientInfo == null) {
                logger.warn { "Could not find client info for $ip" }
                error("Unable to identify client based on provided IP address")
            }
            // NOTE: improve user facing error handling?

            ClientInfo(
                ipAddress = ip,
                macAddress = clientInfo.macAddress,
                hostname = clientInfo.hostname,
            )
        }
    }
}
