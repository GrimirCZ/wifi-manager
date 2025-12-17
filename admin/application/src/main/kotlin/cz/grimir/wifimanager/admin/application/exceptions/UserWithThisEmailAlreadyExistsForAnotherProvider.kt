package cz.grimir.wifimanager.admin.application.exceptions

/**
 * Thrown when a new login (issuer+subject) would create a user with an email that already belongs to
 * another existing local user (e.g. created from a different provider).
 *
 * This is checked pre-insert to avoid relying on the DB unique constraint for control flow.
 */
class UserWithThisEmailAlreadyExistsForAnotherProvider(
    val email: String,
) : RuntimeException("User with email '$email' already exists for another provider")
