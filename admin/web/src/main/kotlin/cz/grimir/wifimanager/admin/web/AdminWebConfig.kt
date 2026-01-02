package cz.grimir.wifimanager.admin.web

import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import cz.grimir.wifimanager.shared.ui.SharedUiConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan
@Import(SharedUiConfig::class)
@EnableConfigurationProperties(
    UiSecurityProperties::class,
    AdminWifiProperties::class,
)
class AdminWebConfig
