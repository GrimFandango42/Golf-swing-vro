package com.swingsync.ai.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingsync.ai.data.UserProfile
import com.swingsync.ai.ml.SwingAnalyzer
import com.swingsync.ai.utils.HapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Magical onboarding wizard that guides users through setup in under 2 minutes
 * with zero confusion and maximum delight
 */
class OnboardingWizard @Inject constructor(
    private val autoSetupEngine: AutoSetupEngine,
    private val cameraGuide: CameraPositioningGuide,
    private val profileGenerator: SmartProfileGenerator,
    private val swingAnalyzer: SwingAnalyzer,
    private val hapticFeedback: HapticFeedback
) : ViewModel() {

    // Onboarding states
    sealed class OnboardingState {
        object Welcome : OnboardingState()
        object AutoDetecting : OnboardingState()
        object QuickQuestions : OnboardingState()
        object CameraSetup : OnboardingState()
        object SampleAnalysis : OnboardingState()
        object Complete : OnboardingState()
        data class Error(val message: String) : OnboardingState()
    }

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Welcome)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _setupTime = MutableStateFlow(0L)
    val setupTime: StateFlow<Long> = _setupTime.asStateFlow()

    private val _magicMoments = MutableSharedFlow<MagicMoment>()
    val magicMoments: SharedFlow<MagicMoment> = _magicMoments.asSharedFlow()

    private var startTime = 0L

    data class MagicMoment(
        val type: MagicType,
        val message: String,
        val duration: Long = 2000L
    )

    enum class MagicType {
        SPARKLE, SUCCESS, INSIGHT, CELEBRATION, TIPS
    }

    /**
     * Start the magical onboarding journey
     */
    fun startMagicalJourney(context: Context) {
        startTime = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                // Welcome animation
                _state.value = OnboardingState.Welcome
                animateProgress(0.1f)
                delay(1500)

                // Auto-detect everything
                _state.value = OnboardingState.AutoDetecting
                animateProgress(0.3f)
                performAutoDetection(context)

                // Quick fun questions
                _state.value = OnboardingState.QuickQuestions
                animateProgress(0.5f)
                delay(500) // Let UI transition

                // Camera setup with AI guidance
                _state.value = OnboardingState.CameraSetup
                animateProgress(0.7f)
                delay(500)

                // Sample analysis demo
                _state.value = OnboardingState.SampleAnalysis
                animateProgress(0.9f)
                performSampleAnalysis()

                // Complete!
                _state.value = OnboardingState.Complete
                animateProgress(1.0f)
                celebrateCompletion()

            } catch (e: Exception) {
                _state.value = OnboardingState.Error(e.message ?: "Setup failed")
            }
        }
    }

    /**
     * Perform intelligent auto-detection of device capabilities and settings
     */
    private suspend fun performAutoDetection(context: Context) {
        // Start background optimizations
        autoSetupEngine.startAutoConfiguration(context)

        // Detect device capabilities
        _magicMoments.emit(MagicMoment(
            MagicType.SPARKLE,
            "Detecting your device's superpowers..."
        ))
        delay(1000)

        val capabilities = autoSetupEngine.detectCapabilities()
        
        // Configure optimal settings
        _magicMoments.emit(MagicMoment(
            MagicType.SUCCESS,
            "Found ${capabilities.cameraCount} cameras ready for action!"
        ))
        delay(800)

        // Setup permissions intelligently
        if (autoSetupEngine.needsPermissions()) {
            _magicMoments.emit(MagicMoment(
                MagicType.TIPS,
                "Quick permissions needed for the magic to work"
            ))
            autoSetupEngine.requestSmartPermissions(context)
        }

        // Configure cloud sync
        _magicMoments.emit(MagicMoment(
            MagicType.SPARKLE,
            "Setting up your personal swing vault..."
        ))
        autoSetupEngine.setupCloudSync()
        delay(1000)

        hapticFeedback.success()
    }

    /**
     * Generate user profile from fun questions
     */
    fun generateProfileFromAnswers(answers: ProfileAnswers) {
        viewModelScope.launch {
            val profile = profileGenerator.generateSmartProfile(answers)
            
            _magicMoments.emit(MagicMoment(
                MagicType.INSIGHT,
                "Perfect! Created your ${profile.playerType} profile"
            ))
            
            hapticFeedback.tap()
        }
    }

    /**
     * Perform sample swing analysis to show immediate value
     */
    private suspend fun performSampleAnalysis() {
        _magicMoments.emit(MagicMoment(
            MagicType.SPARKLE,
            "Let me show you what I can do..."
        ))
        delay(1500)

        // Analyze sample swing
        val sampleAnalysis = swingAnalyzer.analyzeSampleSwing()
        
        _magicMoments.emit(MagicMoment(
            MagicType.INSIGHT,
            "Found ${sampleAnalysis.improvements.size} ways to improve your swing!",
            duration = 3000L
        ))
        
        delay(2000)
        hapticFeedback.success()
    }

    /**
     * Celebrate successful completion
     */
    private suspend fun celebrateCompletion() {
        val totalTime = System.currentTimeMillis() - startTime
        _setupTime.value = totalTime

        _magicMoments.emit(MagicMoment(
            MagicType.CELEBRATION,
            "Setup complete in ${totalTime / 1000} seconds! Let's improve that swing!",
            duration = 4000L
        ))

        hapticFeedback.celebrate()
    }

    /**
     * Animate progress smoothly
     */
    private fun animateProgress(target: Float) {
        viewModelScope.launch {
            val start = _progress.value
            val animator = ValueAnimator.ofFloat(start, target).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
            }
            
            animator.addUpdateListener { animation ->
                _progress.value = animation.animatedValue as Float
            }
            
            animator.start()
        }
    }

    /**
     * Skip to main app (for returning users)
     */
    fun skipToApp() {
        viewModelScope.launch {
            _state.value = OnboardingState.Complete
            _progress.value = 1.0f
            hapticFeedback.tap()
        }
    }

    /**
     * Get camera positioning assistance
     */
    fun getCameraGuidance(): Flow<CameraPositioningGuide.Guidance> {
        return cameraGuide.startGuidance()
    }

    /**
     * Fun profile questions
     */
    data class ProfileAnswers(
        val experienceLevel: String,  // "Just started", "Weekend warrior", "Serious player"
        val mainGoal: String,         // "Have fun", "Lower scores", "Go pro"
        val favoriteClub: String,     // "Driver", "Irons", "Putter"
        val practiceFrequency: String // "Daily", "Weekly", "Monthly"
    )

    companion object {
        // Animation helpers
        fun createMagicAnimation(view: View, type: MagicType): AnimatorSet {
            return when (type) {
                MagicType.SPARKLE -> createSparkleAnimation(view)
                MagicType.SUCCESS -> createSuccessAnimation(view)
                MagicType.INSIGHT -> createInsightAnimation(view)
                MagicType.CELEBRATION -> createCelebrationAnimation(view)
                MagicType.TIPS -> createTipsAnimation(view)
            }
        }

        private fun createSparkleAnimation(view: View): AnimatorSet {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.2f, 1.0f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.2f, 1.0f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1.0f)
            
            return AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 600
                interpolator = OvershootInterpolator()
            }
        }

        private fun createSuccessAnimation(view: View): AnimatorSet {
            val translateY = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            
            return AnimatorSet().apply {
                playTogether(translateY, alpha)
                duration = 400
                interpolator = DecelerateInterpolator()
            }
        }

        private fun createInsightAnimation(view: View): AnimatorSet {
            val rotation = ObjectAnimator.ofFloat(view, "rotation", -5f, 5f, 0f)
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1.05f, 1.0f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1.05f, 1.0f)
            
            return AnimatorSet().apply {
                playTogether(rotation, scaleX, scaleY)
                duration = 800
            }
        }

        private fun createCelebrationAnimation(view: View): AnimatorSet {
            val jump = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
            val rotate = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f)
            
            return AnimatorSet().apply {
                playTogether(jump, rotate, scaleX, scaleY)
                duration = 1000
                interpolator = OvershootInterpolator()
            }
        }

        private fun createTipsAnimation(view: View): AnimatorSet {
            val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            val slideIn = ObjectAnimator.ofFloat(view, "translationX", -50f, 0f)
            
            return AnimatorSet().apply {
                playTogether(fadeIn, slideIn)
                duration = 300
                interpolator = DecelerateInterpolator()
            }
        }
    }
}