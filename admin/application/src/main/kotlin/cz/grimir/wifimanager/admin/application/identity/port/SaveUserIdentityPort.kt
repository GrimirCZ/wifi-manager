package cz.grimir.wifimanager.admin.application.identity.port

import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity

interface SaveUserIdentityPort {
    fun save(identity: UserIdentity): UserIdentity
}
