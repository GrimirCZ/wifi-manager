package cz.grimir.wifimanager.admin.application.exceptions

/**
 * Thrown when a new login (issuer+subject) would create a user with an email that already belongs to
 * another existing local user (e.g. created from a different provider).
 *
 * Currently the system does not support users having multiple identities with the same email from different providers.
 */
class UserWithThisEmailAlreadyExistsForAnotherProvider(
    val email: String,
) : RuntimeException("User with email '$email' already exists for another provider")
