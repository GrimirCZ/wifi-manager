package cz.grimir.wifimanager.shared.security

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer

typealias AuthorizationRegistry =
    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry

fun interface ModuleAuthorizationRules {
    fun configure(registry: AuthorizationRegistry)
}
