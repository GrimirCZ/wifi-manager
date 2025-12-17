package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminUserJpaRepository : JpaRepository<AdminUserEntity, UUID> {
}
