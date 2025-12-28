package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminAuthorizedDeviceEntity
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AdminAuthorizedDeviceJpaRepository : JpaRepository<AdminAuthorizedDeviceEntity, UUID> {
    fun findByMacAndTicketId(
        mac: String,
        ticketId: TicketId,
    ): AdminAuthorizedDeviceEntity?

    fun findAllByTicketId(ticketId: UUID): List<AdminAuthorizedDeviceEntity>

    @Query(
        "SELECT COUNT(ad) FROM AdminAuthorizedDeviceEntity ad" +
            " WHERE ad.ticketId = :ticketId",
    )
    fun countByTicketId(ticketId: UUID): Long
}
