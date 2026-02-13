package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.DeleteUserDevicePort
import cz.grimir.wifimanager.admin.application.ports.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.ports.SaveUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.admin.persistence.mapper.AdminUserDeviceMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Repository

@Repository
class AdminJpaUserDeviceRepositoryAdapter(
    private val jpaRepository: AdminUserDeviceJpaRepository,
    private val mapper: AdminUserDeviceMapper,
) : FindUserDevicePort,
    SaveUserDevicePort,
    DeleteUserDevicePort {
    override fun findByUserId(userId: UserId): List<UserDevice> = jpaRepository.findAllByUserId(userId.id).map(mapper::toDomain)

    override fun findByUserIdAndMac(
        userId: UserId,
        mac: String,
    ): UserDevice? = jpaRepository.findByUserIdAndDeviceMac(userId.id, mac)?.let(mapper::toDomain)

    override fun findAll(): List<UserDevice> = jpaRepository.findAll().map(mapper::toDomain)

    override fun save(device: UserDevice) {
        jpaRepository.save(mapper.toEntity(device))
    }

    override fun delete(
        userId: UserId,
        mac: String,
    ) {
        jpaRepository.deleteByUserIdAndDeviceMac(userId.id, mac)
    }
}
