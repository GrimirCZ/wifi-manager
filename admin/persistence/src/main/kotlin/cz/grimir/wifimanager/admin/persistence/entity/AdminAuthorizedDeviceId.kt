package cz.grimir.wifimanager.admin.persistence.entity

import java.io.Serializable
import java.util.UUID

data class AdminAuthorizedDeviceId(
    val mac: String = "",
    val ticketId: UUID = UUID.randomUUID(),
) : Serializable
