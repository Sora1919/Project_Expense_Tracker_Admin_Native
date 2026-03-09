package com.example.projectexpensetrackeradmin

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val projectId: String,
    val projectName: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val projectManager: String,
    val projectStatus: String,
    val budget: Double,
    val specialRequirements: String?,
    val clientDepartment: String?,
    val priority: String?
)