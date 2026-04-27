package cz.grimir.wifimanager.shared.application

import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiClient
import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("google")
@ComponentScan
@EnableConfigurationProperties(GoogleDirectoryApiProperties::class)
class SharedApplicationConfig {
    @Bean
    fun googleDirectoryApiClient(properties: GoogleDirectoryApiProperties): GoogleDirectoryApiClient = GoogleDirectoryApiClient(properties)
}
