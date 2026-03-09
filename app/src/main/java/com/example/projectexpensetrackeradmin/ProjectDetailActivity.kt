package com.example.projectexpensetrackeradmin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projectexpensetrackeradmin.databinding.ActivityProjectDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
    }

    private lateinit var binding: ActivityProjectDetailBinding
    private lateinit var projectId: String
    private var projectName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        if (projectId.isBlank()) finish()

        binding.btnManageExpenses.setOnClickListener {
            val i = Intent(this, ExpenseListActivity::class.java)
            i.putExtra(ExpenseListActivity.EXTRA_PROJECT_ID, projectId)
            i.putExtra(ExpenseListActivity.EXTRA_PROJECT_NAME, projectName)
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDetails()
    }

    private fun loadDetails() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@ProjectDetailActivity)

            val project = withContext(Dispatchers.IO) {
                db.projectDao().getProjectById(projectId)
            } ?: return@launch

            projectName = project.projectName
            binding.tvTitle.text = "Project • ${project.projectName}"

            // Optional: show totals by currency
            val expenses = withContext(Dispatchers.IO) {
                db.expenseDao().getExpensesForProject(projectId)
            }
            val totals = expenses.groupBy { it.currency }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val totalsText = if (totals.isEmpty()) {
                "Total Expenses: 0"
            } else {
                "Total Expenses:\n" + totals.entries.joinToString("\n") { (cur, amt) -> "- $amt $cur" }
            }

            binding.tvBody.text = """
Project ID: ${project.projectId}
Status: ${project.projectStatus}
Manager/Owner: ${project.projectManager}

Start Date: ${project.startDate}
End Date: ${project.endDate}

Budget: ${project.budget}

Description:
${project.description}

Special Requirements: ${project.specialRequirements ?: "-"}
Client/Department: ${project.clientDepartment ?: "-"}
Priority: ${project.priority ?: "-"}

$totalsText
            """.trimIndent()
        }
    }
}