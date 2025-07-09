package com.golfswing.vro.pixel.security

import android.util.Log
import java.lang.ref.WeakReference
import java.security.SecureRandom
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureMemoryManager @Inject constructor() {
    
    companion object {
        private const val TAG = "SecureMemoryManager"
        private const val MEMORY_POOL_SIZE = 1024 * 1024 // 1MB
        private const val CLEANUP_INTERVAL = 60_000L // 1 minute
        private const val MAX_SENSITIVE_DATA_AGE = 300_000L // 5 minutes
    }
    
    private val sensitiveDataRegistry = ConcurrentHashMap<Long, SensitiveDataEntry>()
    private val memoryPool = ConcurrentLinkedQueue<ByteArray>()
    private val cleanupThread = Thread(::cleanupLoop)
    private val isRunning = AtomicBoolean(true)
    private val nextDataId = AtomicLong(1)
    
    data class SensitiveDataEntry(
        val id: Long,
        val dataRef: WeakReference<ByteArray>,
        val createdAt: Long,
        val description: String,
        val isCleared: AtomicBoolean = AtomicBoolean(false)
    )
    
    init {
        cleanupThread.isDaemon = true
        cleanupThread.start()
        
        // Initialize memory pool
        repeat(10) {
            memoryPool.offer(ByteArray(1024))
        }
        
        Log.d(TAG, "SecureMemoryManager initialized")
    }
    
    /**
     * Register sensitive data for automatic cleanup
     */
    fun registerSensitiveData(data: ByteArray, description: String): Long {
        val id = nextDataId.getAndIncrement()
        val entry = SensitiveDataEntry(
            id = id,
            dataRef = WeakReference(data),
            createdAt = System.currentTimeMillis(),
            description = description
        )
        
        sensitiveDataRegistry[id] = entry
        Log.d(TAG, "Registered sensitive data: $description (ID: $id)")
        
        return id
    }
    
    /**
     * Manually clear sensitive data
     */
    fun clearSensitiveData(dataId: Long) {
        val entry = sensitiveDataRegistry[dataId]
        if (entry != null) {
            entry.dataRef.get()?.let { data ->
                secureWipeArray(data)
                entry.isCleared.set(true)
                Log.d(TAG, "Cleared sensitive data: ${entry.description} (ID: $dataId)")
            }
            sensitiveDataRegistry.remove(dataId)
        }
    }
    
    /**
     * Clear all sensitive data
     */
    fun clearAllSensitiveData() {
        val entries = sensitiveDataRegistry.values.toList()
        entries.forEach { entry ->
            entry.dataRef.get()?.let { data ->
                secureWipeArray(data)
                entry.isCleared.set(true)
            }
        }
        sensitiveDataRegistry.clear()
        Log.d(TAG, "Cleared all sensitive data (${entries.size} entries)")
    }
    
    /**
     * Secure wipe array with multiple overwrites
     */
    fun secureWipeArray(array: ByteArray) {
        try {
            val random = SecureRandom()
            
            // Multiple overwrite passes
            repeat(3) {
                random.nextBytes(array)
            }
            
            // Final zero fill
            Arrays.fill(array, 0.toByte())
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely wipe array", e)
        }
    }
    
    /**
     * Secure wipe string by converting to char array
     */
    fun secureWipeString(string: String): String {
        return try {
            val chars = string.toCharArray()
            secureWipeCharArray(chars)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely wipe string", e)
            string
        }
    }
    
    /**
     * Secure wipe char array
     */
    fun secureWipeCharArray(array: CharArray) {
        try {
            val random = SecureRandom()
            
            // Multiple overwrite passes
            repeat(3) {
                for (i in array.indices) {
                    array[i] = random.nextInt(65536).toChar()
                }
            }
            
            // Final zero fill
            Arrays.fill(array, 0.toChar())
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely wipe char array", e)
        }
    }
    
    /**
     * Get secure byte array from pool
     */
    fun getSecureByteArray(size: Int): ByteArray {
        return if (size <= 1024) {
            memoryPool.poll()?.also { Arrays.fill(it, 0.toByte()) } ?: ByteArray(size)
        } else {
            ByteArray(size)
        }
    }
    
    /**
     * Return byte array to pool after secure wiping
     */
    fun returnSecureByteArray(array: ByteArray) {
        if (array.size <= 1024) {
            secureWipeArray(array)
            memoryPool.offer(array)
        } else {
            secureWipeArray(array)
        }
    }
    
    /**
     * Create secure copy of byte array
     */
    fun createSecureCopy(original: ByteArray): ByteArray {
        val copy = getSecureByteArray(original.size)
        System.arraycopy(original, 0, copy, 0, original.size)
        return copy
    }
    
    /**
     * Secure comparison of byte arrays
     */
    fun secureEquals(array1: ByteArray, array2: ByteArray): Boolean {
        if (array1.size != array2.size) {
            return false
        }
        
        var result = 0
        for (i in array1.indices) {
            result = result or (array1[i].toInt() xor array2[i].toInt())
        }
        
        return result == 0
    }
    
    /**
     * Secure comparison of strings
     */
    fun secureEquals(string1: String, string2: String): Boolean {
        if (string1.length != string2.length) {
            return false
        }
        
        var result = 0
        for (i in string1.indices) {
            result = result or (string1[i].code xor string2[i].code)
        }
        
        return result == 0
    }
    
    /**
     * Generate secure random bytes
     */
    fun generateSecureRandom(size: Int): ByteArray {
        val random = ByteArray(size)
        SecureRandom().nextBytes(random)
        return random
    }
    
    /**
     * Generate secure random string
     */
    fun generateSecureRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
    
    /**
     * Memory cleanup loop
     */
    private fun cleanupLoop() {
        while (isRunning.get()) {
            try {
                Thread.sleep(CLEANUP_INTERVAL)
                performCleanup()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in cleanup loop", e)
            }
        }
    }
    
    /**
     * Perform memory cleanup
     */
    private fun performCleanup() {
        val currentTime = System.currentTimeMillis()
        val toRemove = mutableListOf<Long>()
        
        sensitiveDataRegistry.forEach { (id, entry) ->
            val data = entry.dataRef.get()
            
            if (data == null || entry.isCleared.get()) {
                // Data was garbage collected or manually cleared
                toRemove.add(id)
            } else if (currentTime - entry.createdAt > MAX_SENSITIVE_DATA_AGE) {
                // Data is too old, force cleanup
                secureWipeArray(data)
                entry.isCleared.set(true)
                toRemove.add(id)
                Log.d(TAG, "Force cleaned old sensitive data: ${entry.description} (ID: $id)")
            }
        }
        
        toRemove.forEach { id ->
            sensitiveDataRegistry.remove(id)
        }
        
        if (toRemove.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${toRemove.size} sensitive data entries")
        }
        
        // Force garbage collection
        System.gc()
    }
    
    /**
     * Get memory statistics
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        return MemoryStats(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            sensitiveDataCount = sensitiveDataRegistry.size,
            memoryPoolSize = memoryPool.size
        )
    }
    
    data class MemoryStats(
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usedMemory: Long,
        val sensitiveDataCount: Int,
        val memoryPoolSize: Int
    )
    
    /**
     * Force memory cleanup
     */
    fun forceCleanup() {
        performCleanup()
    }
    
    /**
     * Shutdown memory manager
     */
    fun shutdown() {
        isRunning.set(false)
        cleanupThread.interrupt()
        clearAllSensitiveData()
        
        // Clear memory pool
        memoryPool.forEach { array ->
            secureWipeArray(array)
        }
        memoryPool.clear()
        
        Log.d(TAG, "SecureMemoryManager shutdown complete")
    }
    
    /**
     * Check if memory pressure is high
     */
    fun isMemoryPressureHigh(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return usedMemory > maxMemory * 0.85 // 85% threshold
    }
    
    /**
     * Handle memory pressure
     */
    fun handleMemoryPressure() {
        if (isMemoryPressureHigh()) {
            Log.w(TAG, "High memory pressure detected, forcing cleanup")
            forceCleanup()
            
            // Clear older sensitive data more aggressively
            val currentTime = System.currentTimeMillis()
            val toRemove = mutableListOf<Long>()
            
            sensitiveDataRegistry.forEach { (id, entry) ->
                if (currentTime - entry.createdAt > MAX_SENSITIVE_DATA_AGE / 2) {
                    entry.dataRef.get()?.let { data ->
                        secureWipeArray(data)
                        entry.isCleared.set(true)
                        toRemove.add(id)
                    }
                }
            }
            
            toRemove.forEach { id ->
                sensitiveDataRegistry.remove(id)
            }
            
            System.gc()
        }
    }
}