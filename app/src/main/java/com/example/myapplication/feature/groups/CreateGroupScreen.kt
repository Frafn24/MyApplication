package com.example.myapplication.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.auth.AuthValidation
import com.example.myapplication.ui.components.StatusMessage

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    // members: the email addresses entered under "Group Members".
    onCreateGroup: (title: String, description: String, members: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    message: String? = null
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var newMember by rememberSaveable { mutableStateOf("") }
    val membersSaver = listSaver<SnapshotStateList<String>, String>(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
    val members = rememberSaveable(saver = membersSaver) { mutableStateListOf<String>() }

    fun addMember() {
        val member = newMember.trim()
        if (AuthValidation.isValidEmail(member) && members.none { it.equals(member, ignoreCase = true) }) {
            members.add(member)
        }
        newMember = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Go back"
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Create Group")
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDE1E7))
                    .clickable(onClick = onProfileClick)
                    .semantics { contentDescription = "Open profile" }
            )
        }

        Spacer(Modifier.height(36.dp))

        Text("Title")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g. Summer Trip") }
        )

        Spacer(Modifier.height(18.dp))

        Text("Description")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            placeholder = { Text("What is this group for?") }
        )

        Spacer(Modifier.height(18.dp))

        Text("Group Members")
        Spacer(Modifier.height(8.dp))
        members.forEach { member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(member, modifier = Modifier.weight(1f))
                IconButton(onClick = { members.remove(member) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove $member")
                }
            }
        }
        OutlinedTextField(
            value = newMember,
            onValueChange = { newMember = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { addMember() },
            modifier = Modifier.fillMaxWidth(),
            enabled = AuthValidation.isValidEmail(newMember)
        ) {
            Text("+ Add Member")
        }

        Spacer(Modifier.height(24.dp))

        message?.let { StatusMessage(it) }

        Button(
            onClick = {
                // A member typed but not yet added is included too.
                addMember()
                onCreateGroup(title, description, members.toList())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && !isLoading
        ) {
            Text("Create Group")
        }
    }
}
