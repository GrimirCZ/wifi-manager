package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.UUID

@Entity
class CaptiveDeviceEntity(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    @Id
    val mac: String,

    /**
     * Device hostname, if provided.
     */
    val name: String?,
)
