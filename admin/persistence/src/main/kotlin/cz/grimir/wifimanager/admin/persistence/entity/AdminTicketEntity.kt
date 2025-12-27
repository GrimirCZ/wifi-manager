package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ticket", schema = "admin")
class AdminTicketEntity(
    @Id
    val id: UUID,
    @Column(name = "access_code")
    val accessCode: String,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "valid_until")
    var validUntil: Instant,
    @Column(name = "was_canceled")
    var wasCanceled: Boolean,
    @Column(name = "author_id")
    val authorId: UUID,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "kicked_mac_addresses", columnDefinition = "text[]")
    var kickedMacAddresses: Array<String>,
) {
    @OneToMany(mappedBy = "ticket")
    var authorizedDevices = mutableSetOf<AdminAuthorizedDeviceEntity>()
}
