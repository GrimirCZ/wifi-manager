package cz.grimir.wifimanager.captive.application.query

import cz.grimir.wifimanager.captive.application.query.model.NetworkUser

data class ResolveNetworkUserLimitQuery(
    val user: NetworkUser,
)
