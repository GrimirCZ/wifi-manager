package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.queries.FindTicketsByAuthorIdWithDeviceCountQuery
import cz.grimir.wifimanager.admin.application.queries.models.TicketWithDeviceCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindTicketsByAuthorIdWithDeviceCountUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(query: FindTicketsByAuthorIdWithDeviceCountQuery): List<TicketWithDeviceCount> =
        findTicketPort.findByAuthorIdWithDeviceCount(query.authorId.id)
}
