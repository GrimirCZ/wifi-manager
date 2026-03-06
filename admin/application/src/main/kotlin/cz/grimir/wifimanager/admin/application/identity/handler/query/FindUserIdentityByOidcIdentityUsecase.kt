package cz.grimir.wifimanager.admin.application.identity.handler.query

import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity
import cz.grimir.wifimanager.admin.application.identity.port.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.identity.query.FindUserByOidcIdentityQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindUserIdentityByOidcIdentityUsecase(
    private val findUserPort: FindUserIdentityPort,
) {
    fun find(query: FindUserByOidcIdentityQuery): UserIdentity? =
        findUserPort.findByIssuerAndSubject(
            query.issuer,
            query.subject,
        )
}
