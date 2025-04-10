package com.example.myapplication.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.myapplication.R
import com.example.myapplication.model.RiskArea
import com.example.myapplication.model.RiskLevel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import kotlin.random.Random
import androidx.compose.runtime.State

open class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val markerMutableList = mutableListOf<Marker>()
    private val polygonMutableList = mutableListOf<Polygon>()
    private val riskAreaMutableList = mutableListOf<RiskArea>()

    open val markerList: List<Marker> get() = markerMutableList
    val polygons: List<Polygon> get() = polygonMutableList
    val riskAreas: List<RiskArea> get() = riskAreaMutableList

    open lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var context: Context
    private val _isMapReady = mutableStateOf(false)
    open val isMapReady: State<Boolean> = _isMapReady
    private val _latitude = mutableStateOf<Double?>(null)
    val latitude: State<Double?> = _latitude

    private val _longitude = mutableStateOf<Double?>(null)
    val longitude: State<Double?> = _longitude


    @SuppressLint("MissingPermission")
    fun onMapReady(map: GoogleMap, navController: NavController, context: Context, radiusKm: Float, selectedTags: Set<String>) {
        this.mMap = map
        this.context = context
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (!Places.isInitialized()) {
            val applicationInfo: ApplicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            Places.initialize(context, apiKey)
        }

        placesClient = Places.createClient(context)

        setupMap()

        checkLocationPermission()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                _latitude.value = location.latitude
                _longitude.value = location.longitude
                val latLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                getNearbyPlaces(latLng)

                loadIncidentsFromFirestore(
                    center = latLng,
                    radiusKm = radiusKm,
                    selectedTags = selectedTags
                )
            } else {
                Toast.makeText(context, "Failed to get current location", Toast.LENGTH_SHORT).show()
            }
        }

        mMap.setOnPolygonClickListener { polygon ->
            val riskArea = riskAreaMutableList.find { polygonMutableList.indexOf(polygon) == riskAreaMutableList.indexOf(it) }
            riskArea?.let { showRiskAreaInfo(it) }
        }

        mMap.setOnMarkerClickListener { marker ->
            val id = marker.tag as? String
            if (id != null) {
                navController.navigate("incident_detail/$id?isAdmin=false")
            } else {
                Toast.makeText(context, "Incident ID not found", Toast.LENGTH_SHORT).show()
            }
            true
        }

        _isMapReady.value = true
    }

    private fun setupMap() {
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
            isMyLocationButtonEnabled = true
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(context as? androidx.activity.ComponentActivity
                ?: return, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(context)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission to show your location on the map")
                    .setPositiveButton("OK") { _, _ -> requestLocationPermission() }
                    .create()
                    .show()
            }
            else -> requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            context as? androidx.activity.ComponentActivity
                ?: return,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    getNearbyPlaces(latLng)
                }
            }
        }
    }

    private fun getNearbyPlaces(center: LatLng) {
        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
            response.placeLikelihoods.firstOrNull()?.place?.latLng?.let {
                createNeighborhoodPolygons(it)
            }
        }.addOnFailureListener {
            createNeighborhoodPolygons(center)
        }
    }

    private fun createNeighborhoodPolygons(center: LatLng) {
        riskAreaMutableList.clear()
        polygonMutableList.forEach { it.remove() }
        polygonMutableList.clear()

        val blockSize = 0.005
        val gridSize = 5

        for (i in -gridSize..gridSize) {
            for (j in -gridSize..gridSize) {
                val blockCenter = LatLng(center.latitude + (i * blockSize), center.longitude + (j * blockSize))
                val points = listOf(
                    LatLng(blockCenter.latitude - blockSize / 2, blockCenter.longitude - blockSize / 2),
                    LatLng(blockCenter.latitude - blockSize / 2, blockCenter.longitude + blockSize / 2),
                    LatLng(blockCenter.latitude + blockSize / 2, blockCenter.longitude + blockSize / 2),
                    LatLng(blockCenter.latitude + blockSize / 2, blockCenter.longitude - blockSize / 2)
                )

                val riskLevel = when (Random.nextInt(3)) {
                    0 -> RiskLevel.LOW
                    1 -> RiskLevel.MEDIUM
                    else -> RiskLevel.HIGH
                }

                val riskArea = RiskArea(points, riskLevel, "Area Risk Assessment\n${riskLevel.description}")
                riskAreaMutableList.add(riskArea)
            }
        }

        drawRiskAreas()
    }

    private fun drawRiskAreas() {
        polygonMutableList.forEach { it.remove() }
        polygonMutableList.clear()

        riskAreaMutableList.forEach { area ->
            val polygon = mMap.addPolygon(
                PolygonOptions()
                    .addAll(area.points)
                    .fillColor(area.color)
                    .strokeColor(area.color)
                    .strokeWidth(2f)
                    .clickable(true)
            )
            polygonMutableList.add(polygon)
        }
    }

    private fun showRiskAreaInfo(riskArea: RiskArea) {
        AlertDialog.Builder(context)
            .setTitle(riskArea.title)
            .setMessage(riskArea.description)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun loadIncidentsFromFirestore(
        center: LatLng,
        radiusKm: Float,
        selectedTags: Set<String>
    ) {
        markerMutableList.forEach { it.remove() }
        markerMutableList.clear()

        db.collection("incidents").get()
            .addOnSuccessListener { result ->
                result.documents.forEach { document ->
                    val name = document.getString("name") ?: return@forEach
                    val lat = document.getDouble("lat") ?: return@forEach
                    val lon = document.getDouble("lon") ?: return@forEach
                    val severity = document.getLong("severity")?.toInt() ?: 0
                    val tags = document.get("tags") as? List<*> ?: emptyList<Any>()
                    val location = LatLng(lat, lon)

                    /*
                    // Filter by radius
                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(
                        center.latitude, center.longitude,
                        location.latitude, location.longitude,
                        distance
                    )
                    val distanceKm = distance[0] / 1000f
                    if (distanceKm > radiusKm) return@forEach
                    */
                    // Filter by tags
                    val incidentTags = tags.mapNotNull { it?.toString() }
                    if (!incidentTags.any { selectedTags.contains(it) }) return@forEach

                    // Add marker if passed both filters
                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title(name)
                            .snippet("Severity: $severity")
                    )
                    marker?.tag = document.id
                    marker?.let { markerMutableList.add(it) }
                }
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    open fun refreshFilteredMarkers(radiusKm: Float, selectedTags: Set<String>) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val center = LatLng(location.latitude, location.longitude)
                loadIncidentsFromFirestore(
                    center = center,
                    radiusKm = radiusKm,
                    selectedTags = selectedTags
                )
            }
        }
    }
}
