package com.example.myapplication.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.example.myapplication.ui.components.AppLogo
import com.example.myapplication.ui.components.AuthTextField
import com.example.myapplication.ui.components.PrimaryButton

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit
) {
    LaunchedEffect(uiState.loginSuccessful) {
        if (uiState.loginSuccessful) onSignIn()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        //AppLogo(Modifier.size(96.dp))
        Spacer(Modifier.height(24.dp))
        Text("Hello Welcome!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))

        AuthTextField(
            "Email", uiState.email, onEmailChanged, "Enter email",
            Icons.Default.Email, keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        AuthTextField(
            "Password", uiState.password, onPasswordChanged, "Enter password",
            Icons.Default.Lock, isPassword = true, keyboardType = KeyboardType.Password
        )

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", fontSize = 12.sp, color = Color.Black)
        }
        Spacer(Modifier.height(12.dp))

        PrimaryButton("Sign in", {
            onLogin()
        }, enabled = !uiState.isLoading)

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Don't have an account? ", fontSize = 12.sp)
            Text(
                "Sign up", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUp() }
            )
        }
    }
}
