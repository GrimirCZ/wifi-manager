package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.query.model.NetworkUser
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserReadPort {
    fun findByUserId(userId: UserId): NetworkUser?
}
