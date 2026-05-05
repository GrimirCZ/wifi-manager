package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.query.FindAllTicketsWithDeviceCountQuery
import cz.grimir.wifimanager.admin.application.query.model.TicketWithDeviceCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindAllTicketsWithDeviceCountUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(query: FindAllTicketsWithDeviceCountQuery): List<TicketWithDeviceCount> = findTicketPort.findAllWithDeviceCount()
}
