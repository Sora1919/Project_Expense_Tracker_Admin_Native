package com.example.projectexpensetrackeradmin

data class ProjectSummary(
    val projectId: String,
    val projectName: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val budget: Double,
    val spentInCurrency: Double,
    val currency: String
)