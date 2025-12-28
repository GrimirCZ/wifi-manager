package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity
import cz.grimir.wifimanager.admin.persistence.projection.AdminTicketWithDeviceCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface AdminTicketJpaRepository : JpaRepository<AdminTicketEntity, UUID> {
    fun findByAccessCode(accessCode: String): AdminTicketEntity?

    fun findByAuthorId(authorId: UUID): List<AdminTicketEntity>

    @Query(
        "SELECT NEW" +
            " cz.grimir.wifimanager.admin.persistence.projection.AdminTicketWithDeviceCountProjection(t, COUNT(ad))" +
            " FROM AdminTicketEntity t LEFT JOIN t.authorizedDevices ad" +
            " WHERE t.authorId = :authorId GROUP BY t"
    )
    fun findByAuthorIdWithDeviceCount(authorId: UUID): List<AdminTicketWithDeviceCountProjection>

    fun findAllByWasCanceledFalseAndValidUntilLessThanEqual(validUntil: Instant): List<AdminTicketEntity>
}
