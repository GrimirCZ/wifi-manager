package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.query.model.NetworkUser

interface NetworkUserWritePort {
    fun save(user: NetworkUser): NetworkUser
}
