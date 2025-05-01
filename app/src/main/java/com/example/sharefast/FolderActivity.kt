package com.example.sharefast

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sharefast.adapter.FileAdapter
import com.example.sharefast.model.FileItem
import com.example.sharefast.model.Folder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FolderActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var selectedCountText: TextView
    private lateinit var bottomActions: View
    private val selectedFiles = mutableSetOf<FileItem>()
    private var currentPhotoPath: String? = null
    private var currentFolder: Folder? = null
    private val allFiles = mutableListOf<FileItem>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadCurrentFolderFiles()
        } else {
            Toast.makeText(this, "Permissions required to access files", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val fileItem = FileItem.fromFile(file)
                    fileAdapter.addFile(fileItem)
                    updateSelectedCount()
                }
            }
        }
    }

    private val selectFilesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val file = createFileFromUri(uri)
            file?.let {
                val fileItem = FileItem.fromFile(it)
                fileAdapter.addFile(fileItem)
            }
        }
        updateSelectedCount()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_folder)

            Log.d("ShareFast", "Starting folder initialization")
            val folderPath = intent.getStringExtra("folder_path")
            val folderName = intent.getStringExtra("folder_name")

            Log.d("ShareFast", "Folder path: $folderPath, name: $folderName")

            if (folderPath == null) {
                Log.e("ShareFast", "Folder path is null")
                Toast.makeText(this, "Invalid folder path", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // First check if we can access external storage
            val state = Environment.getExternalStorageState()
            if (state != Environment.MEDIA_MOUNTED) {
                Log.e("ShareFast", "External storage is not mounted. State: $state")
                Toast.makeText(this, "External storage is not accessible", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val folder = File(folderPath)
            Log.d("ShareFast", "Checking folder: ${folder.absolutePath}")
            Log.d("ShareFast", "Folder exists: ${folder.exists()}, isDirectory: ${folder.isDirectory}")

            if (!folder.exists()) {
                // Try to create the folder if it doesn't exist
                val created = folder.mkdirs()
                Log.d("ShareFast", "Attempted to create folder: $created")
                if (!created) {
                    Log.e("ShareFast", "Could not create folder")
                    Toast.makeText(this, "Could not create folder", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }

            if (!folder.isDirectory) {
                Log.e("ShareFast", "Path exists but is not a directory")
                Toast.makeText(this, "Invalid folder path", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            if (!folder.canRead()) {
                Log.e("ShareFast", "Cannot read folder")
                Toast.makeText(this, "Cannot access folder", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            currentFolder = Folder(
                id = UUID.randomUUID().toString(),
                name = folderName ?: folder.name,
                path = folderPath
            )

            Log.d("ShareFast", "Folder initialized successfully")
            setupViews()
            setupToolbar()
            checkPermissions()
        } catch (e: SecurityException) {
            Log.e("ShareFast", "Security exception in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Permission denied to access folder", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("ShareFast", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error initializing folder", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.title = currentFolder?.name
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupViews() {
        selectedCountText = findViewById(R.id.selectedCount)
        recyclerView = findViewById(R.id.fileRecyclerView)
        bottomActions = findViewById(R.id.bottomActions)
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        fileAdapter = FileAdapter(
            onFileClick = { fileItem -> toggleFileSelection(fileItem) },
            onDeleteClick = { fileItem -> deleteFile(fileItem) }
        )
        recyclerView.adapter = fileAdapter

        setupButtons()
        setupShareButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.selectAllBtn).setOnClickListener {
            if (selectedFiles.size == fileAdapter.itemCount) {
                selectedFiles.clear()
            } else {
                selectedFiles.addAll(fileAdapter.getFiles())
            }
            fileAdapter.notifyDataSetChanged()
            updateSelectedCount()
        }

        findViewById<Button>(R.id.selectFilesBtn).setOnClickListener {
            selectFiles()
        }

        findViewById<Button>(R.id.cameraBtn).setOnClickListener {
            takePicture()
        }
    }

    private fun setupShareButtons() {
        findViewById<ImageButton>(R.id.shareFacebook).setOnClickListener {
            shareFiles("com.facebook.katana")
        }
        findViewById<ImageButton>(R.id.shareWhatsapp).setOnClickListener {
            shareFiles("com.whatsapp")
        }
        findViewById<ImageButton>(R.id.shareBluetooth).setOnClickListener {
            shareFiles("com.android.bluetooth")
        }
        findViewById<ImageButton>(R.id.shareGmail).setOnClickListener {
            shareFiles("com.google.android.gm")
        }
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
        val baseDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val newFolder = File(baseDir, name)
        
        if (!newFolder.exists() && newFolder.mkdirs()) {
            val folder = Folder(
                id = UUID.randomUUID().toString(),
                name = name,
                path = newFolder.absolutePath
            )
            loadFolders()
            bottomActions.visibility = View.VISIBLE
            Toast.makeText(this, "Folder created: $name", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(fileItem: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${fileItem.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val file = File(fileItem.path)
                if (file.exists() && file.delete()) {
                    fileAdapter.removeFile(fileItem)
                    selectedFiles.remove(fileItem)
                    updateSelectedCount()
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkPermissions() {
        try {
            Log.d("ShareFast", "Checking permissions")
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

            val missingPermissions = permissions.filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isEmpty()) {
                Log.d("ShareFast", "All permissions granted")
                loadCurrentFolderFiles()
            } else {
                Log.d("ShareFast", "Missing permissions: $missingPermissions")
                requestPermissionLauncher.launch(permissions)
            }
        } catch (e: Exception) {
            Log.e("ShareFast", "Error checking permissions: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error checking permissions", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadFolders() {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        baseDir?.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val folder = Folder(
                id = UUID.randomUUID().toString(),
                name = dir.name,
                path = dir.absolutePath
            )
            loadCurrentFolderFiles()
        }
    }

    private fun loadCurrentFolderFiles() {
        try {
            Log.d("ShareFast", "Loading folder files")
            currentFolder?.let { folder ->
                val folderFile = File(folder.path)
                Log.d("ShareFast", "Loading files from: ${folderFile.absolutePath}")
                
                if (!folderFile.exists()) {
                    Log.e("ShareFast", "Folder no longer exists")
                    Toast.makeText(this, "Folder no longer exists", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                if (!folderFile.canRead()) {
                    Log.e("ShareFast", "Cannot read folder")
                    Toast.makeText(this, "Cannot access folder. Permission denied.", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                folder.loadFiles()
                val fileCount = folder.files.size
                Log.d("ShareFast", "Found $fileCount files")

                allFiles.clear()
                allFiles.addAll(folder.files)
                fileAdapter.updateFiles(folder.files)
                updateSelectedCount()
            } ?: run {
                Log.e("ShareFast", "Current folder is null")
                Toast.makeText(this, "Invalid folder", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: SecurityException) {
            Log.e("ShareFast", "Security error accessing files: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Permission denied to access files", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("ShareFast", "Error loading files: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error loading files", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun toggleFileSelection(fileItem: FileItem) {
        if (selectedFiles.contains(fileItem)) {
            selectedFiles.remove(fileItem)
        } else {
            selectedFiles.add(fileItem)
        }
        fileAdapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    private fun updateSelectedCount() {
        selectedCountText.text = "${selectedFiles.size} selected"
    }

    private fun selectFiles() {
        selectFilesLauncher.launch("*/*")
    }

    private fun takePicture() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            val photoURI = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = currentFolder?.getFile() ?: getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            if (storageDir == null) {
                Toast.makeText(this, "Storage directory not available", Toast.LENGTH_SHORT).show()
                return null
            }
            
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Toast.makeText(this, "Failed to create storage directory", Toast.LENGTH_SHORT).show()
                return null
            }

            if (!storageDir.canWrite()) {
                Toast.makeText(this, "Cannot write to storage directory", Toast.LENGTH_SHORT).show()
                return null
            }

            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: SecurityException) {
            Log.e("ShareFast", "Security error creating image file: ${e.message}")
            Toast.makeText(this, "Permission denied to create file", Toast.LENGTH_SHORT).show()
            null
        } catch (ex: Exception) {
            Log.e("ShareFast", "Error creating image file: ${ex.message}")
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun createFileFromUri(uri: Uri): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = currentFolder?.getFile() ?: getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            if (storageDir?.exists() != true) {
                storageDir?.mkdirs()
            }
            val mimeType = contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "tmp"
            val file = File.createTempFile(
                "FILE_${timeStamp}_",
                ".$extension",
                storageDir
            )
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (ex: Exception) {
            Log.e("ShareFast", "Error copying file: ${ex.message}")
            Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun shareFiles(packageName: String) {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "Please select files to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uris = selectedFiles.mapNotNull { fileItem ->
                val file = File(fileItem.path)
                if (!file.exists()) {
                    Toast.makeText(this, "File ${fileItem.name} no longer exists", Toast.LENGTH_SHORT).show()
                    return@mapNotNull null
                }
                try {
                    FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        file
                    )
                } catch (e: IllegalArgumentException) {
                    Log.e("ShareFast", "Error getting URI for file ${fileItem.name}: ${e.message}")
                    Toast.makeText(this, "Error sharing ${fileItem.name}", Toast.LENGTH_SHORT).show()
                    null
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(this, "No valid files to share", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                setPackage(packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(Intent.createChooser(shareIntent, "Share files via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No app found to handle sharing", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ShareFast", "Error sharing files: ${e.message}")
            Toast.makeText(this, "Error sharing files", Toast.LENGTH_SHORT).show()
        }
    }
}