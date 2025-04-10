package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.Marker
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.testTag

@Composable
fun SearchBarOverlay (
    searchQuery: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    searchResults: List<Marker>,
    onResultClick: (Marker) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White)
            .padding(8.dp)
            .testTag("SearchBar")
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search for locations...") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        if (searchResults.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 4.dp)
            ) {
                searchResults.forEach { marker ->
                    Text(
                        text = marker.title ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .background(Color(0xFFF5F5F5))
                            .clickable { onResultClick(marker) }
                    )
                }
            }
        }
    }
}
