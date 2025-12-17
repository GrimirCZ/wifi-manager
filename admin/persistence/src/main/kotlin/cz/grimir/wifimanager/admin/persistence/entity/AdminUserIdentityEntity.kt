package cz.grimir.wifimanager.admin.persistence.entity

import cz.grimir.wifimanager.admin.application.model.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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
    var email: String,
    var username: String,
    @Column(name = "first_name")
    var firstName: String?,
    @Column(name = "last_name")
    var lastName: String?,
    @Column(name = "picture_url")
    var pictureUrl: String?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "last_login_at")
    val lastLoginAt: Instant,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", columnDefinition = "text[]")
    var roles: Array<String>,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    lateinit var user: AdminUserEntity
}
