package cz.grimir.wifimanager.captive.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
@EnableConfigurationProperties(CaptiveFingerprintingProperties::class)
class CaptiveApplicationConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
}
