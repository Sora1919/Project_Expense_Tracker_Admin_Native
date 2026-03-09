package com.example.projectexpensetrackeradmin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
//import com.example.projectexpensetrackeradmin.data.AppDatabase
//import com.example.projectexpensetrackeradmin.data.Project
import com.example.projectexpensetrackeradmin.databinding.ActivityAddProjectBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddProjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProjectBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingProjectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        if (editingProjectId != null) {
            binding.btnSaveProject.text = "Update Project"
            binding.etProjectId.isEnabled = false
            loadProject(editingProjectId!!)
        }

        setupDatePickers()
        setupSaveButton()
    }

    //For support edit mode
    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
    }
    private var editingProjectId: String? = null


    private fun loadProject(id: String) {
        lifecycleScope.launch {
            val project = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                AppDatabase.getInstance(this@AddProjectActivity).projectDao().getProjectById(id)
            } ?: return@launch

            binding.etProjectId.setText(project.projectId)
            binding.etProjectName.setText(project.projectName)
            binding.etDescription.setText(project.description)
            binding.etStartDate.setText(project.startDate)
            binding.etEndDate.setText(project.endDate)
            binding.etProjectManager.setText(project.projectManager)
            binding.etBudget.setText(project.budget.toString())
            binding.etSpecialRequirements.setText(project.specialRequirements ?: "")
            binding.etClientDepartment.setText(project.clientDepartment ?: "")

            setSpinnerToValue(binding.spinnerProjectStatus, project.projectStatus)
            setSpinnerToValue(binding.spinnerPriority, project.priority ?: "Low")
        }
    }

    private fun setSpinnerToValue(spinner: android.widget.Spinner, value: String) {
        val index = (0 until spinner.count).firstOrNull { spinner.getItemAtPosition(it).toString() == value } ?: 0
        spinner.setSelection(index)
    }

    private fun setupDatePickers() {
        // Start Date picker
        binding.etStartDate.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                binding.etStartDate.setText(dateFormat.format(selectedDate.time))
            }
        }

        // End Date picker
        binding.etEndDate.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                binding.etEndDate.setText(dateFormat.format(selectedDate.time))
            }
        }
    }

    private fun showDatePicker(onDateSet: (year: Int, month: Int, dayOfMonth: Int) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            onDateSet(y, m, d)
        }, year, month, day).show()
    }

    private fun setupSaveButton() {
        binding.btnSaveProject.setOnClickListener {
            if (validateInputs()) {
                showConfirmationDialog()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Helper to check required fields
        fun validateRequired(textInputLayout: com.google.android.material.textfield.TextInputLayout, text: String?): Boolean {
            return if (text.isNullOrBlank()) {
                textInputLayout.error = "This field is required"
                false
            } else {
                textInputLayout.error = null
                true
            }
        }

        isValid = isValid && validateRequired(binding.tilProjectId, binding.etProjectId.text.toString())
        isValid = isValid && validateRequired(binding.tilProjectName, binding.etProjectName.text.toString())
        isValid = isValid && validateRequired(binding.tilDescription, binding.etDescription.text.toString())
        isValid = isValid && validateRequired(binding.tilStartDate, binding.etStartDate.text.toString())
        isValid = isValid && validateRequired(binding.tilEndDate, binding.etEndDate.text.toString())
        isValid = isValid && validateRequired(binding.tilProjectManager, binding.etProjectManager.text.toString())

        // Budget validation
        val budgetStr = binding.etBudget.text.toString()
        if (budgetStr.isBlank()) {
            binding.tilBudget.error = "Budget is required"
            isValid = false
        } else {
            try {
                budgetStr.toDouble()
                binding.tilBudget.error = null
            } catch (e: NumberFormatException) {
                binding.tilBudget.error = "Enter a valid number"
                isValid = false
            }
        }

        //Date validation

        val start = binding.etStartDate.text.toString()
        val end = binding.etEndDate.text.toString()
        if (start.isNotBlank() && end.isNotBlank() && end < start) {
            binding.tilEndDate.error = "End date must be after start date"
            isValid = false
        } else {
            binding.tilEndDate.error = null
        }

        // Status is a spinner, but we need to ensure something is selected (by default first item is selected)
        // No explicit validation needed for spinner because it always has a value.

        return isValid
    }

    private fun showConfirmationDialog() {
        val summary = """
        Project ID: ${binding.etProjectId.text}
        Project Name: ${binding.etProjectName.text}

        Description: ${binding.etDescription.text}

        Start Date: ${binding.etStartDate.text}
        End Date: ${binding.etEndDate.text}

        Manager: ${binding.etProjectManager.text}
        Status: ${binding.spinnerProjectStatus.selectedItem}
        Budget: ${binding.etBudget.text}

        Special Req: ${binding.etSpecialRequirements.text?.toString().orEmpty().ifBlank { "None" }}
        Client/Dept: ${binding.etClientDepartment.text?.toString().orEmpty().ifBlank { "None" }}
        Priority: ${binding.spinnerPriority.selectedItem}""".trimIndent()

        val view = layoutInflater.inflate(R.layout.dialog_project_confirm, null)
        val tvBody = view.findViewById<android.widget.TextView>(R.id.tvConfirmBody)
        tvBody.text = summary

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Confirm") { _, _ ->
                saveProjectToDatabase()
            }
            .setNegativeButton("Edit") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveProjectToDatabase() {
        val project = Project(
            projectId = binding.etProjectId.text.toString(),
            projectName = binding.etProjectName.text.toString(),
            description = binding.etDescription.text.toString(),
            startDate = binding.etStartDate.text.toString(),
            endDate = binding.etEndDate.text.toString(),
            projectManager = binding.etProjectManager.text.toString(),
            projectStatus = binding.spinnerProjectStatus.selectedItem.toString(),
            budget = binding.etBudget.text.toString().toDouble(),
            specialRequirements = binding.etSpecialRequirements.text.toString().takeIf { it.isNotBlank() },
            clientDepartment = binding.etClientDepartment.text.toString().takeIf { it.isNotBlank() },
            priority = binding.spinnerPriority.selectedItem.toString().takeIf { it.isNotBlank() }
        )

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@AddProjectActivity).projectDao()
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (editingProjectId == null) dao.insert(project) else dao.update(project)
                }
                android.widget.Toast.makeText(
                    this@AddProjectActivity,
                    if (editingProjectId == null) "Project saved!" else "Project updated!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                binding.tilProjectId.error = "Project ID already exists"
            }
        }
    }
}