package com.example.projectexpensetrackeradmin

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await


class CloudSyncRtdbService(
    private val root: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    /**
     * Upload ALL projects (and their expenses) in ONE upload operation using updateChildren().
     * This matches: "all projects can be uploaded at once".  (Coursework feature e)
     */
    suspend fun uploadAllProjects(db: AppDatabase) {
        val projects = db.projectDao().getAllProjects()

        // Build one big update map so it's "upload at once"
        val updates = HashMap<String, Any?>()

        for (p in projects) {
            val projectPath = "projects/${p.projectId}"
            updates["$projectPath/projectId"] = p.projectId
            updates["$projectPath/projectName"] = p.projectName
            updates["$projectPath/description"] = p.description
            updates["$projectPath/startDate"] = p.startDate
            updates["$projectPath/endDate"] = p.endDate
            updates["$projectPath/projectManager"] = p.projectManager
            updates["$projectPath/projectStatus"] = p.projectStatus
            updates["$projectPath/budget"] = p.budget
            updates["$projectPath/specialRequirements"] = p.specialRequirements
            updates["$projectPath/clientDepartment"] = p.clientDepartment
            updates["$projectPath/priority"] = p.priority
            updates["$projectPath/updatedAt"] = System.currentTimeMillis()

            val expenses = db.expenseDao().getExpensesForProject(p.projectId)
            for (e in expenses) {
                val expPath = "$projectPath/expenses/${e.expenseId}"
                updates["$expPath/expenseId"] = e.expenseId
                updates["$expPath/projectId"] = e.projectId
                updates["$expPath/dateOfExpense"] = e.dateOfExpense
                updates["$expPath/amount"] = e.amount
                updates["$expPath/currency"] = e.currency
                updates["$expPath/typeOfExpense"] = e.typeOfExpense
                updates["$expPath/paymentMethod"] = e.paymentMethod
                updates["$expPath/claimant"] = e.claimant
                updates["$expPath/paymentStatus"] = e.paymentStatus
                updates["$expPath/description"] = e.description
                updates["$expPath/location"] = e.location
                updates["$expPath/updatedAt"] = System.currentTimeMillis()
            }
        }

        // One request -> upload everything at once
        root.updateChildren(updates).await()
    }

    // Optional helpers if you later do auto-sync
    suspend fun deleteProject(projectId: String) {
        root.child("projects").child(projectId).removeValue().await()
    }

    suspend fun deleteExpense(projectId: String, expenseId: String) {
        root.child("projects").child(projectId)
            .child("expenses").child(expenseId)
            .removeValue().await()
    }
}