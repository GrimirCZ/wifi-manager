package cz.grimir.wifimanager.captive.persistence

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan
@EnableJpaRepositories(basePackages = ["cz.grimir.wifimanager.captive.persistence"])
@EntityScan(basePackages = ["cz.grimir.wifimanager.captive.persistence"])
class CaptivePersistenceConfig
