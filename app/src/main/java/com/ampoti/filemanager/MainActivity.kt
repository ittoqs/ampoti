package com.ampoti.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ampoti.filemanager.databinding.ActivityMainBinding
import java.io.File
import java.util.Arrays

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileAdapter: FileAdapter
    private var currentDir: File? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        if (checkPermission()) {
            loadFiles(Environment.getExternalStorageDirectory())
        } else {
            requestPermission()
        }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(emptyList()) { file ->
            if (file.isDirectory) {
                loadFiles(file)
            } else {
                handleFileClick(file)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = fileAdapter
    }

    private fun loadFiles(directory: File) {
        currentDir = directory
        binding.tvCurrentPath.text = directory.absolutePath

        val files = directory.listFiles()
        val fileList = if (files != null) {
            Arrays.asList(*files).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } else {
            emptyList()
        }

        fileAdapter.updateFiles(fileList)
    }

    private fun handleFileClick(file: File) {
        val extension = file.extension.lowercase()
        if (ArchiveHelper.isSupportedArchive(extension)) {
            // Simple extraction logic for demonstration
            val outputDir = File(currentDir, file.nameWithoutExtension)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            Thread {
                val success = ArchiveHelper.extractArchive(file, outputDir)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Extracted successfully", Toast.LENGTH_SHORT).show()
                        currentDir?.let { loadFiles(it) }
                    } else {
                        Toast.makeText(this, "Failed to extract", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val readAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val writeAccepted = grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (readAccepted && writeAccepted) {
                    loadFiles(Environment.getExternalStorageDirectory())
                } else {
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadFiles(Environment.getExternalStorageDirectory())
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (currentDir != null && currentDir != Environment.getExternalStorageDirectory()) {
            loadFiles(currentDir!!.parentFile!!)
        } else {
            super.onBackPressed()
        }
    }
}
