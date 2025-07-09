package com.golfswing.vro.pixel.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GolfSwingCameraManager @Inject constructor(
    private val context: Context
) {
    // Optimized thread pool for camera operations
    private val cameraExecutor: ExecutorService = ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()
    ) { runnable ->
        Thread(runnable, "CameraThread").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
        }
    }
    
    // Dedicated thread for image analysis to prevent blocking
    private val analysisExecutor: ExecutorService = ThreadPoolExecutor(
        1, 2, 30L, TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()
    ) { runnable ->
        Thread(runnable, "AnalysisThread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    
    private val _cameraState = MutableStateFlow(CameraState.IDLE)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    // Frame dropping and performance tracking
    private val isProcessing = AtomicBoolean(false)
    private val frameDropCount = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private val targetFrameInterval = 33L // 30 FPS = 33ms interval
    
    // Performance monitoring
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    data class PerformanceMetrics(
        val fps: Float = 0f,
        val frameDrops: Long = 0,
        val processingTime: Long = 0,
        val memoryUsage: Long = 0
    )

    enum class CameraState {
        IDLE, INITIALIZING, READY, RECORDING, ERROR
    }

    /**
     * Initialize camera with Pixel-optimized settings for golf swing analysis
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        onFrameAnalyzed: (ImageProxy) -> Unit
    ) {
        try {
            _cameraState.value = CameraState.INITIALIZING
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            // Configure preview with optimal settings
            val preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080)) // Full HD for detailed analysis
                .setTargetRotation(previewView.display.rotation)
                .build()
            
            // Configure image analysis for pose detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720)) // Balanced resolution for real-time processing
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            
            imageAnalyzer?.setAnalyzer(analysisExecutor) { imageProxy ->
                processFrameAsync(imageProxy, onFrameAnalyzed)
            }
            
            // Configure video capture with high frame rate
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Select camera (prefer rear camera for golf swings)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            
            // Bind use cases to camera
            cameraProvider?.let { provider ->
                provider.unbindAll()
                
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer,
                    videoCapture
                )
                
                // Connect preview to PreviewView
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                // Configure camera for optimal golf swing capture
                configureCameraForGolfSwing(camera)
            }
            
            _cameraState.value = CameraState.READY
            
        } catch (e: Exception) {
            _cameraState.value = CameraState.ERROR
            throw e
        }
    }

    /**
     * Start recording golf swing with optimized settings
     */
    fun startRecording(outputFile: java.io.File) {
        try {
            _isRecording.value = true
            _cameraState.value = CameraState.RECORDING
            
            val outputOptions = FileOutputOptions.Builder(outputFile).build()
            
            videoCapture?.output
                ?.prepareRecording(context, outputOptions)
                ?.start(cameraExecutor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            // Recording started
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                _cameraState.value = CameraState.ERROR
                            } else {
                                _cameraState.value = CameraState.READY
                            }
                            _isRecording.value = false
                        }
                    }
                }
                
        } catch (e: Exception) {
            _isRecording.value = false
            _cameraState.value = CameraState.ERROR
            throw e
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        videoCapture?.output?.stop()
        _isRecording.value = false
        _cameraState.value = CameraState.READY
    }

    /**
     * Configure camera settings optimized for golf swing capture
     */
    private fun configureCameraForGolfSwing(camera: Camera) {
        val cameraControl = camera.cameraControl
        val cameraInfo = camera.cameraInfo
        
        // Set focus mode to continuous for tracking the ball
        cameraControl.setLinearZoom(0.0f) // No zoom for full field of view
        
        // Enable auto-exposure for varying lighting conditions
        cameraControl.setExposureCompensationIndex(0)
        
        // Configure for high frame rate capture if supported
        if (cameraInfo.isZslSupported) {
            cameraControl.enableTorch(false) // Disable torch for better battery life
        }
    }

    /**
     * Process frame asynchronously with frame dropping and performance monitoring
     */
    private fun processFrameAsync(imageProxy: ImageProxy, onFrameAnalyzed: (ImageProxy) -> Unit) {
        val currentTime = System.currentTimeMillis()
        
        // Check if we should drop this frame to maintain target FPS
        if (isProcessing.get() || (currentTime - lastFrameTime.get()) < targetFrameInterval) {
            frameDropCount.incrementAndGet()
            imageProxy.close() // Critical: Always close to prevent memory leaks
            return
        }
        
        // Set processing flag
        isProcessing.set(true)
        lastFrameTime.set(currentTime)
        
        try {
            val processingStart = System.currentTimeMillis()
            
            // Process frame
            onFrameAnalyzed(imageProxy)
            
            // Update performance metrics
            val processingTime = System.currentTimeMillis() - processingStart
            updatePerformanceMetrics(processingTime)
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Always close ImageProxy to prevent memory leaks
            imageProxy.close()
            isProcessing.set(false)
        }
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics(processingTime: Long) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastFrameTime.get()
        val fps = if (timeDiff > 0) 1000f / timeDiff else 0f
        
        _performanceMetrics.value = PerformanceMetrics(
            fps = fps,
            frameDrops = frameDropCount.get(),
            processingTime = processingTime,
            memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        )
    }

    /**
     * Capture a single frame for analysis
     */
    fun captureFrame(onImageCaptured: (ImageProxy) -> Unit) {
        imageAnalyzer?.setAnalyzer(analysisExecutor) { imageProxy ->
            processFrameAsync(imageProxy, onImageCaptured)
        }
    }

    /**
     * Switch between front and rear camera
     */
    suspend fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        onFrameAnalyzed: (ImageProxy) -> Unit,
        useFrontCamera: Boolean = false
    ) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                if (useFrontCamera) CameraSelector.LENS_FACING_FRONT 
                else CameraSelector.LENS_FACING_BACK
            )
            .build()
        
        cameraProvider?.let { provider ->
            provider.unbindAll()
            
            val preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080))
                .build()
            
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
                videoCapture
            )
            
            preview.setSurfaceProvider(previewView.surfaceProvider)
            configureCameraForGolfSwing(camera)
        }
    }

    /**
     * Get camera capabilities for optimization
     */
    fun getCameraCapabilities(): CameraCharacteristics? {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set secure file permissions for privacy
     */
    private fun setSecureFilePermissions(file: java.io.File) {
        try {
            // Remove all permissions first
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            
            // Set permissions for owner only
            file.setReadable(true, true)
            file.setWritable(true, true)
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }

    /**
     * Release camera resources with proper cleanup
     */
    fun release() {
        try {
            // Unbind all use cases
            cameraProvider?.unbindAll()
            
            // Stop recording if active
            if (_isRecording.value) {
                stopRecording()
            }
            
            // Shutdown executors with timeout
            cameraExecutor.shutdown()
            analysisExecutor.shutdown()
            
            try {
                if (!cameraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow()
                }
                if (!analysisExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    analysisExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                cameraExecutor.shutdownNow()
                analysisExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            }
            
            // Reset state
            _cameraState.value = CameraState.IDLE
            _isRecording.value = false
            frameDropCount.set(0)
            lastFrameTime.set(0)
            isProcessing.set(false)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics = _performanceMetrics.value
    
    /**
     * Reset performance counters
     */
    fun resetPerformanceMetrics() {
        frameDropCount.set(0)
        lastFrameTime.set(System.currentTimeMillis())
        _performanceMetrics.value = PerformanceMetrics()
    }
}