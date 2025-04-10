package com.example.myapplication.admin.adminhome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdminHomeScreen(navController: NavController, viewModel: AdminHomeViewModel = koinViewModel()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Admin Panel", style = MaterialTheme.typography.headlineLarge)

        Button(onClick = { navController.navigate("admin_incidents") }) {
            Text("Manage Incidents")
        }

        Button(onClick = { navController.navigate("admin_tags") }) {
            Text("Manage Tags")
        }

        Button(onClick = { navController.navigate("admin_users") }) {
            Text("Manage Users")
        }
    }
}