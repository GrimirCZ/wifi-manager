package cz.grimir.wifimanager.admin.application.ticket.handler.query

import cz.grimir.wifimanager.admin.application.ticket.model.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.application.ticket.port.FindTicketPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindAllTicketsWithDeviceCountUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(): List<TicketWithDeviceCount> = findTicketPort.findAllWithDeviceCount()
}
