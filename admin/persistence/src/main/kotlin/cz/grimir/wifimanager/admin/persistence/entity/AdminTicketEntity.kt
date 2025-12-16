package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "admin_ticket", schema = "admin")
class AdminTicketEntity(
    @Id
    val id: UUID,
    @Column(name = "access_code")
    val accessCode: String,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "valid_until")
    var validUntil: Instant,
    @Column(name = "was_canceled")
    var wasCanceled: Boolean,
    @Column(name = "author_id")
    val authorId: UUID,
)
