package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.query.ResolveNetworkUserLimitQuery
import org.springframework.stereotype.Service

@Service
class ResolveNetworkUserLimitUsecase {
    fun resolve(query: ResolveNetworkUserLimitQuery): Int = query.user.adminOverrideLimit ?: query.user.allowedDeviceCount
}
