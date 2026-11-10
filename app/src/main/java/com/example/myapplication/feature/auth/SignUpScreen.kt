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
fun SignUpScreen(onSignUp: () -> Unit, onSignIn: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var acceptNews by remember { mutableStateOf(false) }

    val canSubmit = AuthValidation.canSubmitSignUp(name, email, password, confirm, acceptTerms)

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

        AuthTextField("Name", name, { name = it }, "Enter name", Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Email",
            email,
            { email = it },
            "Enter email",
            Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Password",
            password,
            { password = it },
            "Enter password",
            Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            "Confirm Password",
            confirm,
            { confirm = it },
            "Confirm password",
            Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )

        Spacer(Modifier.height(12.dp))
        CheckRow("I accept terms and conditions*", acceptTerms) { acceptTerms = it }
        CheckRow("I accept to get newsletters and offers", acceptNews) { acceptNews = it }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign Up", onSignUp, enabled = canSubmit)

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
