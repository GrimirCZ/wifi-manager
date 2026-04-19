package cz.grimir.wifimanager.app.routing

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CaptiveRequestRoutingProperties::class)
class CaptiveRequestRoutingConfig
