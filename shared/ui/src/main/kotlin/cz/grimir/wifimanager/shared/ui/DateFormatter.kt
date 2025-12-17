package cz.grimir.wifimanager.shared.ui

import cz.grimir.wifimanager.shared.ui.util.TimeProvider
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Component("DateFormatter")
class DateFormatter(
    private val currentTimeProvider: TimeProvider,
) {
    private val formatter =
        DateTimeFormatter
            .ofPattern("dd. MM. yyyy HH:mm", Locale.getDefault())

    fun absolute(dt: Instant): String = LocalDateTime.ofInstant(dt, ZoneId.systemDefault()).format(formatter)

    fun timeRelativeNow(dt: Instant): String = timeRelative(currentTimeProvider.get(), dt)

    fun timeRelative(
        relativeTo: Instant,
        ts: Instant,
    ): String {
        if (relativeTo.until(ts, ChronoUnit.HOURS) < 24) {
            return LocalDateTime
                .ofInstant(ts, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
        }

        return absolute(ts)
    }
}
