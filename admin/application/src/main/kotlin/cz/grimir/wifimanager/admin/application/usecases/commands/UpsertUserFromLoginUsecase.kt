package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.UpsertUserFromLoginCommand
import cz.grimir.wifimanager.admin.application.exceptions.UserWithThisEmailAlreadyExistsForAnotherProvider
import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.ports.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.FindUserPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Creates a new local user+identity mapping on first successful login, and updates the local record on subsequent logins.
 *
 * This usecase is intentionally application-layer only; the admin core domain uses only `UserId`.
 */
@Service
class UpsertUserFromLoginUsecase(
    private val findUserIdentityPort: FindUserIdentityPort,
    private val findUserPort: FindUserPort,
    private val saveUserPort: SaveUserPort,
    private val saveUserIdentityPort: SaveUserIdentityPort,
) {
    @Transactional
    fun upsert(command: UpsertUserFromLoginCommand): UserId {
        val identity = findUserIdentityPort.findByIssuerAndSubject(command.issuer, command.subject)
        if (identity == null) {
            if (findUserIdentityPort.findByEmail(command.email) != null) {
                throw UserWithThisEmailAlreadyExistsForAnotherProvider(command.email)
            }

            val user = createUser(command)
            val savedUser = saveUserPort.save(user)

            saveUserIdentityPort.save(
                UserIdentity(
                    id = UUID.randomUUID(),
                    userId = savedUser.id,
                    issuer = command.issuer,
                    subject = command.subject,
                    email = command.email,
                    username = command.username,
                    firstName = command.firstName,
                    lastName = command.lastName,
                    pictureUrl = command.pictureUrl,
                    roles = command.roles,
                    lastLoginAt = command.loginAt,
                    createdAt = command.loginAt,
                ),
            )

            return savedUser.id
        }

        val user = findUserPort.findById(identity.userId)
            ?: error("User identity ${identity.id} points to missing user ${identity.userId.id}")

        user.lastLoginAt = command.loginAt
        user.updatedAt = command.loginAt

        identity.email = command.email
        identity.username = command.username
        identity.firstName = command.firstName
        identity.lastName = command.lastName
        identity.pictureUrl = command.pictureUrl
        identity.roles = command.roles
        identity.lastLoginAt = command.loginAt

        val savedUser = saveUserPort.save(user)
        saveUserIdentityPort.save(identity)

        return savedUser.id
    }

    private fun createUser(command: UpsertUserFromLoginCommand): User =
        User(
            id = UserId(UUID.randomUUID()),
            createdAt = command.loginAt,
            updatedAt = command.loginAt,
            lastLoginAt = command.loginAt,
        )
}
