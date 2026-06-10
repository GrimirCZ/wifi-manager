package cz.grimir.wifimanager.captive.routeragent.grpc

import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RouterAgentHubTest {
    @Test
    fun `concurrent get client info broadcasts serialize writes to one response observer`() {
        val hub = RouterAgentHub(Duration.ofSeconds(5))
        val observer = AckingObserver(hub)
        val connectionId = hub.registerConnection(observer)
        observer.connectionId = connectionId
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val tasks = 24

        val futures =
            (1..tasks).map { index ->
                executor.submit<CommandAck?> {
                    start.await()
                    hub.broadcastGetClientInfo("192.0.2.$index")
                }
            }

        start.countDown()
        val acks = futures.map { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()

        assertEquals(tasks, observer.commands.size)
        assertFalse(observer.concurrentWriteDetected.get())
        assertEquals(1, observer.maxConcurrentWrites.get())
        assertEquals(tasks, acks.size)
        acks.forEach { ack ->
            assertNotNull(ack)
            assertEquals("aa:bb:cc:dd:ee:ff", ack?.macAddress)
        }
    }

    @Test
    fun `failed response observer write removes connection and later commands are not sent to it`() {
        val hub = RouterAgentHub(Duration.ofMillis(200))
        val observer = ThrowingObserver()
        hub.registerConnection(observer)

        assertThrows(IllegalStateException::class.java) {
            hub.broadcastGetClientInfo("192.0.2.10")
        }

        val ack = hub.broadcastGetClientInfo("192.0.2.11")

        assertNull(ack)
        assertEquals(1, observer.sendAttempts.get())
    }
}

private class AckingObserver(
    private val hub: RouterAgentHub,
) : StreamObserver<RouterAgentCommand> {
    lateinit var connectionId: String
    val commands: MutableList<RouterAgentCommand> = Collections.synchronizedList(mutableListOf())
    val concurrentWriteDetected = AtomicBoolean(false)
    val maxConcurrentWrites = AtomicInteger(0)
    private val activeWrites = AtomicInteger(0)

    override fun onNext(command: RouterAgentCommand) {
        val active = activeWrites.incrementAndGet()
        maxConcurrentWrites.accumulateAndGet(active, ::maxOf)
        if (active > 1) {
            concurrentWriteDetected.set(true)
        }

        try {
            Thread.sleep(10)
            commands.add(command)
            hub.handleAck(
                connectionId,
                CommandAck
                    .newBuilder()
                    .setId(command.getClientInfo.id)
                    .setSuccess(true)
                    .setMacAddress("aa:bb:cc:dd:ee:ff")
                    .build(),
            )
        } finally {
            activeWrites.decrementAndGet()
        }
    }

    override fun onError(t: Throwable) = Unit

    override fun onCompleted() = Unit
}

private class ThrowingObserver : StreamObserver<RouterAgentCommand> {
    val sendAttempts = AtomicInteger(0)

    override fun onNext(value: RouterAgentCommand) {
        sendAttempts.incrementAndGet()
        throw IllegalStateException("stream write failed")
    }

    override fun onError(t: Throwable) = Unit

    override fun onCompleted() = Unit
}
