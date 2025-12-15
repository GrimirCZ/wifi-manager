package cz.grimir.wifimanager.captive.persistence.entity

import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.annotations.Type
import java.util.UUID

@Entity
class CaptiveAuthorizationTokenEntity(
    @Id
    val id: UUID,

    /**
     * Access code required to use this ticket.
     */
    val accessCode: String,

    /**
     * List of devices authorized using this ticket.
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var authorizedDevices: MutableList<CaptiveDeviceEntity>,

    /**
     * List of MAC addresses whose access was revoked by the user.
     */
    @Type(StringArrayType::class)
    @Column(name = "kicked_macs", columnDefinition = "text[]")
    var kickedMacAddresses: MutableSet<String>
)
