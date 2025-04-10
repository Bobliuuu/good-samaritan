package com.example.myapplication.model

data class IncidentMeta(
    val id: String,
    val name: String,
    val severity: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val location: String
)