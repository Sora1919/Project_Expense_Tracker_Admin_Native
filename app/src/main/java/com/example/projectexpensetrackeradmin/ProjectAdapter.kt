package com.example.projectexpensetrackeradmin

import android.telephony.ims.SipDetails
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projectexpensetrackeradmin.databinding.ItemProjectBinding

class ProjectAdapter(
    private val onExpenses: (Project) -> Unit,
    private val onEdit: (Project) -> Unit,
    private val onDelete: (Project) -> Unit,
    private val onDetails: (Project) -> Unit
) : ListAdapter<Project, ProjectAdapter.ProjectVH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project) =
                oldItem.projectId == newItem.projectId

            override fun areContentsTheSame(oldItem: Project, newItem: Project) =
                oldItem == newItem
        }
    }

    inner class ProjectVH(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(p: Project) {
            binding.tvTitle.text = p.projectName
            binding.tvSubtitle.text =
                "${p.projectId} • ${p.projectStatus} • ${p.startDate} → ${p.endDate} • Budget: ${p.budget}"

            binding.btnExpenses.setOnClickListener { onExpenses(p) }
            binding.btnEdit.setOnClickListener { onEdit(p) }
            binding.btnDelete.setOnClickListener { onDelete(p) }
            binding.root.setOnClickListener { onDetails(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectVH {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectVH(binding)
    }

    override fun onBindViewHolder(holder: ProjectVH, position: Int) {
        holder.bind(getItem(position))
    }
}