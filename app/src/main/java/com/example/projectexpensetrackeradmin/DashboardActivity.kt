package com.example.projectexpensetrackeradmin

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectexpensetrackeradmin.databinding.ActivityDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: ProjectSummaryAdapter

    private val df = DecimalFormat("#,###.##")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProjectSummaryAdapter { summary ->
            val i = Intent(this, ProjectDetailActivity::class.java)
            i.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, summary.projectId)
            startActivity(i)
        }

        binding.rvProjectSummaries.layoutManager = LinearLayoutManager(this)
        binding.rvProjectSummaries.adapter = adapter

        // whenever currency changes, reload summaries
        binding.spinnerCurrencyFilter.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    loadDashboard()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    override fun onResume() {
        super.onResume()
        showLastUploadTime()
        loadDashboard()
    }

    private fun showLastUploadTime() {
        val prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val t = prefs.getLong("last_upload_time", 0L)
        binding.tvLastUpload.text = if (t == 0L) {
            "Last cloud upload: -"
        } else {
            val formatted = DateFormat.format("yyyy-MM-dd HH:mm", t).toString()
            "Last cloud upload: $formatted"
        }
    }

    private fun loadDashboard() {
        val selectedCurrency = binding.spinnerCurrencyFilter.selectedItem?.toString() ?: "MMK"

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@DashboardActivity)

            val result = withContext(Dispatchers.IO) {
                val projects = db.projectDao().getAllProjects()
                val expenses = db.expenseDao().getAllExpenses()

                val byCurrency = expenses.groupBy { it.currency }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val active = projects.count { it.projectStatus == "Active" }
                val completed = projects.count { it.projectStatus == "Completed" }
                val onHold = projects.count { it.projectStatus == "On Hold" }

                val expensesByProject = expenses.groupBy { it.projectId }

                val summaries = projects.map { p ->
                    val spent = (expensesByProject[p.projectId] ?: emptyList())
                        .filter { it.currency == selectedCurrency }
                        .sumOf { it.amount }

                    ProjectSummary(
                        projectId = p.projectId,
                        projectName = p.projectName,
                        status = p.projectStatus,
                        startDate = p.startDate,
                        endDate = p.endDate,
                        budget = p.budget,
                        spentInCurrency = spent,
                        currency = selectedCurrency
                    )
                }.sortedByDescending { it.spentInCurrency }

                DashboardData(
                    totalProjects = projects.size,
                    active = active,
                    completed = completed,
                    onHold = onHold,
                    totalsByCurrency = byCurrency,
                    summaries = summaries
                )
            }

            // bind UI
            binding.tvTotalProjects.text = result.totalProjects.toString()
            binding.tvActive.text = result.active.toString()
            binding.tvCompleted.text = result.completed.toString()

            binding.tvTotalsByCurrency.text =
                if (result.totalsByCurrency.isEmpty()) {
                    "No expenses yet."
                } else {
                    result.totalsByCurrency.entries.joinToString("\n") { (cur, amt) ->
                        "$cur: ${df.format(amt)}"
                    }
                }

            adapter.submitList(result.summaries)
        }
    }

    private data class DashboardData(
        val totalProjects: Int,
        val active: Int,
        val completed: Int,
        val onHold: Int,
        val totalsByCurrency: Map<String, Double>,
        val summaries: List<ProjectSummary>
    )
}