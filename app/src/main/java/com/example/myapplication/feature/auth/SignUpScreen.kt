package com.example.myapplication.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AuthTextField
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.StatusMessage

@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTermsChanged: (Boolean) -> Unit,
    onNewsChanged: (Boolean) -> Unit,
    onSignUpSubmit: () -> Unit,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit
) {
    LaunchedEffect(uiState.signUpSuccessful) {
        if (uiState.signUpSuccessful) onSignUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        Text("Sign up", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        AuthTextField("Name", uiState.name, onNameChanged, "Enter name", Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Email",
            uiState.email,
            onEmailChanged,
            "Enter email",
            Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Password",
            uiState.password,
            onPasswordChanged,
            "Enter password",
            Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Confirm Password",
            uiState.confirmPassword,
            onConfirmPasswordChanged,
            "Confirm password",
            Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )

        Spacer(Modifier.height(12.dp))
        CheckRow("I accept terms and conditions*", uiState.acceptTerms, onTermsChanged)
        CheckRow("I accept to get newsletters and offers", uiState.acceptNews, onNewsChanged)

        uiState.errorMessage?.let {
            StatusMessage(it)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign Up", {
            onSignUpSubmit()
        }, enabled = uiState.canSubmit && !uiState.isLoading)

        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Already have an account? ", fontSize = 12.sp)
            Text(
                "Sign in",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignIn() }
            )
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, fontSize = 11.sp)
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}
