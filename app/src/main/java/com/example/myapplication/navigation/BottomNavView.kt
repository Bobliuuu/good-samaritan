package com.example.myapplication.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.ui.components.BottomNavViewModel

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    viewModel: BottomNavViewModel
) {
    val navItems by viewModel.navItems.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    // Prevent navigating to the same destination multiple times
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(item.label)
                }
            )
        }
    }
}
