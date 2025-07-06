package com.swingsync.ai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.R
import com.swingsync.ai.SwingSyncApplication
import com.swingsync.ai.databinding.ActivityMainBinding
import com.swingsync.ai.ui.camera.CameraActivity
import com.swingsync.ai.utils.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Activity - Entry point for the SwingSync AI app
 * Provides navigation to camera analysis and history viewing
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { SwingSyncApplication.instance }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAppInitialization()
    }

    private fun setupUI() {
        // Start Analysis button
        binding.btnStartAnalysis.setOnClickListener {
            if (PermissionUtils.areAllPermissionsGranted(this)) {
                startCameraActivity()
            } else {
                requestPermissions()
            }
        }

        // View History button
        binding.btnViewHistory.setOnClickListener {
            Toast.makeText(this, "History feature coming soon!", Toast.LENGTH_SHORT).show()
            // TODO: Implement history activity
        }
    }

    private fun checkAppInitialization() {
        binding.statusBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Initializing MediaPipe..."

        lifecycleScope.launch {
            try {
                // Wait for MediaPipe to initialize
                var attempts = 0
                val maxAttempts = 30 // 3 seconds max
                
                while (!app.mediaPipeManager.isReady() && attempts < maxAttempts) {
                    delay(100)
                    attempts++
                }

                if (app.mediaPipeManager.isReady()) {
                    binding.tvStatus.text = "Testing backend connection..."
                    
                    // Test backend connection
                    val result = app.swingAnalysisRepository.testBackendConnection()
                    
                    if (result.isSuccess) {
                        binding.tvStatus.text = "Ready!"
                        delay(1000)
                        hideStatusBar()
                    } else {
                        binding.tvStatus.text = "Backend unavailable - offline mode"
                        delay(2000)
                        hideStatusBar()
                    }
                } else {
                    binding.tvStatus.text = "MediaPipe initialization failed"
                    binding.progressStatus.visibility = View.GONE
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to initialize pose detection. Please restart the app.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                binding.tvStatus.text = "Initialization error"
                binding.progressStatus.visibility = View.GONE
                
                Toast.makeText(
                    this@MainActivity,
                    "App initialization failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun hideStatusBar() {
        binding.statusBar.visibility = View.GONE
    }

    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun requestPermissions() {
        binding.statusBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Requesting permissions..."
        binding.progressStatus.visibility = View.GONE

        PermissionUtils.requestAllPermissions(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionUtils.handlePermissionResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onPermissionGranted = {
                binding.tvStatus.text = "Permissions granted"
                hideStatusBar()
                startCameraActivity()
            },
            onPermissionDenied = { deniedPermissions ->
                hideStatusBar()
                
                val permissionNames = deniedPermissions.map { 
                    PermissionUtils.getPermissionDisplayName(it) 
                }.joinToString(", ")
                
                Toast.makeText(
                    this,
                    "Required permissions denied: $permissionNames",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        
        // Check if coming back from camera activity
        if (PermissionUtils.areAllPermissionsGranted(this) && binding.statusBar.visibility == View.VISIBLE) {
            hideStatusBar()
        }
    }
}