package cz.grimir.wifimanager.app

import cz.grimir.wifimanager.admin.application.AdminApplicationConfig
import cz.grimir.wifimanager.admin.events.local.AdminLocalEventsConfig
import cz.grimir.wifimanager.admin.persistence.AdminPersistenceConfig
import cz.grimir.wifimanager.admin.web.AdminWebConfig
import cz.grimir.wifimanager.captive.application.CaptiveApplicationConfig
import cz.grimir.wifimanager.captive.events.local.CaptiveLocalEventsConfig
import cz.grimir.wifimanager.captive.persistence.CaptivePersistenceConfig
import cz.grimir.wifimanager.web.captive.CaptiveWebConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableTransactionManagement
@Import(
    AdminApplicationConfig::class,
    AdminPersistenceConfig::class,
    AdminWebConfig::class,
    AdminLocalEventsConfig::class,
    CaptiveApplicationConfig::class,
    CaptivePersistenceConfig::class,
    CaptiveWebConfig::class,
    CaptiveLocalEventsConfig::class,
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
