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
            if (findUserPort.findByEmail(command.email) != null) {
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
                    emailAtProvider = command.emailAtProvider ?: command.email,
                    providerUsername = command.providerUsername,
                    createdAt = command.loginAt,
                    lastLoginAt = command.loginAt,
                ),
            )

            return savedUser.id
        }

        val user = findUserPort.findById(identity.userId)
            ?: error("User identity ${identity.id} points to missing user ${identity.userId.id}")

        user.email = command.email
        user.displayName = command.displayName
        user.pictureUrl = command.pictureUrl
        user.lastLoginAt = command.loginAt
        user.updatedAt = command.loginAt

        identity.emailAtProvider = command.emailAtProvider ?: command.email
        identity.providerUsername = command.providerUsername ?: identity.providerUsername
        identity.lastLoginAt = command.loginAt

        val savedUser = saveUserPort.save(user)
        saveUserIdentityPort.save(identity)

        return savedUser.id
    }

    private fun createUser(command: UpsertUserFromLoginCommand): User =
        User(
            id = UserId(UUID.randomUUID()),
            email = command.email,
            displayName = command.displayName,
            pictureUrl = command.pictureUrl,
            isActive = true,
            createdAt = command.loginAt,
            updatedAt = command.loginAt,
            lastLoginAt = command.loginAt,
        )
}
