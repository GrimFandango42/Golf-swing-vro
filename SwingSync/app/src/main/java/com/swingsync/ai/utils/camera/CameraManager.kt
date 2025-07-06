package com.swingsync.ai.utils.camera

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val _cameraState = MutableStateFlow(CameraState.IDLE)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private var currentRecording: Recording? = null
    
    suspend fun initializeCamera(): Result<Unit> {
        return try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            _cameraState.value = CameraState.INITIALIZED
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing camera")
            _cameraState.value = CameraState.ERROR
            Result.failure(e)
        }
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        onImageAnalyzed: (ImageProxy) -> Unit
    ): Result<Unit> {
        return try {
            val cameraProvider = this.cameraProvider
                ?: return Result.failure(Exception("Camera provider not initialized"))
            
            // Preview
            preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Video capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Image analyzer
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        onImageAnalyzed(imageProxy)
                    })
                }
            
            // Select camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalyzer
                )
                
                _cameraState.value = CameraState.STARTED
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Use case binding failed")
                _cameraState.value = CameraState.ERROR
                Result.failure(e)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting camera")
            _cameraState.value = CameraState.ERROR
            Result.failure(e)
        }
    }
    
    fun stopCamera() {
        cameraProvider?.unbindAll()
        _cameraState.value = CameraState.STOPPED
    }
    
    fun startRecording(outputFile: File, onRecordingStateChanged: (RecordingState) -> Unit): Result<Unit> {
        return try {
            val videoCapture = this.videoCapture
                ?: return Result.failure(Exception("Video capture not initialized"))
            
            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).build()
            
            val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()
            
            currentRecording = videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .apply {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _recordingState.value = RecordingState.RECORDING
                            onRecordingStateChanged(RecordingState.RECORDING)
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                _recordingState.value = RecordingState.FINISHED
                                onRecordingStateChanged(RecordingState.FINISHED)
                            } else {
                                _recordingState.value = RecordingState.ERROR
                                onRecordingStateChanged(RecordingState.ERROR)
                                Timber.e("Video recording error: ${recordEvent.error}")
                            }
                        }
                        is VideoRecordEvent.Status -> {
                            // Recording progress update
                        }
                        is VideoRecordEvent.Pause -> {
                            _recordingState.value = RecordingState.PAUSED
                            onRecordingStateChanged(RecordingState.PAUSED)
                        }
                        is VideoRecordEvent.Resume -> {
                            _recordingState.value = RecordingState.RECORDING
                            onRecordingStateChanged(RecordingState.RECORDING)
                        }
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting recording")
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        }
    }
    
    fun stopRecording(): Result<Unit> {
        return try {
            currentRecording?.stop()
            currentRecording = null
            _recordingState.value = RecordingState.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping recording")
            Result.failure(e)
        }
    }
    
    fun pauseRecording(): Result<Unit> {
        return try {
            currentRecording?.pause()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error pausing recording")
            Result.failure(e)
        }
    }
    
    fun resumeRecording(): Result<Unit> {
        return try {
            currentRecording?.resume()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error resuming recording")
            Result.failure(e)
        }
    }
    
    fun captureImage(outputFile: File, onImageCaptured: (Boolean) -> Unit) {
        val imageCapture = this.imageCapture ?: return
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onImageCaptured(true)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Photo capture failed")
                    onImageCaptured(false)
                }
            }
        )
    }
    
    fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoDir = File(context.getExternalFilesDir(null), "Videos")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        return File(videoDir, "swing_${timestamp}.mp4")
    }
    
    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDir = File(context.getExternalFilesDir(null), "Images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        return File(imageDir, "swing_${timestamp}.jpg")
    }
    
    fun release() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}

enum class CameraState {
    IDLE,
    INITIALIZED,
    STARTED,
    STOPPED,
    ERROR
}

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    FINISHED,
    ERROR
}