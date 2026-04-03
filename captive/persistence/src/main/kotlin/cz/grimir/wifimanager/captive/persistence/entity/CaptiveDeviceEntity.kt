package cz.grimir.wifimanager.captive.persistence.entity

import com.fasterxml.jackson.databind.JsonNode
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "captive_device", schema = "captive")
class CaptiveDeviceEntity(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    @Id
    @Column(name = "mac")
    val mac: String,
    @Column(name = "display_name")
    val displayName: String?,
    /**
     * Device hostname or another device-provided label, if provided.
     */
    @Column(name = "device_name")
    val deviceName: String?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fingerprint_profile", columnDefinition = "jsonb")
    val fingerprintProfile: JsonNode? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "fingerprint_status")
    val fingerprintStatus: DeviceFingerprintStatus = DeviceFingerprintStatus.NONE,
    @Column(name = "fingerprint_verified_at")
    val fingerprintVerifiedAt: Instant? = null,
    @Column(name = "reauth_required_at")
    val reauthRequiredAt: Instant? = null,
)
