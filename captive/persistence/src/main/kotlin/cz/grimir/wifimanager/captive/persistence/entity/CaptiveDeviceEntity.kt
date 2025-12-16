package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "captive_device", schema = "captive")
class CaptiveDeviceEntity(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    @Id
    @Column(name = "mac")
    val mac: String,

    /**
     * Device hostname, if provided.
     */
    @Column(name = "name")
    val name: String?,
)
