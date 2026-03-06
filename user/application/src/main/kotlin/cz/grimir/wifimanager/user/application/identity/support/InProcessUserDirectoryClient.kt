package cz.grimir.wifimanager.user.application.identity.support

import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.ResolveUserResult
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.user.application.identity.handler.UserDirectoryService
import org.springframework.stereotype.Service

@Service
class InProcessUserDirectoryClient(
    private val userDirectoryService: UserDirectoryService,
) : UserDirectoryClient {
    override fun resolveUser(command: ResolveUserCommand): ResolveUserResult = userDirectoryService.resolveOrCreate(command)
}
