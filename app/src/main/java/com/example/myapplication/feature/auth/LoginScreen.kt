package com.example.myapplication.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AuthTextField
import com.example.myapplication.ui.components.PrimaryButton

@Composable
fun LoginScreen(onSignIn: () -> Unit, onForgotPassword: () -> Unit, onSignUp: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        // AppLogo(Modifier.size(96.dp))
        Spacer(Modifier.height(24.dp))
        Text("Hello Welcome!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))

        AuthTextField(
            "Email",
            email,
            { email = it },
            "Enter email",
            Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        AuthTextField(
            "Password",
            password,
            { password = it },
            "Enter password",
            Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", fontSize = 12.sp, color = Color.Black)
        }
        Spacer(Modifier.height(12.dp))

        PrimaryButton("Sign in", onSignIn)

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Don't have an account? ", fontSize = 12.sp)
            Text(
                "Sign up",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUp() }
            )
        }
    }
}
