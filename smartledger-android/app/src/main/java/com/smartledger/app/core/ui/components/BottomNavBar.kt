package com.smartledger.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartledger.app.core.navigation.AppRoute

/**
 * Bottom navigation bar with 5 main tabs.
 * Tab order: Dashboard | Parties | Orders | Finance | More
 *
 * @param currentRoute The currently selected route
 * @param onNavigate Callback invoked when a tab is selected
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) }
            )
        }
    }
}

/**
 * Definition of all bottom navigation tabs.
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(
        route = AppRoute.DASHBOARD,
        label = "Dashboard",
        icon = Icons.Default.Dashboard
    ),
    PARTIES(
        route = AppRoute.PARTIES,
        label = "Parties",
        icon = Icons.Default.Group
    ),
    ORDERS(
        route = AppRoute.ORDERS,
        label = "Orders",
        icon = Icons.Default.ShoppingBag
    ),
    FINANCE(
        route = AppRoute.FINANCE,
        label = "Finance",
        icon = Icons.Default.AccountBalance
    ),
    MORE(
        route = AppRoute.MORE,
        label = "More",
        icon = Icons.Default.MoreHoriz
    )
}
