package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.query.model.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.application.port.FindTicketPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FindAllTicketsWithDeviceCountUsecase(
    private val findTicketPort: FindTicketPort,
) {
    fun find(): List<TicketWithDeviceCount> = findTicketPort.findAllWithDeviceCount()
}
