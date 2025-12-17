package cz.grimir.wifimanager.admin.web.security.support

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class CurrentUserIdentityCache {
    private val requestAttr = CurrentUserIdentityCache::class.java.name + ".APP_USER_IDENTITY"

    fun getOrLoad(request: HttpServletRequest, loader: () -> UserIdentity): UserIdentity {
        val existing = request.getAttribute(requestAttr) as? UserIdentity
        if (existing != null) return existing

        val loaded = loader()
        request.setAttribute(requestAttr, loaded)
        return loaded
    }
}