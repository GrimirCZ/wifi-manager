package cz.grimir.wifimanager.captive.application.query

import cz.grimir.wifimanager.shared.core.UserId

data class FindNetworkUserByUserIdQuery(
    val userId: UserId,
)
