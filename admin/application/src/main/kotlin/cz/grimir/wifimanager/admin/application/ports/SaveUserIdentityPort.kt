package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.application.model.UserIdentity

interface SaveUserIdentityPort {
    fun save(identity: UserIdentity): UserIdentity
}
