package cz.grimir.wifimanager.captive.persistence.entity

import java.io.Serializable
import java.util.UUID

data class NetworkUserDeviceId(
    val userId: UUID = UUID.randomUUID(),
    val deviceMac: String = "",
) : Serializable
