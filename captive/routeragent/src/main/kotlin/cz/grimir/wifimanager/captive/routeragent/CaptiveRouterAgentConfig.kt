package cz.grimir.wifimanager.captive.routeragent

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.devicefingerprint.AuthorizedClientFingerprintGuard
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import cz.grimir.wifimanager.captive.routeragent.grpc.GrpcServerRouterAgent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor

@Configuration
@EnableConfigurationProperties(RouterAgentProperties::class)
class CaptiveRouterAgentConfig {
    @Bean
    @ConditionalOnProperty(
        prefix = "wifimanager.captive.router-agent",
        name = ["type"],
        havingValue = "dummy",
        matchIfMissing = true,
    )
    fun dummyRouterAgent(properties: RouterAgentProperties): RouterAgentPort = DummyRouterAgent(properties.dummy)

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
        prefix = "wifimanager.captive.router-agent",
        name = ["type"],
        havingValue = "grpc",
    )
    fun grpcRouterAgent(
        properties: RouterAgentProperties,
        findAuthorizationTokenPort: FindAuthorizationTokenPort,
        allowedMacReadPort: AllowedMacReadPort,
        authorizedClientFingerprintGuard: AuthorizedClientFingerprintGuard,
        deviceFingerprintService: DeviceFingerprintService,
        applicationTaskExecutor: TaskExecutor,
    ) = GrpcServerRouterAgent(
        properties.grpc,
        findAuthorizationTokenPort,
        allowedMacReadPort,
        authorizedClientFingerprintGuard,
        deviceFingerprintService,
        applicationTaskExecutor,
    )
}
