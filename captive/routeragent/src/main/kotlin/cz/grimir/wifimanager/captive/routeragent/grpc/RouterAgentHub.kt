package cz.grimir.wifimanager.captive.routeragent.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class RouterAgentHub(
    private val commandTimeout: Duration,
) {
    private val connections = ConcurrentHashMap<String, RouterAgentConnection>()

    fun registerConnection(responseObserver: StreamObserver<RouterAgentCommand>): String {
        val connectionId = UUID.randomUUID().toString()
        connections[connectionId] = RouterAgentConnection(connectionId, responseObserver, commandTimeout)
        return connectionId
    }

    fun unregisterConnection(
        connectionId: String,
        cause: Throwable? = null,
    ) {
        connections.remove(connectionId)?.close(cause)
        logger.info { "Router agent disconnected id=$connectionId total=${connections.size}" }
    }

    fun handleAck(
        connectionId: String,
        ack: CommandAck,
    ) {
        connections[connectionId]?.handleAck(ack)
    }

    fun broadcastAllowClientAccess(macAddresses: List<String>) {
        if (macAddresses.isEmpty()) {
            return
        }

        val commandId = UUID.randomUUID().toString()
        val command =
            RouterAgentCommand
                .newBuilder()
                .setAllowClientAccess(
                    AllowClientAccess
                        .newBuilder()
                        .setId(commandId)
                        .addAllMacAddresses(macAddresses)
                        .build(),
                ).build()
        logger.info { "Allowing client access for macAddresses=$macAddresses, commandId=$commandId" }
        waitForAllAcks(sendToAll(command), commandId(command))
    }

    fun broadcastRevokeClientAccess(macAddresses: List<String>) {
        if (macAddresses.isEmpty()) {
            return
        }

        val commandId = UUID.randomUUID().toString()
        val command =
            RouterAgentCommand
                .newBuilder()
                .setRevokeClientAccess(
                    RevokeClientAccess
                        .newBuilder()
                        .setId(commandId)
                        .addAllMacAddresses(macAddresses)
                        .build(),
                ).build()
        logger.info { "Revoking client access for macAddresses=$macAddresses, commandId=$commandId" }
        waitForAllAcks(sendToAll(command), commandId(command))
    }

    fun broadcastSetAllowedClients(macAddresses: List<String>) {
        val commandId = UUID.randomUUID().toString()
        val command =
            RouterAgentCommand
                .newBuilder()
                .setSetAllowedClients(
                    SetAllowedClients
                        .newBuilder()
                        .setId(commandId)
                        .addAllMacAddresses(macAddresses)
                        .build(),
                ).build()
        logger.info { "Setting allowed macAddresses=$macAddresses, command=$commandId" }
        waitForAllAcks(sendToAll(command), commandId(command))
    }

    fun broadcastGetClientInfo(ipAddress: String): CommandAck? {
        val commandId = UUID.randomUUID().toString()
        val command =
            RouterAgentCommand
                .newBuilder()
                .setGetClientInfo(
                    GetClientInfo
                        .newBuilder()
                        .setId(commandId)
                        .setIpAddress(ipAddress)
                        .build(),
                ).build()
        logger.info { "Getting mac for ip=$ipAddress, commandId=$commandId" }
        return waitForFirstSuccessfulAck(sendToAll(command), commandId(command))
    }

    fun broadcastListNetworkClients(): CommandAck? {
        val commandId = UUID.randomUUID().toString()
        val command =
            RouterAgentCommand
                .newBuilder()
                .setListNetworkClients(
                    ListNetworkClients
                        .newBuilder()
                        .setId(commandId)
                        .build(),
                ).build()
        logger.info { "Listing network clients commandId=$commandId" }
        return waitForFirstSuccessfulAck(sendToAll(command), commandId(command))
    }

    private fun sendToAll(command: RouterAgentCommand): List<CompletableFuture<CommandAck>> {
        if (connections.isEmpty()) {
            logger.warn { "No router agents available for command=$command" }
            return emptyList()
        }

        return connections.values.map { it.send(command) }
    }

    private fun waitForAllAcks(
        futures: List<CompletableFuture<CommandAck>>,
        commandId: String,
    ): List<CommandAck> {
        if (futures.isEmpty()) {
            logger.warn { "No router agent connected to respond to commandId=$commandId" }
            return emptyList()
        }

        return futures
            .map { future ->
                try {
                    future.get(commandTimeout.toMillis(), TimeUnit.MILLISECONDS)
                } catch (ex: Exception) {
                    throw IllegalStateException("Router agent command failed id=$commandId", ex)
                }
            }.also { acks ->
                val failure = acks.firstOrNull { !it.success }
                if (failure != null) {
                    throw IllegalStateException(
                        "Router agent command failed id=$commandId error=${failure.errorMessage}",
                    )
                }
            }
    }

    private fun waitForFirstSuccessfulAck(
        futures: List<CompletableFuture<CommandAck>>,
        commandId: String,
    ): CommandAck? {
        if (futures.isEmpty()) {
            logger.warn { "No router agent connected to respond to commandId=$commandId" }
            return null
        }

        val remaining = AtomicInteger(futures.size)
        val result = CompletableFuture<CommandAck?>()
        // NOTE: cannot use CompletableFuture.anyOf because it completes on the first completion (success or failure).
        // This implementation is needed to wait for the first successful ack.
        futures.forEach { future ->
            future.whenComplete { ack: CommandAck?, ex: Throwable? ->
                if (ex == null && ack != null) {
                    if (ack.success) {
                        result.complete(ack)
                        return@whenComplete
                    } else {
                        logger.warn { "Router agent command failed id=$commandId error=${ack.errorMessage}" }
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    result.completeExceptionally(
                        IllegalStateException("Router agent failed to complete id=$commandId"),
                    )
                }
            }
        }

        return try {
            result.get(commandTimeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            throw IllegalStateException("Router agent command failed id=$commandId", ex)
        }
    }

    private fun commandId(command: RouterAgentCommand): String =
        when (command.commandCase) {
            RouterAgentCommand.CommandCase.GET_CLIENT_INFO -> command.getClientInfo.id
            RouterAgentCommand.CommandCase.ALLOW_CLIENT_ACCESS -> command.allowClientAccess.id
            RouterAgentCommand.CommandCase.REVOKE_CLIENT_ACCESS -> command.revokeClientAccess.id
            RouterAgentCommand.CommandCase.SET_ALLOWED_CLIENTS -> command.setAllowedClients.id
            RouterAgentCommand.CommandCase.LIST_NETWORK_CLIENTS -> command.listNetworkClients.id
            RouterAgentCommand.CommandCase.COMMAND_NOT_SET -> error("Invalid command: $command")
        }
}

private class RouterAgentConnection(
    private val id: String,
    private val responseObserver: StreamObserver<RouterAgentCommand>,
    private val commandTimeout: Duration,
) {
    private val pending = ConcurrentHashMap<String, CompletableFuture<CommandAck>>()

    fun send(command: RouterAgentCommand): CompletableFuture<CommandAck> {
        val commandId = commandId(command)
        val future =
            CompletableFuture<CommandAck>()
                .orTimeout(commandTimeout.toMillis(), TimeUnit.MILLISECONDS)
        pending[commandId] = future

        try {
            responseObserver.onNext(command)
        } catch (ex: Exception) {
            pending.remove(commandId)
            future.completeExceptionally(ex)
        }
        return future
    }

    fun handleAck(ack: CommandAck) {
        pending.remove(ack.id)?.complete(ack)
    }

    fun close(cause: Throwable?) {
        val exception = cause ?: IllegalStateException("Router agent connection closed id=$id")
        pending.values.forEach { it.completeExceptionally(exception) }
    }

    private fun commandId(command: RouterAgentCommand): String =
        when (command.commandCase) {
            RouterAgentCommand.CommandCase.GET_CLIENT_INFO -> command.getClientInfo.id
            RouterAgentCommand.CommandCase.ALLOW_CLIENT_ACCESS -> command.allowClientAccess.id
            RouterAgentCommand.CommandCase.REVOKE_CLIENT_ACCESS -> command.revokeClientAccess.id
            RouterAgentCommand.CommandCase.SET_ALLOWED_CLIENTS -> command.setAllowedClients.id
            RouterAgentCommand.CommandCase.LIST_NETWORK_CLIENTS -> command.listNetworkClients.id
            RouterAgentCommand.CommandCase.COMMAND_NOT_SET -> error("Invalid command: $command")
        }
}
