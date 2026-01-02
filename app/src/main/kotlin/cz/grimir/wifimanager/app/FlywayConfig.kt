package cz.grimir.wifimanager.app

import org.flywaydb.core.Flyway
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayConfig {
    @Bean
    fun adminFlyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("admin")
            .defaultSchema("admin")
            .locations("classpath:db/migration/admin")
            .load()

    @Bean
    fun captiveFlyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("captive")
            .defaultSchema("captive")
            .locations("classpath:db/migration/captive")
            .load()

    @Bean
    fun userFlyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("auth")
            .defaultSchema("auth")
            .locations("classpath:db/migration/auth")
            .load()

    @Bean
    fun adminFlywayInitializer(adminFlyway: Flyway) = FlywayMigrationInitializer(adminFlyway)

    @Bean
    fun captiveFlywayInitializer(captiveFlyway: Flyway) = FlywayMigrationInitializer(captiveFlyway)

    @Bean
    fun userFlywayInitializer(userFlyway: Flyway) = FlywayMigrationInitializer(userFlyway)
}
