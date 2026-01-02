package cz.grimir.wifimanager.shared.security.oidc

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class OidcAccessTokenDecoder {
    private val decodersByJwkSetUri = ConcurrentHashMap<String, JwtDecoder>()

    fun decode(userRequest: OidcUserRequest): Jwt? {
        val tokenValue = userRequest.accessToken.tokenValue

        val jwkSetUri = userRequest.clientRegistration.providerDetails.jwkSetUri ?: return null

        val decoder =
            decodersByJwkSetUri.computeIfAbsent(jwkSetUri) {
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
            }

        return decoder.decode(tokenValue)
    }
}
