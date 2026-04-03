package cz.grimir.wifimanager.captive.application

import cz.grimir.wifimanager.captive.application.migration.CaptiveMigrationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
@EnableConfigurationProperties(CaptiveMigrationProperties::class)
class CaptiveApplicationConfig
