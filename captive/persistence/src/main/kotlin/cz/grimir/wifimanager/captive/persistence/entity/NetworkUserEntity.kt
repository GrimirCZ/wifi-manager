package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "network_user", schema = "captive")
class NetworkUserEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID,
    @Column(name = "identity_id")
    val identityId: UUID,
    @Column(name = "allowed_device_count")
    val allowedDeviceCount: Int,
    @Column(name = "admin_override_limit")
    val adminOverrideLimit: Int?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    val updatedAt: Instant,
    @Column(name = "last_login_at")
    val lastLoginAt: Instant,
)
