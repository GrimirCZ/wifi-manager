package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "admin_user_identity", schema = "admin")
class AdminUserIdentityEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id")
    val userId: UUID,
    val issuer: String,
    val subject: String,
    @Column(name = "email_at_provider")
    val emailAtProvider: String?,
    @Column(name = "provider_username")
    val providerUsername: String?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "last_login_at")
    val lastLoginAt: Instant,
)
