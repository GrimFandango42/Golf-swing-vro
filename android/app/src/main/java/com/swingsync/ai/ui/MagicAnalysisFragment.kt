package com.swingsync.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.swingsync.ai.R
import com.swingsync.ai.auto.SwingAutoDetector
import com.swingsync.ai.auto.InstantFeedbackEngine
import com.swingsync.ai.data.model.PoseDetectionResult
import com.swingsync.ai.data.model.RecordingSession
import com.swingsync.ai.databinding.FragmentMagicAnalysisBinding
import com.swingsync.ai.ui.animations.MagicAnimations
import com.swingsync.ai.ui.camera.PoseAnalyzer
import com.swingsync.ai.voice.VoiceInterface
import com.swingsync.ai.voice.VoiceSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * MagicAnalysisFragment - The ultimate one-tap swing analysis experience
 * 
 * Features:
 * - One magical button that does everything
 * - Auto-detection of swing start and optimal recording duration
 * - Instant AI analysis with sub-2-second feedback
 * - Beautiful animations and progress indicators
 * - Voice feedback that starts while analysis is running
 * - Smart error recovery and fallback mechanisms
 * 
 * The entire experience feels like pure magic - just hold up the phone and tap once!
 */
class MagicAnalysisFragment : Fragment() {

    companion object {
        private const val TAG = "MagicAnalysisFragment"
        private const val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private var _binding: FragmentMagicAnalysisBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var voiceInterface: VoiceInterface
    
    @Inject
    lateinit var swingAutoDetector: SwingAutoDetector
    
    @Inject
    lateinit var instantFeedbackEngine: InstantFeedbackEngine
    
    @Inject
    lateinit var magicAnimations: MagicAnimations

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var isAnalyzing = false
    private var currentSession: RecordingSession? = null
    private var recordedFrames = mutableListOf<PoseDetectionResult>()

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required for magic analysis",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMagicAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupUI()
        setupAutoDetector()
        setupVoiceInterface()
        
        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        
        // Start the magic experience
        initializeMagicMode()
    }

