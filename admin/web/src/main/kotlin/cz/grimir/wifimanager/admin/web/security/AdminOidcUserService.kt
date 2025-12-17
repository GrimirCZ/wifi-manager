package cz.grimir.wifimanager.admin.web.security

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.usecases.commands.UpsertUserFromLoginUsecase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AdminOidcUserService(
    private val upsertUserFromLoginUsecase: UpsertUserFromLoginUsecase,
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)

        val issuer =
            oidcUser.issuer?.toString()
                ?: userRequest.clientRegistration.providerDetails.issuerUri
                ?: userRequest.clientRegistration.registrationId

        val email =
            oidcUser.getClaimAsString("email")
                ?: error("OIDC claim 'email' is required for admin login")

        val displayName =
            oidcUser.getClaimAsString("name")
                ?: oidcUser.getClaimAsString("preferred_username")
                ?: email

        val pictureUrl = oidcUser.getClaimAsString("picture")
        val providerUsername = oidcUser.getClaimAsString("preferred_username")

        upsertUserFromLoginUsecase.upsert(
            UpsertUserFromLoginCommand(
                issuer = issuer,
                subject = oidcUser.subject,
                email = email,
                displayName = displayName,
                pictureUrl = pictureUrl,
                emailAtProvider = email,
                providerUsername = providerUsername,
                loginAt = Instant.now(),
            ),
        )

        return oidcUser
    }
}

