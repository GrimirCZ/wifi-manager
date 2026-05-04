package cz.grimir.wifimanager.captive.persistence.mapper

import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserDeviceEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class CaptiveNetworkUserDeviceMapper(
    private val deviceFingerprintService: DeviceFingerprintService,
) {
    fun toDomain(entity: NetworkUserDeviceEntity): NetworkUserDevice =
        NetworkUserDevice(
            userId = UserId(entity.userId),
            mac = entity.deviceMac,
            name = entity.name,
            hostname = entity.hostname,
            isRandomized = entity.isRandomized,
            authorizedAt = entity.authorizedAt,
            lastSeenAt = entity.lastSeenAt,
            fingerprintProfile = deviceFingerprintService.fromJsonNode(entity.fingerprintProfile),
            fingerprintStatus = entity.fingerprintStatus,
            fingerprintVerifiedAt = entity.fingerprintVerifiedAt,
            reauthRequiredAt = entity.reauthRequiredAt,
        )

    fun toEntity(domain: NetworkUserDevice): NetworkUserDeviceEntity =
        NetworkUserDeviceEntity(
            userId = domain.userId.id,
            deviceMac = domain.mac,
            name = domain.name,
            hostname = domain.hostname,
            isRandomized = domain.isRandomized,
            authorizedAt = domain.authorizedAt,
            lastSeenAt = domain.lastSeenAt,
            fingerprintProfile = deviceFingerprintService.toJsonNode(domain.fingerprintProfile),
            fingerprintStatus = domain.fingerprintStatus,
            fingerprintVerifiedAt = domain.fingerprintVerifiedAt,
            reauthRequiredAt = domain.reauthRequiredAt,
        )
}
