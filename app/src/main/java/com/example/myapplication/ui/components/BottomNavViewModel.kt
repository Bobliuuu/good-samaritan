package com.example.myapplication.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

class BottomNavViewModel : ViewModel() {

    private val _navItems = MutableStateFlow<List<BottomNavItem>>(emptyList())
    val navItems: StateFlow<List<BottomNavItem>> = _navItems

    val isAdmin = mutableStateOf(false)

    fun setAdmin(value: Boolean) {
        isAdmin.value = value
        _navItems.value = if (value) {
            listOf(
                BottomNavItem("admin_home", Icons.Default.Home, "Home"),
                BottomNavItem("admin_incidents", Icons.Default.Warning, "Incidents"),
                BottomNavItem("admin_tags", Icons.Default.Settings, "Tags"),
                BottomNavItem("admin_users", Icons.Default.People, "Users")
            )
        } else {
            listOf(
                BottomNavItem("map", Icons.Filled.Home, "Map"),
                BottomNavItem("family", Icons.Filled.People, "Family"),
                BottomNavItem("alerts", Icons.Filled.Warning, "Alerts"),
                BottomNavItem("notifications", Icons.Filled.Settings, "Settings")
            )
        }
    }

}