package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.allowed.AllowedMac
import cz.grimir.wifimanager.captive.application.ports.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.ports.AllowedMacWritePort
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveDeviceEntity
import cz.grimir.wifimanager.captive.persistence.mapper.CaptiveAllowedMacMapper
import org.springframework.stereotype.Repository

@Repository
class CaptiveJpaAllowedMacRepositoryAdapter(
    private val allowedMacRepository: CaptiveAllowedMacJpaRepository,
    private val deviceRepository: CaptiveDeviceJpaRepository,
    private val mapper: CaptiveAllowedMacMapper,
) : AllowedMacReadPort,
    AllowedMacWritePort {
    override fun findAll(): List<AllowedMac> = allowedMacRepository.findAll().map(mapper::toDomain)

    override fun findAllMacs(): List<String> = allowedMacRepository.findAllMacs()

    override fun findByMac(mac: String): AllowedMac? = allowedMacRepository.findById(mac).orElse(null)?.let(mapper::toDomain)

    override fun save(allowedMac: AllowedMac) {
        if (!deviceRepository.existsById(allowedMac.mac)) {
            deviceRepository.save(CaptiveDeviceEntity(mac = allowedMac.mac, name = null))
        }
        allowedMacRepository.save(mapper.toEntity(allowedMac))
    }

    override fun deleteByMac(mac: String) {
        allowedMacRepository.deleteById(mac)
    }
}
