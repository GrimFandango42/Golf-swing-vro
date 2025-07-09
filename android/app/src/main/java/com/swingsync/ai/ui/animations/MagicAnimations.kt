package com.swingsync.ai.ui.animations

import android.animation.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.swingsync.ai.R
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * MagicAnimations - Beautiful animations for the magic swing analysis experience
 * 
 * Features:
 * - Magical button animations with pulsing effects
 * - Smooth progress indicators and loading animations
 * - Particle effects for visual appeal
 * - Ambient background animations
 * - Smooth transitions between states
 * - Performance-optimized animations
 * - Contextual animations based on analysis state
 * 
 * This class creates the "wow factor" that makes the analysis feel truly magical.
 */
@Singleton
class MagicAnimations @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "MagicAnimations"
        
        // Animation durations
        private const val MAGIC_BUTTON_PULSE_DURATION = 1500L
        private const val PROGRESS_UPDATE_DURATION = 300L
        private const val PARTICLE_ANIMATION_DURATION = 2000L
        private const val AMBIENT_ANIMATION_DURATION = 3000L
        private const val TRANSITION_DURATION = 400L
        
        // Animation values
        private const val PULSE_SCALE_FACTOR = 1.1f
        private const val SHIMMER_ALPHA_MAX = 0.7f
        private const val PARTICLE_COUNT = 20
        private const val RIPPLE_COUNT = 3
    }

    // Animation state
    private var isAnimating = false
    private val activeAnimators = mutableListOf<Animator>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Animation containers
    private var rootView: ViewGroup? = null
    private var particleContainer: FrameLayout? = null
    private var ambientContainer: FrameLayout? = null
    
    // Animation jobs
    private var ambientJob: Job? = null
    private var particleJob: Job? = null
    private var pulseJob: Job? = null

    /**
     * Initialize magic mode with ambient animations
     */
    fun initializeMagicMode(rootView: ViewGroup) {
        this.rootView = rootView
        
        // Create animation containers
        createAnimationContainers()
        
        // Start ambient background effects
        startAmbientAnimations()
    }

    /**
     * Setup the magic button with pulsing animation
     */
    fun setupMagicButton(button: Button) {
        // Create gradient background
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ContextCompat.getColor(context, R.color.magic_accent),
                ContextCompat.getColor(context, R.color.recording_button)
            )
        )
        gradientDrawable.cornerRadius = 30f
        button.background = gradientDrawable
        
        // Start pulsing animation
        startButtonPulseAnimation(button)
        
        // Add shimmer effect
        addShimmerEffect(button)
    }

    /**
     * Start magic analysis animations
     */
    fun startMagicAnalysis(button: Button, progressContainer: ViewGroup) {
        // Stop button pulsing
        stopButtonPulseAnimation(button)
        
        // Transform button
        animateButtonTransform(button, "Analyzing...")
        
        // Show progress container with animation
        showProgressContainer(progressContainer)
        
        // Start analysis particle effects
        startAnalysisParticles()
        
        // Start ripple animations
        startRippleAnimations(button)
    }

    /**
     * Start recording animation
     */
    fun startRecordingAnimation(progressContainer: ViewGroup) {
        // Add recording indicator
        addRecordingIndicator(progressContainer)
        
        // Start recording pulse effect
        startRecordingPulse(progressContainer)
        
        // Add motion tracking particles
        startMotionTrackingParticles()
    }

    /**
     * Start analysis animation
     */
    fun startAnalysisAnimation(progressContainer: ViewGroup) {
        // Transform to analysis mode
        transformToAnalysisMode(progressContainer)
        
        // Start brain-like thinking animation
        startThinkingAnimation(progressContainer)
        
        // Add processing particles
        startProcessingParticles()
    }

    /**
     * Start results animation
     */
    fun startResultsAnimation(progressContainer: ViewGroup) {
        // Transform to results mode
        transformToResultsMode(progressContainer)
        
        // Start success celebration animation
        startCelebrationAnimation(progressContainer)
        
        // Add success particles
        startSuccessParticles()
    }

    /**
     * Start ambient animations
     */
    fun startAmbientAnimations() {
        ambientJob?.cancel()
        ambientJob = coroutineScope.launch {
            while (isActive) {
                // Floating orbs animation
                startFloatingOrbs()
                
                // Subtle background shimmer
                startBackgroundShimmer()
                
                delay(AMBIENT_ANIMATION_DURATION)
            }
        }
    }

    /**
     * Stop all animations
     */
    fun stopAllAnimations() {
        isAnimating = false
        
        // Cancel all coroutine jobs
        ambientJob?.cancel()
        particleJob?.cancel()
        pulseJob?.cancel()
        
        // Stop all animators
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
        
        // Clean up containers
        particleContainer?.removeAllViews()
        ambientContainer?.removeAllViews()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopAllAnimations()
        coroutineScope.cancel()
        rootView = null
        particleContainer = null
        ambientContainer = null
    }

    /**
     * Create animation containers
     */
    private fun createAnimationContainers() {
        rootView?.let { root ->
            // Particle container
            particleContainer = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
            }
            root.addView(particleContainer)
            
            // Ambient container
            ambientContainer = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
            }
            root.addView(ambientContainer)
        }
    }

    /**
     * Start button pulse animation
     */
    private fun startButtonPulseAnimation(button: Button) {
        pulseJob?.cancel()
        pulseJob = coroutineScope.launch {
            while (isActive) {
                val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, PULSE_SCALE_FACTOR, 1f)
                val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, PULSE_SCALE_FACTOR, 1f)
                val alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.8f, 1f)
                
                val animatorSet = AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha)
                    duration = MAGIC_BUTTON_PULSE_DURATION
                    interpolator = AccelerateDecelerateInterpolator()
                }
                
                activeAnimators.add(animatorSet)
                animatorSet.start()
                
                delay(MAGIC_BUTTON_PULSE_DURATION)
            }
        }
    }

    /**
     * Stop button pulse animation
     */
    private fun stopButtonPulseAnimation(button: Button) {
        pulseJob?.cancel()
        
        // Reset button scale
        button.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(TRANSITION_DURATION)
            .start()
    }

    /**
     * Add shimmer effect to button
     */
    private fun addShimmerEffect(button: Button) {
        val shimmerView = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(100, 255, 255, 255),
                    Color.TRANSPARENT
                )
            )
            alpha = 0f
        }
        
        if (button.parent is ViewGroup) {
            (button.parent as ViewGroup).addView(shimmerView)
            
            // Animate shimmer
            val shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "alpha", 0f, SHIMMER_ALPHA_MAX, 0f)
            shimmerAnimator.apply {
                duration = 2000L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
            }
            
            activeAnimators.add(shimmerAnimator)
            shimmerAnimator.start()
        }
    }

    /**
     * Animate button transform
     */
    private fun animateButtonTransform(button: Button, newText: String) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f),
                ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f),
                ObjectAnimator.ofFloat(button, "alpha", 1f, 0.7f)
            )
            duration = TRANSITION_DURATION / 2
        }
        
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(button, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(button, "scaleY", 0.9f, 1f),
                ObjectAnimator.ofFloat(button, "alpha", 0.7f, 1f)
            )
            duration = TRANSITION_DURATION / 2
        }
        
        val transformAnimator = AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    button.text = newText
                }
            })
        }
        
        activeAnimators.add(transformAnimator)
        transformAnimator.start()
    }

    /**
     * Show progress container with animation
     */
    private fun showProgressContainer(progressContainer: ViewGroup) {
        progressContainer.visibility = View.VISIBLE
        progressContainer.alpha = 0f
        
        val fadeIn = ObjectAnimator.ofFloat(progressContainer, "alpha", 0f, 1f)
        fadeIn.duration = TRANSITION_DURATION
        fadeIn.interpolator = DecelerateInterpolator()
        
        activeAnimators.add(fadeIn)
        fadeIn.start()
    }

    /**
     * Start analysis particles
     */
    private fun startAnalysisParticles() {
        particleJob?.cancel()
        particleJob = coroutineScope.launch {
            while (isActive) {
                createAnalysisParticle()
                delay(100L)
            }
        }
    }

    /**
     * Create a single analysis particle
     */
    private fun createAnalysisParticle() {
        particleContainer?.let { container ->
            val particle = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(8, 8)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.magic_accent))
                }
                alpha = 0f
            }
            
            container.addView(particle)
            
            // Random start position
            val startX = (0..container.width).random().toFloat()
            val startY = (0..container.height).random().toFloat()
            
            particle.x = startX
            particle.y = startY
            
            // Animate particle
            val animatorSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(particle, "alpha", 0f, 1f, 0f),
                    ObjectAnimator.ofFloat(particle, "scaleX", 0.5f, 1.5f, 0.5f),
                    ObjectAnimator.ofFloat(particle, "scaleY", 0.5f, 1.5f, 0.5f),
                    ObjectAnimator.ofFloat(particle, "x", startX, startX + (-50..50).random()),
                    ObjectAnimator.ofFloat(particle, "y", startY, startY + (-50..50).random())
                )
                duration = PARTICLE_ANIMATION_DURATION
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(animatorSet)
            animatorSet.start()
        }
    }

    /**
     * Start ripple animations
     */
    private fun startRippleAnimations(centerView: View) {
        repeat(RIPPLE_COUNT) { index ->
            coroutineScope.launch {
                delay(index * 500L)
                createRipple(centerView)
            }
        }
    }

    /**
     * Create a ripple effect
     */
    private fun createRipple(centerView: View) {
        particleContainer?.let { container ->
            val ripple = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(100, 100)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(4, ContextCompat.getColor(context, R.color.magic_accent))
                }
                alpha = 0.8f
            }
            
            container.addView(ripple)
            
            // Center on the button
            val centerX = centerView.x + centerView.width / 2 - 50
            val centerY = centerView.y + centerView.height / 2 - 50
            
            ripple.x = centerX
            ripple.y = centerY
            
            // Animate ripple
            val animatorSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(ripple, "scaleX", 0.5f, 3f),
                    ObjectAnimator.ofFloat(ripple, "scaleY", 0.5f, 3f),
                    ObjectAnimator.ofFloat(ripple, "alpha", 0.8f, 0f)
                )
                duration = 1500L
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(ripple)
                    }
                })
            }
            
            activeAnimators.add(animatorSet)
            animatorSet.start()
        }
    }

    /**
     * Add recording indicator
     */
    private fun addRecordingIndicator(progressContainer: ViewGroup) {
        val indicator = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.recording_button_stop))
            }
        }
        
        progressContainer.addView(indicator)
        
        // Pulsing animation
        val pulseAnimator = ObjectAnimator.ofFloat(indicator, "alpha", 1f, 0.3f, 1f)
        pulseAnimator.apply {
            duration = 800L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        activeAnimators.add(pulseAnimator)
        pulseAnimator.start()
    }

    /**
     * Start recording pulse effect
     */
    private fun startRecordingPulse(progressContainer: ViewGroup) {
        val pulseAnimator = ObjectAnimator.ofFloat(progressContainer, "alpha", 1f, 0.8f, 1f)
        pulseAnimator.apply {
            duration = 1000L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        activeAnimators.add(pulseAnimator)
        pulseAnimator.start()
    }

    /**
     * Start motion tracking particles
     */
    private fun startMotionTrackingParticles() {
        // Similar to analysis particles but with different colors and behavior
        particleJob?.cancel()
        particleJob = coroutineScope.launch {
            while (isActive) {
                createMotionParticle()
                delay(150L)
            }
        }
    }

    /**
     * Create motion tracking particle
     */
    private fun createMotionParticle() {
        particleContainer?.let { container ->
            val particle = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(6, 6)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.golf_blue))
                }
                alpha = 0f
            }
            
            container.addView(particle)
            
            // Motion-like path
            val path = Path().apply {
                moveTo(0f, container.height.toFloat())
                quadTo(
                    container.width / 2f,
                    container.height / 4f,
                    container.width.toFloat(),
                    container.height / 2f
                )
            }
            
            val pathAnimator = ObjectAnimator.ofFloat(particle, "x", "y", path)
            pathAnimator.apply {
                duration = 2000L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedFraction
                    particle.alpha = sin(progress * PI).toFloat()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(pathAnimator)
            pathAnimator.start()
        }
    }

    /**
     * Transform to analysis mode
     */
    private fun transformToAnalysisMode(progressContainer: ViewGroup) {
        // Change background color
        val colorAnimator = ValueAnimator.ofArgb(
            ContextCompat.getColor(context, R.color.magic_overlay),
            ContextCompat.getColor(context, R.color.magic_card_background)
        )
        colorAnimator.apply {
            duration = TRANSITION_DURATION
            addUpdateListener { animation ->
                progressContainer.setBackgroundColor(animation.animatedValue as Int)
            }
        }
        
        activeAnimators.add(colorAnimator)
        colorAnimator.start()
    }

    /**
     * Start thinking animation
     */
    private fun startThinkingAnimation(progressContainer: ViewGroup) {
        // Create thinking dots
        val dotContainer = FrameLayout(context)
        progressContainer.addView(dotContainer)
        
        repeat(3) { index ->
            val dot = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(12, 12)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.magic_accent))
                }
                x = (index * 30).toFloat()
                y = 0f
            }
            
            dotContainer.addView(dot)
            
            // Animate dot
            val dotAnimator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f)
            dotAnimator.apply {
                duration = 800L
                startDelay = index * 200L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            activeAnimators.add(dotAnimator)
            dotAnimator.start()
        }
    }

    /**
     * Start processing particles
     */
    private fun startProcessingParticles() {
        // Create swirling particles around the progress indicator
        particleJob?.cancel()
        particleJob = coroutineScope.launch {
            while (isActive) {
                createProcessingParticle()
                delay(200L)
            }
        }
    }

    /**
     * Create processing particle
     */
    private fun createProcessingParticle() {
        particleContainer?.let { container ->
            val particle = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(8, 8)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.golf_gold))
                }
                alpha = 0f
            }
            
            container.addView(particle)
            
            // Spiral animation
            val centerX = container.width / 2f
            val centerY = container.height / 2f
            val radius = 100f
            
            val spiralAnimator = ValueAnimator.ofFloat(0f, 4 * PI.toFloat())
            spiralAnimator.apply {
                duration = 3000L
                addUpdateListener { animation ->
                    val angle = animation.animatedValue as Float
                    val currentRadius = radius * (1 - animation.animatedFraction)
                    
                    particle.x = centerX + currentRadius * cos(angle)
                    particle.y = centerY + currentRadius * sin(angle)
                    particle.alpha = sin(animation.animatedFraction * PI).toFloat()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(spiralAnimator)
            spiralAnimator.start()
        }
    }

    /**
     * Transform to results mode
     */
    private fun transformToResultsMode(progressContainer: ViewGroup) {
        // Successful completion animation
        val scaleAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(progressContainer, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(progressContainer, "scaleY", 1f, 1.1f, 1f)
            )
            duration = TRANSITION_DURATION
            interpolator = BounceInterpolator()
        }
        
        activeAnimators.add(scaleAnimator)
        scaleAnimator.start()
    }

    /**
     * Start celebration animation
     */
    private fun startCelebrationAnimation(progressContainer: ViewGroup) {
        // Create celebration burst
        repeat(10) { index ->
            coroutineScope.launch {
                delay(index * 50L)
                createCelebrationParticle(progressContainer)
            }
        }
    }

    /**
     * Create celebration particle
     */
    private fun createCelebrationParticle(progressContainer: ViewGroup) {
        particleContainer?.let { container ->
            val particle = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(12, 12)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.golf_gold))
                }
                alpha = 1f
            }
            
            container.addView(particle)
            
            // Burst animation
            val centerX = container.width / 2f
            val centerY = container.height / 2f
            val angle = (0..360).random() * PI / 180
            val distance = (100..300).random()
            
            particle.x = centerX
            particle.y = centerY
            
            val burstAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(particle, "x", centerX, centerX + distance * cos(angle).toFloat()),
                    ObjectAnimator.ofFloat(particle, "y", centerY, centerY + distance * sin(angle).toFloat()),
                    ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(particle, "scaleX", 1f, 0.5f),
                    ObjectAnimator.ofFloat(particle, "scaleY", 1f, 0.5f)
                )
                duration = 1000L
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(burstAnimator)
            burstAnimator.start()
        }
    }

    /**
     * Start success particles
     */
    private fun startSuccessParticles() {
        // Golden particles falling from top
        coroutineScope.launch {
            repeat(15) { index ->
                delay(index * 100L)
                createSuccessParticle()
            }
        }
    }

    /**
     * Create success particle
     */
    private fun createSuccessParticle() {
        particleContainer?.let { container ->
            val particle = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(10, 10)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.golf_gold))
                }
                alpha = 1f
            }
            
            container.addView(particle)
            
            // Falling animation
            val startX = (0..container.width).random().toFloat()
            val endY = container.height.toFloat()
            
            particle.x = startX
            particle.y = -20f
            
            val fallAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(particle, "y", -20f, endY),
                    ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(particle, "rotation", 0f, 360f)
                )
                duration = 2000L
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(fallAnimator)
            fallAnimator.start()
        }
    }

    /**
     * Start floating orbs
     */
    private fun startFloatingOrbs() {
        ambientContainer?.let { container ->
            repeat(5) { index ->
                coroutineScope.launch {
                    delay(index * 600L)
                    createFloatingOrb(container)
                }
            }
        }
    }

    /**
     * Create floating orb
     */
    private fun createFloatingOrb(container: ViewGroup) {
        val orb = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(30, 255, 255, 255))
            }
            alpha = 0f
        }
        
        container.addView(orb)
        
        // Floating animation
        val startX = (0..container.width).random().toFloat()
        val startY = (container.height * 0.3f..container.height * 0.7f).random()
        val endX = (0..container.width).random().toFloat()
        val endY = (container.height * 0.3f..container.height * 0.7f).random()
        
        orb.x = startX
        orb.y = startY
        
        val floatAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(orb, "x", startX, endX),
                ObjectAnimator.ofFloat(orb, "y", startY, endY),
                ObjectAnimator.ofFloat(orb, "alpha", 0f, 0.3f, 0f),
                ObjectAnimator.ofFloat(orb, "scaleX", 0.5f, 1f, 0.5f),
                ObjectAnimator.ofFloat(orb, "scaleY", 0.5f, 1f, 0.5f)
            )
            duration = 6000L
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    container.removeView(orb)
                }
            })
        }
        
        activeAnimators.add(floatAnimator)
        floatAnimator.start()
    }

    /**
     * Start background shimmer
     */
    private fun startBackgroundShimmer() {
        ambientContainer?.let { container ->
            val shimmer = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.argb(10, 255, 255, 255),
                        Color.TRANSPARENT
                    )
                )
                alpha = 0f
            }
            
            container.addView(shimmer)
            
            val shimmerAnimator = ObjectAnimator.ofFloat(shimmer, "alpha", 0f, 1f, 0f)
            shimmerAnimator.apply {
                duration = 4000L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(shimmer)
                    }
                })
            }
            
            activeAnimators.add(shimmerAnimator)
            shimmerAnimator.start()
        }
    }
}