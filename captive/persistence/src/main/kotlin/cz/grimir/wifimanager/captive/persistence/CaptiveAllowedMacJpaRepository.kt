package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAllowedMacEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CaptiveAllowedMacJpaRepository : JpaRepository<CaptiveAllowedMacEntity, String> {
    @Query(
        """
        select mac.mac
        from CaptiveAllowedMacEntity mac
        """,
    )
    fun findAllMacs(): List<String>
}
