package com.golfswing.vro.pixel.pose

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.AndroidPacketCreator
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.ProtoUtil
import com.google.mediapipe.solutions.pose.Pose
import com.google.mediapipe.solutions.pose.PoseOptions
import com.google.mediapipe.solutions.pose.PoseLandmark
import com.google.mediapipe.solutions.pose.PoseResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import com.golfswing.vro.pixel.metrics.*
import com.golfswing.vro.pixel.biomechanics.BiomechanicalCalculations
import com.golfswing.vro.pixel.benchmarking.ProfessionalBenchmarking

@Singleton
class GolfSwingPoseDetector @Inject constructor(
    private val context: Context
) {
    private val biomechanicalCalculations = BiomechanicalCalculations()
    private val professionalBenchmarking = ProfessionalBenchmarking()
    // Dedicated thread pool for pose detection
    private val poseExecutor = ThreadPoolExecutor(
        1, 2, 30L, TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()
    ) { runnable ->
        Thread(runnable, "PoseDetectionThread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }
    
    private val detectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var pose: Pose? = null
    private val _poseResult = MutableStateFlow<GolfSwingPoseResult?>(null)
    val poseResult: StateFlow<GolfSwingPoseResult?> = _poseResult.asStateFlow()
    
    private val _swingPhase = MutableStateFlow(SwingPhase.SETUP)
    val swingPhase: StateFlow<SwingPhase> = _swingPhase.asStateFlow()
    
    private val previousPositions = mutableListOf<PoseLandmark>()
    private var frameCount = 0
    private val swingPhaseHistory = mutableListOf<SwingPhase>()
    private val landmarkHistory = mutableListOf<List<PoseLandmark>>()
    private val enhancedMetricsHistory = mutableListOf<EnhancedSwingMetrics>()
    private val xFactorHistory = mutableListOf<Float>()
    private val frameRate = 30f
    
    // Performance optimization flags
    private val isProcessingFrame = AtomicBoolean(false)
    private val frameProcessingCount = AtomicLong(0)
    private val lastProcessingTime = AtomicLong(0)
    private val processingInterval = 50L // Process every 50ms minimum
    
    // Resource management
    private val bitmapPool = mutableListOf<Bitmap>()
    private val maxPoolSize = 5
    private val isInitialized = AtomicBoolean(false)

    enum class SwingPhase {
        SETUP, ADDRESS, TAKEAWAY, BACKSWING, TRANSITION, DOWNSWING, IMPACT, FOLLOW_THROUGH, FINISH
    }

    data class GolfSwingPoseResult(
        val landmarks: List<PoseLandmark>,
        val swingPhase: SwingPhase,
        val swingMetrics: SwingMetrics,
        val enhancedMetrics: EnhancedSwingMetrics,
        val professionalComparison: ProfessionalComparison,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class SwingMetrics(
        val shoulderAngle: Float,
        val hipAngle: Float,
        val kneeFlexion: Float,
        val armExtension: Float,
        val headPosition: Float,
        val weightDistribution: Float,
        val clubPlane: Float,
        val tempo: Float,
        val balance: Float
    )

    /**
     * Initialize MediaPipe Pose with golf-optimized settings
     */
    fun initialize() {
        detectionScope.launch {
            try {
                AndroidAssetUtil.initializeNativeAssetManager(context)
                
                val poseOptions = PoseOptions.builder()
                    .setStaticImageMode(false)
                    .setModelComplexity(1) // Balance between accuracy and performance
                    .setSmoothLandmarks(true)
                    .setMinDetectionConfidence(0.7f)
                    .setMinTrackingConfidence(0.5f)
                    .build()

                pose = Pose(context, poseOptions)
                pose?.setResultListener { result ->
                    processPoseResultAsync(result)
                }
                
                pose?.setErrorListener { error ->
                    // Handle pose detection errors
                    error.printStackTrace()
                }
                
                isInitialized.set(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Process camera frame for pose detection with throttling
     */
    fun processFrame(imageProxy: ImageProxy) {
        if (!isInitialized.get() || isProcessingFrame.get()) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Throttle processing to maintain performance
        if (currentTime - lastProcessingTime.get() < processingInterval) {
            return
        }
        
        isProcessingFrame.set(true)
        lastProcessingTime.set(currentTime)
        
        detectionScope.launch {
            try {
                val bitmap = getReusableBitmap(imageProxy)
                pose?.send(bitmap)
                frameCount++
                frameProcessingCount.incrementAndGet()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessingFrame.set(false)
            }
        }
    }
    
    /**
     * Get or create reusable bitmap from pool
     */
    private fun getReusableBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmap = bitmapPool.firstOrNull { 
            it.width == imageProxy.width && it.height == imageProxy.height && !it.isRecycled 
        } ?: run {
            val newBitmap = imageProxyToBitmap(imageProxy)
            if (bitmapPool.size < maxPoolSize) {
                bitmapPool.add(newBitmap)
            }
            newBitmap
        }
        
        // Update bitmap with new image data
        return imageProxyToBitmap(imageProxy)
    }

    /**
     * Process pose detection result asynchronously
     */
    private fun processPoseResultAsync(result: PoseResult) {
        detectionScope.launch {
            processPoseResult(result)
        }
    }
    
    /**
     * Process pose detection result and extract golf swing metrics
     */
    private fun processPoseResult(result: PoseResult) {
        val landmarks = result.poseLandmarks() ?: return
        
        try {
            // Store landmark history for biomechanical analysis
            synchronized(landmarkHistory) {
                landmarkHistory.add(landmarks)
                if (landmarkHistory.size > 60) { // Keep last 2 seconds at 30fps
                    landmarkHistory.removeAt(0)
                }
            }
            
            // Analyze swing phase with enhanced accuracy
            val currentPhase = analyzeSwingPhaseEnhanced(landmarks)
            _swingPhase.value = currentPhase
            
            // Calculate traditional swing metrics
            val metrics = calculateSwingMetrics(landmarks)
            
            // Calculate enhanced professional metrics
            val enhancedMetrics = calculateEnhancedSwingMetrics(landmarks)
            
            // Store enhanced metrics history
            synchronized(enhancedMetricsHistory) {
                enhancedMetricsHistory.add(enhancedMetrics)
                if (enhancedMetricsHistory.size > 100) {
                    enhancedMetricsHistory.removeAt(0)
                }
            }
            
            // Compare against professional benchmarks
            val professionalComparison = professionalBenchmarking.benchmarkSwing(
                enhancedMetrics,
                determineSkillLevel(enhancedMetrics),
                "MID_IRON" // TODO: Make this configurable
            )
            
            // Create result
            val golfSwingResult = GolfSwingPoseResult(
                landmarks = landmarks,
                swingPhase = currentPhase,
                swingMetrics = metrics,
                enhancedMetrics = enhancedMetrics,
                professionalComparison = professionalComparison
            )
            
            _poseResult.value = golfSwingResult
            
            // Store for historical analysis (thread-safe)
            synchronized(swingPhaseHistory) {
                swingPhaseHistory.add(currentPhase)
                if (swingPhaseHistory.size > 60) { // Keep last 2 seconds at 30fps
                    swingPhaseHistory.removeAt(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Analyze current swing phase with enhanced biomechanical accuracy
     */
    private fun analyzeSwingPhaseEnhanced(landmarks: List<PoseLandmark>): SwingPhase {
        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        
        // Calculate X-Factor for phase detection
        val xFactor = biomechanicalCalculations.calculateXFactor(leftShoulder, rightShoulder, leftHip, rightHip)
        synchronized(xFactorHistory) {
            xFactorHistory.add(xFactor)
            if (xFactorHistory.size > 30) {
                xFactorHistory.removeAt(0)
            }
        }
        
        // Enhanced phase detection using biomechanical markers
        val shoulderRotation = calculateShoulderRotation(leftShoulder, rightShoulder)
        val hipRotation = calculateHipRotation(leftHip, rightHip)
        val armPosition = calculateArmPosition(leftWrist, rightWrist, leftShoulder, rightShoulder)
        val wristHeight = calculateWristHeight(leftWrist, rightWrist, leftShoulder, rightShoulder)
        
        return when {
            isInSetupPosition(landmarks) -> SwingPhase.SETUP
            isInAddressPosition(landmarks) -> SwingPhase.ADDRESS
            isInTakeawayPositionEnhanced(armPosition, shoulderRotation, xFactor) -> SwingPhase.TAKEAWAY
            isInBackswingPositionEnhanced(armPosition, shoulderRotation, xFactor, wristHeight) -> SwingPhase.BACKSWING
            isInTransitionPositionEnhanced(landmarks, xFactor) -> SwingPhase.TRANSITION
            isInDownswingPositionEnhanced(armPosition, shoulderRotation, xFactor) -> SwingPhase.DOWNSWING
            isInImpactPositionEnhanced(landmarks, xFactor) -> SwingPhase.IMPACT
            isInFollowThroughPositionEnhanced(armPosition, shoulderRotation) -> SwingPhase.FOLLOW_THROUGH
            else -> SwingPhase.FINISH
        }
    }
    
    /**
     * Legacy swing phase analysis for backward compatibility
     */
    private fun analyzeSwingPhase(landmarks: List<PoseLandmark>): SwingPhase {
        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        
        // Calculate key angles and positions
        val shoulderRotation = calculateShoulderRotation(leftShoulder, rightShoulder)
        val hipRotation = calculateHipRotation(leftHip, rightHip)
        val armPosition = calculateArmPosition(leftWrist, rightWrist, leftShoulder, rightShoulder)
        
        // Determine swing phase based on body position
        return when {
            isInSetupPosition(landmarks) -> SwingPhase.SETUP
            isInAddressPosition(landmarks) -> SwingPhase.ADDRESS
            isInTakeawayPosition(armPosition, shoulderRotation) -> SwingPhase.TAKEAWAY
            isInBackswingPosition(armPosition, shoulderRotation) -> SwingPhase.BACKSWING
            isInTransitionPosition(landmarks) -> SwingPhase.TRANSITION
            isInDownswingPosition(armPosition, shoulderRotation) -> SwingPhase.DOWNSWING
            isInImpactPosition(landmarks) -> SwingPhase.IMPACT
            isInFollowThroughPosition(armPosition, shoulderRotation) -> SwingPhase.FOLLOW_THROUGH
            else -> SwingPhase.FINISH
        }
    }

    /**
     * Calculate enhanced professional swing metrics
     */
    private fun calculateEnhancedSwingMetrics(landmarks: List<PoseLandmark>): EnhancedSwingMetrics {
        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        
        // Calculate X-Factor (most important golf metric)
        val xFactor = biomechanicalCalculations.calculateXFactor(leftShoulder, rightShoulder, leftHip, rightHip)
        val xFactorStretch = biomechanicalCalculations.calculateXFactorStretch(xFactorHistory)
        
        // Analyze kinematic sequence
        val kinematicSequence = biomechanicalCalculations.analyzeKinematicSequence(
            landmarks, 
            landmarkHistory, 
            frameRate
        )
        
        // Calculate power metrics
        val powerMetrics = biomechanicalCalculations.calculatePowerMetrics(
            landmarks, 
            landmarkHistory, 
            frameRate
        )
        
        // Calculate attack angle
        val attackAngle = biomechanicalCalculations.calculateAttackAngle(
            leftWrist, 
            rightWrist, 
            getPreviousWristPositions(), 
            frameRate
        )
        
        // Calculate swing plane
        val swingPlane = biomechanicalCalculations.calculateSwingPlane(
            leftWrist, 
            rightWrist, 
            leftShoulder, 
            rightShoulder
        )
        
        // Calculate club path
        val clubPath = biomechanicalCalculations.calculateClubPath(
            leftWrist, 
            rightWrist, 
            getPreviousWristPositions()
        )
        
        // Calculate ground force analysis
        val groundForce = biomechanicalCalculations.calculateGroundForce(
            landmarks, 
            landmarkHistory, 
            frameRate
        )
        
        // Calculate energy transfer
        val energyTransfer = biomechanicalCalculations.calculateEnergyTransfer(
            landmarks, 
            landmarkHistory, 
            frameRate
        )
        
        // Calculate swing consistency
        val swingConsistency = biomechanicalCalculations.calculateSwingConsistency(
            enhancedMetricsHistory.lastOrNull() ?: createDefaultEnhancedMetrics(),
            enhancedMetricsHistory
        )
        
        // Calculate swing timing
        val swingTiming = biomechanicalCalculations.calculateSwingTiming(
            swingPhaseHistory.map { it.name }, 
            frameRate
        )
        
        // Calculate traditional metrics for compatibility
        val traditionalMetrics = calculateSwingMetrics(landmarks)
        
        // Create professional comparison placeholder (will be calculated in processPoseResult)
        val professionalComparison = ProfessionalComparison(
            overallScore = 0f,
            xFactorScore = 0f,
            kinematicScore = 0f,
            powerScore = 0f,
            consistencyScore = 0f,
            benchmarkCategory = SkillLevel.BEGINNER,
            improvementPotential = 0f,
            tourAverageComparison = emptyMap()
        )
        
        return EnhancedSwingMetrics(
            xFactor = xFactor,
            xFactorStretch = xFactorStretch,
            kinematicSequence = kinematicSequence,
            powerMetrics = powerMetrics,
            
            // Traditional metrics
            shoulderAngle = traditionalMetrics.shoulderAngle,
            hipAngle = traditionalMetrics.hipAngle,
            kneeFlexion = traditionalMetrics.kneeFlexion,
            armExtension = traditionalMetrics.armExtension,
            headPosition = traditionalMetrics.headPosition,
            weightDistribution = traditionalMetrics.weightDistribution,
            clubPlane = traditionalMetrics.clubPlane,
            tempo = traditionalMetrics.tempo,
            balance = traditionalMetrics.balance,
            
            // Professional metrics
            attackAngle = attackAngle,
            swingPlane = swingPlane,
            clubPath = clubPath,
            faceAngle = 0f, // Would need additional analysis
            dynamicLoft = 0f, // Would need additional analysis
            
            // Advanced metrics
            groundForce = groundForce,
            energyTransfer = energyTransfer,
            swingConsistency = swingConsistency,
            swingTiming = swingTiming,
            professionalComparison = professionalComparison
        )
    }
    
    /**
     * Calculate comprehensive swing metrics
     */
    private fun calculateSwingMetrics(landmarks: List<PoseLandmark>): SwingMetrics {
        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        val leftKnee = landmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = landmarks[PoseLandmark.RIGHT_KNEE]
        val nose = landmarks[PoseLandmark.NOSE]
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        val leftElbow = landmarks[PoseLandmark.LEFT_ELBOW]
        val rightElbow = landmarks[PoseLandmark.RIGHT_ELBOW]

        return SwingMetrics(
            shoulderAngle = calculateShoulderAngle(leftShoulder, rightShoulder),
            hipAngle = calculateHipAngle(leftHip, rightHip),
            kneeFlexion = calculateKneeFlexion(leftKnee, rightKnee, leftHip, rightHip),
            armExtension = calculateArmExtension(leftWrist, rightWrist, leftElbow, rightElbow),
            headPosition = calculateHeadPosition(nose, leftShoulder, rightShoulder),
            weightDistribution = calculateWeightDistribution(leftHip, rightHip, leftKnee, rightKnee),
            clubPlane = calculateClubPlane(leftWrist, rightWrist, leftShoulder, rightShoulder),
            tempo = calculateTempo(),
            balance = calculateBalance(landmarks)
        )
    }

    // Phase detection helper methods
    private fun isInSetupPosition(landmarks: List<PoseLandmark>): Boolean {
        // Check if player is in initial setup position
        val nose = landmarks[PoseLandmark.NOSE]
        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        
        // Head should be relatively centered
        val headCenter = (leftShoulder.x + rightShoulder.x) / 2
        return abs(nose.x - headCenter) < 0.05f
    }

    private fun isInAddressPosition(landmarks: List<PoseLandmark>): Boolean {
        // Check if player is addressing the ball
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        
        // Hands should be near address position
        return leftWrist.y > leftHip.y && rightWrist.y > leftHip.y
    }

    private fun isInTakeawayPosition(armPosition: Float, shoulderRotation: Float): Boolean {
        return armPosition > 0.1f && shoulderRotation > 0.05f
    }

    private fun isInBackswingPosition(armPosition: Float, shoulderRotation: Float): Boolean {
        return armPosition > 0.3f && shoulderRotation > 0.2f
    }

    private fun isInTransitionPosition(landmarks: List<PoseLandmark>): Boolean {
        // Detect the transition from backswing to downswing
        return swingPhaseHistory.takeLast(5).count { it == SwingPhase.BACKSWING } >= 3
    }

    private fun isInDownswingPosition(armPosition: Float, shoulderRotation: Float): Boolean {
        return armPosition < 0.3f && shoulderRotation < 0.2f
    }

    private fun isInImpactPosition(landmarks: List<PoseLandmark>): Boolean {
        // Detect impact position based on hand position and hip rotation
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        
        return abs(leftWrist.y - leftHip.y) < 0.1f && abs(rightWrist.y - leftHip.y) < 0.1f
    }

    private fun isInFollowThroughPosition(armPosition: Float, shoulderRotation: Float): Boolean {
        return armPosition < -0.1f && shoulderRotation < -0.05f
    }

    // Metric calculation methods
    private fun calculateShoulderAngle(leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): Float {
        val deltaY = rightShoulder.y - leftShoulder.y
        val deltaX = rightShoulder.x - leftShoulder.x
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    private fun calculateHipAngle(leftHip: PoseLandmark, rightHip: PoseLandmark): Float {
        val deltaY = rightHip.y - leftHip.y
        val deltaX = rightHip.x - leftHip.x
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    private fun calculateKneeFlexion(leftKnee: PoseLandmark, rightKnee: PoseLandmark, 
                                   leftHip: PoseLandmark, rightHip: PoseLandmark): Float {
        val leftFlexion = abs(leftKnee.y - leftHip.y)
        val rightFlexion = abs(rightKnee.y - rightHip.y)
        return (leftFlexion + rightFlexion) / 2f
    }

    private fun calculateArmExtension(leftWrist: PoseLandmark, rightWrist: PoseLandmark,
                                    leftElbow: PoseLandmark, rightElbow: PoseLandmark): Float {
        val leftExtension = calculateDistance(leftWrist, leftElbow)
        val rightExtension = calculateDistance(rightWrist, rightElbow)
        return (leftExtension + rightExtension) / 2f
    }

    private fun calculateHeadPosition(nose: PoseLandmark, leftShoulder: PoseLandmark, 
                                    rightShoulder: PoseLandmark): Float {
        val shoulderCenter = (leftShoulder.x + rightShoulder.x) / 2
        return abs(nose.x - shoulderCenter)
    }

    private fun calculateWeightDistribution(leftHip: PoseLandmark, rightHip: PoseLandmark,
                                         leftKnee: PoseLandmark, rightKnee: PoseLandmark): Float {
        val leftWeight = calculateDistance(leftHip, leftKnee)
        val rightWeight = calculateDistance(rightHip, rightKnee)
        return if (leftWeight + rightWeight > 0) leftWeight / (leftWeight + rightWeight) else 0.5f
    }

    private fun calculateClubPlane(leftWrist: PoseLandmark, rightWrist: PoseLandmark,
                                 leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): Float {
        val wristCenter = PoseLandmark.create(
            (leftWrist.x + rightWrist.x) / 2,
            (leftWrist.y + rightWrist.y) / 2,
            (leftWrist.z + rightWrist.z) / 2
        )
        val shoulderCenter = PoseLandmark.create(
            (leftShoulder.x + rightShoulder.x) / 2,
            (leftShoulder.y + rightShoulder.y) / 2,
            (leftShoulder.z + rightShoulder.z) / 2
        )
        
        val deltaY = wristCenter.y - shoulderCenter.y
        val deltaX = wristCenter.x - shoulderCenter.x
        return atan2(deltaY, deltaX) * 180f / PI.toFloat()
    }

    private fun calculateTempo(): Float {
        if (swingPhaseHistory.size < 10) return 0f
        
        val recentPhases = swingPhaseHistory.takeLast(10)
        val phaseChanges = recentPhases.zipWithNext().count { (prev, curr) -> prev != curr }
        return phaseChanges.toFloat() / 10f
    }

    private fun calculateBalance(landmarks: List<PoseLandmark>): Float {
        val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]
        val nose = landmarks[PoseLandmark.NOSE]
        
        val ankleCenter = (leftAnkle.x + rightAnkle.x) / 2
        return 1f - abs(nose.x - ankleCenter) // Higher value = better balance
    }

    // Utility methods
    private fun calculateDistance(point1: PoseLandmark, point2: PoseLandmark): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val dz = point1.z - point2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun calculateShoulderRotation(leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): Float {
        return rightShoulder.x - leftShoulder.x
    }

    private fun calculateHipRotation(leftHip: PoseLandmark, rightHip: PoseLandmark): Float {
        return rightHip.x - leftHip.x
    }

    private fun calculateArmPosition(leftWrist: PoseLandmark, rightWrist: PoseLandmark,
                                   leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): Float {
        val wristCenter = (leftWrist.x + rightWrist.x) / 2
        val shoulderCenter = (leftShoulder.x + rightShoulder.x) / 2
        return wristCenter - shoulderCenter
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Generate text description of pose data for Gemini Nano
     */
    fun generatePoseDescription(result: GolfSwingPoseResult): String {
        val metrics = result.swingMetrics
        
        return """
        Swing Phase: ${result.swingPhase}
        Shoulder Angle: ${metrics.shoulderAngle.toInt()}°
        Hip Angle: ${metrics.hipAngle.toInt()}°
        Knee Flexion: ${String.format("%.2f", metrics.kneeFlexion)}
        Arm Extension: ${String.format("%.2f", metrics.armExtension)}
        Head Position: ${String.format("%.2f", metrics.headPosition)}
        Weight Distribution: ${String.format("%.2f", metrics.weightDistribution)}
        Club Plane: ${metrics.clubPlane.toInt()}°
        Tempo: ${String.format("%.2f", metrics.tempo)}
        Balance: ${String.format("%.2f", metrics.balance)}
        """.trimIndent()
    }

    // Enhanced swing phase detection methods
    private fun calculateWristHeight(leftWrist: PoseLandmark, rightWrist: PoseLandmark,
                                   leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): Float {
        val wristCenter = (leftWrist.y + rightWrist.y) / 2f
        val shoulderCenter = (leftShoulder.y + rightShoulder.y) / 2f
        return wristCenter - shoulderCenter
    }
    
    private fun isInTakeawayPositionEnhanced(armPosition: Float, shoulderRotation: Float, xFactor: Float): Boolean {
        return armPosition > 0.05f && shoulderRotation > 0.03f && xFactor > 10f
    }
    
    private fun isInBackswingPositionEnhanced(armPosition: Float, shoulderRotation: Float, xFactor: Float, wristHeight: Float): Boolean {
        return armPosition > 0.2f && shoulderRotation > 0.15f && xFactor > 25f && wristHeight > 0.1f
    }
    
    private fun isInTransitionPositionEnhanced(landmarks: List<PoseLandmark>, xFactor: Float): Boolean {
        // Transition is when X-Factor reaches maximum and begins to decrease
        val recentXFactors = xFactorHistory.takeLast(5)
        val isXFactorDecreasing = recentXFactors.size >= 3 && 
                                 recentXFactors.last() < recentXFactors[recentXFactors.size - 2]
        return xFactor > 30f && isXFactorDecreasing
    }
    
    private fun isInDownswingPositionEnhanced(armPosition: Float, shoulderRotation: Float, xFactor: Float): Boolean {
        return armPosition < 0.2f && shoulderRotation < 0.15f && xFactor > 15f
    }
    
    private fun isInImpactPositionEnhanced(landmarks: List<PoseLandmark>, xFactor: Float): Boolean {
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        
        val wristCenter = (leftWrist.y + rightWrist.y) / 2f
        val hipCenter = (leftHip.y + rightHip.y) / 2f
        
        // Impact when hands are near hip level and X-Factor is releasing
        return abs(wristCenter - hipCenter) < 0.05f && xFactor < 20f
    }
    
    private fun isInFollowThroughPositionEnhanced(armPosition: Float, shoulderRotation: Float): Boolean {
        return armPosition < -0.05f && shoulderRotation < -0.03f
    }
    
    // Helper methods for enhanced metrics
    private fun getPreviousWristPositions(): List<Pair<PoseLandmark, PoseLandmark>> {
        return synchronized(landmarkHistory) {
            landmarkHistory.takeLast(10).map { landmarks ->
                Pair(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.RIGHT_WRIST])
            }
        }
    }
    
    private fun createDefaultEnhancedMetrics(): EnhancedSwingMetrics {
        return EnhancedSwingMetrics(
            xFactor = 0f,
            xFactorStretch = 0f,
            kinematicSequence = KinematicSequence(
                sequenceOrder = emptyList(),
                peakVelocityOrder = emptyList(),
                sequenceEfficiency = 0f,
                isOptimalSequence = false,
                sequenceGaps = emptyList()
            ),
            powerMetrics = PowerMetrics(
                totalPower = 0f,
                peakPower = 0f,
                powerTransferEfficiency = 0f,
                groundForceContribution = 0f,
                rotationalPower = 0f,
                linearPower = 0f,
                powerSequence = emptyList()
            ),
            shoulderAngle = 0f,
            hipAngle = 0f,
            kneeFlexion = 0f,
            armExtension = 0f,
            headPosition = 0f,
            weightDistribution = 0f,
            clubPlane = 0f,
            tempo = 0f,
            balance = 0f,
            attackAngle = 0f,
            swingPlane = 0f,
            clubPath = 0f,
            faceAngle = 0f,
            dynamicLoft = 0f,
            groundForce = GroundForce(
                verticalForce = 0f,
                horizontalForce = 0f,
                forceDistribution = WeightDistribution(0.5f, 0.5f, 0f, 0f, 0f),
                forceSequence = emptyList(),
                groundForceIndex = 0f
            ),
            energyTransfer = EnergyTransfer(
                kineticEnergy = 0f,
                potentialEnergy = 0f,
                energyLoss = 0f,
                transferEfficiency = 0f,
                energySequence = emptyList()
            ),
            swingConsistency = SwingConsistency(
                overallConsistency = 0f,
                temporalConsistency = 0f,
                spatialConsistency = 0f,
                kinematicConsistency = 0f,
                metricVariations = emptyMap(),
                repeatabilityScore = 0f,
                consistencyTrend = ConsistencyTrend(TrendDirection.STABLE, 0f, 0f, 0f)
            ),
            swingTiming = SwingTiming(0f, 0f, 0f, 0f, 0f, 0f, emptyMap()),
            professionalComparison = ProfessionalComparison(
                overallScore = 0f,
                xFactorScore = 0f,
                kinematicScore = 0f,
                powerScore = 0f,
                consistencyScore = 0f,
                benchmarkCategory = SkillLevel.BEGINNER,
                improvementPotential = 0f,
                tourAverageComparison = emptyMap()
            )
        )
    }
    
    private fun determineSkillLevel(metrics: EnhancedSwingMetrics): SkillLevel {
        val overallScore = calculateSimpleOverallScore(metrics)
        return when {
            overallScore >= 0.9f -> SkillLevel.TOUR_LEVEL
            overallScore >= 0.8f -> SkillLevel.PROFESSIONAL
            overallScore >= 0.7f -> SkillLevel.SCRATCH
            overallScore >= 0.6f -> SkillLevel.ADVANCED
            overallScore >= 0.4f -> SkillLevel.INTERMEDIATE
            else -> SkillLevel.BEGINNER
        }
    }
    
    private fun calculateSimpleOverallScore(metrics: EnhancedSwingMetrics): Float {
        val xFactorScore = (metrics.xFactor / 50f).coerceIn(0f, 1f)
        val consistencyScore = metrics.swingConsistency.overallConsistency
        val balanceScore = metrics.balance
        val sequenceScore = metrics.kinematicSequence.sequenceEfficiency
        
        return (xFactorScore + consistencyScore + balanceScore + sequenceScore) / 4f
    }
    
    /**
     * Get enhanced swing metrics history for analysis
     */
    fun getEnhancedMetricsHistory(): List<EnhancedSwingMetrics> {
        return synchronized(enhancedMetricsHistory) {
            enhancedMetricsHistory.toList()
        }
    }
    
    /**
     * Get X-Factor history for analysis
     */
    fun getXFactorHistory(): List<Float> {
        return synchronized(xFactorHistory) {
            xFactorHistory.toList()
        }
    }
    
    /**
     * Generate enhanced pose description for AI analysis
     */
    fun generateEnhancedPoseDescription(result: GolfSwingPoseResult): String {
        val metrics = result.enhancedMetrics
        val comparison = result.professionalComparison
        
        return \"\"\"
        Golf Swing Analysis - ${result.swingPhase}
        
        Core Biomechanics:
        - X-Factor: ${metrics.xFactor.toInt()}° (Stretch: ${metrics.xFactorStretch.toInt()}°)
        - Kinematic Sequence: ${if (metrics.kinematicSequence.isOptimalSequence) "Optimal" else "Suboptimal"} (${(metrics.kinematicSequence.sequenceEfficiency * 100).toInt()}%)
        - Power Generation: ${metrics.powerMetrics.totalPower.toInt()}W (Peak: ${metrics.powerMetrics.peakPower.toInt()}W)
        
        Swing Mechanics:
        - Attack Angle: ${metrics.attackAngle.toInt()}°
        - Swing Plane: ${metrics.swingPlane.toInt()}°
        - Club Path: ${metrics.clubPath.toInt()}°
        - Ground Force Index: ${(metrics.groundForce.groundForceIndex * 100).toInt()}%
        
        Consistency & Timing:
        - Overall Consistency: ${(metrics.swingConsistency.overallConsistency * 100).toInt()}%
        - Tempo: ${String.format("%.1f", metrics.tempo)}
        - Balance: ${(metrics.balance * 100).toInt()}%
        
        Professional Comparison:
        - Overall Score: ${comparison.overallScore.toInt()}/10
        - Skill Level: ${comparison.benchmarkCategory}
        - Improvement Potential: ${(comparison.improvementPotential * 100).toInt()}%
        \"\"\".trimIndent()
    }

    /**
     * Release resources with proper cleanup
     */
    fun release() {
        try {
            // Cancel all coroutines
            detectionScope.cancel()
            
            // Close MediaPipe pose
            pose?.close()
            pose = null
            
            // Clean up bitmap pool
            bitmapPool.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            bitmapPool.clear()
            
            // Shutdown executor
            poseExecutor.shutdown()
            try {
                if (!poseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    poseExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                poseExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            }
            
            // Reset state
            isInitialized.set(false)
            isProcessingFrame.set(false)
            frameCount = 0
            frameProcessingCount.set(0)
            lastProcessingTime.set(0)
            
            // Clear history
            synchronized(swingPhaseHistory) {
                swingPhaseHistory.clear()
            }
            synchronized(landmarkHistory) {
                landmarkHistory.clear()
            }
            synchronized(enhancedMetricsHistory) {
                enhancedMetricsHistory.clear()
            }
            synchronized(xFactorHistory) {
                xFactorHistory.clear()
            }
            previousPositions.clear()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get processing statistics
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            totalFrames = frameCount,
            processedFrames = frameProcessingCount.get(),
            isProcessing = isProcessingFrame.get(),
            averageInterval = if (frameProcessingCount.get() > 0) 
                (System.currentTimeMillis() - lastProcessingTime.get()) / frameProcessingCount.get() 
                else 0L
        )
    }
    
    data class ProcessingStats(
        val totalFrames: Int,
        val processedFrames: Long,
        val isProcessing: Boolean,
        val averageInterval: Long
    )
}