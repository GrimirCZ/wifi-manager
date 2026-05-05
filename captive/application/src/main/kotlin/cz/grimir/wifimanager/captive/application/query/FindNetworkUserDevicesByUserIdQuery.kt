package cz.grimir.wifimanager.captive.application.query

import cz.grimir.wifimanager.shared.core.UserId

data class FindNetworkUserDevicesByUserIdQuery(
    val userId: UserId,
)
