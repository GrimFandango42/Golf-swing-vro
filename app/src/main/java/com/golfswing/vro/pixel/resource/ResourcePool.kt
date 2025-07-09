package com.golfswing.vro.pixel.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourcePool @Inject constructor() {
    
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Bitmap pool for efficient reuse
    private val bitmapPool = ConcurrentHashMap<String, ConcurrentLinkedQueue<Bitmap>>()
    private val bitmapPoolSize = AtomicInteger(0)
    private val maxBitmapPoolSize = 20
    
    // ByteBuffer pool for camera frames
    private val byteBufferPool = ConcurrentLinkedQueue<ByteBuffer>()
    private val byteBufferPoolSize = AtomicInteger(0)
    private val maxByteBufferPoolSize = 10
    
    // Memory tracking
    private val totalMemoryUsed = AtomicLong(0)
    private val bitmapMemoryUsed = AtomicLong(0)
    private val bufferMemoryUsed = AtomicLong(0)
    
    // Statistics
    private val bitmapHits = AtomicLong(0)
    private val bitmapMisses = AtomicLong(0)
    private val bufferHits = AtomicLong(0)
    private val bufferMisses = AtomicLong(0)
    
    init {
        // Start cleanup task
        startCleanupTask()
    }
    
    /**
     * Get or create a bitmap from pool
     */
    fun getBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val key = getBitmapKey(width, height, config)
        val pool = bitmapPool[key]
        
        pool?.poll()?.let { bitmap ->
            if (!bitmap.isRecycled && bitmap.width == width && bitmap.height == height) {
                bitmapHits.incrementAndGet()
                return bitmap
            } else {
                // Remove invalid bitmap
                if (!bitmap.isRecycled) {
                    val memorySize = bitmap.byteCount
                    bitmapMemoryUsed.addAndGet(-memorySize.toLong())
                    bitmap.recycle()
                }
                bitmapPoolSize.decrementAndGet()
            }
        }
        
        // Create new bitmap
        bitmapMisses.incrementAndGet()
        val bitmap = Bitmap.createBitmap(width, height, config)
        val memorySize = bitmap.byteCount
        bitmapMemoryUsed.addAndGet(memorySize.toLong())
        totalMemoryUsed.addAndGet(memorySize.toLong())
        
        return bitmap
    }
    
    /**
     * Return bitmap to pool
     */
    fun returnBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled || bitmapPoolSize.get() >= maxBitmapPoolSize) {
            if (!bitmap.isRecycled) {
                val memorySize = bitmap.byteCount
                bitmapMemoryUsed.addAndGet(-memorySize.toLong())
                totalMemoryUsed.addAndGet(-memorySize.toLong())
                bitmap.recycle()
            }
            return
        }
        
        val key = getBitmapKey(bitmap.width, bitmap.height, bitmap.config)
        val pool = bitmapPool.computeIfAbsent(key) { ConcurrentLinkedQueue() }
        
        // Clear bitmap content for reuse
        bitmap.eraseColor(0)
        
        pool.offer(bitmap)
        bitmapPoolSize.incrementAndGet()
    }
    
    /**
     * Get or create ByteBuffer from pool
     */
    fun getByteBuffer(capacity: Int): ByteBuffer {
        byteBufferPool.poll()?.let { buffer ->
            if (buffer.capacity() >= capacity) {
                buffer.clear()
                bufferHits.incrementAndGet()
                return buffer
            } else {
                // Return undersized buffer to pool
                byteBufferPool.offer(buffer)
            }
        }
        
        // Create new buffer
        bufferMisses.incrementAndGet()
        val buffer = ByteBuffer.allocateDirect(capacity)
        val memorySize = capacity.toLong()
        bufferMemoryUsed.addAndGet(memorySize)
        totalMemoryUsed.addAndGet(memorySize)
        
        return buffer
    }
    
    /**
     * Return ByteBuffer to pool
     */
    fun returnByteBuffer(buffer: ByteBuffer) {
        if (byteBufferPoolSize.get() >= maxByteBufferPoolSize) {
            val memorySize = buffer.capacity().toLong()
            bufferMemoryUsed.addAndGet(-memorySize)
            totalMemoryUsed.addAndGet(-memorySize)
            return
        }
        
        buffer.clear()
        byteBufferPool.offer(buffer)
        byteBufferPoolSize.incrementAndGet()
    }
    
    /**
     * Create bitmap key for pooling
     */
    private fun getBitmapKey(width: Int, height: Int, config: Bitmap.Config): String {
        return "${width}x${height}_${config.name}"
    }
    
    /**
     * Start cleanup task to manage pool size
     */
    private fun startCleanupTask() {
        cleanupScope.launch {
            while (isActive) {
                try {
                    delay(30000) // Cleanup every 30 seconds
                    cleanup()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Cleanup old and unused resources
     */
    private fun cleanup() {
        cleanupScope.launch {
            // Cleanup bitmaps
            bitmapPool.values.forEach { pool ->
                val iterator = pool.iterator()
                while (iterator.hasNext()) {
                    val bitmap = iterator.next()
                    if (bitmap.isRecycled) {
                        iterator.remove()
                        bitmapPoolSize.decrementAndGet()
                    }
                }
            }
            
            // Remove empty pools
            bitmapPool.entries.removeAll { it.value.isEmpty() }
            
            // If under memory pressure, reduce pool size
            if (isUnderMemoryPressure()) {
                reducePoolSize()
            }
        }
    }
    
    /**
     * Check if system is under memory pressure
     */
    private fun isUnderMemoryPressure(): Boolean {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return (usedMemory.toFloat() / totalMemory.toFloat()) > 0.8f
    }
    
    /**
     * Reduce pool size when under memory pressure
     */
    private fun reducePoolSize() {
        // Reduce bitmap pool by 50%
        bitmapPool.values.forEach { pool ->
            val removeCount = pool.size / 2
            repeat(removeCount) {
                pool.poll()?.let { bitmap ->
                    if (!bitmap.isRecycled) {
                        val memorySize = bitmap.byteCount
                        bitmapMemoryUsed.addAndGet(-memorySize.toLong())
                        totalMemoryUsed.addAndGet(-memorySize.toLong())
                        bitmap.recycle()
                    }
                    bitmapPoolSize.decrementAndGet()
                }
            }
        }
        
        // Reduce buffer pool by 50%
        val removeCount = byteBufferPool.size / 2
        repeat(removeCount) {
            byteBufferPool.poll()?.let { buffer ->
                val memorySize = buffer.capacity().toLong()
                bufferMemoryUsed.addAndGet(-memorySize)
                totalMemoryUsed.addAndGet(-memorySize)
                byteBufferPoolSize.decrementAndGet()
            }
        }
    }
    
    /**
     * Get pool statistics
     */
    fun getPoolStats(): PoolStats {
        return PoolStats(
            bitmapPoolSize = bitmapPoolSize.get(),
            byteBufferPoolSize = byteBufferPoolSize.get(),
            totalMemoryUsed = totalMemoryUsed.get(),
            bitmapMemoryUsed = bitmapMemoryUsed.get(),
            bufferMemoryUsed = bufferMemoryUsed.get(),
            bitmapHitRate = calculateHitRate(bitmapHits.get(), bitmapMisses.get()),
            bufferHitRate = calculateHitRate(bufferHits.get(), bufferMisses.get()),
            bitmapPoolKeys = bitmapPool.size
        )
    }
    
    /**
     * Calculate hit rate percentage
     */
    private fun calculateHitRate(hits: Long, misses: Long): Float {
        val total = hits + misses
        return if (total > 0) (hits.toFloat() / total.toFloat()) * 100f else 0f
    }
    
    /**
     * Clear all pools
     */
    fun clearPools() {
        cleanupScope.launch {
            // Clear bitmap pools
            bitmapPool.values.forEach { pool ->
                while (pool.isNotEmpty()) {
                    pool.poll()?.let { bitmap ->
                        if (!bitmap.isRecycled) {
                            val memorySize = bitmap.byteCount
                            bitmapMemoryUsed.addAndGet(-memorySize.toLong())
                            totalMemoryUsed.addAndGet(-memorySize.toLong())
                            bitmap.recycle()
                        }
                        bitmapPoolSize.decrementAndGet()
                    }
                }
            }
            bitmapPool.clear()
            
            // Clear buffer pools
            while (byteBufferPool.isNotEmpty()) {
                byteBufferPool.poll()?.let { buffer ->
                    val memorySize = buffer.capacity().toLong()
                    bufferMemoryUsed.addAndGet(-memorySize)
                    totalMemoryUsed.addAndGet(-memorySize)
                    byteBufferPoolSize.decrementAndGet()
                }
            }
        }
    }
    
    /**
     * Get memory usage in MB
     */
    fun getMemoryUsageMB(): Long {
        return totalMemoryUsed.get() / 1024 / 1024
    }
    
    /**
     * Force garbage collection of unused resources
     */
    fun forceGarbageCollection() {
        cleanupScope.launch {
            cleanup()
            System.gc()
        }
    }
    
    /**
     * Pool statistics data class
     */
    data class PoolStats(
        val bitmapPoolSize: Int,
        val byteBufferPoolSize: Int,
        val totalMemoryUsed: Long,
        val bitmapMemoryUsed: Long,
        val bufferMemoryUsed: Long,
        val bitmapHitRate: Float,
        val bufferHitRate: Float,
        val bitmapPoolKeys: Int
    )
    
    /**
     * Create bitmap factory options for optimal memory usage
     */
    fun createOptimalBitmapOptions(): BitmapFactory.Options {
        return BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            inSampleSize = 1
            inJustDecodeBounds = false
            inMutable = true // Allow bitmap reuse
            inTempStorage = ByteArray(16 * 1024) // 16KB temp storage
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        cleanupScope.cancel()
        clearPools()
    }
}