package cz.grimir.wifimanager.captive.application.networkuser.port

import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser

interface NetworkUserWritePort {
    fun save(user: NetworkUser): NetworkUser
}
