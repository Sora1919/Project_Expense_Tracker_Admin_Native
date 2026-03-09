package com.example.projectexpensetrackeradmin

import androidx.room.*

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY dateOfExpense DESC")
    suspend fun getExpensesForProject(projectId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE expenseId = :id")
    suspend fun getExpenseById(id: String): Expense?

    @Query("DELETE FROM expenses WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: String)

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>
}