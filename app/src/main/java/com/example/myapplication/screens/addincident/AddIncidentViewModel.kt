package com.example.myapplication.screens.addincident

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.viewModelScope
import java.util.Locale
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AddIncidentViewModel(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ViewModel() {

    val uploadedImageUrls = MutableStateFlow<List<String>>(emptyList())
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags

    private val _currentLatitude = MutableStateFlow<Double?>(null)
    val currentLatitude: StateFlow<Double?> = _currentLatitude
    
    private val _currentLongitude = MutableStateFlow<Double?>(null)
    val currentLongitude: StateFlow<Double?> = _currentLongitude

    private val _currentLocationName = MutableStateFlow<String?>(null)
    val currentLocationName: StateFlow<String?> = _currentLocationName

    private val _predictedSeverity = MutableStateFlow<Int>(0)
    val predictedSeverity: StateFlow<Int> = _predictedSeverity

    init {
        loadTags()
    }

    private fun loadTags() {
        FirebaseFirestore.getInstance().collection("tags")
            .get()
            .addOnSuccessListener { result ->
                _tags.value = result.documents.mapNotNull { it.getString("name") }
            }
    }

    fun getCurrentLocation(context: Context) {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        _currentLatitude.value = it.latitude
                        _currentLongitude.value = it.longitude
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateLocationName(context: Context) {
        val lat = _currentLatitude.value
        val lon = _currentLongitude.value
        if (lat != null && lon != null) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    _currentLocationName.value = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown"
                }
            } catch (e: Exception) {
                _currentLocationName.value = "Unknown"
            }
        }
    }

    private suspend fun fetchSeverityFromLLM(description: String, apiKey: String): Int = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()

        val json = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an assistant that returns only one number from 0 to 10 indicating severity.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "How severe is this incident: \"$description\"?")
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(json.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        try {

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: run {
                Log.e("LLM", "Empty response body")
                return@withContext 0
            }

            Log.d("LLM", "Raw response: $responseBody")

            val jsonObject = JSONObject(responseBody)

            if (jsonObject.has("choices")) {
                val content = jsonObject
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                Log.d("LLM", "Returned response: $content")

                val severity = content.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 10) ?: 0
                Log.d("LLM", "Parsed severity: $severity")

                return@withContext severity
            } else {
                Log.e("LLM", "Missing 'choices' in response")
                return@withContext 0
            }

        } catch (e: Exception) {
            Log.e("LLM", "Error during OpenAI call", e)
            return@withContext 0
        }
    }

    fun getSeverityFromDescription(
        description: String,
        apiKey: String,
        onSeverityPredicted: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val result = fetchSeverityFromLLM(description, apiKey)
            _predictedSeverity.value = result
            onSeverityPredicted(result)
        }
    }

    fun uploadImagesAndSaveIncident(
        name: String,
        lat: Double,
        lon: Double,
        severity: Int,
        description: String,
        selectedTags: List<String>,
        imageUris: List<Uri>,
        context: Context,
        onComplete: (success: Boolean) -> Unit
    ) {
        if (auth.currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }

        val userId = auth.currentUser!!.uid
        val uploadedUrls = mutableListOf<String>()

        if (imageUris.isEmpty()) {
            saveIncident(
                name, lat, lon, severity, description, selectedTags,
                uploadedUrls, userId, context, onComplete
            )
            return
        }

        var completed = 0
        val total = imageUris.size

        imageUris.forEachIndexed { index, uri ->
            val filename = "incidents/$userId/${System.currentTimeMillis()}_$index.jpg"
            val ref = storage.reference.child(filename)

            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->
                    uploadedUrls.add(downloadUrl.toString())
                    completed++
                    if (completed == total) {
                        uploadedImageUrls.value = uploadedUrls
                        saveIncident(
                            name, lat, lon, severity, description, selectedTags,
                            uploadedUrls, userId, context, onComplete
                        )
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == total) {
                        uploadedImageUrls.value = uploadedUrls
                        saveIncident(
                            name, lat, lon, severity, description, selectedTags,
                            uploadedUrls, userId, context, onComplete
                        )
                    }
                }
        }
    }

    private fun saveIncident(
        name: String,
        lat: Double,
        lon: Double,
        severity: Int,
        description: String,
        selectedTags: List<String>,
        imageUrls: List<String>,
        userId: String,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        val incident = hashMapOf(
            "name" to name,
            "lat" to lat,
            "lon" to lon,
            "severity" to severity,
            "description" to description,
            "tags" to selectedTags,
            "images" to imageUrls,
            "votes" to 0,
            "location" to (_currentLocationName.value ?: "Canada")
        )

        db.collection("incidents").add(incident)
            .addOnSuccessListener {
                db.collection("settings").document(userId)
                    .collection("preferences").document("filters")
                    .set(mapOf("tags" to selectedTags), SetOptions.merge())
                Toast.makeText(context, "Incident added successfully", Toast.LENGTH_SHORT).show()
                onComplete(true)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add incident", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
    }

    fun voteOnIncident(incidentId: String, userId: String, voteValue: Int, onComplete: (Boolean) -> Unit) {
        val incidentRef = db.collection("incidents").document(incidentId)
        val userVoteRef = incidentRef.collection("votes").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(incidentRef)
            val currentVotes = snapshot.getLong("votes") ?: 0

            val userVoteDoc = transaction.get(userVoteRef)
            if (userVoteDoc.exists()) {
                throw Exception("User has already voted")
            }

            transaction.update(incidentRef, "votes", currentVotes + voteValue)
            transaction.set(userVoteRef, mapOf("value" to voteValue))
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

}
