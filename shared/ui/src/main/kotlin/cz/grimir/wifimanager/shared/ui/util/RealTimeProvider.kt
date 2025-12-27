package cz.grimir.wifimanager.shared.ui.util

import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RealTimeProvider : TimeProvider {
    override fun get(): Instant = Instant.now()
}
