package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.ports.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.FindUserPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserPort
import cz.grimir.wifimanager.admin.persistence.mapper.UserIdentityMapper
import cz.grimir.wifimanager.admin.persistence.mapper.UserMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepositoryAdapter(
    private val userRepository: AdminUserJpaRepository,
    private val identityRepository: AdminUserIdentityJpaRepository,
    private val userMapper: UserMapper,
    private val identityMapper: UserIdentityMapper,
) : FindUserPort,
    SaveUserPort,
    FindUserIdentityPort,
    SaveUserIdentityPort {
    override fun findById(id: UserId): User? = userRepository.findByIdOrNull(id.id)?.let(userMapper::userToApplication)

    override fun save(user: User): User =
        userRepository.save(userMapper.userToEntity(user)).let(userMapper::userToApplication)

    override fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity? =
        identityRepository.findByIssuerAndSubject(issuer, subject)?.let(identityMapper::identityToApplication)

    override fun findByEmail(email: String): UserIdentity?  =
        identityRepository.findByEmail(email)?.let(identityMapper::identityToApplication)

    override fun save(identity: UserIdentity): UserIdentity =
        identityRepository.save(identityMapper.identityToEntity(identity)).let(identityMapper::identityToApplication)
}
