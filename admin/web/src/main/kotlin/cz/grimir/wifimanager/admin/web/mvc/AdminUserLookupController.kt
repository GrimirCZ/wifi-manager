package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.identity.handler.query.FindUserIdentityByUserIdUsecase
import cz.grimir.wifimanager.admin.application.identity.query.FindUserIdentityByUserIdQuery
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.mvc.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Controller
class AdminUserLookupController(
    private val findUserIdentityByUserIdUsecase: FindUserIdentityByUserIdUsecase,
) {
    @GetMapping("/admin/users/{userId}/_display-name")
    fun displayName(
        @CurrentUser user: UserIdentitySnapshot,
        @PathVariable userId: UUID,
        modelMap: ModelMap,
    ): String {
        if (!user.can(UserRole::canCancelOtherUsersTickets)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        val identity =
            findUserIdentityByUserIdUsecase.find(FindUserIdentityByUserIdQuery(UserId(userId)))
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        modelMap.addAttribute("userId", userId)
        modelMap.addAttribute("displayName", identity.displayName)
        return "admin/fragments/user-display-name :: userDisplayName"
    }
}
