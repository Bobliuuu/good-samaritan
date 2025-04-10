package com.example.myapplication.screens.notification

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.lazy.grid.items

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchPreferences()
    }

    val nameState = remember { mutableStateOf(TextFieldValue(viewModel.displayName.value)) }

    LaunchedEffect(viewModel.displayName.value) {
        if (viewModel.displayName.value != nameState.value.text) {
            nameState.value = nameState.value.copy(text = viewModel.displayName.value)
        }
    }

    val radius by viewModel.radius.collectAsState()
    val relevance by viewModel.relevance.collectAsState()
    val cleanName = nameState.value.text.substringBefore("@")
    val selectedTags by viewModel.selectedTags.collectAsState()
    val tags by viewModel.availableTags.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hi, $cleanName",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        OutlinedTextField(
            value = nameState.value,
            onValueChange = {
                nameState.value = it
                viewModel.updateName(it.text)
            },
            label = { Text("Change name") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Radius (km): ${radius.toInt()}")
        Slider(
            value = radius,
            onValueChange = {
                viewModel.updateRadiusAndRelevance(it, relevance)
            },
            valueRange = 1f..100f
        )

        Text(text = "Relevance: ${relevance.toInt()}")
        Slider(
            value = relevance,
            onValueChange = {
                viewModel.updateRadiusAndRelevance(radius, it)
            },
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Event Types")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(tags) { tag ->
                val isSelected = selectedTags[tag] == true
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.toggleTag(tag) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = tag, color = if (isSelected) Color.White else Color.Black)
                }
            }
        }
    }
}
