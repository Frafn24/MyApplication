package com.example.myapplication.feature.groups

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF374151)

/** One row in a group's expense list. */
data class GroupExpenseItem(val id: String, val title: String, val amountText: String, val detailText: String)

data class GroupDetails(
    val id: String,
    val name: String,
    val description: String,
    val balanceText: String,
    val expenses: List<GroupExpenseItem>
)

@Composable
fun GroupDetailsScreen(
    group: GroupDetails,
    onBack: () -> Unit,
    onExpenseClick: (GroupExpenseItem) -> Unit,
    onAddExpense: () -> Unit,
    onManageMembers: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(group.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (group.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(group.description, fontSize = 12.sp, color = TextSecondary)
        }

        Spacer(Modifier.height(16.dp))
        Text(group.balanceText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                Text("+ Add Expense")
            }
            OutlinedButton(onClick = onManageMembers, modifier = Modifier.weight(1f)) {
                Text("Members")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Expenses", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        message?.let { StatusMessage(it) }

        if (group.expenses.isEmpty() && message == null) {
            StatusMessage("No expenses yet.")
        }

        group.expenses.forEach { expense ->
            ExpenseCard(expense = expense, onClick = { onExpenseClick(expense) })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExpenseCard(expense: GroupExpenseItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(expense.detailText, fontSize = 10.sp, color = TextSecondary)
        }
        Text(expense.amountText, fontSize = 12.sp, color = TextPrimary)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextPrimary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun GroupDetailsScreenPreview() {
    GroupDetailsScreen(
        group = GroupDetails(
            id = "1",
            name = "Summer Trip",
            description = "Beach house in July",
            balanceText = "You are owed 250 DKK",
            expenses = listOf(
                GroupExpenseItem("1", "Dinner", "600 DKK", "Alex paid · your share 150 DKK"),
                GroupExpenseItem("2", "Groceries", "320 DKK", "You paid · your share 80 DKK")
            )
        ),
        onBack = {},
        onExpenseClick = {},
        onAddExpense = {},
        onManageMembers = {}
    )
}
