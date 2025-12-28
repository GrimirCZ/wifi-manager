package cz.grimir.wifimanager.captive.routeragent

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "wifimanager.captive.router-agent")
data class RouterAgentProperties(
    val type: RouterAgentType = RouterAgentType.DUMMY,
    val dummy: DummyRouterAgentProperties = DummyRouterAgentProperties(),
    val grpc: GrpcServerRouterAgentProperties = GrpcServerRouterAgentProperties(),
)

enum class RouterAgentType {
    DUMMY,
    GRPC,
}

data class DummyRouterAgentProperties(
    val clientMacAddressByIp: Map<String, String> = emptyMap(),
    val defaultClientMacAddress: String = "00:00:00:00:00:00",
    val clientHostnameByIp: Map<String, String> = emptyMap(),
    val defaultClientHostname: String? = "dummy-device",
)

data class GrpcServerRouterAgentProperties(
    val port: Int = 9091,
    val commandTimeout: Duration = Duration.ofSeconds(5),
    val tls: GrpcServerTlsProperties = GrpcServerTlsProperties(),
)

data class GrpcServerTlsProperties(
    val enabled: Boolean = true,
    val keyStorePath: String = "certs/server.p12",
    val keyStorePassword: String = "changeit",
    val keyStoreType: String = "PKCS12",
    val keyPassword: String? = null,
    val trustStorePath: String = "certs/truststore.p12",
    val trustStorePassword: String = "changeit",
    val trustStoreType: String = "PKCS12",
    val requireClientAuth: Boolean = true,
)
