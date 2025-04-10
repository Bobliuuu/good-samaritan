package com.example.myapplication.admin.admintag

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdminTagScreen(
    viewModel: AdminTagViewModel = koinViewModel()
) {
    var newTag by remember { mutableStateOf("") }
    val tags by viewModel.tags.collectAsState()

    Column(Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Manage Tags", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it },
            label = { Text("New Tag") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            if (newTag.isNotBlank()) {
                viewModel.addTag(newTag)
                newTag = ""
            }
        }, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Add Tag")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Current Tags:")

        tags.forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("• $tag")
                Button(onClick = { viewModel.deleteTag(tag) }) {
                    Text("Delete")
                }
            }
        }
    }
}