package cz.grimir.wifimanager.user.persistence

import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.user.application.ports.UserDirectoryRepository
import cz.grimir.wifimanager.user.application.ports.UserIdentityRecord
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.UUID

@Repository
class UserDirectoryJdbcRepository(
    private val jdbcTemplate: JdbcTemplate,
) : UserDirectoryRepository {
    override fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentityRecord? =
        jdbcTemplate
            .query(
                FIND_IDENTITY_SQL,
                { ps ->
                    ps.setString(1, issuer)
                    ps.setString(2, subject)
                },
            ) { rs, _ -> mapRecord(rs, inserted = false) }
            .firstOrNull()

    override fun createIdentity(
        identityId: UUID,
        issuer: String,
        subject: String,
        displayName: String,
        email: String,
        pictureUrl: String?,
        roles: Set<UserRole>,
        loginAt: java.time.Instant,
    ): UserIdentityRecord? {
        val loginTimestamp = Timestamp.from(loginAt)
        val userId = UUID.randomUUID()

        jdbcTemplate.update { connection ->
            connection.prepareStatement(INSERT_USER_SQL).apply {
                setObject(1, userId)
                setTimestamp(2, loginTimestamp)
                setTimestamp(3, loginTimestamp)
            }
        }

        val record =
            jdbcTemplate
                .query(
                    { connection ->
                        connection.prepareStatement(INSERT_IDENTITY_SQL).apply {
                            val roleNames = roles.map(UserRole::name).toTypedArray()
                            setObject(1, identityId)
                            setString(2, issuer)
                            setString(3, subject)
                            setObject(4, userId)
                            setString(5, displayName)
                            setString(6, email)
                            setString(7, pictureUrl)
                            setArray(8, connection.createArrayOf("text", roleNames))
                            setTimestamp(9, loginTimestamp)
                            setTimestamp(10, loginTimestamp)
                            setTimestamp(11, loginTimestamp)
                        }
                    },
                ) { rs, _ -> mapRecord(rs, inserted = true) }
                .firstOrNull()

        if (record == null) {
            jdbcTemplate.update(
                "delete from auth.app_user where user_id = ?",
                userId,
            )
        }

        return record
    }

    override fun updateIdentity(
        identityId: UUID,
        displayName: String,
        email: String,
        pictureUrl: String?,
        roles: Set<UserRole>,
        loginAt: java.time.Instant,
    ): UserIdentityRecord {
        val loginTimestamp = Timestamp.from(loginAt)

        val record =
            jdbcTemplate
                .query(
                    { connection ->
                        connection.prepareStatement(UPDATE_IDENTITY_SQL).apply {
                            val roleNames = roles.map(UserRole::name).toTypedArray()
                            setString(1, displayName)
                            setString(2, email)
                            setString(3, pictureUrl)
                            setArray(4, connection.createArrayOf("text", roleNames))
                            setTimestamp(5, loginTimestamp)
                            setTimestamp(6, loginTimestamp)
                            setObject(7, identityId)
                        }
                    },
                ) { rs, _ -> mapRecord(rs, inserted = false) }
                .firstOrNull()
                ?: error("User identity update returned no result for identityId=$identityId")

        jdbcTemplate.update(
            "update auth.app_user set last_login_at = ? where user_id = ?",
            loginTimestamp,
            record.userId,
        )

        return record
    }

    override fun updateLastLoginAt(
        identityId: UUID,
        loginAt: java.time.Instant,
    ) {
        val loginTimestamp = Timestamp.from(loginAt)

        val userId =
            jdbcTemplate
                .query(
                    { connection ->
                        connection.prepareStatement(TOUCH_IDENTITY_SQL).apply {
                            setTimestamp(1, loginTimestamp)
                            setObject(2, identityId)
                        }
                    },
                ) { rs, _ -> rs.getObject("user_id", UUID::class.java) }
                .firstOrNull()
                ?: error("User identity touch returned no result for identityId=$identityId")

        jdbcTemplate.update(
            "update auth.app_user set last_login_at = ? where user_id = ?",
            loginTimestamp,
            userId,
        )
    }

    private companion object {
        private val FIND_IDENTITY_SQL =
            """
            select
                id,
                user_id,
                issuer,
                subject,
                display_name,
                email,
                picture_url,
                roles,
                created_at,
                updated_at,
                last_login_at
            from auth.user_identity
            where issuer = ? and subject = ?
            """.trimIndent()

        private val INSERT_USER_SQL =
            """
            insert into auth.app_user (user_id, is_active, created_at, last_login_at)
            values (?, true, ?, ?)
            """.trimIndent()

        private val INSERT_IDENTITY_SQL =
            """
            insert into auth.user_identity (
                id,
                issuer,
                subject,
                user_id,
                display_name,
                email,
                picture_url,
                roles,
                created_at,
                updated_at,
                last_login_at
            )
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (issuer, subject) do nothing
            returning
                id,
                user_id,
                issuer,
                subject,
                display_name,
                email,
                picture_url,
                roles,
                created_at,
                updated_at,
                last_login_at
            """.trimIndent()

        private val UPDATE_IDENTITY_SQL =
            """
            update auth.user_identity
            set display_name = ?,
                email = ?,
                picture_url = ?,
                roles = ?,
                last_login_at = ?,
                updated_at = ?
            where id = ?
            returning
                id,
                user_id,
                issuer,
                subject,
                display_name,
                email,
                picture_url,
                roles,
                created_at,
                updated_at,
                last_login_at
            """.trimIndent()

        private val TOUCH_IDENTITY_SQL =
            """
            update auth.user_identity
            set last_login_at = ?
            where id = ?
            returning user_id
            """.trimIndent()
    }

    private fun mapRecord(
        rs: java.sql.ResultSet,
        inserted: Boolean,
    ): UserIdentityRecord {
        val roleValues =
            (rs.getArray("roles")?.array as? Array<*>)
                ?.filterIsInstance<String>()
                ?.map(UserRole::valueOf)
                ?.toSet()
                .orEmpty()

        return UserIdentityRecord(
            identityId = rs.getObject("id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            issuer = rs.getString("issuer"),
            subject = rs.getString("subject"),
            displayName = rs.getString("display_name"),
            email = rs.getString("email"),
            pictureUrl = rs.getString("picture_url"),
            roles = roleValues,
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            lastLoginAt = rs.getTimestamp("last_login_at").toInstant(),
            inserted = inserted,
        )
    }
}
