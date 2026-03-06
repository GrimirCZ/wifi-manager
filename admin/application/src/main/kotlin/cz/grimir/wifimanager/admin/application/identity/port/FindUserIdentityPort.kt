package cz.grimir.wifimanager.admin.application.identity.port

import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity

interface FindUserIdentityPort {
    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity?
}
