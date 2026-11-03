package com.example.myapplication.data.mock

import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.Balance
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary

object MockData {
    val groups = listOf(
        GroupSummary("1", "Summer Trip", 4, "You are owed 250 DKK"),
        GroupSummary("2", "Roommates", 3, "You owe 100 DKK"),
    )

    val summerTripMembers = listOf(
        GroupMember("current-user", "You", isAdmin = true),
        GroupMember("2", "Alex"),
        GroupMember("3", "Sarah"),
        GroupMember("4", "John"),
    )

    val expenses = listOf(
        Expense("1", "1", "Dinner", 600, "Alex", listOf("You", "Alex", "Sarah", "John")),
        Expense("2", "1", "Gas", 300, "You", listOf("You", "Alex", "Sarah", "John")),
        Expense("3", "1", "Groceries", 180, "Sarah", listOf("You", "Alex", "Sarah", "John")),
    )

    val balances = listOf(
        Balance("1", "Summer Trip", "Alex owes you", 100, true),
        Balance("1", "Summer Trip", "Sarah owes you", 150, true),
        Balance("2", "Roommates", "You owe John", 100, false),
    )

    val notifications = listOf(
        AppNotification("1", "New Expense", "Alex added Dinner - 600 DKK", "10 min ago"),
        AppNotification("2", "Payment Reminder", "You owe John 100 DKK", "Yesterday"),
        AppNotification("3", "Group Updated", "Sarah joined Summer Trip", "2 days ago"),
    )
}
