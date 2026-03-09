package com.example.projectexpensetrackeradmin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projectexpensetrackeradmin.databinding.ItemProjectSummaryBinding
import java.text.DecimalFormat

class ProjectSummaryAdapter(
    private val onClick: (ProjectSummary) -> Unit
) : ListAdapter<ProjectSummary, ProjectSummaryAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProjectSummary>() {
            override fun areItemsTheSame(old: ProjectSummary, new: ProjectSummary) =
                old.projectId == new.projectId

            override fun areContentsTheSame(old: ProjectSummary, new: ProjectSummary) =
                old == new
        }

        private val df = DecimalFormat("#,###.##")
    }

    inner class VH(private val binding: ItemProjectSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(s: ProjectSummary) {
            binding.tvName.text = s.projectName
            binding.tvMeta.text = "${s.projectId} • ${s.status} • ${s.startDate} → ${s.endDate}"

            binding.tvBudgetSpent.text =
                "Budget: ${df.format(s.budget)} | Spent (${s.currency}): ${df.format(s.spentInCurrency)}"

            val progress = if (s.budget <= 0) 0 else ((s.spentInCurrency / s.budget) * 100).toInt().coerceIn(0, 100)
            binding.progressSpent.progress = progress

            val remaining = s.budget - s.spentInCurrency
            binding.tvRemaining.text =
                if (remaining >= 0) "Remaining: ${df.format(remaining)}"
                else "Over budget by: ${df.format(-remaining)}"

            binding.root.setOnClickListener { onClick(s) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProjectSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}