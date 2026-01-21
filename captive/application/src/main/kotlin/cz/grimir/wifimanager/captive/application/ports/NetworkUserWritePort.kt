package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.network.NetworkUser

interface NetworkUserWritePort {
    fun save(user: NetworkUser): NetworkUser
}
