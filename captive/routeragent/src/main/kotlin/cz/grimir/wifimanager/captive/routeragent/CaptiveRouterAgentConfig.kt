package cz.grimir.wifimanager.captive.routeragent

import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.captive.routeragent.grpc.GrpcServerRouterAgent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
    ) = GrpcServerRouterAgent(properties.grpc, findAuthorizationTokenPort)
}
