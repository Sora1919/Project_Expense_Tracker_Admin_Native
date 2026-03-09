package com.example.projectexpensetrackeradmin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projectexpensetrackeradmin.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
    }

    private lateinit var binding: ActivityAddExpenseBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var projectId: String = ""
    private var editingExpenseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        if (projectId.isBlank()) {
            Toast.makeText(this, "Project ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupDatePicker()
        setupSaveButton()

        // Edit mode
        editingExpenseId = intent.getStringExtra(EXTRA_EXPENSE_ID)
        if (editingExpenseId != null) {
            binding.tvTitle.text = "Edit Expense"
            binding.btnSaveExpense.text = "Update Expense"
            binding.etExpenseId.isEnabled = false
            loadExpense(editingExpenseId!!)
        }
    }

    private fun setupDatePicker() {
        binding.etExpenseDate.setOnClickListener {
            showDatePicker { y, m, d ->
                val selected = Calendar.getInstance().apply { set(y, m, d) }
                binding.etExpenseDate.setText(dateFormat.format(selected.time))
            }
        }
    }

    private fun showDatePicker(onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, day ->
            onDateSet(year, month, day)
        }, y, m, d).show()
    }

    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener {
            clearErrors()
            if (validateInputs()) {
                showConfirmationDialog()
            }
        }
    }

    private fun clearErrors() {
        binding.tilExpenseId.error = null
        binding.tilExpenseDate.error = null
        binding.tilAmount.error = null
        binding.tilClaimant.error = null
        // spinners usually always have a value, so no error needed there
    }

    private fun validateInputs(): Boolean {
        var ok = true

        fun required(til: com.google.android.material.textfield.TextInputLayout, value: String): Boolean {
            return if (value.isBlank()) {
                til.error = "Required"
                false
            } else {
                til.error = null
                true
            }
        }

        ok = ok && required(binding.tilExpenseId, binding.etExpenseId.text.toString().trim())
        ok = ok && required(binding.tilExpenseDate, binding.etExpenseDate.text.toString().trim())
        ok = ok && required(binding.tilClaimant, binding.etClaimant.text.toString().trim())

        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isBlank()) {
            binding.tilAmount.error = "Required"
            ok = false
        } else {
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.tilAmount.error = "Enter a valid amount"
                ok = false
            } else {
                binding.tilAmount.error = null
            }
        }

        return ok
    }

    private fun showConfirmationDialog() {
        val summary = """
            Expense ID: ${binding.etExpenseId.text}
            Date: ${binding.etExpenseDate.text}

            Amount: ${binding.etAmount.text} ${binding.spinnerCurrency.selectedItem}
            Type: ${binding.spinnerExpenseType.selectedItem}
            Payment Method: ${binding.spinnerPaymentMethod.selectedItem}
            Payment Status: ${binding.spinnerPaymentStatus.selectedItem}
            Claimant: ${binding.etClaimant.text}

            Description: ${binding.etDescription.text?.toString().orEmpty().ifBlank { "None" }}
            Location: ${binding.etLocation.text?.toString().orEmpty().ifBlank { "None" }}
        """.trimIndent()

        val view = layoutInflater.inflate(R.layout.dialog_expense_confirm, null)
        val tvBody = view.findViewById<android.widget.TextView>(R.id.tvConfirmBody)
        tvBody.text = summary

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Confirm") { _, _ -> saveExpenseToDatabase() }
            .setNegativeButton("Edit") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun loadExpense(expenseId: String) {
        lifecycleScope.launch {
            val expense = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@AddExpenseActivity)
                    .expenseDao()
                    .getExpenseById(expenseId)
            } ?: return@launch

            // Fill UI
            binding.etExpenseId.setText(expense.expenseId)
            binding.etExpenseDate.setText(expense.dateOfExpense)
            binding.etAmount.setText(expense.amount.toString())
            binding.etClaimant.setText(expense.claimant)
            binding.etDescription.setText(expense.description ?: "")
            binding.etLocation.setText(expense.location ?: "")

            setSpinnerToValue(binding.spinnerCurrency, expense.currency)
            setSpinnerToValue(binding.spinnerExpenseType, expense.typeOfExpense)
            setSpinnerToValue(binding.spinnerPaymentMethod, expense.paymentMethod)
            setSpinnerToValue(binding.spinnerPaymentStatus, expense.paymentStatus)
        }
    }

    private fun setSpinnerToValue(spinner: android.widget.Spinner, value: String) {
        val index = (0 until spinner.count)
            .firstOrNull { spinner.getItemAtPosition(it).toString() == value } ?: 0
        spinner.setSelection(index)
    }

    private fun saveExpenseToDatabase() {
        val expense = Expense(
            expenseId = binding.etExpenseId.text.toString().trim(),
            projectId = projectId,
            dateOfExpense = binding.etExpenseDate.text.toString().trim(),
            amount = binding.etAmount.text.toString().trim().toDouble(),
            currency = binding.spinnerCurrency.selectedItem.toString(),
            typeOfExpense = binding.spinnerExpenseType.selectedItem.toString(),
            paymentMethod = binding.spinnerPaymentMethod.selectedItem.toString(),
            claimant = binding.etClaimant.text.toString().trim(),
            paymentStatus = binding.spinnerPaymentStatus.selectedItem.toString(),
            description = binding.etDescription.text?.toString()?.trim().takeIf { !it.isNullOrBlank() },
            location = binding.etLocation.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        )

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@AddExpenseActivity).expenseDao()

            try {
                withContext(Dispatchers.IO) {
                    if (editingExpenseId == null) dao.insert(expense) else dao.update(expense)
                }

                Toast.makeText(
                    this@AddExpenseActivity,
                    if (editingExpenseId == null) "Expense saved!" else "Expense updated!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

            } catch (e: Exception) {
                // Most common: duplicate expenseId
                binding.tilExpenseId.error = "Expense ID already exists"
            }
        }
    }
}