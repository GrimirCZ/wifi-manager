package cz.grimir.wifimanager.admin.application.identity.query

import cz.grimir.wifimanager.shared.core.UserId

data class FindUserIdentityByUserIdQuery(
    val userId: UserId,
)
