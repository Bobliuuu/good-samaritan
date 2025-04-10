package com.example.myapplication.model

import com.google.android.gms.maps.model.LatLng
import android.graphics.Color

data class RiskArea(
    val points: List<LatLng>,
    val riskLevel: RiskLevel,
    val description: String
) {
    val color: Int
        get() = when (riskLevel) {
            RiskLevel.LOW -> Color.argb(25, 0, 255, 0)
            RiskLevel.MEDIUM -> Color.argb(25, 255, 255, 0)
            RiskLevel.HIGH -> Color.argb(25, 255, 0, 0)
        }
    
    val title: String
        get() = "Area Historic Risk: ${riskLevel.displayName}"
}

enum class RiskLevel(val displayName: String, val description: String) {
    LOW("Low", "Generally safe area, normal precautions advised."),
    MEDIUM("Medium", "Exercise some caution in this area."),
    HIGH("High", "Exercise increased caution in this area.")
} 