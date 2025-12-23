package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user", schema = "admin")
class AdminUserEntity(
    @Id
    val id: UUID,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    val updatedAt: Instant,
    @Column(name = "last_login_at")
    val lastLoginAt: Instant,
) {
    @OneToMany(mappedBy = "user")
    val identities = mutableSetOf<AdminUserIdentityEntity>()
}
