package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.DeleteAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.FindAllowedMacPort
import cz.grimir.wifimanager.admin.application.ports.SaveAllowedMacPort
import cz.grimir.wifimanager.admin.core.value.AllowedMac
import cz.grimir.wifimanager.admin.persistence.mapper.AdminAllowedMacMapper
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class AdminJpaAllowedMacRepositoryAdapter(
    private val repository: AdminAllowedMacJpaRepository,
    private val mapper: AdminAllowedMacMapper,
) : FindAllowedMacPort,
    SaveAllowedMacPort,
    DeleteAllowedMacPort {
    override fun findByMac(mac: String): AllowedMac? = repository.findByMac(mac)?.let(mapper::toDomain)

    override fun findAll(): List<AllowedMac> = repository.findAllByOrderByMacAsc().map(mapper::toDomain)

    override fun findExpired(now: Instant): List<AllowedMac> = repository.findExpired(now).map(mapper::toDomain)

    override fun save(allowedMac: AllowedMac) {
        repository.save(mapper.toEntity(allowedMac))
    }

    override fun deleteByMac(mac: String) {
        repository.deleteById(mac)
    }
}
