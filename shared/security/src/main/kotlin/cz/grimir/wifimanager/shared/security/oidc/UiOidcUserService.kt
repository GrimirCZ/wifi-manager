package cz.grimir.wifimanager.shared.security.oidc

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class UiOidcUserService(
    private val authoritiesProviderRegistry: OidcAuthoritiesProviderRegistry,
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)
        val authorities = authoritiesProviderRegistry.getAuthorities(oidcUser, userRequest)
        return DefaultOidcUser(authorities, oidcUser.idToken, oidcUser.userInfo, "sub")
    }
}
