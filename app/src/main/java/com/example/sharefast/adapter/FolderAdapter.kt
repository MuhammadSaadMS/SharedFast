package com.example.sharefast.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.R
import com.example.sharefast.model.Folder
import java.io.File

class FolderAdapter(
    private val context: Context,
    private var folders: MutableList<File>,
    private val onFolderClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: ImageView = itemView.findViewById(R.id.folderIcon)
        private val folderName: TextView = itemView.findViewById(R.id.folderName)
        private val itemCount: TextView = itemView.findViewById(R.id.itemCount)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(folder: File) {
            folderName.text = folder.name
            val filesCount = folder.listFiles()?.size ?: 0
            itemCount.text = "$filesCount items"

            if (folder.isDirectory) {
                folderIcon.setImageResource(R.drawable.ic_folder)
            } else {
                folderIcon.setImageResource(R.drawable.ic_file)
            }

            itemView.setOnClickListener {
                onFolderClick(folder)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(folder)
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    folders.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.bind(folder)
    }

    override fun getItemCount(): Int = folders.size

    fun updateFolders(newFolders: List<File>) {
        folders.clear()
        folders.addAll(newFolders)
        notifyDataSetChanged()
    }

    fun removeFolder(folder: File) {
        val position = folders.indexOf(folder)
        if (position != -1) {
            folders.removeAt(position)
            notifyItemRemoved(position)
        }
    }
} 