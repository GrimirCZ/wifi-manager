package cz.grimir.wifimanager.admin.application.query

import cz.grimir.wifimanager.shared.core.UserId

data class FindUserDevicesByUserIdQuery(
    val userId: UserId,
)