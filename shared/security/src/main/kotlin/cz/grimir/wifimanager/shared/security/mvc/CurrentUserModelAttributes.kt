package cz.grimir.wifimanager.shared.security.mvc

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

@ControllerAdvice
class CurrentUserModelAttributes {
    @ModelAttribute("currentUser")
    fun currentUser(request: HttpServletRequest): UserIdentitySnapshot? =
        request
            .getSession(false)
            ?.getAttribute(SessionUserIdentity.SESSION_KEY)
            ?.let { it as? SessionUserIdentity }
            ?.toSnapshot()
}
