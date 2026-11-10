package com.example.myapplication.feature.auth

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginScreenNavigatesToSignUp() {
        var signUpClicked = false
        composeRule.setContent {
            LoginScreen(onSignIn = {}, onForgotPassword = {}, onSignUp = { signUpClicked = true })
        }

        composeRule.onNodeWithText("Sign up").performClick()

        assertTrue(signUpClicked)
    }

    @Test
    fun forgotPasswordButtonEnabledOnlyWithValidEmail() {
        composeRule.setContent {
            ForgotPasswordScreen(onBack = {}, onSendEmail = {})
        }

        composeRule.onNodeWithText("Send Email").assertIsNotEnabled()
        composeRule.onNodeWithText("Enter Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Send Email").assertIsEnabled()
    }
}
