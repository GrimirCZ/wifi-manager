package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindUserPort
import cz.grimir.wifimanager.admin.application.queries.FindUserByIdQuery
import cz.grimir.wifimanager.admin.application.queries.UserView
import org.springframework.stereotype.Service

@Service
class FindUserByIdUsecase(
    private val findUserPort: FindUserPort,
) {
    fun find(query: FindUserByIdQuery): UserView? =
        findUserPort.findById(query.id)?.let { user ->
            UserView(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                pictureUrl = user.pictureUrl,
                isActive = user.isActive,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                lastLoginAt = user.lastLoginAt,
            )
        }
}
