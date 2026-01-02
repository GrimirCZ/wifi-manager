package cz.grimir.wifimanager.user.application

import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.ResolveUserResult
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import cz.grimir.wifimanager.user.application.ports.UserDirectoryRepository
import cz.grimir.wifimanager.user.application.ports.UserEventPublisher
import cz.grimir.wifimanager.user.application.ports.UserIdentityRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserDirectoryService(
    private val repository: UserDirectoryRepository,
    private val roleMapper: RoleMapper,
    private val eventPublisher: UserEventPublisher,
) {
    @Transactional
    fun resolveOrCreate(command: ResolveUserCommand): ResolveUserResult {
        val roles = roleMapper.mapToUserRoles(command.roleMapping)
        val existing = repository.findByIssuerAndSubject(command.issuer, command.subject)
        val (current, created) =
            if (existing == null) {
                val inserted =
                    repository.createIdentity(
                        identityId = UUID.randomUUID(),
                        issuer = command.issuer,
                        subject = command.subject,
                        displayName = command.profile.displayName,
                        email = command.profile.email,
                        pictureUrl = command.profile.pictureUrl,
                        roles = roles,
                        loginAt = command.loginAt,
                    )
                if (inserted != null) {
                    inserted to true
                } else {
                    loadExisting(command) to false
                }
            } else {
                existing to false
            }

        val changed = !created && hasProfileChanges(current, command, roles)
        val updated =
            if (changed) {
                repository.updateIdentity(
                    identityId = current.identityId,
                    displayName = command.profile.displayName,
                    email = command.profile.email,
                    pictureUrl = command.profile.pictureUrl,
                    roles = roles,
                    loginAt = command.loginAt,
                )
            } else if (created) {
                current
            } else {
                repository.updateLastLoginAt(current.identityId, command.loginAt)
                current.copy(lastLoginAt = command.loginAt)
            }
        val publishEvent = created || changed

        val result =
            ResolveUserResult(
                userId = updated.userId,
                identityId = updated.identityId,
                displayName = updated.displayName,
                email = updated.email,
                pictureUrl = updated.pictureUrl,
                roles = updated.roles,
            )

        if (publishEvent) {
            eventPublisher.publish(
                UserCreatedOrUpdatedEvent(
                    userId = UserId(updated.userId),
                    identityId = updated.identityId,
                    issuer = updated.issuer,
                    subject = updated.subject,
                    displayName = updated.displayName,
                    email = updated.email,
                    pictureUrl = updated.pictureUrl,
                    roles = updated.roles,
                    createdAt = updated.createdAt,
                    lastLoginAt = updated.lastLoginAt,
                ),
            )
        }

        return result
    }

    private fun loadExisting(command: ResolveUserCommand): UserIdentityRecord =
        repository.findByIssuerAndSubject(command.issuer, command.subject)
            ?: error("User identity resolve failed for issuer=${command.issuer} subject=${command.subject}")

    private fun hasProfileChanges(
        record: UserIdentityRecord,
        command: ResolveUserCommand,
        roles: Set<UserRole>,
    ): Boolean =
        record.displayName != command.profile.displayName ||
            record.email != command.profile.email ||
            record.pictureUrl != command.profile.pictureUrl ||
            record.roles != roles
}
