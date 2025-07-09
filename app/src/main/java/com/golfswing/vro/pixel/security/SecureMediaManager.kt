package com.golfswing.vro.pixel.security

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureMediaManager @Inject constructor(
    private val context: Context,
    private val securePreferences: SecurePreferencesManager
) {
    
    companion object {
        private const val TAG = "SecureMediaManager"
        private const val RECORDINGS_DIR = "recordings"
        private const val TEMP_DIR = "temp"
        private const val BACKUP_DIR = "backup"
        private const val EXPORTS_DIR = "exports"
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_SIZE = 32 // 256 bits
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024 // 100MB
        private const val ALLOWED_EXTENSIONS = setOf("mp4", "mov", "jpg", "png", "json")
        private const val MEDIA_KEY_PREFIX = "media_key_"
    }
    
    private val activeFiles = ConcurrentHashMap<String, File>()
    private val fileMetadata = ConcurrentHashMap<String, MediaMetadata>()
    
    data class MediaMetadata(
        val originalName: String,
        val contentType: String,
        val createdAt: Long,
        val size: Long,
        val checksum: String
    )
    
    init {
        createSecureDirectories()
        cleanupTemporaryFiles()
    }
    
    /**
     * Create secure directories for media storage
     */
    private fun createSecureDirectories() {
        try {
            val directories = listOf(RECORDINGS_DIR, TEMP_DIR, BACKUP_DIR, EXPORTS_DIR)
            directories.forEach { dirName ->
                val dir = File(context.filesDir, dirName)
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    if (!created) {
                        Log.e(TAG, "Failed to create directory: $dirName")
                    } else {
                        Log.d(TAG, "Created secure directory: $dirName")
                    }
                }
                
                // Set secure permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dir.setReadable(true, true)
                    dir.setWritable(true, true)
                    dir.setExecutable(true, true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create secure directories", e)
            throw SecurityException("Failed to create secure directories", e)
        }
    }
    
    /**
     * Create secure temporary file for recording
     */
    fun createSecureRecordingFile(fileName: String): File {
        validateFileName(fileName)
        
        try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            val secureFileName = generateSecureFileName(fileName)
            val file = File(recordingsDir, secureFileName)
            
            // Create file with secure permissions
            if (file.createNewFile()) {
                file.setReadable(true, true)
                file.setWritable(true, true)
                file.setExecutable(false, false)
                
                activeFiles[secureFileName] = file
                Log.d(TAG, "Created secure recording file: $secureFileName")
                return file
            } else {
                throw SecurityException("Failed to create secure recording file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create secure recording file", e)
            throw SecurityException("Failed to create secure recording file", e)
        }
    }
    
    /**
     * Encrypt and store media file
     */
    fun encryptMediaFile(inputFile: File, outputFileName: String): File {
        validateFile(inputFile)
        validateFileName(outputFileName)
        
        try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            val encryptedFile = File(recordingsDir, "${outputFileName}.enc")
            
            val key = generateMediaKey(outputFileName)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, ALGORITHM))
            
            val iv = cipher.iv
            val metadata = createMediaMetadata(inputFile, outputFileName)
            
            FileOutputStream(encryptedFile).use { fos ->
                // Write IV first
                fos.write(iv)
                
                // Write encrypted metadata
                val metadataBytes = serializeMetadata(metadata)
                val encryptedMetadata = cipher.doFinal(metadataBytes)
                fos.write(encryptedMetadata.size.toByte())
                fos.write(encryptedMetadata)
                
                // Encrypt and write file content
                FileInputStream(inputFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedData = cipher.update(buffer, 0, bytesRead)
                        if (encryptedData != null) {
                            fos.write(encryptedData)
                        }
                    }
                    
                    val finalData = cipher.doFinal()
                    if (finalData != null) {
                        fos.write(finalData)
                    }
                }
            }
            
            // Store metadata
            fileMetadata[outputFileName] = metadata
            
            // Securely delete original file
            secureDeleteFile(inputFile)
            
            Log.d(TAG, "Successfully encrypted media file: $outputFileName")
            return encryptedFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt media file", e)
            throw SecurityException("Failed to encrypt media file", e)
        }
    }
    
    /**
     * Decrypt and retrieve media file
     */
    fun decryptMediaFile(encryptedFileName: String, outputFile: File): Boolean {
        validateFileName(encryptedFileName)
        
        try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            val encryptedFile = File(recordingsDir, "${encryptedFileName}.enc")
            
            if (!encryptedFile.exists()) {
                Log.w(TAG, "Encrypted file not found: $encryptedFileName")
                return false
            }
            
            val key = getMediaKey(encryptedFileName)
            if (key == null) {
                Log.e(TAG, "Media key not found for: $encryptedFileName")
                return false
            }
            
            FileInputStream(encryptedFile).use { fis ->
                // Read IV
                val iv = ByteArray(GCM_IV_LENGTH)
                fis.read(iv)
                
                // Read encrypted metadata
                val metadataSize = fis.read()
                val encryptedMetadata = ByteArray(metadataSize)
                fis.read(encryptedMetadata)
                
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, ALGORITHM), spec)
                
                // Decrypt metadata
                val metadataBytes = cipher.doFinal(encryptedMetadata)
                val metadata = deserializeMetadata(metadataBytes)
                
                // Decrypt file content
                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedData = cipher.update(buffer, 0, bytesRead)
                        if (decryptedData != null) {
                            fos.write(decryptedData)
                        }
                    }
                    
                    val finalData = cipher.doFinal()
                    if (finalData != null) {
                        fos.write(finalData)
                    }
                }
            }
            
            // Verify file integrity
            val actualChecksum = calculateFileChecksum(outputFile)
            val expectedChecksum = fileMetadata[encryptedFileName]?.checksum
            
            if (actualChecksum != expectedChecksum) {
                Log.e(TAG, "File integrity check failed for: $encryptedFileName")
                secureDeleteFile(outputFile)
                return false
            }
            
            Log.d(TAG, "Successfully decrypted media file: $encryptedFileName")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt media file", e)
            return false
        }
    }
    
    /**
     * Get secure URI for sharing
     */
    fun getSecureUri(fileName: String): Uri? {
        try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            val file = File(recordingsDir, fileName)
            
            if (!file.exists()) {
                Log.w(TAG, "File not found for URI: $fileName")
                return null
            }
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get secure URI", e)
            return null
        }
    }
    
    /**
     * Clean up temporary files
     */
    fun cleanupTemporaryFiles() {
        try {
            val tempDir = File(context.filesDir, TEMP_DIR)
            if (tempDir.exists()) {
                val files = tempDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile() && shouldDeleteTemporaryFile(file)) {
                        secureDeleteFile(file)
                    }
                }
            }
            
            Log.d(TAG, "Temporary files cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup temporary files", e)
        }
    }
    
    /**
     * Secure delete file with multiple overwrites
     */
    private fun secureDeleteFile(file: File) {
        try {
            if (!file.exists()) return
            
            val fileSize = file.length()
            val random = SecureRandom()
            
            // Overwrite file content multiple times
            FileOutputStream(file).use { fos ->
                repeat(3) {
                    val randomData = ByteArray(fileSize.toInt())
                    random.nextBytes(randomData)
                    fos.write(randomData)
                    fos.flush()
                }
            }
            
            // Finally delete the file
            val deleted = file.delete()
            if (!deleted) {
                Log.w(TAG, "Failed to delete file: ${file.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely delete file", e)
        }
    }
    
    /**
     * Validate file before processing
     */
    private fun validateFile(file: File) {
        if (!file.exists()) {
            throw SecurityException("File does not exist")
        }
        
        if (file.length() > MAX_FILE_SIZE) {
            throw SecurityException("File size exceeds maximum allowed size")
        }
        
        val extension = file.extension.lowercase()
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw SecurityException("File type not allowed: $extension")
        }
    }
    
    /**
     * Validate file name for security
     */
    private fun validateFileName(fileName: String) {
        if (fileName.isEmpty() || fileName.contains("..") || fileName.contains("/")) {
            throw SecurityException("Invalid file name")
        }
    }
    
    /**
     * Generate secure file name
     */
    private fun generateSecureFileName(originalName: String): String {
        val timestamp = System.currentTimeMillis()
        val random = SecureRandom().nextInt(10000)
        val extension = File(originalName).extension
        return "secure_${timestamp}_${random}.$extension"
    }
    
    /**
     * Generate encryption key for media file
     */
    private fun generateMediaKey(fileName: String): ByteArray {
        val key = ByteArray(KEY_SIZE)
        SecureRandom().nextBytes(key)
        
        // Store key securely
        securePreferences.storeEncryptedData("$MEDIA_KEY_PREFIX$fileName", key)
        
        return key
    }
    
    /**
     * Retrieve encryption key for media file
     */
    private fun getMediaKey(fileName: String): ByteArray? {
        return securePreferences.getEncryptedData("$MEDIA_KEY_PREFIX$fileName")
    }
    
    /**
     * Create media metadata
     */
    private fun createMediaMetadata(file: File, fileName: String): MediaMetadata {
        val contentType = getContentType(file)
        val checksum = calculateFileChecksum(file)
        
        return MediaMetadata(
            originalName = fileName,
            contentType = contentType,
            createdAt = System.currentTimeMillis(),
            size = file.length(),
            checksum = checksum
        )
    }
    
    /**
     * Calculate file checksum for integrity verification
     */
    private fun calculateFileChecksum(file: File): String {
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            return digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate file checksum", e)
            throw SecurityException("Failed to calculate file checksum", e)
        }
    }
    
    /**
     * Get content type from file
     */
    private fun getContentType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * Serialize metadata to bytes
     */
    private fun serializeMetadata(metadata: MediaMetadata): ByteArray {
        val json = com.google.gson.Gson().toJson(metadata)
        return json.toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Deserialize metadata from bytes
     */
    private fun deserializeMetadata(bytes: ByteArray): MediaMetadata {
        val json = String(bytes, Charsets.UTF_8)
        return com.google.gson.Gson().fromJson(json, MediaMetadata::class.java)
    }
    
    /**
     * Check if temporary file should be deleted
     */
    private fun shouldDeleteTemporaryFile(file: File): Boolean {
        val maxAge = 24 * 60 * 60 * 1000 // 24 hours
        return System.currentTimeMillis() - file.lastModified() > maxAge
    }
    
    /**
     * Get media metadata
     */
    fun getMediaMetadata(fileName: String): MediaMetadata? {
        return fileMetadata[fileName]
    }
    
    /**
     * List all encrypted media files
     */
    fun listEncryptedFiles(): List<String> {
        return try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            recordingsDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".enc") }
                ?.map { it.name.removeSuffix(".enc") }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list encrypted files", e)
            emptyList()
        }
    }
    
    /**
     * Delete encrypted media file
     */
    fun deleteEncryptedFile(fileName: String): Boolean {
        return try {
            val recordingsDir = File(context.filesDir, RECORDINGS_DIR)
            val encryptedFile = File(recordingsDir, "${fileName}.enc")
            
            if (encryptedFile.exists()) {
                secureDeleteFile(encryptedFile)
            }
            
            // Remove metadata and key
            fileMetadata.remove(fileName)
            securePreferences.removeEncryptedData("$MEDIA_KEY_PREFIX$fileName")
            
            Log.d(TAG, "Successfully deleted encrypted file: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete encrypted file", e)
            false
        }
    }
}