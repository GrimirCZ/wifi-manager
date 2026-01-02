package cz.grimir.wifimanager.shared.security.oidc

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

/**
 * Customizes the OIDC/OAuth2 authorization request used by UI modules.
 *
 * Forces `prompt=login` so clicking "Login with SSO" always shows the IdP login screen.
 */
class PromptLoginAuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val delegate =
        DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? = delegate.resolve(request)?.withPromptLogin()

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? = delegate.resolve(request, clientRegistrationId)?.withPromptLogin()
}

private fun OAuth2AuthorizationRequest.withPromptLogin(): OAuth2AuthorizationRequest =
    OAuth2AuthorizationRequest
        .from(this)
        .additionalParameters { params ->
            params["prompt"] = "login"
        }.build()
