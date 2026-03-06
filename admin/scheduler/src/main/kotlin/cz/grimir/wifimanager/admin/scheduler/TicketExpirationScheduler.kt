package cz.grimir.wifimanager.admin.scheduler

import cz.grimir.wifimanager.admin.application.ticket.command.ExpireTicketsCommand
import cz.grimir.wifimanager.admin.application.ticket.handler.command.ExpireExpiredTicketsUsecase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TicketExpirationScheduler(
    private val expireExpiredTicketsUsecase: ExpireExpiredTicketsUsecase,
) {
    @Scheduled(cron = "*/30 * * * * *")
    @SchedulerLock(
        name = "admin.ticket-expiration",
        lockAtMostFor = "PT25S",
        lockAtLeastFor = "PT5S",
    )
    fun expireTickets() {
        expireExpiredTicketsUsecase.expireExpiredTickets(ExpireTicketsCommand())
    }
}
