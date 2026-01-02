package cz.grimir.wifimanager.user.persistence

import cz.grimir.wifimanager.shared.core.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.BufferedReader
import java.time.Instant
import java.util.UUID

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDirectoryJdbcRepositoryTest {
    companion object {
        @Container
        @JvmField
        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("wifimanager")
                .withUsername("test")
                .withPassword("test")!!
    }

    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var repository: UserDirectoryJdbcRepository

    @BeforeAll
    fun setupDatabase() {
        val dataSource =
            DriverManagerDataSource().apply {
                setDriverClassName("org.postgresql.Driver")
                url = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
            }
        jdbcTemplate = JdbcTemplate(dataSource)
        repository = UserDirectoryJdbcRepository(jdbcTemplate)
        applyMigration("db/migration/auth/V1__create_user_directory.sql")
    }

    @BeforeEach
    fun resetData() {
        jdbcTemplate.execute("truncate auth.user_identity, auth.app_user cascade")
    }

    @Test
    fun `createIdentity inserts user and identity`() {
        val identityId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val loginAt = Instant.parse("2025-01-01T12:00:00Z")

        val record =
            repository.createIdentity(
                identityId = identityId,
                issuer = "issuer",
                subject = "subject",
                displayName = "Alice",
                email = "alice@example.com",
                pictureUrl = null,
                roles = setOf(UserRole.WIFI_STAFF),
                loginAt = loginAt,
            )

        assertNotNull(record)
        assertTrue(record!!.inserted)
        assertEquals(identityId, record.identityId)
        assertEquals(1, countRows("auth.app_user"))
        assertEquals(1, countRows("auth.user_identity"))
    }

    @Test
    fun `createIdentity returns null on conflict and leaves no orphaned user`() {
        val identityId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val loginAt = Instant.parse("2025-01-01T12:00:00Z")

        repository.createIdentity(
            identityId = identityId,
            issuer = "issuer",
            subject = "subject",
            displayName = "Alice",
            email = "alice@example.com",
            pictureUrl = null,
            roles = setOf(UserRole.WIFI_STAFF),
            loginAt = loginAt,
        )

        val conflict =
            repository.createIdentity(
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000011"),
                issuer = "issuer",
                subject = "subject",
                displayName = "Alice Updated",
                email = "alice@example.com",
                pictureUrl = null,
                roles = setOf(UserRole.WIFI_ADMIN),
                loginAt = loginAt,
            )

        assertNull(conflict)
        assertEquals(1, countRows("auth.app_user"))
        assertEquals(1, countRows("auth.user_identity"))
    }

    @Test
    fun `updateIdentity updates fields and last login`() {
        val identityId = UUID.fromString("00000000-0000-0000-0000-000000000020")
        val loginAt = Instant.parse("2025-01-01T12:00:00Z")
        val updatedAt = Instant.parse("2025-01-01T13:00:00Z")

        val inserted =
            repository.createIdentity(
                identityId = identityId,
                issuer = "issuer",
                subject = "subject",
                displayName = "Alice",
                email = "alice@example.com",
                pictureUrl = null,
                roles = setOf(UserRole.WIFI_STAFF),
                loginAt = loginAt,
            ) ?: error("Insert failed")

        val updated =
            repository.updateIdentity(
                identityId = identityId,
                displayName = "Alice Doe",
                email = "alice.doe@example.com",
                pictureUrl = "https://example.com/avatar.png",
                roles = setOf(UserRole.WIFI_ADMIN),
                loginAt = updatedAt,
            )

        assertEquals("Alice Doe", updated.displayName)
        assertEquals("alice.doe@example.com", updated.email)
        assertEquals("https://example.com/avatar.png", updated.pictureUrl)
        assertEquals(setOf(UserRole.WIFI_ADMIN), updated.roles)
        assertEquals(updatedAt, updated.lastLoginAt)
        assertEquals(updatedAt, updated.updatedAt)

        val userLoginAt =
            jdbcTemplate
                .queryForObject(
                    "select last_login_at from auth.app_user where user_id = ?",
                    java.sql.Timestamp::class.java,
                    inserted.userId,
                )?.toInstant()
        assertEquals(updatedAt, userLoginAt)
    }

    @Test
    fun `updateLastLoginAt only touches login timestamps`() {
        val identityId = UUID.fromString("00000000-0000-0000-0000-000000000030")
        val loginAt = Instant.parse("2025-01-01T12:00:00Z")
        val newLoginAt = Instant.parse("2025-01-01T14:00:00Z")

        repository.createIdentity(
            identityId = identityId,
            issuer = "issuer",
            subject = "subject",
            displayName = "Alice",
            email = "alice@example.com",
            pictureUrl = null,
            roles = setOf(UserRole.WIFI_STAFF),
            loginAt = loginAt,
        )

        val before = repository.findByIssuerAndSubject("issuer", "subject") ?: error("Missing identity")

        repository.updateLastLoginAt(identityId, newLoginAt)

        val after = repository.findByIssuerAndSubject("issuer", "subject") ?: error("Missing identity")
        assertEquals(before.displayName, after.displayName)
        assertEquals(before.email, after.email)
        assertEquals(before.updatedAt, after.updatedAt)
        assertEquals(newLoginAt, after.lastLoginAt)

        val userLoginAt =
            jdbcTemplate
                .queryForObject(
                    "select last_login_at from auth.app_user where user_id = ?",
                    java.sql.Timestamp::class.java,
                    before.userId,
                )?.toInstant()
        assertEquals(newLoginAt, userLoginAt)
    }

    @Test
    fun `findByIssuerAndSubject returns null when missing`() {
        val record = repository.findByIssuerAndSubject("missing", "user")
        assertNull(record)
    }

    private fun countRows(table: String): Int = jdbcTemplate.queryForObject("select count(*) from $table", Int::class.java) ?: 0

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
}
