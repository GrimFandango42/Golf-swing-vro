package com.swingsync.ai.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.swingsync.ai.R
import com.swingsync.ai.SwingSyncApplication
import com.swingsync.ai.data.model.RecordingSession
import com.swingsync.ai.databinding.ActivityCameraBinding
import com.swingsync.ai.ui.analysis.AnalysisActivity
import com.swingsync.ai.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Camera Activity for real-time golf swing recording and pose detection
 * Features:
 * - Real-time camera preview with 60fps capability
 * - MediaPipe pose detection integration
 * - Live pose overlay visualization
 * - Recording session management
 * - Performance optimizations for mobile devices
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
        private const val TARGET_FPS = 60f
        private const val RECORDING_TIME_UPDATE_INTERVAL = 100L // Update every 100ms
        
        // Camera permissions
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var viewModel: CameraViewModel
    private lateinit var cameraExecutor: ExecutorService
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    
    private var isRecording = false
    private var recordingStartTime = 0L
    private var currentRecordingSession: RecordingSession? = null
    
    // Register for permission results
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allPermissionsGranted = true
        permissions.entries.forEach { entry ->
            if (!entry.value) {
                allPermissionsGranted = false
            }
        }
        
        if (allPermissionsGranted) {
            startCamera()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_denied),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        // Setup camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Club selection spinner
        binding.spinnerClubSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedClub = parent?.getItemAtPosition(position).toString()
                viewModel.setSelectedClub(selectedClub)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Recording button
        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        // Analyze button
        binding.btnAnalyze.setOnClickListener {
            currentRecordingSession?.let { session ->
                val intent = Intent(this, AnalysisActivity::class.java)
                intent.putExtra("recording_session", session)
                startActivity(intent)
            }
        }
    }

    private fun observeViewModel() {
        // Observe pose detection results
        viewModel.poseDetectionResult.observe(this) { result ->
            result?.let {
                binding.poseOverlay.updatePoseKeypoints(it.keypoints)
                
                // Update status
                val statusText = if (it.confidence > 0.7f) {
                    getString(R.string.pose_detected)
                } else {
                    getString(R.string.no_pose_detected)
                }
                binding.tvPoseStatus.text = statusText
                
                // Update frame rate
                binding.tvFrameRate.text = getString(R.string.frame_rate, viewModel.currentFPS.value ?: 0f)
                
                // Add to recording session if recording
                if (isRecording) {
                    currentRecordingSession?.let { session ->
                        val updatedSession = session.copy(
                            totalFrames = session.totalFrames + 1,
                            poseDetectionResults = session.poseDetectionResults + it
                        )
                        currentRecordingSession = updatedSession
                    }
                }
            }
        }

        // Observe errors
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error: $it")
            }
        }

        // Observe processing state
        viewModel.isProcessing.observe(this) { isProcessing ->
            binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(this, getString(R.string.error_camera_unavailable), Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Camera selector for back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview use case
            val preview = Preview.Builder()
                .setTargetFrameRate(Range(TARGET_FPS.toInt(), TARGET_FPS.toInt()))
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            // Image analysis use case for pose detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetFrameRate(Range(TARGET_FPS.toInt(), TARGET_FPS.toInt()))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer { poseResult ->
                        runOnUiThread {
                            viewModel.processPoseDetection(poseResult)
                        }
                    })
                }

            // Video capture use case
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer,
                videoCapture
            )

            Log.d(TAG, "Camera use cases bound successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            Toast.makeText(this, getString(R.string.error_camera_unavailable), Toast.LENGTH_LONG).show()
        }
    }

    private fun startRecording() {
        val selectedClub = binding.spinnerClubSelection.selectedItem.toString()
        val sessionId = "session_${System.currentTimeMillis()}"
        val userId = "user_${System.currentTimeMillis()}" // TODO: Get from user manager
        
        // Create recording session
        currentRecordingSession = RecordingSession(
            sessionId = sessionId,
            userId = userId,
            clubUsed = selectedClub,
            startTime = System.currentTimeMillis(),
            fps = TARGET_FPS,
            isRecording = true
        )
        
        isRecording = true
        recordingStartTime = SystemClock.elapsedRealtime()
        
        // Update UI
        binding.btnRecord.text = getString(R.string.stop_recording)
        binding.btnRecord.setBackgroundColor(ContextCompat.getColor(this, R.color.recording_button_stop))
        binding.tvRecordingTime.visibility = View.VISIBLE
        binding.btnAnalyze.isEnabled = false
        
        // Start recording timer
        startRecordingTimer()
        
        Log.d(TAG, "Recording started for session: $sessionId")
    }

    private fun stopRecording() {
        isRecording = false
        
        // Update recording session
        currentRecordingSession?.let { session ->
            currentRecordingSession = session.copy(
                endTime = System.currentTimeMillis(),
                isRecording = false
            )
        }
        
        // Update UI
        binding.btnRecord.text = getString(R.string.start_recording)
        binding.btnRecord.setBackgroundColor(ContextCompat.getColor(this, R.color.recording_button))
        binding.tvRecordingTime.visibility = View.GONE
        binding.btnAnalyze.isEnabled = true
        
        Log.d(TAG, "Recording stopped")
        
        // Show completion message
        Toast.makeText(this, getString(R.string.analysis_complete), Toast.LENGTH_SHORT).show()
    }

    private fun startRecordingTimer() {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            while (isRecording) {
                val elapsedTime = SystemClock.elapsedRealtime() - recordingStartTime
                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.tvRecordingTime.text = getString(R.string.recording_duration, timeString)
                
                kotlinx.coroutines.delay(RECORDING_TIME_UPDATE_INTERVAL)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }
}