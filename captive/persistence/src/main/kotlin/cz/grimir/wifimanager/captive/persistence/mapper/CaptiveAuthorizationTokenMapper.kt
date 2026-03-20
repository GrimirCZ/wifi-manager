package cz.grimir.wifimanager.captive.persistence.mapper

import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAuthorizationTokenEntity
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveDeviceEntity
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.stereotype.Component

@Component
class CaptiveAuthorizationTokenMapper {
    fun tokenToDomain(entity: CaptiveAuthorizationTokenEntity): AuthorizationToken =
        AuthorizationToken(
            id = TicketId(entity.id),
            accessCode = entity.accessCode,
            validUntil = entity.validUntil,
            requireUserNameOnLogin = entity.requireUserNameOnLogin,
            authorizedDevices = entity.authorizedDevices.map(::deviceToDomain).toMutableList(),
            kickedMacAddresses = entity.kickedMacAddresses.toMutableSet(),
        )

    fun tokenToEntity(domain: AuthorizationToken): CaptiveAuthorizationTokenEntity =
        CaptiveAuthorizationTokenEntity(
            id = domain.id.id,
            accessCode = domain.accessCode,
            validUntil = domain.validUntil,
            requireUserNameOnLogin = domain.requireUserNameOnLogin,
            authorizedDevices = domain.authorizedDevices.map(::deviceToEntity).toMutableList(),
            kickedMacAddresses = domain.kickedMacAddresses.toTypedArray(),
        )

    fun deviceToDomain(entity: CaptiveDeviceEntity): Device =
        Device(
            mac = entity.mac,
            displayName = entity.displayName,
            deviceName = entity.deviceName,
        )

    fun deviceToEntity(domain: Device): CaptiveDeviceEntity =
        CaptiveDeviceEntity(
            mac = domain.mac,
            displayName = domain.displayName,
            deviceName = domain.deviceName,
        )
}
