package cz.grimir.wifimanager.user.application

import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.UserRole
import org.springframework.stereotype.Component

@Component
class RoleMapper {
    fun mapToUserRoles(input: RoleMappingInput): Set<UserRole> {
        val normalized =
            (input.roles + input.groups)
                .map { it.removePrefix("ROLE_") }
                .map { it.uppercase() }
                .toSet()

        return UserRole.entries.filter { normalized.contains(it.name) }.toSet()
    }
}
