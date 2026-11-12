
package com.example.myapplication.feature.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.math.BigDecimal
import java.math.RoundingMode

data class ExpenseDraft(val title: String, val amount: BigDecimal, val paidBy: String, val splitBetween: List<String>)

@Composable
fun AddExpenseScreen(
    members: List<String>,
    onBack: () -> Unit,
    onAddExpense: (ExpenseDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidBy by remember(members) {
        mutableStateOf(members.firstOrNull().orEmpty())
    }
    var selectedMembers by remember(members) {
        mutableStateOf(members.toSet())
    }
    var payerMenuOpen by remember { mutableStateOf(false) }

    val amount = amountText.replace(',', '.').toBigDecimalOrNull()
    val validAmount = amount != null && amount > BigDecimal.ZERO

    val validForm = title.isNotBlank() &&
        validAmount &&
        paidBy.isNotBlank() &&
        selectedMembers.isNotEmpty()

    val splitAmount = if (validAmount && selectedMembers.isNotEmpty()) {
        amount!!.divide(
            BigDecimal(selectedMembers.size),
            2,
            RoundingMode.HALF_UP
        )
    } else {
        BigDecimal.ZERO
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Add Expense",
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Dinner") },
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )

            // Amount
            Text(
                text = "Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all {
                            it.isDigit() || it == '.' || it == ','
                        }
                    ) {
                        amountText = input
                    }
                },
                placeholder = { Text("600") },
                suffix = { Text("DKK") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )

            // Paid by dropdown
            Text(
                text = "Paid By",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedButton(
                    onClick = { payerMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape
                ) {
                    Text(
                        text = paidBy.ifBlank { "Choose payer" },
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Choose payer"
                    )
                }

                DropdownMenu(
                    expanded = payerMenuOpen,
                    onDismissRequest = {
                        payerMenuOpen = false
                    }
                ) {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member) },
                            onClick = {
                                paidBy = member
                                payerMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Split between members
            Text(
                text = "Split Between",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column {
                members.forEach { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = member in selectedMembers,
                            onCheckedChange = { checked ->
                                selectedMembers = if (checked) {
                                    selectedMembers + member
                                } else {
                                    selectedMembers - member
                                }
                            }
                        )

                        Text(member)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Automatically calculated split
            Text(
                text = "Split equally between ${selectedMembers.size} people"
            )

            Text(
                text = "${splitAmount.toPlainString()} DKK per person",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Add expense button
            Button(
                onClick = {
                    if (validForm) {
                        onAddExpense(
                            ExpenseDraft(
                                title = title.trim(),
                                amount = amount!!,
                                paidBy = paidBy,
                                splitBetween = members.filter {
                                    it in selectedMembers
                                }
                            )
                        )
                    }
                },
                enabled = validForm,
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Add Expense",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// Preview in Android Studio
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun AddExpenseScreenPreview() {
    MyApplicationTheme {
        AddExpenseScreen(
            members = listOf(
                "You",
                "Alex",
                "Sarah",
                "John"
            ),
            onBack = {},
            onAddExpense = {}
        )
    }
}
