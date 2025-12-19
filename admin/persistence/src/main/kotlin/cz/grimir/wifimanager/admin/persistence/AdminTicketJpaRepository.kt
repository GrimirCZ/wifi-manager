package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface AdminTicketJpaRepository : JpaRepository<AdminTicketEntity, UUID> {
    fun findByAccessCode(accessCode: String): AdminTicketEntity?

    fun findByAuthorId(authorId: UUID): List<AdminTicketEntity>

    fun findAllByWasCanceledFalseAndValidUntilLessThanEqual(validUntil: Instant): List<AdminTicketEntity>
}
