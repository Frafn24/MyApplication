package com.example.myapplication.feature.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun `valid email is accepted`() {
        assertTrue(AuthValidation.isValidEmail("user@example.com"))
        assertTrue(AuthValidation.isValidEmail("  user@example.com  "))
    }

    @Test
    fun `invalid emails are rejected`() {
        listOf("", "user", "@example.com", "user@", "a@b@c", "us er@example.com").forEach {
            assertFalse("Expected '$it' to be invalid", AuthValidation.isValidEmail(it))
        }
    }

    @Test
    fun `password must have minimum length`() {
        assertFalse(AuthValidation.isValidPassword("a".repeat(MIN_PASSWORD_LENGTH - 1)))
        assertTrue(AuthValidation.isValidPassword("a".repeat(MIN_PASSWORD_LENGTH)))
    }

    @Test
    fun `sign up is allowed when all fields are valid`() {
        assertTrue(validSignUp())
    }

    @Test
    fun `sign up is blocked by any invalid field`() {
        assertFalse(validSignUp(name = "  "))
        assertFalse(validSignUp(email = "not-an-email"))
        assertFalse(validSignUp(password = "123", confirm = "123"))
        assertFalse(validSignUp(confirm = "different"))
        assertFalse(validSignUp(acceptedTerms = false))
    }

    private fun validSignUp(
        name: String = "Alice",
        email: String = "alice@example.com",
        password: String = "secret123",
        confirm: String = password,
        acceptedTerms: Boolean = true
    ) = AuthValidation.canSubmitSignUp(name, email, password, confirm, acceptedTerms)
}
