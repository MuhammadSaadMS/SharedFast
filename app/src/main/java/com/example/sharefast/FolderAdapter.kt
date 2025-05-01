package com.example.sharefast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.databinding.ItemFolderBinding

class FolderAdapter(
    private val allFolders: List<String>,
    private val onFolderClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private var filteredFolders = allFolders.toList()

    fun filter(query: String) {
        filteredFolders = if (query.isEmpty()) {
            allFolders.toList()
        } else {
            allFolders.filter { it.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(filteredFolders[position])
    }

    override fun getItemCount() = filteredFolders.size

    inner class FolderViewHolder(private val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFolderClick(filteredFolders[position])
                }
            }
        }

        fun bind(folderName: String) {
            binding.folderName.text = folderName
        }
    }
}