package cz.grimir.wifimanager.app

import org.flywaydb.core.Flyway
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayConfig {
    @Bean
    fun multiSchemaFlywayRunner(dataSource: DataSource): ApplicationRunner =
        ApplicationRunner {
            Flyway.configure()
                .dataSource(dataSource)
                .createSchemas(true)
                .schemas("admin")
                .defaultSchema("admin")
                .locations("classpath:db/migration/admin")
                .load()
                .migrate()

            Flyway.configure()
                .dataSource(dataSource)
                .createSchemas(true)
                .schemas("captive")
                .defaultSchema("captive")
                .locations("classpath:db/migration/captive")
                .load()
                .migrate()
        }
}
