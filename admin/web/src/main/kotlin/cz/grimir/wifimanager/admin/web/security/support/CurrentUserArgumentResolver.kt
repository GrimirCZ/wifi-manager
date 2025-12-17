package cz.grimir.wifimanager.admin.web.security.support

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.queries.FindUserByOidcIdentityQuery
import cz.grimir.wifimanager.admin.application.usecases.queries.FindUserIdentityByOidcIdentityUsecase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver(
    private val findUserUsecase: FindUserIdentityByOidcIdentityUsecase,
    private val cache: CurrentUserIdentityCache,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentUser::class.java)
        val isAppUser = UserIdentity::class.java.isAssignableFrom(parameter.parameterType)
        return hasAnnotation && isAppUser
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: error("No HttpServletRequest")

        return cache.getOrLoad(request) {
            val auth = SecurityContextHolder.getContext().authentication
                ?: error("No authentication")

            val oidc = auth.principal as? OidcUser
                ?: error("Principal is not OidcUser")

            val issuer = oidc.issuer?.toString()
                ?: oidc.idToken?.issuer?.toString()
                ?: error("OIDC issuer not available")

            val subject = oidc.subject
                ?: oidc.getClaimAsString("sub")
                ?: error("OIDC subject (sub) not available")

            findUserUsecase.find(FindUserByOidcIdentityQuery(issuer, subject)) ?: error("User not found")
        }
    }
}