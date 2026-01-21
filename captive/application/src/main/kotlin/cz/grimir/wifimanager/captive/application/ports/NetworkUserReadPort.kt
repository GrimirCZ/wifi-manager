package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import cz.grimir.wifimanager.shared.core.UserId

interface NetworkUserReadPort {
    fun findByUserId(userId: UserId): NetworkUser?
}
