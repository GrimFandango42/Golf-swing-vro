package com.swingsync.ai.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling app permissions
 */
object PermissionUtils {

    // Permission request codes
    const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    const val STORAGE_PERMISSION_REQUEST_CODE = 1002
    const val AUDIO_PERMISSION_REQUEST_CODE = 1003
    const val ALL_PERMISSIONS_REQUEST_CODE = 1004

    // Required permissions
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    val AUDIO_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val ALL_REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if storage permissions are granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if audio permission is granted
     */
    fun isAudioPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return ALL_REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            CAMERA_PERMISSIONS,
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            STORAGE_PERMISSIONS,
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request audio permission
     */
    fun requestAudioPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            AUDIO_PERMISSIONS,
            AUDIO_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request all required permissions
     */
    fun requestAllPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            ALL_REQUIRED_PERMISSIONS,
            ALL_PERMISSIONS_REQUEST_CODE
        )
    }

    /**
     * Check if permission should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Get permissions that are not granted
     */
    fun getNotGrantedPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: (List<String>) -> Unit
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE,
            STORAGE_PERMISSION_REQUEST_CODE,
            AUDIO_PERMISSION_REQUEST_CODE,
            ALL_PERMISSIONS_REQUEST_CODE -> {
                val deniedPermissions = mutableListOf<String>()
                
                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permissions[i])
                    }
                }
                
                if (deniedPermissions.isEmpty()) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied(deniedPermissions)
                }
            }
        }
    }

    /**
     * Get user-friendly permission name
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage (Write)"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (Read)"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get permission explanation message
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> 
                "Camera access is required to capture your golf swing for analysis."
            Manifest.permission.RECORD_AUDIO -> 
                "Microphone access is required to record audio with your swing video."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> 
                "Storage access is required to save your swing recordings and analysis results."
            Manifest.permission.READ_EXTERNAL_STORAGE -> 
                "Storage access is required to read saved swing data and media files."
            else -> "This permission is required for the app to function properly."
        }
    }
}