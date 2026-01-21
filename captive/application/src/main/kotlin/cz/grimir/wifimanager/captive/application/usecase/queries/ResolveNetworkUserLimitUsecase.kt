package cz.grimir.wifimanager.captive.application.usecase.queries

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import org.springframework.stereotype.Service

@Service
class ResolveNetworkUserLimitUsecase {
    fun resolve(user: NetworkUser): Int = user.adminOverrideLimit ?: user.allowedDeviceCount
}
