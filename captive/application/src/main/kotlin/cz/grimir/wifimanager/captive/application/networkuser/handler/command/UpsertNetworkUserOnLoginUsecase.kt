package cz.grimir.wifimanager.captive.application.networkuser.handler.command

import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser
import cz.grimir.wifimanager.captive.application.networkuser.port.NetworkUserReadPort
import cz.grimir.wifimanager.captive.application.networkuser.port.NetworkUserWritePort
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpsertNetworkUserOnLoginUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
    private val networkUserWritePort: NetworkUserWritePort,
    private val timeProvider: TimeProvider,
) {
    @Transactional
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
