package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.util.concurrent.RejectedExecutionException

private val logger = KotlinLogging.logger {}

class RouterAgentGrpcService(
    private val hub: RouterAgentHub,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
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
                                    val macAddresses = findAuthorizationTokenPort.findAllAuthorizedDeviceMacs()
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

                    else -> logger.warn { "Unhandled router agent message ${message.messageCase}" }
                }
            }

            override fun onError(t: Throwable) {
                hub.unregisterConnection(connectionId, t)
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
