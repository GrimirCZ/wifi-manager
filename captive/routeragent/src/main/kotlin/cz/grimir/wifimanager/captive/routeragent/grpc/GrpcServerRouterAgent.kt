package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.ports.ClientInfo
import cz.grimir.wifimanager.captive.application.ports.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.shared.application.network.NetworkClient
import cz.grimir.wifimanager.captive.routeragent.GrpcServerRouterAgentProperties
import cz.grimir.wifimanager.captive.routeragent.GrpcServerTlsProperties
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Server
import io.grpc.ServerCall
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.task.TaskExecutor
import java.io.File
import java.io.FileInputStream
import java.net.SocketAddress
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory

class GrpcServerRouterAgent(
    private val properties: GrpcServerRouterAgentProperties,
    findAuthorizationTokenPort: FindAuthorizationTokenPort,
    allowedMacReadPort: AllowedMacReadPort,
    commandExecutor: TaskExecutor,
) : RouterAgentPort,
    AutoCloseable,
    InitializingBean {
    private val hub = RouterAgentHub(properties.commandTimeout)
    private val service = RouterAgentGrpcService(hub, findAuthorizationTokenPort, allowedMacReadPort, commandExecutor)
    private val server = buildServer(properties, service)

    override fun afterPropertiesSet() {
        server.start()
    }

    override fun getClientInfo(ipAddress: String): ClientInfo? {
        val ack = hub.broadcastGetClientInfo(ipAddress)
        return ack?.takeIf { it.macAddress != null }?.let {
            ClientInfo(
                macAddress = it.macAddress!!,
                hostname = it.hostname,
            )
        }
    }

    override fun allowClientAccess(macAddresses: List<String>) {
        hub.broadcastAllowClientAccess(macAddresses)
    }

    override fun revokeClientAccess(macAddresses: List<String>) {
        hub.broadcastRevokeClientAccess(macAddresses)
    }

    override fun listNetworkClients(): List<NetworkClient> {
        val ack = hub.broadcastListNetworkClients() ?: return emptyList()
        if (!ack.success) {
            return emptyList()
        }
        return ack.clientsList.map { client ->
            NetworkClient(
                macAddress = client.macAddress,
                ipAddresses = client.ipAddressesList,
                hostname = client.hostname,
                allowed = client.allowed,
            )
        }
    }

    override fun close() {
        server.shutdown()
        server.awaitTermination(2, TimeUnit.SECONDS)
    }

    private fun buildServer(
        properties: GrpcServerRouterAgentProperties,
        service: RouterAgentGrpcService,
    ): Server {
        val builder =
            NettyServerBuilder
                .forPort(properties.port)
                .addService(ServerInterceptors.intercept(service, clientInfoInterceptor()))

        if (properties.tls.enabled) {
            builder.sslContext(buildSslContext(properties.tls))
        }

        return builder.build()
    }

    private fun buildSslContext(tls: GrpcServerTlsProperties) =
        GrpcSslContexts
            .configure(
                SslContextBuilder
                    .forServer(buildKeyManagerFactory(tls))
                    .trustManager(buildTrustManagerFactory(tls))
                    .clientAuth(if (tls.requireClientAuth) ClientAuth.REQUIRE else ClientAuth.OPTIONAL),
            ).build()

    private fun buildKeyManagerFactory(tls: GrpcServerTlsProperties): KeyManagerFactory {
        val keyStore = KeyStore.getInstance(tls.keyStoreType)
        FileInputStream(File(tls.keyStorePath)).use { input ->
            keyStore.load(input, tls.keyStorePassword.toCharArray())
        }
        val keyPassword = tls.keyPassword?.toCharArray() ?: tls.keyStorePassword.toCharArray()
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keyPassword)
        return keyManagerFactory
    }

    private fun buildTrustManagerFactory(tls: GrpcServerTlsProperties): TrustManagerFactory {
        val trustStore = KeyStore.getInstance(tls.trustStoreType)
        FileInputStream(File(tls.trustStorePath)).use { input ->
            trustStore.load(input, tls.trustStorePassword.toCharArray())
        }
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        return trustManagerFactory
    }

    private fun clientInfoInterceptor(): ServerInterceptor =
        object : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: io.grpc.Metadata,
                next: io.grpc.ServerCallHandler<ReqT, RespT>,
            ): ServerCall.Listener<ReqT> {
                val remoteAddress: SocketAddress? = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
                val sslSession: SSLSession? = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
                val peerCertSubject =
                    if (properties.tls.enabled) {
                        try {
                            sslSession
                                ?.peerCertificates
                                ?.firstNotNullOf { (it as? X509Certificate)?.subjectX500Principal?.name }
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        "mTLS disabled"
                    }

                val context =
                    Context
                        .current()
                        .withValue(RouterAgentClientInfo.REMOTE_ADDR_KEY, remoteAddress)
                        .withValue(RouterAgentClientInfo.PEER_CERT_SUBJECT_KEY, peerCertSubject)
                return Contexts.interceptCall(context, call, headers, next)
            }
        }
}
