package cz.grimir.wifimanager.captive.routeragent.grpc

import cz.grimir.wifimanager.captive.application.command.AllowedClientsPresenceEntry
import cz.grimir.wifimanager.captive.application.command.ApplyAllowedClientsPresenceCommand
import cz.grimir.wifimanager.captive.application.command.handler.ApplyAllowedClientsPresenceUsecase
import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.events.AuthorizedClientObservedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.concurrent.RejectedExecutionException

private val logger = KotlinLogging.logger {}

class RouterAgentGrpcService(
    private val hub: RouterAgentHub,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val allowedMacReadPort: AllowedMacReadPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val applyAllowedClientsPresenceUsecase: ApplyAllowedClientsPresenceUsecase,
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

                    RouterAgentMessage.MessageCase.ALLOWED_CLIENTS_PRESENCE -> {
                        val presence = message.allowedClientsPresence
                        try {
                            commandExecutor.execute {
                                try {
                                    val entries = parseAllowedClientsPresenceEntries(presence)
                                    if (entries.isNotEmpty()) {
                                        applyAllowedClientsPresenceUsecase.apply(
                                            ApplyAllowedClientsPresenceCommand(entries),
                                        )
                                    }
                                } catch (ex: Exception) {
                                    logger.error(ex) { "Failed to apply allowed clients presence" }
                                }
                            }
                        } catch (ex: RuntimeException) {
                            when (ex) {
                                is TaskRejectedException,
                                is RejectedExecutionException,
                                -> logger.error(ex) { "Failed to schedule allowed clients presence" }

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

private fun parseAllowedClientsPresenceEntries(presence: AllowedClientsPresence): List<AllowedClientsPresenceEntry> {
    val out = mutableListOf<AllowedClientsPresenceEntry>()
    for (e in presence.entriesList) {
        val mac = e.macAddress.trim()
        if (mac.isEmpty()) {
            continue
        }
        val ts = e.lastSeenAt.trim()
        if (ts.isEmpty()) {
            continue
        }
        val instant =
            try {
                Instant.parse(ts)
            } catch (_: DateTimeParseException) {
                logger.warn { "Skipping allowed clients presence entry invalid last_seen_at mac=$mac value=$ts" }
                continue
            }
        out.add(AllowedClientsPresenceEntry(macAddress = mac, lastSeenAt = instant))
    }
    return out
}
