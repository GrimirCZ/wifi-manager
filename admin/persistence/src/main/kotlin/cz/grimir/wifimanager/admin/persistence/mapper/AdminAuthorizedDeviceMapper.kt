package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.admin.persistence.entity.AdminAuthorizedDeviceEntity
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.stereotype.Component

/**
 * Maps persisted authorized devices to/from the admin domain authorized device model.
 */
@Component
class AdminAuthorizedDeviceMapper {
    fun authorizedDeviceToDomain(entity: AdminAuthorizedDeviceEntity): AuthorizedDevice =
        AuthorizedDevice(
            mac = entity.mac,
            name = entity.name,
            ticketId = TicketId(entity.ticketId),
            wasAccessRevoked = entity.wasAccessRevoked,
        )

    fun authorizedDeviceToEntity(domain: AuthorizedDevice): AdminAuthorizedDeviceEntity =
        AdminAuthorizedDeviceEntity(
            mac = domain.mac,
            name = domain.name,
            ticketId = domain.ticketId.id,
            wasAccessRevoked = domain.wasAccessRevoked,
        )
}
