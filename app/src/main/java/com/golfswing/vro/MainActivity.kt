package com.golfswing.vro

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.golfswing.vro.ui.navigation.GolfSwingNavigation
import com.golfswing.vro.ui.theme.GolfSwingVroTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            
            if (isGranted) {
                Timber.d("Permission granted: $permission")
            } else {
                Timber.w("Permission denied: $permission")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        requestPermissions()
        
        setContent {
            GolfSwingVroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    @Composable
    private fun MainContent() {
        val context = LocalContext.current
        var hasPermissions by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            hasPermissions = requiredPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
            }
        }
        
        if (hasPermissions) {
            GolfSwingNavigation()
        } else {
            // Show permission request screen
            PermissionRequestScreen(
                onPermissionRequest = { requestPermissions() }
            )
        }
    }
}