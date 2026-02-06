package cz.grimir.wifimanager.admin.scheduler

import cz.grimir.wifimanager.admin.application.usecases.commands.AdminDeleteExpiredAllowedMacsUsecase
import cz.grimir.wifimanager.shared.core.TimeProvider
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AllowedMacExpirationScheduler(
    private val deleteExpiredAllowedMacsUsecase: AdminDeleteExpiredAllowedMacsUsecase,
    private val timeProvider: TimeProvider,
) {
    @Scheduled(cron = "*/30 * * * * *")
    @SchedulerLock(
        name = "admin.allowed-mac-expiration",
        lockAtMostFor = "PT25S",
        lockAtLeastFor = "PT5S",
    )
    fun expireAllowedMacs() {
        deleteExpiredAllowedMacsUsecase.deleteExpired(timeProvider.get())
    }
}
