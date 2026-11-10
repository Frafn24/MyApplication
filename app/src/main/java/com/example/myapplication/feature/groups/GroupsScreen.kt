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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBackground = Color(0xFFDCE0E6)
private val ButtonBackground = Color(0xFF4B5563)
private val AvatarBackground = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF374151)

data class GroupSummary(val id: String, val name: String, val memberCount: Int, val balanceText: String)

private data class BottomTab(val label: String, val icon: ImageVector)

@Composable
fun GroupsScreen(
    groups: List<GroupSummary>,
    onGroupClick: (GroupSummary) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onBalancesClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {}
) {
    val tabs = listOf(
        BottomTab("Groups", Icons.Default.Person),
        BottomTab("Balances", Icons.AutoMirrored.Filled.List),
        BottomTab("Alerts", Icons.Default.Notifications)
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == 0,
                        onClick = {
                            when (index) {
                                1 -> onBalancesClick()
                                2 -> onAlertsClick()
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
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
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(4.dp))
                Text("Create New Group", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
