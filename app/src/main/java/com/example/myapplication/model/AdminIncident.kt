package com.example.myapplication.model

data class AdminIncident(
    val id: String,
    val name: String,
    val votes: Int? = 0
)
