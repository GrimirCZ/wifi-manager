package cz.grimir.wifimanager.captive.application.usecase.commands

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import cz.grimir.wifimanager.captive.application.ports.NetworkUserReadPort
import cz.grimir.wifimanager.captive.application.ports.NetworkUserWritePort
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Service

@Service
class UpsertNetworkUserOnLoginUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
    private val networkUserWritePort: NetworkUserWritePort,
    private val timeProvider: TimeProvider,
) {
    fun upsert(
        identity: UserIdentitySnapshot,
        allowedDeviceCount: Int,
    ): NetworkUser {
        val loginAt = timeProvider.get()
        val existing = networkUserReadPort.findByUserId(identity.userId)
        val user =
            if (existing == null) {
                NetworkUser(
                    userId = identity.userId,
                    identityId = identity.identityId,
                    allowedDeviceCount = allowedDeviceCount,
                    adminOverrideLimit = null,
                    createdAt = loginAt,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                )
            } else {
                existing.copy(
                    allowedDeviceCount = allowedDeviceCount,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                )
            }
        return networkUserWritePort.save(user)
    }
}
