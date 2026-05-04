package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.query.model.NetworkUser
import org.springframework.stereotype.Service

@Service
class ResolveNetworkUserLimitUsecase {
    fun resolve(user: NetworkUser): Int = user.adminOverrideLimit ?: user.allowedDeviceCount
}
