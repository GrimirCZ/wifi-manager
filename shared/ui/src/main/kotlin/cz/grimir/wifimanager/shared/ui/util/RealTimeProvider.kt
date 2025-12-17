package cz.grimir.wifimanager.shared.ui.util

import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RealTimeProvider : TimeProvider {
    override fun get(): Instant = Instant.now()
}
