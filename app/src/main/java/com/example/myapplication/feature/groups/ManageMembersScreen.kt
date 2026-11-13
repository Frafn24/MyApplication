package com.example.myapplication.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.auth.AuthValidation
import com.example.myapplication.ui.components.StatusMessage

data class GroupMember(val id: String, val name: String, val isAdmin: Boolean = false, val isMe: Boolean = false)

/**
 * [canManage]: the signed-in user is an admin, so they can add members and
 * remove members who are not admins. [onAddMember] gets an email address.
 */
@Composable
fun ManageMembersScreen(
    members: List<GroupMember>,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onAddMember: (email: String) -> Unit,
    onRemoveMember: (GroupMember) -> Unit,
    modifier: Modifier = Modifier,
    canManage: Boolean = members.any { it.isMe && it.isAdmin },
    isLoading: Boolean = false,
    message: String? = null
) {
    var newMember by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                    modifier = Modifier.semantics { contentDescription = "Go back" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Manage Members")
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.semantics { contentDescription = "Open profile" }
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(members, key = { it.id }) { member ->
                MemberRow(
                    member = member,
                    canRemove = canManage && !member.isAdmin && !isLoading,
                    onRemove = { onRemoveMember(member) }
                )
            }
        }

        message?.let { StatusMessage(it) }

        if (canManage) {
            OutlinedTextField(
                value = newMember,
                onValueChange = { newMember = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    onAddMember(newMember.trim())
                    newMember = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = AuthValidation.isValidEmail(newMember) && !isLoading
            ) {
                Text("+ Add Member")
            }
        }
    }
}

@Composable
private fun MemberRow(member: GroupMember, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFDDE1E7))
        )
        Spacer(Modifier.width(16.dp))
        Text(if (member.isMe) "${member.name} (you)" else member.name)
        Spacer(Modifier.weight(1f))

        if (member.isAdmin) {
            Text("Admin")
        } else if (canRemove) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .hoverable(interactionSource)
                    .semantics {
                        contentDescription = "Remove ${member.name}"
                    }
            ) {
                Icon(
                    imageVector = if (isHovered) Icons.Default.Close else Icons.Default.Person,
                    contentDescription = null
                )
            }
        }
    }
}
