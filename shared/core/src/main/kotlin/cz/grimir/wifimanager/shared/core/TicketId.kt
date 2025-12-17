package cz.grimir.wifimanager.shared.core

import java.util.UUID

@JvmInline
value class TicketId(
    val id: UUID,
){
    companion object{
        fun new(): TicketId = TicketId(UUID.randomUUID())
    }
}

