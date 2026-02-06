package cz.grimir.wifimanager.captive.persistence.mapper

import cz.grimir.wifimanager.captive.application.allowed.AllowedMac
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAllowedMacEntity
import org.springframework.stereotype.Component

@Component
class CaptiveAllowedMacMapper {
    fun toDomain(entity: CaptiveAllowedMacEntity): AllowedMac =
        AllowedMac(
            mac = entity.mac,
            validUntil = entity.validUntil,
        )

    fun toEntity(domain: AllowedMac): CaptiveAllowedMacEntity =
        CaptiveAllowedMacEntity(
            mac = domain.mac,
            validUntil = domain.validUntil,
        )
}
