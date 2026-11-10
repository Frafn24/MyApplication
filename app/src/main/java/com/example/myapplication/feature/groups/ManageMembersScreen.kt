package com.example.myapplication.feature.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class GroupMember(
    val id: String,
    val name: String,
    val isAdmin: Boolean = false,
)

@Composable
fun ManageMembersScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onAddMember: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val members = remember {
        mutableStateListOf(
            GroupMember("you", "You", isAdmin = true),
            GroupMember("alex", "Alex"),
            GroupMember("sarah", "Sarah"),
            GroupMember("john", "John"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = "Go back" },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Manage Members")
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.semantics { contentDescription = "Open profile" },
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(members, key = { it.id }) { member ->
                MemberRow(
                    member = member,
                    onRemove = { members.remove(member) },
                )
            }
        }

        Button(
            onClick = onAddMember,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("+ Add Member")
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFDDE1E7)),
        )
        Spacer(Modifier.width(16.dp))
        Text(member.name)
        Spacer(Modifier.weight(1f))

        if (member.isAdmin) {
            Text("Admin")
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .hoverable(interactionSource)
                    .semantics {
                        contentDescription = "Remove ${member.name}"
                    },
            ) {
                Icon(
                    imageVector = if (isHovered) Icons.Default.Close else Icons.Default.Person,
                    contentDescription = null,
                )
            }
        }
    }
}
