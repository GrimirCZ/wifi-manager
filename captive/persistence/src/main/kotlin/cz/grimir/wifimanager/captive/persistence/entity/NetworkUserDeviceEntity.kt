package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "network_user_device", schema = "captive")
@IdClass(NetworkUserDeviceId::class)
class NetworkUserDeviceEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID,
    @Id
    @Column(name = "device_mac")
    val deviceMac: String,
    val name: String?,
    val hostname: String?,
    @Column(name = "is_randomized")
    val isRandomized: Boolean,
    @Column(name = "authorized_at")
    val authorizedAt: Instant,
    @Column(name = "last_seen_at")
    val lastSeenAt: Instant,
)
