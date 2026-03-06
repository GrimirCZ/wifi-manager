package cz.grimir.wifimanager.captive.application.networkuser.port

import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserReadPort {
    fun findByUserId(userId: UserId): NetworkUser?
}
