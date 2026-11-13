package com.example.myapplication.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AuthTextField
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.StatusMessage

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSendEmail: () -> Unit,
    onEmailSent: () -> Unit,
) {
    LaunchedEffect(uiState.emailSent) {
        if (uiState.emailSent) onEmailSent()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Forgot password", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(
                "Enter your Email to get a password\nreset link over mail",
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            AuthTextField(
                "Enter Email", uiState.email, onEmailChanged, "Enter email",
                Icons.Default.Email, keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(32.dp))

            uiState.errorMessage?.let {
                StatusMessage(it)
            }

            PrimaryButton(
                "Send Email",
                onClick = onSendEmail,
                enabled = AuthValidation.isValidEmail(uiState.email) &&
                    !uiState.isLoading)
        }
        Spacer(Modifier.height(96.dp))
    }
}
