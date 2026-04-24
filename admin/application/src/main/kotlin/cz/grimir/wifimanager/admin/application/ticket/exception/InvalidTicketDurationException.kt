package cz.grimir.wifimanager.admin.application.ticket.exception

import java.time.Duration

class InvalidTicketDurationException(
    val duration: Duration,
) : IllegalArgumentException("Invalid ticket duration: $duration")
