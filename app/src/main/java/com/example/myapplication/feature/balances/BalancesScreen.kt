package com.example.myapplication.feature.balances

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.example.myapplication.ui.components.StatusMessage

private val CardBackground = Color(0xFFDCE0E6)
private val AvatarBackground = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF374151)

enum class BalanceDirection { OwedToYou, YouOwe }

data class BalanceEntry(val id: String, val personName: String, val amountText: String, val direction: BalanceDirection)

data class GroupBalances(val groupName: String, val entries: List<BalanceEntry>)

@Composable
fun BalancesScreen(
    totalBalanceText: String,
    groups: List<GroupBalances>,
    onRemind: (BalanceEntry) -> Unit,
    onEntryClick: (BalanceEntry) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Balances",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AvatarBackground)
                    .clickable(onClick = onProfileClick)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Your balance",
            fontSize = 12.sp,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = totalBalanceText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(36.dp))

        message?.let { StatusMessage(it) }

        groups.forEach { group ->
            Text(
                text = group.groupName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            group.entries.forEach { entry ->
                BalanceCard(
                    entry = entry,
                    onRemind = { onRemind(entry) },
                    onClick = { onEntryClick(entry) }
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BalanceCard(entry: BalanceEntry, onRemind: () -> Unit, onClick: () -> Unit) {
    val title = when (entry.direction) {
        BalanceDirection.OwedToYou -> "${entry.personName} owes you"
        BalanceDirection.YouOwe -> "You owe ${entry.personName}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.amountText,
                fontSize = 8.sp,
                color = TextSecondary
            )
        }
        when (entry.direction) {
            BalanceDirection.OwedToYou -> Text(
                text = "Remind",
                fontSize = 10.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = onRemind)
                    .padding(4.dp)
            )
            BalanceDirection.YouOwe -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextPrimary
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun BalancesScreenPreview() {
    BalancesScreen(
        totalBalanceText = "+ 150 DKK",
        groups = listOf(
            GroupBalances(
                "Summer Trip",
                listOf(
                    BalanceEntry("1", "Alex", "100 DKK", BalanceDirection.OwedToYou),
                    BalanceEntry("2", "Sarah", "150 DKK", BalanceDirection.OwedToYou)
                )
            ),
            GroupBalances(
                "Roommates",
                listOf(BalanceEntry("3", "John", "100 DKK", BalanceDirection.YouOwe))
            )
        ),
        onRemind = {},
        onEntryClick = {},
        onProfileClick = {}
    )
}
