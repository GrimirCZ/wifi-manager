package cz.grimir.wifimanager.admin.web.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

/**
 * Customizes the OIDC/OAuth2 authorization request used by the admin UI.
 *
 * Currently forces `prompt=login` so clicking "Login with SSO" always shows the IdP login screen
 * (instead of silently reusing an existing SSO session).
 */
class AdminAuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val delegate = DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        delegate.resolve(request)?.withPromptLogin()

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? = delegate.resolve(request, clientRegistrationId)?.withPromptLogin()
}

/**
 * Ensures Keycloak (and most other OIDC providers) prompt for credentials even if a session already exists.
 */
private fun OAuth2AuthorizationRequest.withPromptLogin(): OAuth2AuthorizationRequest =
    OAuth2AuthorizationRequest
        .from(this)
        .additionalParameters { params ->
            params["prompt"] = "login"
        }.build()
