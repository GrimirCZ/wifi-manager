package cz.grimir.wifimanager.captive.web.security.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class CurrentClientIdentityCache {
    private val requestAttr = CurrentClientIdentityCache::class.java.name + ".APP_CLIENT_INFO"

    fun getOrLoad(
        request: HttpServletRequest,
        loader: () -> ClientInfo,
    ): ClientInfo {
        val existing = request.getAttribute(requestAttr) as? ClientInfo
        if (existing != null) return existing

        val loaded = loader()
        request.setAttribute(requestAttr, loaded)
        return loaded
    }
}
