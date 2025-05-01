package com.example.sharefast

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.databinding.ItemImageBinding
import java.io.File
import java.net.URI

class ImageAdapter(
    private val images: List<ImageItem>,
    private val onItemClick: (ImageItem) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val selectedItems = mutableSetOf<ImageItem>()

    fun setSelectedItems(items: Set<ImageItem>) {
        selectedItems.clear()
        selectedItems.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image, selectedItems.contains(image))
    }

    override fun getItemCount() = images.size

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(images[position])
                }
            }
        }

        fun bind(image: ImageItem, isSelected: Boolean) {
            binding.imageTitle.text = image.title
            binding.imageDate.text = "${image.date} ${image.time}"
            
            // Load thumbnail
            try {
                val uri = URI(image.uri)
                val file = File(uri)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Reduce image size for thumbnail
                    }
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    binding.imageThumbnail.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                binding.imageThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Show selection indicator
            binding.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}