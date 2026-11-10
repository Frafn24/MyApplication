package com.example.myapplication.feature.auth

/** Minimum password length accepted on sign-up. */
const val MIN_PASSWORD_LENGTH = 6

/**
 * Pure validation rules for the auth screens, kept outside the composables
 * so they can be unit tested.
 */
object AuthValidation {
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val at = trimmed.indexOf('@')
        return at > 0 &&
            at == trimmed.lastIndexOf('@') &&
            at < trimmed.length - 1 &&
            trimmed.none { it.isWhitespace() }
    }

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    fun canSubmitSignUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): Boolean = name.isNotBlank() &&
        isValidEmail(email) &&
        isValidPassword(password) &&
        password == confirmPassword &&
        acceptedTerms
}
