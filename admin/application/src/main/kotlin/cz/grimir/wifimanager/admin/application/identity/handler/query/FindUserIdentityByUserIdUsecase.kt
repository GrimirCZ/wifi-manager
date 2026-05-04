package cz.grimir.wifimanager.admin.application.identity.handler.query

import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity
import cz.grimir.wifimanager.admin.application.identity.port.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.identity.query.FindUserIdentityByUserIdQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindUserIdentityByUserIdUsecase(
    private val findUserIdentityPort: FindUserIdentityPort,
) {
    fun find(query: FindUserIdentityByUserIdQuery): UserIdentity? = findUserIdentityPort.findByUserId(query.userId)
}
