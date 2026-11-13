package com.example.myapplication.feature.expenses

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.groups.GroupExpenseItem
import com.example.myapplication.ui.components.StatusMessage
import com.example.myapplication.ui.theme.MyApplicationTheme

/** The expenses behind one balance on the Balances screen. */
data class SharedExpenses(val title: String, val totalText: String, val expenses: List<GroupExpenseItem>)

/**
 * Expense Details opened from Balances ("View details"): every expense between the
 * user and one other member of a group, which together make up the balance.
 */
@Composable
fun SharedExpensesScreen(
    shared: SharedExpenses,
    onBack: () -> Unit,
    onExpenseClick: (GroupExpenseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Expense Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(shared.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(shared.totalText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            HorizontalDivider()

            if (shared.expenses.isEmpty()) {
                StatusMessage("No shared expenses.")
            }

            shared.expenses.forEach { expense ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpenseClick(expense) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(expense.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(expense.detailText, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(expense.amountText, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SharedExpensesScreenPreview() {
    MyApplicationTheme {
        SharedExpensesScreen(
            shared = SharedExpenses(
                title = "You and Alex · Summer Trip",
                totalText = "Alex owes you 250 DKK",
                expenses = listOf(
                    GroupExpenseItem("1", "Dinner", "600 DKK", "You paid · Alex's share 150 DKK"),
                    GroupExpenseItem("2", "Taxi", "200 DKK", "You paid · Alex's share 100 DKK")
                )
            ),
            onBack = {},
            onExpenseClick = {}
        )
    }
}
