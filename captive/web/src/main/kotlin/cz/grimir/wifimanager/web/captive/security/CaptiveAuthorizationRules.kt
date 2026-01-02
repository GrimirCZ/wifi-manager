package cz.grimir.wifimanager.web.captive.security

import cz.grimir.wifimanager.shared.security.AuthorizationRegistry
import cz.grimir.wifimanager.shared.security.ModuleAuthorizationRules
import org.springframework.stereotype.Component

@Component
class CaptiveAuthorizationRules : ModuleAuthorizationRules {
    override fun configure(registry: AuthorizationRegistry) {
        registry
            .requestMatchers("/captive")
            .permitAll()
        registry
            .requestMatchers("/captive/device")
            .permitAll()
    }
}
