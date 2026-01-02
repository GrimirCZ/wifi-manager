package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_identity", schema = "admin")
class AdminUserIdentityEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id")
    val userId: UUID,
    val issuer: String,
    val subject: String,
    var email: String,
    @Column(name = "display_name")
    var displayName: String,
    @Column(name = "picture_url")
    var pictureUrl: String?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", columnDefinition = "text[]")
    var roles: Array<String>,
)
