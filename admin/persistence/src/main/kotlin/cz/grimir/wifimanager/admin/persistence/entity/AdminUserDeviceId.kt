package cz.grimir.wifimanager.admin.persistence.entity

import java.io.Serializable
import java.util.UUID

data class AdminUserDeviceId(
    val userId: UUID = UUID.randomUUID(),
    val deviceMac: String = "",
) : Serializable
