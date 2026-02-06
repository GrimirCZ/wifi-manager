package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "allowed_mac", schema = "admin")
class AdminAllowedMacEntity(
    @Id
    @Column(name = "mac")
    val mac: String,
    @Column(name = "owner_user_id")
    val ownerUserId: UUID,
    @Column(name = "owner_display_name")
    val ownerDisplayName: String,
    @Column(name = "owner_email")
    val ownerEmail: String,
    @Column(name = "note")
    val note: String,
    @Column(name = "hostname")
    val hostname: String?,
    @Column(name = "valid_until")
    val validUntil: Instant?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    val updatedAt: Instant,
)
