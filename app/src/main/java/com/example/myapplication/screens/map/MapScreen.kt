package com.example.myapplication.screens.map

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.myapplication.screens.notification.NotificationViewModel
import com.example.myapplication.ui.components.DropdownTagFilter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Marker
import org.koin.androidx.compose.koinViewModel
import com.example.myapplication.ui.components.SearchBarOverlay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Share

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = koinViewModel(),
    notificationViewModel: NotificationViewModel = koinViewModel()
) {
    val isMapReady by viewModel.isMapReady
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf(listOf<Marker>()) }
    val mapView = remember { MapView(context) }

    val radius by notificationViewModel.radius.collectAsState()
    val selectedTagsMap by notificationViewModel.selectedTags.collectAsState()
    val selectedTags = selectedTagsMap.filterValues { it }.keys.toSet()

    LaunchedEffect(Unit) {
        mapView.onCreate(Bundle())
        mapView.onResume()
        mapView.getMapAsync { map ->
            viewModel.onMapReady(map, navController, context, radius, selectedTags)
        }
    }

    LaunchedEffect(isMapReady, radius, selectedTags) {
        if (isMapReady) {
            viewModel.refreshFilteredMarkers(radius, selectedTags)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                actions = {
                    IconButton(onClick = {
                        val lat = viewModel.latitude.value
                        val lon = viewModel.longitude.value

                        val locationLink = "https://maps.google.com/?q=$lat,$lon"
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check my location: $locationLink")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    DropdownTagFilter(viewModel = notificationViewModel)
                },
                modifier = Modifier.testTag("TopAppBar")
            )
        }
    ) { paddingValues ->
        if (isMapReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Map view
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // Search bar overlay
                SearchBarOverlay (
                    searchQuery = searchQuery,
                    onQueryChanged = { query ->
                        searchQuery = query
                        searchResults = if (query.text.isNotBlank()) {
                            viewModel.markerList.filter {
                                it.title?.contains(query.text, ignoreCase = true) == true ||
                                        it.snippet?.contains(query.text, ignoreCase = true) == true
                            }
                        } else emptyList()
                    },
                    searchResults = searchResults,
                    onResultClick = { marker ->
                        viewModel.mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(marker.position, 12f)
                        )
                        searchResults = emptyList()
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().testTag("LoadingIndicator"), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
