package com.example.myapplication.screens.incidentdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.IncidentMeta
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncidentCard(meta: IncidentMeta) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(meta.name, style = MaterialTheme.typography.headlineMedium)

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Severity", style = MaterialTheme.typography.titleMedium)
                val severityColor = when (meta.severity.toIntOrNull()) {
                    1, 2 -> MaterialTheme.colorScheme.primary
                    3, 4 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    5 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(meta.severity, color = severityColor)
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Location", style = MaterialTheme.typography.titleMedium)
                Text(meta.location, style = MaterialTheme.typography.bodyMedium)
            }

            Divider()

            Column {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(meta.description)
            }

            if (meta.tags.isNotEmpty()) {
                Divider(modifier = Modifier.padding(top = 16.dp))
                Text("Tags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meta.tags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
        }
    }
}
