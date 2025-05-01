package com.example.sharefast

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.adapter.FolderAdapter
import com.example.sharefast.model.Folder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private val folders = mutableListOf<File>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadFolders()
        } else {
            Toast.makeText(this, "Permissions required to access files", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        checkPermissions()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.folderRecyclerView)
        searchView = findViewById(R.id.searchView)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        folderAdapter = FolderAdapter(
            context = this,
            folders = folders,
            onFolderClick = { folder ->
                openFolder(folder)
            },
            onDeleteClick = { folder ->
                deleteFolder(folder)
            }
        )
        recyclerView.adapter = folderAdapter

        findViewById<FloatingActionButton>(R.id.addFolderFab).setOnClickListener {
            showAddFolderDialog()
        }

        setupSearchView()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterFolders(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFolders(newText)
                return true
            }
        })
    }

    private fun filterFolders(query: String?) {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (query.isNullOrBlank()) {
            loadFolders()
        } else {
            val filteredFolders = baseDir?.listFiles { file ->
                file.isDirectory && file.name.contains(query, ignoreCase = true)
            }?.toList() ?: emptyList()
            folderAdapter.updateFolders(filteredFolders)
        }
    }

    private fun checkPermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        }

        if (permissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }) {
            loadFolders()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun loadFolders() {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        folders.clear()
        baseDir?.listFiles { file -> file.isDirectory }?.let { folderFiles ->
            folders.addAll(folderFiles)
        }
        
        if (folders.isEmpty()) {
            createFolder("Default")
        }
        folderAdapter.notifyDataSetChanged()
    }

    private fun showAddFolderDialog() {
        val editText = EditText(this)
        editText.hint = "Folder Name"
        
        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val folderName = editText.text.toString()
                if (folderName.isNotBlank()) {
                    createFolder(folderName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createFolder(name: String) {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val newFolder = File(baseDir, name)
        
        if (!newFolder.exists() && newFolder.mkdirs()) {
            folders.add(newFolder)
            folderAdapter.notifyItemInserted(folders.size - 1)
            Toast.makeText(this, "Folder created: $name", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFolder(folder: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete ${folder.name}? All files inside will be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                if (folder.deleteRecursively()) {
                    val position = folders.indexOf(folder)
                    folders.remove(folder)
                    folderAdapter.notifyItemRemoved(position)
                    Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete folder", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFolder(folder: File) {
        val intent = Intent(this, FolderActivity::class.java).apply {
            putExtra("folder_path", folder.absolutePath)
            putExtra("folder_name", folder.name)
        }
        startActivity(intent)
    }
}