package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.screens.notification.NotificationViewModel

@Composable
fun DropdownTagFilter(viewModel: NotificationViewModel) {

    LaunchedEffect(Unit) {
        viewModel.fetchPreferences()
    }

    var expanded by remember { mutableStateOf(false) }
    val availableTags by viewModel.availableTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()

    Box(modifier = Modifier.wrapContentSize()) {
        Button(onClick = { expanded = !expanded }) {
            Text("Tags")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(8.dp)
        ) {
            availableTags.forEach { tag ->
                val checked = selectedTags[tag] ?: false
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.toggleTag(tag) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tag)
                        }
                    },
                    onClick = { 
                        
                     }
                )
            }
        }
    }
}
