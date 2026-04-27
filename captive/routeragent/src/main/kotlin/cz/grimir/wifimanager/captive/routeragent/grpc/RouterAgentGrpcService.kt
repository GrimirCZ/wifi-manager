package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.util.concurrent.RejectedExecutionException

private val logger = KotlinLogging.logger {}

class RouterAgentGrpcService(
    private val hub: RouterAgentHub,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val allowedMacReadPort: AllowedMacReadPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val commandExecutor: TaskExecutor,
) : RouterAgentServiceGrpc.RouterAgentServiceImplBase() {
    override fun connect(responseObserver: StreamObserver<RouterAgentCommand>): StreamObserver<RouterAgentMessage> {
        val connectionId = hub.registerConnection(responseObserver)
        val remoteAddr = RouterAgentClientInfo.REMOTE_ADDR_KEY.get()
        val peerCertSubject = RouterAgentClientInfo.PEER_CERT_SUBJECT_KEY.get()

        logger.info { "Router agent connected id=$connectionId remoteAddr=$remoteAddr peerCert=$peerCertSubject" }

        return object : StreamObserver<RouterAgentMessage> {
            override fun onNext(message: RouterAgentMessage) {
                when (message.messageCase) {
                    RouterAgentMessage.MessageCase.ACK -> {
                        logger.trace {
                            "Router agent ack id=${message.ack.id} success=${message.ack.success} connectionId=$connectionId"
                        }
                        hub.handleAck(connectionId, message.ack)
                    }

                    RouterAgentMessage.MessageCase.SYNCHRONIZE -> {
                        try {
                            commandExecutor.execute {
                                try {
                                    val ticketAuthorizedMacs = findAuthorizationTokenPort.findAllAuthorizedMacs()
                                    val accountDeviceMacs = networkUserDeviceReadPort.findAllAuthorizedMacs()
                                    val allowedMacs = allowedMacReadPort.findAllAuthorizedMacs()
                                    val macAddresses = (ticketAuthorizedMacs + accountDeviceMacs + allowedMacs).distinct()
                                    hub.broadcastSetAllowedClients(macAddresses)
                                } catch (ex: Exception) {
                                    logger.error(ex) { "Failed to synchronize allowed clients" }
                                }
                            }
                        } catch (ex: RuntimeException) {
                            when (ex) {
                                is TaskRejectedException,
                                is RejectedExecutionException,
                                -> logger.error(ex) { "Failed to schedule allowed clients synchronization" }

                                else -> throw ex
                            }
                        }
                    }

                    RouterAgentMessage.MessageCase.AUTHORIZED_CLIENT_OBSERVED -> {
                        val observed = message.authorizedClientObserved
                        applicationEventPublisher.publishEvent(
                            AuthorizedClientObservedEvent(
                                macAddress = observed.macAddress,
                                hostname = observed.hostname.ifBlank { null },
                                dhcpHostname = observed.dhcpHostname.ifBlank { null },
                                dhcpVendorClass = observed.dhcpVendorClass.ifBlank { null },
                                dhcpPrlHash = observed.dhcpPrlHash.ifBlank { null },
                            ),
                        )
                    }

                    else -> logger.warn { "Unhandled router agent message ${message.messageCase}" }
                }
            }

            override fun onError(t: Throwable) {
                hub.unregisterConnection(connectionId, t)
                if (t is StatusRuntimeException && t.status.code == Status.Code.CANCELLED) {
                    logger.info {
                        "Router agent connection cancelled id=$connectionId remoteAddr=$remoteAddr peerCert=$peerCertSubject"
                    }
                    return
                }
                logger.warn(t) { "Router agent connection error id=$connectionId" }
            }

            override fun onCompleted() {
                hub.unregisterConnection(connectionId, null)
                responseObserver.onCompleted()
                logger.info {
                    "Router agent disconnected id=$connectionId remoteAddr=$remoteAddr peerCert=$peerCertSubject"
                }
            }
        }
    }
}
