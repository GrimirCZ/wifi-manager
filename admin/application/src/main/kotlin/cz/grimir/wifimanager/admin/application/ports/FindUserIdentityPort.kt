package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.application.model.UserIdentity

interface FindUserIdentityPort {
    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity?

    fun findByEmail(email: String): UserIdentity?
}
