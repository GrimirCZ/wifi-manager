package cz.grimir.wifimanager.admin.persistence.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_fingerprint_mismatch", schema = "admin")
class AdminDeviceFingerprintMismatchEntity(
    @Id
    val id: UUID,
    @Column(name = "subject_type")
    val subjectType: String,
    @Column(name = "user_id")
    val userId: UUID?,
    @Column(name = "ticket_id")
    val ticketId: UUID?,
    @Column(name = "device_mac")
    val deviceMac: String,
    val score: Int,
    val breached: Boolean,
    @Column(name = "action_taken")
    val actionTaken: String,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reasons", columnDefinition = "text[]")
    val reasons: Array<String>,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_fingerprint", columnDefinition = "jsonb")
    val previousFingerprint: JsonNode?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_fingerprint", columnDefinition = "jsonb")
    val currentFingerprint: JsonNode?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_sources", columnDefinition = "jsonb")
    val previousSources: JsonNode?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_sources", columnDefinition = "jsonb")
    val currentSources: JsonNode?,
    @Column(name = "detected_at")
    val detectedAt: Instant,
)
