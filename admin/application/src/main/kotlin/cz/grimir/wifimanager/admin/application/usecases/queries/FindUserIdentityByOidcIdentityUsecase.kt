package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.ports.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.queries.FindUserByOidcIdentityQuery
import org.springframework.stereotype.Service

@Service
class FindUserIdentityByOidcIdentityUsecase(
    private val findUserPort: FindUserIdentityPort,
) {
    fun find(query: FindUserByOidcIdentityQuery): UserIdentity? = findUserPort.findByIssuerAndSubject(
        query.issuer,
        query.subject
    )
}
