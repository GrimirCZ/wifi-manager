package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminAllowedMacEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface AdminAllowedMacJpaRepository : JpaRepository<AdminAllowedMacEntity, String> {
    fun findByMac(mac: String): AdminAllowedMacEntity?

    fun findAllByOrderByMacAsc(): List<AdminAllowedMacEntity>

    @Query(
        """
        select mac
        from AdminAllowedMacEntity mac
        where mac.validUntil is not null and mac.validUntil <= :now
        """,
    )
    fun findExpired(now: Instant): List<AdminAllowedMacEntity>
}
