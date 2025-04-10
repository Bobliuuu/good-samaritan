package com.example.myapplication.admin.adminincident

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdminIncidentScreen(
    navController: NavHostController,
    viewModel: AdminIncidentViewModel = koinViewModel()
) {
    val incidents by viewModel.incidents.collectAsState()

    Column(Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
    ) {
        Text("Manage Incidents", style = MaterialTheme.typography.headlineMedium)

        incidents.forEach { incident ->
            val darkGreen = Color(0xFF228B22)

            val textColor = if ((incident.votes ?: 0) <= -10) Color.Red else darkGreen

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    navController.navigate("incident_detail/${incident.id}?isAdmin=true")
                }) {
                    Text(incident.name, color = textColor)
                }

                Button(onClick = { viewModel.deleteIncident(incident.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}


