package cz.grimir.wifimanager.captive.persistence.entity

import com.fasterxml.jackson.databind.JsonNode
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fingerprint_profile", columnDefinition = "jsonb")
    val fingerprintProfile: JsonNode?,
    @Enumerated(EnumType.STRING)
    @Column(name = "fingerprint_status")
    val fingerprintStatus: DeviceFingerprintStatus,
    @Column(name = "fingerprint_verified_at")
    val fingerprintVerifiedAt: Instant?,
    @Column(name = "reauth_required_at")
    val reauthRequiredAt: Instant?,
)
