package cz.grimir.wifimanager.admin.web

import cz.grimir.wifimanager.admin.web.security.AdminSecurityProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
@EnableConfigurationProperties(AdminSecurityProperties::class)
class AdminWebConfig
