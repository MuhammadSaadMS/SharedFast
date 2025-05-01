package com.example.sharefast.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Folder(
    val id: String,
    val name: String,
    val path: String,
    var files: List<FileItem> = emptyList()
) : Parcelable {
    fun getFile(): File = File(path)
    
    fun isValid(): Boolean = getFile().exists() && getFile().isDirectory
    
    fun loadFiles() {
        val file = getFile()
        if (file.exists() && file.isDirectory) {
            files = file.listFiles()
                ?.filter { it.isFile }
                ?.map { FileItem.fromFile(it) }
                ?: emptyList()
        }
    }
}

@Parcelize
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long
) : Parcelable {
    companion object {
        fun fromFile(file: File): FileItem {
            return FileItem(
                id = file.absolutePath,
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                mimeType = getMimeType(file.name),
                lastModified = file.lastModified()
            )
        }

        private fun getMimeType(fileName: String): String {
            return when {
                fileName.endsWith(".jpg", true) || 
                fileName.endsWith(".jpeg", true) || 
                fileName.endsWith(".png", true) -> "image/*"
                fileName.endsWith(".mp4", true) || 
                fileName.endsWith(".3gp", true) || 
                fileName.endsWith(".mkv", true) -> "video/*"
                fileName.endsWith(".mp3", true) || 
                fileName.endsWith(".wav", true) || 
                fileName.endsWith(".ogg", true) -> "audio/*"
                else -> "*/*"
            }
        }
    }

    fun getFile(): File = File(path)
} 