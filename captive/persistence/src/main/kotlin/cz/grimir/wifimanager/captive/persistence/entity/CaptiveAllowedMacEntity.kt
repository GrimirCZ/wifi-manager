package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "allowed_mac", schema = "captive")
class CaptiveAllowedMacEntity(
    @Id
    @Column(name = "device_mac")
    val mac: String,
    @Column(name = "valid_until")
    val validUntil: Instant?,
) {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_mac", insertable = false, updatable = false)
    lateinit var device: CaptiveDeviceEntity
}
