package com.example.myapplication.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Selected = Color(0xFF111827)
private val SelectedIndicator = Color(0xFFDCE0E6)
private val Unselected = Color(0xFF9CA3AF)
private val Divider = Color(0xFFE5E7EB)

/** The main screens reachable from the bottom bar on every signed-in screen. */
enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    Groups(Routes.Groups, "Groups", Icons.Default.Person),
    Balances(Routes.Balances, "Balances", Icons.AutoMirrored.Filled.List),
    Alerts(Routes.Notifications, "Alerts", Icons.Default.Notifications),
    Profile(Routes.Profile, "Profile", Icons.Default.Settings);

    companion object {
        /** The tab for [route], or null on screens that are not a tab (then no tab is highlighted). */
        fun forRoute(route: String?): BottomTab? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Bottom bar shown on every screen after sign-in. [selected] is highlighted with a
 * dark icon, bold label and a grey pill behind the icon; null highlights nothing.
 */
@Composable
fun AppBottomBar(selected: BottomTab?, onTabClick: (BottomTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider(color = Divider)
        NavigationBar(containerColor = Color.White) {
            BottomTab.entries.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabClick(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Selected,
                        selectedTextColor = Selected,
                        indicatorColor = SelectedIndicator,
                        unselectedIconColor = Unselected,
                        unselectedTextColor = Unselected
                    )
                )
            }
        }
    }
}
