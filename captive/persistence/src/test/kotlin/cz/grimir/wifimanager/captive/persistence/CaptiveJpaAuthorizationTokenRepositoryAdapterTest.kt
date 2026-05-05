package cz.grimir.wifimanager.captive.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cz.grimir.wifimanager.captive.application.command.ScrubDeauthorizedCaptiveDeviceCommand
import cz.grimir.wifimanager.captive.application.command.handler.ScrubDeauthorizedCaptiveDeviceUsecase
import cz.grimir.wifimanager.captive.application.config.CaptiveFingerprintingProperties
import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.support.ClientAccessAuthorizationResolver
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.UserAgentClassifier
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.core.TicketId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.BufferedReader
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers
@SpringBootTest(classes = [CaptiveJpaAuthorizationTokenRepositoryAdapterTest.TestApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaptiveJpaAuthorizationTokenRepositoryAdapterTest {
    companion object {
        private val ticketId = UUID.fromString("a3072a04-6a92-4785-bdf0-9478f1666e6e")

        @Container
        @JvmField
        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("wifimanager")
                .withUsername("test")
                .withPassword("test")!!

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            if (!postgres.isRunning) {
                postgres.start()
            }
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var repository: CaptiveJpaAuthorizationTokenRepositoryAdapter

    @Autowired
    private lateinit var allowedMacReadPort: AllowedMacReadPort

    @Autowired
    private lateinit var networkUserDeviceReadPort: NetworkUserDeviceReadPort

    @Autowired
    private lateinit var captiveDevicePrivacyPort: CaptiveJpaDevicePrivacyAdapter

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transactions: TransactionTemplate

    @BeforeAll
    fun setupDatabase() {
        transactions = TransactionTemplate(transactionManager)
        applyMigration("db/migration/captive/V1__create_captive_tables.sql")
        applyMigration("db/migration/captive/V2__add_valid_until.sql")
        applyMigration("db/migration/captive/V3__add_user_read_model.sql")
        applyMigration("db/migration/captive/V4__add_network_users.sql")
        applyMigration("db/migration/captive/V5__create_allowed_mac.sql")
        applyMigration("db/migration/captive/V6__add_device_fingerprint_columns.sql")
    }

    @BeforeEach
    fun resetData() {
        jdbcTemplate.execute(
            """
            truncate
                captive.allowed_mac,
                captive.network_user_device,
                captive.network_user,
                captive.user_identity,
                captive.captive_authorized_device,
                captive.captive_device,
                captive.captive_authorization_token
            cascade
            """.trimIndent(),
        )
        insertToken()
    }

    @Test
    fun `saving a token with an existing authorized device can add another device`() {
        insertAuthorizedDevice("96:e8:b2:e8:a7:3e", "first")

        transactions.executeWithoutResult {
            val token = repository.findByAccessCode("ABCDEF") ?: error("Token not found")

            token.authorizeDevice(device("ce:0e:e8:96:11:be", "Galaxy-A15"))
            repository.save(token)
        }

        assertAuthorizedDevices("96:e8:b2:e8:a7:3e", "ce:0e:e8:96:11:be")
    }

    @Test
    fun `concurrent authorization attempts for one ticket do not duplicate existing join rows`() {
        insertAuthorizedDevice("96:e8:b2:e8:a7:3e", "first")

        val startTogether = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val failures = mutableListOf<Throwable>()

        listOf(
            device("c6:16:92:9a:8d:ea", "Redmi-Note-12S"),
            device("ce:0e:e8:96:11:be", "Galaxy-A15"),
        ).forEach { newDevice ->
            executor.submit {
                try {
                    if (!startTogether.await(10, TimeUnit.SECONDS)) {
                        error("Timed out waiting to start")
                    }
                    transactions.executeWithoutResult {
                        val token = repository.findByAccessCodeForAuthorization("ABCDEF") ?: error("Token not found")
                        token.authorizeDevice(newDevice)
                        repository.save(token)
                    }
                } catch (ex: Throwable) {
                    synchronized(failures) {
                        failures += ex
                    }
                }
            }
        }

        startTogether.countDown()
        executor.shutdown()
        if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
            error("Timed out waiting for authorization transactions")
        }

        assertEquals(emptyList<Throwable>(), failures)
        assertAuthorizedDevices(
            "96:e8:b2:e8:a7:3e",
            "c6:16:92:9a:8d:ea",
            "ce:0e:e8:96:11:be",
        )
    }

    @Test
    fun `deleting ticket token keeps captive device used by account authorization`() {
        val mac = "96:e8:b2:e8:a7:3e"
        insertAuthorizedDevice(mac, "first")
        insertNetworkUserDevice(mac)

        transactions.executeWithoutResult {
            repository.deleteByTicketId(TicketId(ticketId))
        }

        assertEquals(0, countRows("captive.captive_authorized_device", "device_mac = ?", mac))
        assertEquals(1, countRows("captive.captive_device", "mac = ?", mac))
        assertEquals(1, countRows("captive.network_user_device", "device_mac = ?", mac))
    }

    @Test
    fun `ticket removal workflow scrubs retained ticket-only device pii`() {
        val mac = "96:e8:b2:e8:a7:3e"
        insertAuthorizedDevice(mac, "first")
        jdbcTemplate.update(
            "update captive.captive_device set display_name = 'Visitor One' where mac = ?",
            mac,
        )
        val scrubUsecase = scrubUsecase()

        transactions.executeWithoutResult {
            repository.deleteByTicketId(TicketId(ticketId))
        }
        transactions.executeWithoutResult {
            scrubUsecase.scrubIfEligible(ScrubDeauthorizedCaptiveDeviceCommand(mac))
        }

        assertEquals(1, countRows("captive.captive_device", "mac = ?", mac))
        assertNull(deviceColumn(mac, "display_name"))
        assertNull(deviceColumn(mac, "device_name"))
    }

    private fun insertToken() {
        jdbcTemplate.update(
            """
            insert into captive.captive_authorization_token (
                id,
                access_code,
                require_user_name_on_login,
                kicked_macs,
                valid_until
            ) values (?, 'ABCDEF', false, '{}', ?)
            """.trimIndent(),
            ticketId,
            Timestamp.from(Instant.parse("2026-04-30T08:50:55.800881Z")),
        )
    }

    private fun insertAuthorizedDevice(
        mac: String,
        deviceName: String,
    ) {
        jdbcTemplate.update(
            """
            insert into captive.captive_device (
                mac,
                display_name,
                device_name,
                fingerprint_status
            ) values (?, null, ?, 'NONE')
            """.trimIndent(),
            mac,
            deviceName,
        )
        jdbcTemplate.update(
            "insert into captive.captive_authorized_device (token_id, device_mac) values (?, ?)",
            ticketId,
            mac,
        )
    }

    private fun insertNetworkUserDevice(mac: String) {
        val userId = UUID.fromString("f5e63072-b8df-4afe-b085-4fd382987efe")
        val identityId = UUID.fromString("b6e9ddc4-1e2a-4b35-a4ef-754e227449e8")
        val now = Timestamp.from(Instant.parse("2026-04-30T08:51:55.800881Z"))
        jdbcTemplate.update(
            """
            insert into captive.network_user (
                user_id,
                identity_id,
                allowed_device_count,
                admin_override_limit,
                created_at,
                updated_at,
                last_login_at
            ) values (?, ?, 3, null, ?, ?, ?)
            """.trimIndent(),
            userId,
            identityId,
            now,
            now,
            now,
        )
        jdbcTemplate.update(
            """
            insert into captive.network_user_device (
                user_id,
                device_mac,
                name,
                hostname,
                is_randomized,
                authorized_at,
                last_seen_at,
                fingerprint_status
            ) values (?, ?, 'Owner phone', 'first', false, ?, ?, 'NONE')
            """.trimIndent(),
            userId,
            mac,
            now,
            now,
        )
    }

    private fun assertAuthorizedDevices(vararg macs: String) {
        val actual =
            jdbcTemplate.queryForList(
                """
                select device_mac
                from captive.captive_authorized_device
                where token_id = ?
                order by device_mac
                """.trimIndent(),
                String::class.java,
                ticketId,
            )

        assertEquals(macs.sorted(), actual)
    }

    private fun countRows(
        table: String,
        where: String,
        vararg args: Any,
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from $table where $where",
            Int::class.java,
            *args,
        ) ?: 0

    private fun deviceColumn(
        mac: String,
        column: String,
    ): String? =
        jdbcTemplate.queryForObject(
            "select $column from captive.captive_device where mac = ?",
            String::class.java,
            mac,
        )

    private fun scrubUsecase(): ScrubDeauthorizedCaptiveDeviceUsecase {
        val resolver =
            ClientAccessAuthorizationResolver(
                allowedMacReadPort = allowedMacReadPort,
                findAuthorizationTokenPort = repository,
                networkUserDeviceReadPort = networkUserDeviceReadPort,
                timeProvider = { Instant.parse("2026-04-30T08:52:55.800881Z") },
            )
        return ScrubDeauthorizedCaptiveDeviceUsecase(
            clientAccessAuthorizationResolver = resolver,
            captiveDevicePrivacyPort = captiveDevicePrivacyPort,
        )
    }

    private fun device(
        mac: String,
        deviceName: String,
    ) = Device(
        mac = mac,
        displayName = null,
        deviceName = deviceName,
    )

    private fun applyMigration(resourcePath: String) {
        val stream =
            javaClass.classLoader.getResourceAsStream(resourcePath)
                ?: error("Migration not found at $resourcePath")

        BufferedReader(stream.reader()).use { reader ->
            reader
                .readText()
                .split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { jdbcTemplate.execute(it) }
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CaptivePersistenceConfig::class)
    class TestApplication {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()

        @Bean
        fun captiveFingerprintingProperties() = CaptiveFingerprintingProperties()

        @Bean
        fun userAgentClassifier() = UserAgentClassifier { null }

        @Bean
        fun deviceFingerprintService(
            objectMapper: ObjectMapper,
            properties: CaptiveFingerprintingProperties,
            userAgentClassifier: UserAgentClassifier,
        ) = DeviceFingerprintService(objectMapper, properties, userAgentClassifier)
    }
}
