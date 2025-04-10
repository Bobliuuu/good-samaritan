package com.example.myapplication.model

import com.google.android.gms.maps.model.LatLng

// Data class to store marker details
data class MarkerData(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val severity: Int = 0,
    val description: String = ""
) {
    fun toLatLng(): LatLng = LatLng(lat, lon)
}