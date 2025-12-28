package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.queries.FindTicketByIdQuery
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindTicketByIdUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(query: FindTicketByIdQuery): Ticket? = findTicketPort.findById(query.id)
}
