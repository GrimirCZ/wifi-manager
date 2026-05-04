package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserRole
import java.time.Duration

object TicketDurationPolicy {
    val STANDARD_MINUTES = listOf(15, 30, 45, 60, 90, 120)
    val EXTENDED_MINUTES = listOf(240, 1440, 2880, 4320, 10080)

    fun allowedMinutesFor(user: UserIdentitySnapshot): List<Int> =
        if (user.can(UserRole::canCreateExtendedTickets)) {
            STANDARD_MINUTES + EXTENDED_MINUTES
        } else {
            STANDARD_MINUTES
        }

    fun isAllowed(
        duration: Duration,
        user: UserIdentitySnapshot,
    ): Boolean {
        val minutes = duration.toMinutes()
        return duration == Duration.ofMinutes(minutes) && allowedMinutesFor(user).contains(minutes.toInt())
    }
}
