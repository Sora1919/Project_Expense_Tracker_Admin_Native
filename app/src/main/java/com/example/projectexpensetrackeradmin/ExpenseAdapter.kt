package com.example.projectexpensetrackeradmin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projectexpensetrackeradmin.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(old: Expense, new: Expense) = old.expenseId == new.expenseId
            override fun areContentsTheSame(old: Expense, new: Expense) = old == new
        }
    }

    inner class VH(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(e: Expense) {
            binding.tvTop.text = "${e.dateOfExpense} • ${e.typeOfExpense}"
            binding.tvMid.text = "Amount: ${e.amount} ${e.currency} • ${e.paymentStatus}"
            binding.tvBottom.text = "Claimant: ${e.claimant} • ID: ${e.expenseId}"

            binding.btnEdit.setOnClickListener { onEdit(e) }
            binding.btnDelete.setOnClickListener { onDelete(e) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}