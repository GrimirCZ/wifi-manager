package cz.grimir.wifimanager.captive.web

import cz.grimir.wifimanager.captive.application.migration.CaptiveMigrationProperties
import cz.grimir.wifimanager.captive.web.security.CaptiveMigrationApiKeyFilter
import cz.grimir.wifimanager.captive.web.security.CaptiveMigrationApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
@EnableConfigurationProperties(CaptiveMigrationApiProperties::class)
class CaptiveWebConfig
{
    @Bean
    fun captiveMigrationApiKeyFilterRegistration(
        migrationProperties: CaptiveMigrationProperties,
        apiProperties: CaptiveMigrationApiProperties,
    ): FilterRegistrationBean<CaptiveMigrationApiKeyFilter> =
        FilterRegistrationBean(
            CaptiveMigrationApiKeyFilter(
                endpointPath = migrationProperties.endpointPath,
                properties = apiProperties,
            ),
        ).apply {
            addUrlPatterns(migrationProperties.endpointPath)
            setName("captiveMigrationApiKeyFilter")
            order = Int.MIN_VALUE
        }
}
