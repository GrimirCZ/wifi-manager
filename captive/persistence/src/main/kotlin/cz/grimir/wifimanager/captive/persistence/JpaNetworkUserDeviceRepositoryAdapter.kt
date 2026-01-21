package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.network.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveDeviceEntity
import cz.grimir.wifimanager.captive.persistence.mapper.NetworkUserDeviceMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaNetworkUserDeviceRepositoryAdapter(
    private val repository: NetworkUserDeviceJpaRepository,
    private val deviceRepository: CaptiveDeviceJpaRepository,
    private val mapper: NetworkUserDeviceMapper,
) : NetworkUserDeviceReadPort,
    NetworkUserDeviceWritePort {
    override fun findByMac(mac: String): NetworkUserDevice? = repository.findByDeviceMac(mac)?.let(mapper::toDomain)

    override fun findByUserId(userId: UserId): List<NetworkUserDevice> = repository.findAllByUserId(userId.id).map(mapper::toDomain)

    override fun countByUserId(userId: UserId): Long = repository.countByUserId(userId.id)

    @Transactional
    override fun save(device: NetworkUserDevice) {
        upsertCaptiveDevice(device.mac, device.hostname)
        repository.save(mapper.toEntity(device))
    }

    override fun delete(
        userId: UserId,
        mac: String,
    ) {
        repository.deleteByUserIdAndDeviceMac(userId.id, mac)
    }

    @Transactional
    override fun touchDevice(
        userId: UserId,
        mac: String,
    ) {
        repository.touchDevice(userId.id, mac)
    }

    private fun upsertCaptiveDevice(
        mac: String,
        hostname: String?,
    ) {
        val existing = deviceRepository.findByIdOrNull(mac)
        val resolvedName = hostname ?: existing?.name
        deviceRepository.save(CaptiveDeviceEntity(mac = mac, name = resolvedName))
    }
}
