package cz.grimir.wifimanager.admin.application.queries

import cz.grimir.wifimanager.shared.core.UserId

data class FindUserByIdQuery(
    val id: UserId,
)