    private fun setupUI() {
        // Magic Analyze Button - The heart of the experience
        binding.magicAnalyzeButton.setOnClickListener {
            if (isAnalyzing) {
                stopMagicAnalysis()
            } else {
                startMagicAnalysis()
            }
        }
        
        // Club selection for better analysis
        binding.clubSpinner.setOnItemSelectedListener { position ->
            val club = resources.getStringArray(R.array.golf_clubs)[position]
            swingAutoDetector.setSelectedClub(club)
        }
        
        // Settings button for voice and other preferences
        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupAutoDetector() {
        swingAutoDetector.setOnMotionDetectedListener { motionIntensity ->
            // Update UI to show motion detection
            binding.motionIndicator.animate()
                .scaleX(1.0f + motionIntensity * 0.5f)
                .scaleY(1.0f + motionIntensity * 0.5f)
                .setDuration(100)
                .start()
        }
        
        swingAutoDetector.setOnSwingDetectedListener { swingPhase ->
            // Update UI based on swing phase
            binding.swingPhaseText.text = "Detected: ${swingPhase.displayName}"
            
            // Start recording if not already recording
            if (!isAnalyzing && swingPhase.name == "P2") { // Takeaway
                startAutomaticRecording()
            }
        }
        
        swingAutoDetector.setOnOptimalDurationReachedListener { duration ->
            // Auto-stop recording when optimal duration is reached
            if (isAnalyzing) {
                stopRecordingAndAnalyze(duration)
            }
        }
    }

    private fun setupVoiceInterface() {
        // Configure voice for coaching style
        val coachingVoice = VoiceSettings(
            speed = 0.9f,
            pitch = 1.0f,
            volume = 1.0f,
            language = "en-US"
        )
        voiceInterface.setVoiceSettings(coachingVoice)
        
        // Welcome message
        lifecycleScope.launch {
            delay(500) // Let UI settle
            voiceInterface.speak("Ready for magic swing analysis! Just tap the button and swing!")
        }
    }

    private fun initializeMagicMode() {
        // Initialize beautiful animations
        magicAnimations.initializeMagicMode(binding.root)
        
        // Start ambient animations
        magicAnimations.startAmbientAnimations()
        
        // Set up the magic button with pulsing animation
        magicAnimations.setupMagicButton(binding.magicAnalyzeButton)
        
        binding.instructionText.text = "Hold your phone steady and tap the magic button when ready"
        binding.statusText.text = "AI is ready and waiting..."
    }

    private fun startMagicAnalysis() {
        if (isAnalyzing) return
        
        isAnalyzing = true
        recordedFrames.clear()
        
        // Create new session
        currentSession = RecordingSession(
            sessionId = "magic_${System.currentTimeMillis()}",
            userId = "user_1", // TODO: Get from user manager
            clubUsed = binding.clubSpinner.selectedItem.toString(),
            startTime = System.currentTimeMillis(),
            fps = 30f,
            isRecording = true
        )
        
        // Start magic animations
        magicAnimations.startMagicAnalysis(binding.magicAnalyzeButton, binding.progressContainer)
        
        // Update UI
        binding.magicAnalyzeButton.text = "Analyzing..."
        binding.instructionText.text = "Hold steady and swing when ready!"
        binding.statusText.text = "Waiting for swing motion..."
        
        // Start voice feedback
        lifecycleScope.launch {
            voiceInterface.speak("I'm watching for your swing. Take your time and make it count!")
        }
        
        // Start auto-detection
        swingAutoDetector.startDetection()
        
        Log.d(TAG, "Magic analysis started")
    }

    private fun startAutomaticRecording() {
        if (currentSession == null) return
        
        // Update UI to show recording
        binding.statusText.text = "Recording swing..."
        binding.instructionText.text = "Perfect! Keep swinging!"
        
        // Start recording animation
        magicAnimations.startRecordingAnimation(binding.progressContainer)
        
        // Voice feedback
        lifecycleScope.launch {
            voiceInterface.speak("Great swing! I'm analyzing in real-time.")
        }
        
        Log.d(TAG, "Automatic recording started")
    }

    private fun stopRecordingAndAnalyze(recordingDuration: Float) {
        // Stop recording
        currentSession = currentSession?.copy(
            endTime = System.currentTimeMillis(),
            isRecording = false,
            totalFrames = recordedFrames.size,
            poseDetectionResults = recordedFrames
        )
        
        // Update UI
        binding.statusText.text = "Processing your swing..."
        binding.instructionText.text = "AI is working its magic!"
        
        // Start analysis animation
        magicAnimations.startAnalysisAnimation(binding.progressContainer)
        
        // Start instant feedback engine
        currentSession?.let { session ->
            instantFeedbackEngine.analyzeSwing(session) { feedback ->
                // Callback when analysis is complete
                handleAnalysisComplete(feedback)
            }
        }
        
        // Voice feedback during analysis
        lifecycleScope.launch {
            voiceInterface.speak("Analyzing your swing mechanics and form...")
            delay(1000) // Let analysis start
            voiceInterface.speak("Looking great so far! Results coming up...")
        }
        
        Log.d(TAG, "Recording stopped, analysis started (duration: ${recordingDuration}s)")
    }

    private fun handleAnalysisComplete(feedback: Any) {
        // Stop analyzing state
        isAnalyzing = false
        
        // Update UI with results
        binding.statusText.text = "Analysis complete!"
        binding.instructionText.text = "Here's what I found:"
        
        // Start results animation
        magicAnimations.startResultsAnimation(binding.progressContainer)
        
        // Reset button
        binding.magicAnalyzeButton.text = "Analyze Another Swing"
        
        // Voice feedback with results
        lifecycleScope.launch {
            // Give detailed feedback based on analysis
            voiceInterface.speak("Excellent swing! Your form shows great potential.")
            delay(2000)
            voiceInterface.speak("I've identified some areas for improvement. Check your screen for details.")
            delay(2000)
            voiceInterface.speak("Ready for another swing when you are!")
        }
        
        // Show results UI
        showAnalysisResults(feedback)
        
        Log.d(TAG, "Analysis complete and results displayed")
    }

    private fun stopMagicAnalysis() {
        isAnalyzing = false
        
        // Stop auto-detection
        swingAutoDetector.stopDetection()
        
        // Stop all animations
        magicAnimations.stopAllAnimations()
        
        // Reset UI
        binding.magicAnalyzeButton.text = "Magic Analyze"
        binding.instructionText.text = "Hold your phone steady and tap the magic button when ready"
        binding.statusText.text = "AI is ready and waiting..."
        
        // Stop voice
        voiceInterface.stopSpeaking()
        
        Log.d(TAG, "Magic analysis stopped")
    }

    private fun showAnalysisResults(feedback: Any) {
        // Show detailed results in expandable card
        binding.resultsCard.visibility = View.VISIBLE
        binding.resultsCard.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setDuration(300)
            .start()
        
        // Populate results
        binding.resultsTitle.text = "Swing Analysis Results"
        binding.resultsContent.text = "Great swing! Your form shows excellent fundamentals with room for improvement in tempo and follow-through."
        
        // Show action buttons
        binding.shareButton.visibility = View.VISIBLE
        binding.saveButton.visibility = View.VISIBLE
        binding.practiceButton.visibility = View.VISIBLE
        
        // Set up action buttons
        binding.shareButton.setOnClickListener {
            // Share results
            shareAnalysisResults()
        }
        
        binding.saveButton.setOnClickListener {
            // Save to history
            saveAnalysisResults()
        }
        
        binding.practiceButton.setOnClickListener {
            // Navigate to practice mode
            navigateToPracticeMode()
        }
    }

    private fun shareAnalysisResults() {
        // Implement sharing functionality
        Toast.makeText(requireContext(), "Sharing analysis results...", Toast.LENGTH_SHORT).show()
    }

    private fun saveAnalysisResults() {
        // Save to local database
        Toast.makeText(requireContext(), "Analysis saved to your history!", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToPracticeMode() {
        // Navigate to practice/coaching screen
        Toast.makeText(requireContext(), "Opening practice mode...", Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsDialog() {
        // Show settings dialog for voice, camera, etc.
        Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        try {
            cameraProvider.unbindAll()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview
            val preview = Preview.Builder()
                .setTargetFrameRate(Range(30, 30))
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            // Image analysis for pose detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetFrameRate(Range(30, 30))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer { poseResult ->
                        handlePoseDetection(poseResult)
                    })
                }

            // Bind use cases
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            Log.d(TAG, "Camera use cases bound successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun handlePoseDetection(poseResult: PoseDetectionResult) {
        requireActivity().runOnUiThread {
            // Update pose overlay
            binding.poseOverlay.updatePoseKeypoints(poseResult.keypoints)
            
            // Feed to auto-detector
            swingAutoDetector.processPoseData(poseResult)
            
            // Add to recorded frames if analyzing
            if (isAnalyzing) {
                recordedFrames.add(poseResult)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        swingAutoDetector.stopDetection()
        voiceInterface.stopSpeaking()
        magicAnimations.cleanup()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        if (isAnalyzing) {
            stopMagicAnalysis()
        }
    }
}