package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_identity", schema = "captive")
class CaptiveUserIdentityEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id")
    val userId: UUID,
    val issuer: String,
    val subject: String,
    @Column(name = "display_name")
    val displayName: String,
    val email: String,
    @Column(name = "picture_url")
    val pictureUrl: String?,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", columnDefinition = "text[]")
    val roles: Array<String>,
    @Column(name = "created_at")
    val createdAt: Instant,
)
