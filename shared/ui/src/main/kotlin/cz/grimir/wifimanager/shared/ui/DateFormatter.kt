package cz.grimir.wifimanager.shared.ui

import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
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
        val relativeToDate = LocalDate.ofInstant(relativeTo, ZoneId.systemDefault())
        val tsDate = LocalDate.ofInstant(ts, ZoneId.systemDefault())
        if (relativeToDate == tsDate) {
            return LocalDateTime
                .ofInstant(ts, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
        }

        return absolute(ts)
    }

    fun toTimestamp(ts: Instant): Long {
        val zoneId = ZoneId.systemDefault()
        val date = LocalDateTime.ofInstant(ts, zoneId)
        return date.toEpochSecond(zoneId.rules.getOffset(date))
    }
}
