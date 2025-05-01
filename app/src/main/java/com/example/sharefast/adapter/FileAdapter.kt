package com.example.sharefast.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.R
import com.example.sharefast.model.FileItem
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (FileItem) -> Unit,
    private val onDeleteClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val files = mutableListOf<FileItem>()
    private val selectedFiles = mutableSetOf<FileItem>()

    fun updateFiles(newFiles: List<FileItem>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    fun addFile(file: FileItem) {
        files.add(file)
        notifyItemInserted(files.size - 1)
    }

    fun removeFile(file: FileItem) {
        val index = files.indexOf(file)
        if (index != -1) {
            files.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun clearFiles() {
        files.clear()
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    fun getFiles(): List<FileItem> = files.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.folderName)
        private val countText: TextView = itemView.findViewById(R.id.itemCount)
        private val iconView: ImageView = itemView.findViewById(R.id.folderIcon)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(file: FileItem) {
            nameText.text = file.name
            countText.text = formatFileSize(file.size)
            
            // Set icon based on file type
            val iconRes = when {
                file.mimeType.startsWith("image/") -> R.drawable.ic_image
                file.mimeType.startsWith("video/") -> R.drawable.ic_video
                file.mimeType.startsWith("audio/") -> R.drawable.ic_audio
                else -> R.drawable.ic_file
            }
            iconView.setImageResource(iconRes)

            // Set selection state
            itemView.isSelected = selectedFiles.contains(file)
            
            itemView.setOnClickListener {
                onFileClick(file)
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file)
                } else {
                    selectedFiles.add(file)
                }
                notifyItemChanged(adapterPosition)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(file)
            }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> "${size / (1024 * 1024 * 1024)} GB"
            }
        }
    }
} 