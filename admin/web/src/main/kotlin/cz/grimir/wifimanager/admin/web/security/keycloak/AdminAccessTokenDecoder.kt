package cz.grimir.wifimanager.admin.web.security.keycloak

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class AdminAccessTokenDecoder {
    private val decodersByJwkSetUri = ConcurrentHashMap<String, JwtDecoder>()

    fun decode(userRequest: OidcUserRequest): Jwt? {
        val tokenValue = userRequest.accessToken.tokenValue
        if (!looksLikeJwt(tokenValue)) return null

        val jwkSetUri = userRequest.clientRegistration.providerDetails.jwkSetUri ?: return null

        val decoder =
            decodersByJwkSetUri.computeIfAbsent(jwkSetUri) {
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
            }

        return decoder.decode(tokenValue)
    }

    private fun looksLikeJwt(tokenValue: String): Boolean = tokenValue.count { it == '.' } == 2
}
