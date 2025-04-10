package com.example.myapplication.screens.addincident

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import com.example.myapplication.BuildConfig
import org.koin.androidx.compose.koinViewModel
import com.example.myapplication.R
import androidx.compose.material.icons.filled.Close

@Composable
fun AddIncidentScreen(
    onIncidentAdded: () -> Unit,
    viewModel: AddIncidentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val channelId = "incident_channel"

    val tagsList by viewModel.tags.collectAsState()
    val selectedTags = remember(tagsList) {
        mutableStateMapOf<String, Boolean>().apply {
            tagsList.forEach { this[it] = false }
        }
    }
    
    val currentLatitude by viewModel.currentLatitude.collectAsState()
    val currentLongitude by viewModel.currentLongitude.collectAsState()

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "incident_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri.value != null) {
            imageUris = imageUris + cameraImageUri.value!!
        }
    }

    val apiKey = BuildConfig.OPENAI_KEY

    val predictedSeverity by viewModel.predictedSeverity.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> imageUris = uris }

    LaunchedEffect(Unit) {
        viewModel.getCurrentLocation(context)
    }
    
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            lat = currentLatitude.toString()
            lon = currentLongitude.toString()
            viewModel.updateLocationName(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val channel = NotificationChannel(channelId, "Incident Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = ""
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Incident", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lat, 
                        onValueChange = { lat = it }, 
                        label = { Text("Latitude") }, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lon, 
                        onValueChange = { lon = it }, 
                        label = { Text("Longitude") }, 
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.getCurrentLocation(context) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Location"
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                OutlinedTextField(
                    value = severity,
                    onValueChange = { severity = it },
                    label = { Text("Severity (0–10)") },
                    placeholder = { Text("$predictedSeverity") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            viewModel.getSeverityFromDescription(
                                description = description,
                                apiKey = apiKey,
                                onSeverityPredicted = { predicted ->
                                    severity = predicted.toString()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Severity")
                }

                OutlinedTextField(
                    value = currentLocationName ?: "",
                    onValueChange = {}, // read-only
                    enabled = false,
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tags", style = MaterialTheme.typography.titleMedium)
                tagsList.forEach { tag ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selectedTags[tag] == true, onCheckedChange = { selectedTags[tag] = it })
                        Text(tag)
                    }
                }

                Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pick Images")
                }

                Button(
                    onClick = {
                        val uri = createImageUri()
                        cameraImageUri.value = uri
                        uri?.let {
                            cameraLauncher.launch(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Photo")
                }

                if (imageUris.isNotEmpty()) {
                    Text("Selected Preview", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                    ) {
                        imageUris.forEach { uri ->
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        imageUris = imageUris.filterNot { it == uri }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val latValue = lat.toDoubleOrNull()
                val lonValue = lon.toDoubleOrNull()
                val sevValue = severity.toIntOrNull()

                if (name.isNotEmpty() && latValue != null && lonValue != null && sevValue != null && description.isNotEmpty()) {
                    isLoading = true
                    viewModel.uploadImagesAndSaveIncident(
                        name = name,
                        lat = latValue,
                        lon = lonValue,
                        severity = sevValue,
                        description = description,
                        selectedTags = selectedTags.filterValues { it }.keys.toList(),
                        imageUris = imageUris,
                        context = context
                    ) { success ->
                        isLoading = false
                        if (success) {
                            NotificationManagerCompat.from(context).notify(
                                System.currentTimeMillis().toInt(),
                                NotificationCompat.Builder(context, channelId)
                                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                                    .setContentTitle("Incident Added")
                                    .setContentText("Incident '$name' was successfully added.")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .build()
                            )
                            onIncidentAdded()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Add Incident")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Test Notification")
                    .setContentText("This is a test notification.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                NotificationManagerCompat.from(context).notify(999, builder.build())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Notification")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
