package cz.grimir.wifimanager.captive.application.networkuser.handler.query

import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser
import org.springframework.stereotype.Service

@Service
class ResolveNetworkUserLimitUsecase {
    fun resolve(user: NetworkUser): Int = user.adminOverrideLimit ?: user.allowedDeviceCount
}
