package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveDeviceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CaptiveDeviceJpaRepository : JpaRepository<CaptiveDeviceEntity, String> {
    @Modifying
    @Query(
        value =
            """
            update captive.captive_device
            set display_name = null,
                device_name = null,
                fingerprint_profile = null,
                fingerprint_status = 'NONE',
                fingerprint_verified_at = null
            where mac = :mac
            """,
        nativeQuery = true,
    )
    fun scrubPiiByMac(mac: String): Int
}
