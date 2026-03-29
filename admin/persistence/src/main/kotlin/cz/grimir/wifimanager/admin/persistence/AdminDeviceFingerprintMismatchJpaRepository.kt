package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminDeviceFingerprintMismatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminDeviceFingerprintMismatchJpaRepository : JpaRepository<AdminDeviceFingerprintMismatchEntity, UUID>
