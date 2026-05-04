package cz.grimir.wifimanager.admin.application.port

import cz.grimir.wifimanager.admin.application.query.model.UserIdentity
import cz.grimir.wifimanager.shared.core.UserId

interface FindUserIdentityPort {
    fun findByUserId(userId: UserId): UserIdentity?
}
