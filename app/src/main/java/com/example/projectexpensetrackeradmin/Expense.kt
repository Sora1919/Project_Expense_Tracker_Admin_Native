package com.example.projectexpensetrackeradmin

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["projectId"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Expense(
    @PrimaryKey
    val expenseId: String,           // Required (spec)
    val projectId: String,           // link to project
    val dateOfExpense: String,       // Required
    val amount: Double,              // Required
    val currency: String,            // Required
    val typeOfExpense: String,       // Required
    val paymentMethod: String,       // Required
    val claimant: String,            // Required
    val paymentStatus: String,       // Required
    val description: String?,        // Optional
    val location: String?            // Optional
)