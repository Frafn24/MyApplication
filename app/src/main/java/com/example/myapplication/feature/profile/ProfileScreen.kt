package com.example.myapplication.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TextPrimary = Color(0xFF111827)
private val AvatarBackground = Color(0xFFE5E7EB)
private val FieldBorder = Color(0xFFD1D5DB)
private val ButtonBackground = Color(0xFFDCE0E6)

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val notificationsEnabled: Boolean,
)

@Composable
fun ProfileScreen(
    profile: UserProfile,
    onSaveChanges: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var notificationsEnabled by remember { mutableStateOf(profile.notificationsEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Profile",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AvatarBackground)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "My Profile",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(20.dp))

        ProfileField("Name", name, { name = it })
        ProfileField("Email", email, { email = it }, KeyboardType.Email)
        ProfileField("Phone", phone, { phone = it }, KeyboardType.Phone)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Enable Notifications",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = TextPrimary,
                    uncheckedBorderColor = TextPrimary,
                    uncheckedThumbColor = TextPrimary,
                    uncheckedTrackColor = Color.White,
                ),
            )
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                onSaveChanges(UserProfile(name, email, phone, notificationsEnabled))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonBackground,
                contentColor = TextPrimary,
            ),
        ) {
            Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary),
        shape = RoundedCornerShape(2.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = FieldBorder,
            focusedBorderColor = TextPrimary,
        ),
    )
    Spacer(Modifier.height(12.dp))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(
        profile = UserProfile(
            name = "John Doe",
            email = "johndoe@test.com",
            phone = "+45 12 34 56 78",
            notificationsEnabled = false,
        ),
        onSaveChanges = {},
    )
}
