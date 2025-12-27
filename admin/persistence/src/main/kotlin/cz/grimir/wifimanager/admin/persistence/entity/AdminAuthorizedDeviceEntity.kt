package cz.grimir.wifimanager.admin.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "authorized_device", schema = "admin")
@IdClass(AdminAuthorizedDeviceId::class)
class AdminAuthorizedDeviceEntity(
    @Id
    val mac: String,
    val name: String?,
    @Id
    @Column(name = "ticket_id")
    val ticketId: UUID,
    @Column(name = "was_access_revoked")
    val wasAccessRevoked: Boolean,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", insertable = false, updatable = false)
    lateinit var ticket: AdminTicketEntity
}
