package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "admin_user", schema = "admin")
class AdminUserEntity(
    @Id
    val id: UUID,
    val email: String,
    @Column(name = "display_name")
    val displayName: String,
    @Column(name = "picture_url")
    val pictureUrl: String?,
    @Column(name = "is_active")
    val isActive: Boolean,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    val updatedAt: Instant,
    @Column(name = "last_login_at")
    val lastLoginAt: Instant,
)
