package cz.grimir.wifimanager.captive.web.security.support

import cz.grimir.wifimanager.shared.security.mvc.ClientIdentityUnavailableException
import cz.grimir.wifimanager.shared.security.mvc.MissingClientMacException
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
class MaybeCurrentClientArgumentResolver(
    private val currentClientResolver: CurrentClientResolver,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(MaybeCurrentClient::class.java)
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

        return try {
            currentClientResolver.resolve(request)
        } catch (e: MissingClientMacException) {
            logger.trace { "Client mac is missing: $e" }
            null
        } catch (e: ClientIdentityUnavailableException) {
            logger.trace { "Current client was not found: $e" }
            null
        }
    }
}
