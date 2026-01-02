package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.ports.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.ports.SaveUserIdentityPort
import cz.grimir.wifimanager.admin.persistence.mapper.UserIdentityMapper
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepositoryAdapter(
    private val identityRepository: AdminUserIdentityJpaRepository,
    private val identityMapper: UserIdentityMapper,
) : FindUserIdentityPort,
    SaveUserIdentityPort {
    override fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentity? = identityRepository.findByIssuerAndSubject(issuer, subject)?.let(identityMapper::identityToApplication)

    override fun save(identity: UserIdentity): UserIdentity =
        identityRepository.save(identityMapper.identityToEntity(identity)).let(identityMapper::identityToApplication)
}
