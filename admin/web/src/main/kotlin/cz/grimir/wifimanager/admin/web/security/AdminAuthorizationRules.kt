package cz.grimir.wifimanager.admin.web.security

import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.AuthorizationRegistry
import cz.grimir.wifimanager.shared.security.ModuleAuthorizationRules
import org.springframework.stereotype.Component

@Component
class AdminAuthorizationRules : ModuleAuthorizationRules {
    override fun configure(registry: AuthorizationRegistry) {
        registry
            .requestMatchers("/admin/**")
            .hasAnyRole(UserRole.WIFI_ADMIN.name, UserRole.WIFI_STAFF.name)
    }
}
