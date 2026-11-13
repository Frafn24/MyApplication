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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.ui.components.StatusMessage

private val CardBackground = Color(0xFFDCE0E6)
private val ButtonBackground = Color(0xFF4B5563)
private val AvatarBackground = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF374151)

@Composable
fun GroupsScreen(
    groups: List<GroupSummary>,
    isLoading: Boolean = false,
    onGroupClick: (GroupSummary) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    message: String? = null
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Groups",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AvatarBackground)
                        .clickable(onClick = onProfileClick)
                )
            }

            Spacer(Modifier.height(20.dp))

            message?.let { StatusMessage(it) }

            if (isLoading && groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                groups.forEach { group ->
                    GroupCard(group = group, onClick = { onGroupClick(group) })
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onCreateGroup,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBackground,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Create New Group",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${group.memberCount} members",
                fontSize = 10.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = group.balanceText,
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextPrimary
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun GroupsScreenPreview() {
    GroupsScreen(
        groups = listOf(
            GroupSummary("1", "Summer Trip", 4, "You are owed 250 DKK"),
            GroupSummary("2", "Roommates", 3, "You owe 100 DKK")
        ),
        onGroupClick = {},
        onCreateGroup = {}
    )
}
