package com.example.projectexpensetrackeradmin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectexpensetrackeradmin.databinding.ActivityExpenseListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ExpenseListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_PROJECT_NAME = "extra_project_name"
    }

    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var adapter: ExpenseAdapter
    private lateinit var projectId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME).orEmpty()
        binding.tvHeader.text = "Expenses • $projectName"

        adapter = ExpenseAdapter(
            onEdit = { expense ->
                val i = Intent(this, AddExpenseActivity::class.java)
                i.putExtra(AddExpenseActivity.EXTRA_PROJECT_ID, projectId)
                i.putExtra(AddExpenseActivity.EXTRA_EXPENSE_ID, expense.expenseId)
                startActivity(i)
            },
            onDelete = { expense -> confirmDelete(expense) }
        )

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter

        binding.btnAddExpense.setOnClickListener {
            val i = Intent(this, AddExpenseActivity::class.java)
            i.putExtra(AddExpenseActivity.EXTRA_PROJECT_ID, projectId)
            startActivity(i)
        }

        setupSyncFromCloud(projectId)
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@ExpenseListActivity)
                    .expenseDao()
                    .getExpensesForProject(projectId)
            }
            adapter.submitList(list)
        }
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete expense?")
            .setMessage("Delete ${expense.expenseId} (${expense.typeOfExpense})?")
            .setPositiveButton("Delete") { _, _ -> deleteExpense(expense) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@ExpenseListActivity).expenseDao().delete(expense)
            }
            loadExpenses()
        }
    }

    private fun setupSyncFromCloud(projectId: String) {
        binding.btnSyncFromCloud.setOnClickListener {

            // 1) network check
            if (!NetworkUtils.isOnline(this)) {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Syncing...")
                .setMessage("Fetching expenses from cloud...")
                .setCancelable(false)
                .create()
            dialog.show()

            lifecycleScope.launch {
                try {
                    // 2) Ensure Firebase auth
                    ensureAnonymousAuth()

                    // 3) Fetch from RTDB
                    val fetched = fetchExpensesFromCloud(projectId)

                    // 4) Upsert into Room
                    val db = AppDatabase.getInstance(this@ExpenseListActivity)
                    val (inserted, updated) = withContext(Dispatchers.IO) {
                        upsertExpensesIntoRoom(db, fetched)
                    }

                    dialog.dismiss()

                    // 5) Refresh UI
                    loadExpenses() // your existing function that reloads RecyclerView

                    Toast.makeText(
                        this@ExpenseListActivity,
                        "Sync complete! Inserted: $inserted, Updated: $updated",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (e: Exception) {
                    dialog.dismiss()
                    Toast.makeText(this@ExpenseListActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun ensureAnonymousAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    /**
     * Reads: projects/{projectId}/expenses
     */
    private suspend fun fetchExpensesFromCloud(projectId: String): List<Expense> {
        val ref = FirebaseDatabase.getInstance()
            .reference
            .child("projects")
            .child(projectId)
            .child("expenses")

        val snapshot = ref.get().await()

        val list = mutableListOf<Expense>()

        for (child in snapshot.children) {
            // expenseId could be stored as field or key
            val expenseId = child.child("expenseId").getValue(String::class.java)
                ?: child.key
                ?: continue

            val date = child.child("dateOfExpense").getValue(String::class.java) ?: ""
            val currency = child.child("currency").getValue(String::class.java) ?: ""
            val type = child.child("typeOfExpense").getValue(String::class.java) ?: ""
            val method = child.child("paymentMethod").getValue(String::class.java) ?: ""
            val claimant = child.child("claimant").getValue(String::class.java) ?: ""
            val status = child.child("paymentStatus").getValue(String::class.java) ?: ""

            // amount can come back as Double or Long depending on how it was written
            val amountDouble = child.child("amount").getValue(Double::class.java)
            val amountLong = child.child("amount").getValue(Long::class.java)
            val amount = amountDouble ?: amountLong?.toDouble() ?: 0.0

            val description = child.child("description").getValue(String::class.java)
            val location = child.child("location").getValue(String::class.java)

            // Skip clearly broken entries (optional safety)
            if (date.isBlank() || currency.isBlank() || type.isBlank() || claimant.isBlank()) continue

            list.add(
                Expense(
                    expenseId = expenseId,
                    projectId = projectId,
                    dateOfExpense = date,
                    amount = amount,
                    currency = currency,
                    typeOfExpense = type,
                    paymentMethod = method,
                    claimant = claimant,
                    paymentStatus = status,
                    description = description,
                    location = location
                )
            )
        }

        return list
    }

    /**
     * Insert if not exists, otherwise update.
     */
    private suspend fun upsertExpensesIntoRoom(db: AppDatabase, expenses: List<Expense>): Pair<Int, Int> {
        val dao = db.expenseDao()

        var inserted = 0
        var updated = 0

        for (e in expenses) {
            val existing = dao.getExpenseById(e.expenseId)
            if (existing == null) {
                dao.insert(e)
                inserted++
            } else {
                dao.update(e)
                updated++
            }
        }

        return inserted to updated
    }
}