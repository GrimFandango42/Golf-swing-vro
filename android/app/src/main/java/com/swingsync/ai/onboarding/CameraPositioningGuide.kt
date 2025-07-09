package com.swingsync.ai.onboarding

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.swingsync.ai.ml.PoseDetector
import com.swingsync.ai.utils.MathUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Intelligent camera positioning guide using computer vision
 * to help users position their camera optimally for swing analysis
 */
@Singleton
class CameraPositioningGuide @Inject constructor(
    private val poseDetector: PoseDetector
) {

    data class Guidance(
        val step: PositioningStep,
        val instruction: String,
        val visualCue: VisualCue,
        val isOptimal: Boolean,
        val confidence: Float,
        val adjustments: List<Adjustment>
    )

    enum class PositioningStep {
        DETECT_PERSON,
        ADJUST_DISTANCE,
        ADJUST_HEIGHT,
        ADJUST_ANGLE,
        PERFECT_POSITION,
        READY_TO_SWING
    }

    data class VisualCue(
        val type: CueType,
        val position: Point,
        val size: Float,
        val color: Int,
        val animation: AnimationType
    )

    enum class CueType {
        CIRCLE_OVERLAY,
        ARROW_DIRECTION,
        GRID_LINES,
        SILHOUETTE_GUIDE,
        DISTANCE_INDICATOR
    }

    enum class AnimationType {
        PULSE, BOUNCE, FADE, SLIDE, ROTATE
    }

    data class Adjustment(
        val type: AdjustmentType,
        val direction: Direction,
        val amount: Float,
        val priority: Priority
    )

    enum class AdjustmentType {
        MOVE_CLOSER, MOVE_FARTHER, RAISE_CAMERA, LOWER_CAMERA, 
        ROTATE_LEFT, ROTATE_RIGHT, MOVE_LEFT, MOVE_RIGHT
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD, CLOCKWISE, COUNTER_CLOCKWISE
    }

    enum class Priority {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    private var currentStep = PositioningStep.DETECT_PERSON
    private var positionHistory = mutableListOf<PersonPosition>()
    private var optimalPosition: PersonPosition? = null

    data class PersonPosition(
        val boundingBox: RectF,
        val keyPoints: Map<String, Point>,
        val distance: Float,
        val angle: Float,
        val height: Float,
        val timestamp: Long
    )

    /**
     * Start intelligent camera positioning guidance
     */
    fun startGuidance(): Flow<Guidance> = flow {
        currentStep = PositioningStep.DETECT_PERSON
        positionHistory.clear()
        
        while (currentStep != PositioningStep.READY_TO_SWING) {
            val guidance = when (currentStep) {
                PositioningStep.DETECT_PERSON -> generateDetectionGuidance()
                PositioningStep.ADJUST_DISTANCE -> generateDistanceGuidance()
                PositioningStep.ADJUST_HEIGHT -> generateHeightGuidance()
                PositioningStep.ADJUST_ANGLE -> generateAngleGuidance()
                PositioningStep.PERFECT_POSITION -> generatePerfectPositionGuidance()
                PositioningStep.READY_TO_SWING -> generateReadyGuidance()
            }
            
            emit(guidance)
            delay(100) // 10 FPS guidance updates
        }
    }

    /**
     * Analyze camera frame for positioning guidance
     */
    fun analyzeFrame(imageProxy: ImageProxy) {
        val pose = poseDetector.detectPose(imageProxy)
        
        if (pose != null) {
            val position = extractPersonPosition(pose, imageProxy)
            positionHistory.add(position)
            
            // Keep only recent positions
            if (positionHistory.size > 30) {
                positionHistory.removeFirst()
            }
            
            updatePositioningStep(position)
        }
    }

    /**
     * Generate detection guidance
     */
    private fun generateDetectionGuidance(): Guidance {
        return Guidance(
            step = PositioningStep.DETECT_PERSON,
            instruction = "Stand in front of the camera so I can see you",
            visualCue = VisualCue(
                type = CueType.SILHOUETTE_GUIDE,
                position = Point(540, 960), // Center of typical phone screen
                size = 300f,
                color = 0x80FFFFFF,
                animation = AnimationType.PULSE
            ),
            isOptimal = false,
            confidence = 0.1f,
            adjustments = listOf(
                Adjustment(
                    type = AdjustmentType.MOVE_CLOSER,
                    direction = Direction.FORWARD,
                    amount = 0.5f,
                    priority = Priority.HIGH
                )
            )
        )
    }

    /**
     * Generate distance guidance
     */
    private fun generateDistanceGuidance(): Guidance {
        val currentPosition = positionHistory.lastOrNull()
        val optimalDistance = 8f // 8 feet for full swing analysis
        
        return if (currentPosition != null) {
            val distance = currentPosition.distance
            val distanceError = distance - optimalDistance
            
            when {
                abs(distanceError) <= 0.5f -> {
                    currentStep = PositioningStep.ADJUST_HEIGHT
                    Guidance(
                        step = PositioningStep.ADJUST_DISTANCE,
                        instruction = "Perfect distance! Now let's adjust the height",
                        visualCue = VisualCue(
                            type = CueType.CIRCLE_OVERLAY,
                            position = Point(540, 960),
                            size = 200f,
                            color = 0x8000FF00,
                            animation = AnimationType.PULSE
                        ),
                        isOptimal = true,
                        confidence = 0.9f,
                        adjustments = emptyList()
                    )
                }
                distanceError > 0.5f -> {
                    Guidance(
                        step = PositioningStep.ADJUST_DISTANCE,
                        instruction = "Move closer to the camera",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(540, 1200),
                            size = 100f,
                            color = 0xFF00BFFF,
                            animation = AnimationType.BOUNCE
                        ),
                        isOptimal = false,
                        confidence = 0.7f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.MOVE_CLOSER,
                                direction = Direction.FORWARD,
                                amount = distanceError,
                                priority = Priority.HIGH
                            )
                        )
                    )
                }
                else -> {
                    Guidance(
                        step = PositioningStep.ADJUST_DISTANCE,
                        instruction = "Move back a bit",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(540, 720),
                            size = 100f,
                            color = 0xFF00BFFF,
                            animation = AnimationType.BOUNCE
                        ),
                        isOptimal = false,
                        confidence = 0.7f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.MOVE_FARTHER,
                                direction = Direction.BACKWARD,
                                amount = abs(distanceError),
                                priority = Priority.HIGH
                            )
                        )
                    )
                }
            }
        } else {
            generateDetectionGuidance()
        }
    }

    /**
     * Generate height guidance
     */
    private fun generateHeightGuidance(): Guidance {
        val currentPosition = positionHistory.lastOrNull()
        val optimalHeight = 0.6f // Camera at 60% of person's height
        
        return if (currentPosition != null) {
            val heightRatio = currentPosition.height
            val heightError = heightRatio - optimalHeight
            
            when {
                abs(heightError) <= 0.1f -> {
                    currentStep = PositioningStep.ADJUST_ANGLE
                    Guidance(
                        step = PositioningStep.ADJUST_HEIGHT,
                        instruction = "Great height! Now let's get the perfect angle",
                        visualCue = VisualCue(
                            type = CueType.GRID_LINES,
                            position = Point(540, 960),
                            size = 400f,
                            color = 0x8000FF00,
                            animation = AnimationType.FADE
                        ),
                        isOptimal = true,
                        confidence = 0.9f,
                        adjustments = emptyList()
                    )
                }
                heightError > 0.1f -> {
                    Guidance(
                        step = PositioningStep.ADJUST_HEIGHT,
                        instruction = "Lower the camera a bit",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(540, 1400),
                            size = 80f,
                            color = 0xFFFF6B00,
                            animation = AnimationType.SLIDE
                        ),
                        isOptimal = false,
                        confidence = 0.8f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.LOWER_CAMERA,
                                direction = Direction.DOWN,
                                amount = heightError,
                                priority = Priority.MEDIUM
                            )
                        )
                    )
                }
                else -> {
                    Guidance(
                        step = PositioningStep.ADJUST_HEIGHT,
                        instruction = "Raise the camera up",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(540, 400),
                            size = 80f,
                            color = 0xFFFF6B00,
                            animation = AnimationType.SLIDE
                        ),
                        isOptimal = false,
                        confidence = 0.8f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.RAISE_CAMERA,
                                direction = Direction.UP,
                                amount = abs(heightError),
                                priority = Priority.MEDIUM
                            )
                        )
                    )
                }
            }
        } else {
            generateDetectionGuidance()
        }
    }

    /**
     * Generate angle guidance
     */
    private fun generateAngleGuidance(): Guidance {
        val currentPosition = positionHistory.lastOrNull()
        val optimalAngle = 90f // Perpendicular to swing plane
        
        return if (currentPosition != null) {
            val angle = currentPosition.angle
            val angleError = angle - optimalAngle
            
            when {
                abs(angleError) <= 5f -> {
                    currentStep = PositioningStep.PERFECT_POSITION
                    Guidance(
                        step = PositioningStep.ADJUST_ANGLE,
                        instruction = "Perfect angle! Position is optimal",
                        visualCue = VisualCue(
                            type = CueType.CIRCLE_OVERLAY,
                            position = Point(540, 960),
                            size = 300f,
                            color = 0x8000FF00,
                            animation = AnimationType.PULSE
                        ),
                        isOptimal = true,
                        confidence = 0.95f,
                        adjustments = emptyList()
                    )
                }
                angleError > 5f -> {
                    Guidance(
                        step = PositioningStep.ADJUST_ANGLE,
                        instruction = "Rotate camera slightly left",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(200, 960),
                            size = 60f,
                            color = 0xFFFF1744,
                            animation = AnimationType.ROTATE
                        ),
                        isOptimal = false,
                        confidence = 0.8f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.ROTATE_LEFT,
                                direction = Direction.COUNTER_CLOCKWISE,
                                amount = angleError,
                                priority = Priority.MEDIUM
                            )
                        )
                    )
                }
                else -> {
                    Guidance(
                        step = PositioningStep.ADJUST_ANGLE,
                        instruction = "Rotate camera slightly right",
                        visualCue = VisualCue(
                            type = CueType.ARROW_DIRECTION,
                            position = Point(880, 960),
                            size = 60f,
                            color = 0xFFFF1744,
                            animation = AnimationType.ROTATE
                        ),
                        isOptimal = false,
                        confidence = 0.8f,
                        adjustments = listOf(
                            Adjustment(
                                type = AdjustmentType.ROTATE_RIGHT,
                                direction = Direction.CLOCKWISE,
                                amount = abs(angleError),
                                priority = Priority.MEDIUM
                            )
                        )
                    )
                }
            }
        } else {
            generateDetectionGuidance()
        }
    }

    /**
     * Generate perfect position guidance
     */
    private fun generatePerfectPositionGuidance(): Guidance {
        val currentPosition = positionHistory.lastOrNull()
        optimalPosition = currentPosition
        
        // Wait for stable position
        val isStable = isPositionStable()
        
        return if (isStable) {
            currentStep = PositioningStep.READY_TO_SWING
            Guidance(
                step = PositioningStep.PERFECT_POSITION,
                instruction = "Perfect! You're all set to record your swing",
                visualCue = VisualCue(
                    type = CueType.CIRCLE_OVERLAY,
                    position = Point(540, 960),
                    size = 400f,
                    color = 0x8000FF00,
                    animation = AnimationType.PULSE
                ),
                isOptimal = true,
                confidence = 1.0f,
                adjustments = emptyList()
            )
        } else {
            Guidance(
                step = PositioningStep.PERFECT_POSITION,
                instruction = "Hold steady for a moment...",
                visualCue = VisualCue(
                    type = CueType.CIRCLE_OVERLAY,
                    position = Point(540, 960),
                    size = 350f,
                    color = 0x80FFFF00,
                    animation = AnimationType.PULSE
                ),
                isOptimal = false,
                confidence = 0.7f,
                adjustments = emptyList()
            )
        }
    }

    /**
     * Generate ready guidance
     */
    private fun generateReadyGuidance(): Guidance {
        return Guidance(
            step = PositioningStep.READY_TO_SWING,
            instruction = "Perfect setup! Ready to analyze your swing",
            visualCue = VisualCue(
                type = CueType.CIRCLE_OVERLAY,
                position = Point(540, 960),
                size = 500f,
                color = 0x8000FF00,
                animation = AnimationType.PULSE
            ),
            isOptimal = true,
            confidence = 1.0f,
            adjustments = emptyList()
        )
    }

    /**
     * Extract person position from pose detection
     */
    private fun extractPersonPosition(pose: Any, imageProxy: ImageProxy): PersonPosition {
        // This would extract actual position data from the pose
        // For now, return mock data
        return PersonPosition(
            boundingBox = RectF(100f, 200f, 500f, 800f),
            keyPoints = mapOf(
                "head" to Point(300, 250),
                "shoulders" to Point(300, 400),
                "hips" to Point(300, 600),
                "feet" to Point(300, 750)
            ),
            distance = 7.5f,
            angle = 88f,
            height = 0.65f,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Update positioning step based on current position
     */
    private fun updatePositioningStep(position: PersonPosition) {
        // Logic to automatically advance through steps
        when (currentStep) {
            PositioningStep.DETECT_PERSON -> {
                currentStep = PositioningStep.ADJUST_DISTANCE
            }
            else -> {
                // Steps are advanced in their respective guidance methods
            }
        }
    }

    /**
     * Check if position is stable
     */
    private fun isPositionStable(): Boolean {
        if (positionHistory.size < 10) return false
        
        val recent = positionHistory.takeLast(10)
        val avgDistance = recent.map { it.distance }.average()
        val avgAngle = recent.map { it.angle }.average()
        val avgHeight = recent.map { it.height }.average()
        
        return recent.all { position ->
            abs(position.distance - avgDistance) < 0.2f &&
            abs(position.angle - avgAngle) < 3f &&
            abs(position.height - avgHeight) < 0.05f
        }
    }

    /**
     * Get optimal position settings
     */
    fun getOptimalPosition(): PersonPosition? = optimalPosition

    /**
     * Reset guidance
     */
    fun reset() {
        currentStep = PositioningStep.DETECT_PERSON
        positionHistory.clear()
        optimalPosition = null
    }
}