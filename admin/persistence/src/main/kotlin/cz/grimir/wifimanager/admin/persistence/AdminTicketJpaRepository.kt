package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface AdminTicketJpaRepository : JpaRepository<AdminTicketEntity, UUID> {
    fun findByAccessCode(accessCode: String): AdminTicketEntity?
}

