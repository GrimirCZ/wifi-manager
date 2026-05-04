package cz.grimir.wifimanager.admin.core.exceptions

import java.time.Duration

class InvalidTicketDurationException(
    val duration: Duration,
) : IllegalArgumentException("Invalid ticket duration: $duration")