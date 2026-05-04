package cz.grimir.wifimanager.admin.application.identity.port

import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity
import cz.grimir.wifimanager.shared.core.UserId

interface FindUserIdentityPort {
    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity?

    fun findByUserId(userId: UserId): UserIdentity?
}
