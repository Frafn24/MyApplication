
package com.example.myapplication.feature.expenses

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.StatusMessage
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.math.BigDecimal
import java.math.RoundingMode

// Represents one member's share of an expense
data class ExpenseSplit(val memberName: String, val amount: BigDecimal)

// Data displayed on the Expense Details screen
data class ExpenseDetails(
    val title: String,
    val amount: BigDecimal,
    val paidBy: String,
    val splits: List<ExpenseSplit>,
    // Only the person who added an expense can edit it.
    val canEdit: Boolean = true,
    // Only the person who paid an expense can delete it.
    val canDelete: Boolean = false
)

@Composable
fun ExpenseDetailsScreen(
    expense: ExpenseDetails,
    onBack: () -> Unit,
    onEditExpense: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteExpense: () -> Unit = {},
    isLoading: Boolean = false,
    message: String? = null
) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete expense?") },
            text = {
                Text("\"${expense.title}\" will be removed for everyone in the group, and balances are updated.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteExpense()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 24.dp,
                        top = 12.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
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
            Spacer(Modifier.height(36.dp))

            // Expense title
            Text(
                text = "Expense Title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            // Total expense amount
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${expense.amount.formatDKK()} DKK",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Who paid the expense
            Text(
                text = "Paid By",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = expense.paidBy,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            // List of members and their shares
            Text(
                text = "Split Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                expense.splits.forEach { split ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = split.memberName,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "${split.amount.formatDKK()} DKK",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            message?.let { StatusMessage(it) }

            // Edit expense button
            if (expense.canEdit) {
                Button(
                    onClick = onEditExpense,
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Edit Expense",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Delete expense button (only for the payer), asks first.
            if (expense.canDelete) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !isLoading,
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Delete Expense",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// Format amounts with two decimal places
private fun BigDecimal.formatDKK(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()

// Android Studio preview with example data
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ExpenseDetailsScreenPreview() {
    MyApplicationTheme {
        ExpenseDetailsScreen(
            expense = ExpenseDetails(
                title = "Dinner",
                amount = BigDecimal("600.00"),
                paidBy = "Alex",
                splits = listOf(
                    ExpenseSplit(
                        memberName = "You",
                        amount = BigDecimal("150.00")
                    ),
                    ExpenseSplit(
                        memberName = "Alex",
                        amount = BigDecimal("150.00")
                    ),
                    ExpenseSplit(
                        memberName = "Sarah",
                        amount = BigDecimal("150.00")
                    ),
                    ExpenseSplit(
                        memberName = "John",
                        amount = BigDecimal("150.00")
                    )
                )
            ),
            onBack = {},
            onEditExpense = {}
        )
    }
}
