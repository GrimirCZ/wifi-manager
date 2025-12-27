package cz.grimir.wifimanager.captive.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "captive_authorization_token", schema = "captive")
class CaptiveAuthorizationTokenEntity(
    @Id
    val id: UUID,
    /**
     * Access code required to use this ticket.
     */
    @Column(name = "access_code")
    val accessCode: String,
    /**
     * UTC time of ticket expiration.
     */
    @Column(name = "valid_until")
    val validUntil: java.time.Instant,
    /**
     * List of devices authorized using this ticket.
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinTable(
        name = "captive_authorized_device",
        schema = "captive",
        joinColumns = [JoinColumn(name = "token_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "device_mac", referencedColumnName = "mac")],
    )
    var authorizedDevices: MutableList<CaptiveDeviceEntity>,
    /**
     * List of MAC addresses whose access was revoked by the user.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "kicked_macs", columnDefinition = "text[]")
    var kickedMacAddresses: Array<String>,
)
