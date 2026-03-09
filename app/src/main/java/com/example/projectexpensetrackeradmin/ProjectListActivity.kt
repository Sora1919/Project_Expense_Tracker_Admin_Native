package com.example.projectexpensetrackeradmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectexpensetrackeradmin.databinding.ActivityProjectListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class ProjectListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectListBinding
    private lateinit var adapter: ProjectAdapter
    private var searchText: String = ""
    private var filterStatus: String? = null
    private var filterOwner: String? = null
    private var filterFromDate: String? = null
    private var filterToDate: String? = null
    private val cloud = CloudSyncRtdbService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProjectAdapter(
            onExpenses = { project ->
                val i = Intent(this, ExpenseListActivity::class.java)
                i.putExtra(ExpenseListActivity.EXTRA_PROJECT_ID, project.projectId)
                i.putExtra(ExpenseListActivity.EXTRA_PROJECT_NAME, project.projectName)
                startActivity(i)
            },
            onEdit = { project ->
                val i = Intent(this, AddProjectActivity::class.java)
                i.putExtra(AddProjectActivity.EXTRA_PROJECT_ID, project.projectId)
                startActivity(i)
            },
            onDelete = { project ->
                confirmDelete(project)
            },
            onDetails = { project ->
                val i = Intent(this, ProjectDetailActivity::class.java)
                i.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.projectId)
                startActivity(i)
            }
        )

        binding.rvProjects.layoutManager = LinearLayoutManager(this)
        binding.rvProjects.adapter = adapter

        binding.etSearch.addTextChangedListener {
            searchText = it?.toString().orEmpty()
            runSearch()
        }

        binding.btnFilters.setOnClickListener {
            showFilterDialog()
        }

        binding.btnAddProject.setOnClickListener {
            startActivity(Intent(this, AddProjectActivity::class.java))
        }

        binding.btnResetDb.setOnClickListener {
            confirmReset()
        }

        binding.btnDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        setupUploadAll()
    }

    override fun onResume() {
        super.onResume()
        runSearch()
    }

    private fun runSearch() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@ProjectListActivity)
                    .projectDao()
                    .searchProjects(
                        text = searchText.trim(),
                        status = filterStatus,
                        owner = filterOwner?.trim()?.takeIf { it.isNotBlank() },
                        fromDate = filterFromDate?.takeIf { it.isNotBlank() },
                        toDate = filterToDate?.takeIf { it.isNotBlank() }
                    )
            }
            adapter.submitList(list)
        }
    }

    private fun showFilterDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_project_filters, null)
        val spStatus = view.findViewById<android.widget.Spinner>(R.id.spinnerStatus)
        val etOwner = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOwner)
        val etFrom = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFromDate)
        val etTo = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etToDate)

        val statusOptions = listOf("Any", "Active", "Completed", "On Hold")
        spStatus.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions
        )

        // restore current values
        etOwner.setText(filterOwner ?: "")
        etFrom.setText(filterFromDate ?: "")
        etTo.setText(filterToDate ?: "")
        val currentStatusIndex = statusOptions.indexOf(filterStatus ?: "Any").coerceAtLeast(0)
        spStatus.setSelection(currentStatusIndex)

        fun showDatePicker(onPicked: (String) -> Unit) {
            val cal = java.util.Calendar.getInstance()
            val y = cal.get(java.util.Calendar.YEAR)
            val m = cal.get(java.util.Calendar.MONTH)
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            android.app.DatePickerDialog(this, { _, yy, mm, dd ->
                val mm2 = (mm + 1).toString().padStart(2, '0')
                val dd2 = dd.toString().padStart(2, '0')
                onPicked("$yy-$mm2-$dd2")
            }, y, m, d).show()
        }

        etFrom.setOnClickListener { showDatePicker { etFrom.setText(it) } }
        etTo.setOnClickListener { showDatePicker { etTo.setText(it) } }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Advanced Filters")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val selected = spStatus.selectedItem.toString()
                filterStatus = if (selected == "Any") null else selected
                filterOwner = etOwner.text?.toString()
                filterFromDate = etFrom.text?.toString()
                filterToDate = etTo.text?.toString()
                runSearch()
            }
            .setNegativeButton("Clear") { _, _ ->
                filterStatus = null
                filterOwner = null
                filterFromDate = null
                filterToDate = null
                runSearch()
            }
            .show()
    }

    private fun confirmDelete(project: Project) {
        AlertDialog.Builder(this)
            .setTitle("Delete project?")
            .setMessage("Delete '${project.projectName}' (${project.projectId})?")
            .setPositiveButton("Delete") { _, _ -> deleteProject(project) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProject(project: Project) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@ProjectListActivity)
                    .projectDao()
                    .delete(project)
            }
            runSearch()
        }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset database?")
            .setMessage("This will delete ALL projects. Continue?")
            .setPositiveButton("Reset") { _, _ -> resetDb() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetDb() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@ProjectListActivity)
                    .projectDao()
                    .deleteAll()
            }
            runSearch()
        }
    }

    private fun setupUploadAll() {
        binding.btnUploadAll.setOnClickListener {
            if (!NetworkUtils.isOnline(this)) {
                Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Uploading...")
                .setMessage("Uploading all projects to the cloud.")
                .setCancelable(false)
                .create()
            dialog.show()

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@ProjectListActivity)
                    withContext(Dispatchers.IO) {
                        cloud.uploadAllProjects(db)
                    }
                    dialog.dismiss()
                    Toast.makeText(this@ProjectListActivity, "Upload complete!", Toast.LENGTH_SHORT).show()
                    getSharedPreferences("sync_prefs", MODE_PRIVATE)
                        .edit {
                            putLong("last_upload_time", System.currentTimeMillis())
                        }
                } catch (e: Exception) {
                    dialog.dismiss()
                    Toast.makeText(this@ProjectListActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}