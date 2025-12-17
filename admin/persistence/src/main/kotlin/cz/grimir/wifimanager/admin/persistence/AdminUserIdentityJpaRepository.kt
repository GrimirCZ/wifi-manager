package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminUserIdentityJpaRepository : JpaRepository<AdminUserIdentityEntity, UUID> {
    fun findByEmail(email: String): AdminUserIdentityEntity?

    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): AdminUserIdentityEntity?
}
