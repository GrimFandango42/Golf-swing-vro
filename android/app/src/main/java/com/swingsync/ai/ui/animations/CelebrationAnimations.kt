package com.swingsync.ai.ui.animations

import android.animation.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.swingsync.ai.R
import com.swingsync.ai.celebration.CelebrationLevel
import com.swingsync.ai.celebration.CelebrationType
import com.swingsync.ai.celebration.PersonalizedElement
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

/**
 * CelebrationAnimations - Personalized celebration animations based on user personality
 * 
 * This system creates magical, personalized celebration experiences that adapt to
 * user preferences and personality traits. Each celebration feels unique and meaningful.
 * 
 * Personality-Based Styles:
 * - Dynamic: High-energy, explosive animations
 * - Elegant: Smooth, sophisticated transitions
 * - Playful: Fun, bouncy, colorful effects
 * - Minimalist: Clean, simple, focused animations
 * - Classic: Traditional, timeless celebrations
 * - Futuristic: Modern, tech-inspired effects
 * 
 * Animation Categories:
 * - Particle Systems: Confetti, sparkles, fireworks
 * - Physics-Based: Bouncing, gravity, collision effects
 * - Morphing: Shape transformations, color transitions
 * - Kinetic: Movement, rotation, scaling effects
 * - Atmospheric: Background ambience, lighting
 * - Interactive: Touch-responsive celebrations
 * 
 * Each animation adapts to the celebration level and user personality for maximum impact.
 */
@Singleton
class CelebrationAnimations @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "CelebrationAnimations"
        
        // Animation timing
        private const val MICRO_DURATION = 800L
        private const val SHORT_DURATION = 1500L
        private const val MEDIUM_DURATION = 3000L
        private const val LONG_DURATION = 5000L
        private const val EPIC_DURATION = 8000L
        
        // Particle counts
        private const val MICRO_PARTICLES = 5
        private const val LIGHT_PARTICLES = 15
        private const val MEDIUM_PARTICLES = 30
        private const val HEAVY_PARTICLES = 50
        private const val EPIC_PARTICLES = 100
        
        // Animation intensities
        private const val SUBTLE_SCALE = 1.1f
        private const val MODERATE_SCALE = 1.3f
        private const val HIGH_SCALE = 1.6f
        private const val EPIC_SCALE = 2.0f
    }
    
    // State management
    private var isAnimating = false
    private val activeAnimators = mutableListOf<Animator>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Animation containers
    private var rootView: ViewGroup? = null
    private var particleContainer: FrameLayout? = null
    private var overlayContainer: FrameLayout? = null
    
    // User personality preferences
    private var userPersonality: CelebrationPersonality = CelebrationPersonality.DYNAMIC
    private var preferredIntensity: Float = 1.0f
    private var enableHaptics: Boolean = true
    private var enableSound: Boolean = true
    
    /**
     * Initialize celebration animations
     */
    fun initialize(rootView: ViewGroup) {
        this.rootView = rootView
        createAnimationContainers()
    }
    
    /**
     * Execute personalized celebration based on user personality
     */
    fun executePersonalizedCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        personalizedElements: List<PersonalizedElement>,
        personality: CelebrationPersonality = userPersonality
    ) {
        if (isAnimating) {
            stopAllAnimations()
        }
        
        isAnimating = true
        
        coroutineScope.launch {
            try {
                when (personality) {
                    CelebrationPersonality.DYNAMIC -> executeDynamicCelebration(level, type, personalizedElements)
                    CelebrationPersonality.ELEGANT -> executeElegantCelebration(level, type, personalizedElements)
                    CelebrationPersonality.PLAYFUL -> executePlayfulCelebration(level, type, personalizedElements)
                    CelebrationPersonality.MINIMALIST -> executeMinimalistCelebration(level, type, personalizedElements)
                    CelebrationPersonality.CLASSIC -> executeClassicCelebration(level, type, personalizedElements)
                    CelebrationPersonality.FUTURISTIC -> executeFuturisticCelebration(level, type, personalizedElements)
                }
                
                // Add haptic feedback
                if (enableHaptics) {
                    triggerHapticFeedback(level)
                }
                
                // Wait for celebration duration
                delay(getDurationForLevel(level))
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error executing celebration", e)
            } finally {
                cleanup()
            }
        }
    }
    
    /**
     * Execute dynamic celebration (high-energy, explosive)
     */
    private suspend fun executeDynamicCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Explosive burst animation
            createExplosiveBurst(root, level)
            
            // Rapid particle shower
            createRapidParticleShower(root, level)
            
            // Pulsing screen effect
            createPulsingScreenEffect(root)
            
            // Dynamic color waves
            createColorWaves(root, level)
            
            // High-energy text animations
            animateTextDynamically(root, elements)
            
            // Screen shake for epic celebrations
            if (level >= CelebrationLevel.EPIC) {
                createScreenShake(root)
            }
        }
    }
    
    /**
     * Execute elegant celebration (smooth, sophisticated)
     */
    private suspend fun executeElegantCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Graceful particle flow
            createGracefulParticleFlow(root, level)
            
            // Smooth golden shimmer
            createGoldenShimmer(root)
            
            // Elegant fade transitions
            createElegantFadeTransitions(root, elements)
            
            // Sophisticated color gradients
            createSophisticatedGradients(root, level)
            
            // Gentle scaling animations
            createGentleScaling(root, level)
        }
    }
    
    /**
     * Execute playful celebration (fun, bouncy, colorful)
     */
    private suspend fun executePlayfulCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Bouncing confetti
            createBouncingConfetti(root, level)
            
            // Rainbow particle effects
            createRainbowParticles(root, level)
            
            // Bouncy text animations
            createBouncyTextAnimations(root, elements)
            
            // Playful sound waves visualization
            createSoundWaveVisualization(root)
            
            // Fun rotation effects
            createFunRotationEffects(root, level)
            
            // Emoji rain
            createEmojiRain(root, type)
        }
    }
    
    /**
     * Execute minimalist celebration (clean, simple, focused)
     */
    private suspend fun executeMinimalistCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Clean fade-in effect
            createCleanFadeIn(root, elements)
            
            // Minimal particle accent
            createMinimalParticleAccent(root, level)
            
            // Subtle color highlight
            createSubtleColorHighlight(root)
            
            // Simple scaling animation
            createSimpleScaling(root, level)
            
            // Clean typography animation
            createCleanTypographyAnimation(root, elements)
        }
    }
    
    /**
     * Execute classic celebration (traditional, timeless)
     */
    private suspend fun executeClassicCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Traditional fireworks
            createTraditionalFireworks(root, level)
            
            // Golden trophy animation
            createGoldenTrophyAnimation(root, type)
            
            // Classic confetti fall
            createClassicConfettiFall(root, level)
            
            // Regal color scheme
            createRegalColorScheme(root)
            
            // Traditional text reveal
            createTraditionalTextReveal(root, elements)
        }
    }
    
    /**
     * Execute futuristic celebration (modern, tech-inspired)
     */
    private suspend fun executeFuturisticCelebration(
        level: CelebrationLevel,
        type: CelebrationType,
        elements: List<PersonalizedElement>
    ) {
        rootView?.let { root ->
            // Digital particle matrix
            createDigitalParticleMatrix(root, level)
            
            // Holographic effects
            createHolographicEffects(root)
            
            // Neon glow animations
            createNeonGlowAnimations(root, level)
            
            // Tech-style text effects
            createTechStyleTextEffects(root, elements)
            
            // Circuit pattern overlay
            createCircuitPatternOverlay(root)
            
            // Laser beam effects
            createLaserBeamEffects(root, level)
        }
    }
    
    /**
     * Create explosive burst animation
     */
    private fun createExplosiveBurst(root: ViewGroup, level: CelebrationLevel) {
        val particleCount = getParticleCountForLevel(level)
        val centerX = root.width / 2f
        val centerY = root.height / 2f
        
        repeat(particleCount) { i ->
            val particle = createParticle(getBurstColor(i))
            particleContainer?.addView(particle)
            
            val angle = (360f / particleCount) * i
            val distance = Random.nextFloat() * 300f + 200f
            val endX = centerX + distance * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = centerY + distance * sin(Math.toRadians(angle.toDouble())).toFloat()
            
            animateBurstParticle(particle, centerX, centerY, endX, endY)
        }
    }
    
    /**
     * Create rapid particle shower
     */
    private fun createRapidParticleShower(root: ViewGroup, level: CelebrationLevel) {
        val particleCount = getParticleCountForLevel(level) / 2
        
        coroutineScope.launch {
            repeat(particleCount) { i ->
                delay(i * 50L)
                
                val particle = createParticle(getShowerColor())
                particleContainer?.addView(particle)
                
                val startX = Random.nextFloat() * root.width
                val startY = -50f
                val endY = root.height + 50f
                
                animateShowerParticle(particle, startX, startY, endY)
            }
        }
    }
    
    /**
     * Create pulsing screen effect
     */
    private fun createPulsingScreenEffect(root: ViewGroup) {
        val overlay = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.argb(50, 255, 255, 255))
            alpha = 0f
        }
        
        overlayContainer?.addView(overlay)
        
        val pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 1000L
            repeatCount = 3
            addUpdateListener { animation ->
                overlay.alpha = animation.animatedValue as Float * 0.3f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlayContainer?.removeView(overlay)
                }
            })
        }
        
        activeAnimators.add(pulseAnimator)
        pulseAnimator.start()
    }
    
    /**
     * Create color waves
     */
    private fun createColorWaves(root: ViewGroup, level: CelebrationLevel) {
        val waveCount = when (level) {
            CelebrationLevel.LEGENDARY, CelebrationLevel.EPIC -> 5
            CelebrationLevel.EXCELLENCE, CelebrationLevel.GREAT -> 3
            else -> 2
        }
        
        repeat(waveCount) { i ->
            coroutineScope.launch {
                delay(i * 300L)
                createSingleWave(root, getWaveColor(i))
            }
        }
    }
    
    /**
     * Create single wave
     */
    private fun createSingleWave(root: ViewGroup, color: Int) {
        val wave = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(100, 100)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(8, color)
                setColor(Color.TRANSPARENT)
            }
            alpha = 0.8f
        }
        
        overlayContainer?.addView(wave)
        
        val centerX = root.width / 2f - 50f
        val centerY = root.height / 2f - 50f
        
        wave.x = centerX
        wave.y = centerY
        
        val waveAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(wave, "scaleX", 1f, 8f),
                ObjectAnimator.ofFloat(wave, "scaleY", 1f, 8f),
                ObjectAnimator.ofFloat(wave, "alpha", 0.8f, 0f)
            )
            duration = 2000L
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlayContainer?.removeView(wave)
                }
            })
        }
        
        activeAnimators.add(waveAnimator)
        waveAnimator.start()
    }
    
    /**
     * Create graceful particle flow
     */
    private fun createGracefulParticleFlow(root: ViewGroup, level: CelebrationLevel) {
        val particleCount = getParticleCountForLevel(level)
        
        repeat(particleCount) { i ->
            coroutineScope.launch {
                delay(i * 100L)
                
                val particle = createParticle(Color.parseColor("#FFD700"))
                particleContainer?.addView(particle)
                
                animateGracefulFlow(particle, root)
            }
        }
    }
    
    /**
     * Animate graceful flow
     */
    private fun animateGracefulFlow(particle: View, root: ViewGroup) {
        val startX = Random.nextFloat() * root.width
        val startY = Random.nextFloat() * root.height * 0.3f
        
        particle.x = startX
        particle.y = startY
        
        val path = Path().apply {
            moveTo(startX, startY)
            val controlX1 = startX + Random.nextFloat() * 200f - 100f
            val controlY1 = startY + Random.nextFloat() * 100f + 50f
            val controlX2 = startX + Random.nextFloat() * 200f - 100f
            val controlY2 = startY + Random.nextFloat() * 100f + 150f
            val endX = startX + Random.nextFloat() * 100f - 50f
            val endY = root.height + 50f
            
            cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
        }
        
        val pathAnimator = ObjectAnimator.ofFloat(particle, "x", "y", path).apply {
            duration = 4000L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedFraction
                particle.alpha = 1f - progress
                particle.scaleX = 1f - progress * 0.5f
                particle.scaleY = 1f - progress * 0.5f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    particleContainer?.removeView(particle)
                }
            })
        }
        
        activeAnimators.add(pathAnimator)
        pathAnimator.start()
    }
    
    /**
     * Create golden shimmer
     */
    private fun createGoldenShimmer(root: ViewGroup) {
        val shimmer = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(30, 255, 215, 0),
                    Color.TRANSPARENT
                )
            )
            alpha = 0f
        }
        
        overlayContainer?.addView(shimmer)
        
        val shimmerAnimator = ObjectAnimator.ofFloat(shimmer, "alpha", 0f, 1f, 0f).apply {
            duration = 3000L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlayContainer?.removeView(shimmer)
                }
            })
        }
        
        activeAnimators.add(shimmerAnimator)
        shimmerAnimator.start()
    }
    
    /**
     * Create bouncing confetti
     */
    private fun createBouncingConfetti(root: ViewGroup, level: CelebrationLevel) {
        val confettiCount = getParticleCountForLevel(level)
        val colors = arrayOf(
            Color.parseColor("#FF6B35"),
            Color.parseColor("#F7931E"),
            Color.parseColor("#FFD23F"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1")
        )
        
        repeat(confettiCount) { i ->
            val confetti = createConfettiPiece(colors[i % colors.size])
            particleContainer?.addView(confetti)
            
            animateBouncingConfetti(confetti, root)
        }
    }
    
    /**
     * Create confetti piece
     */
    private fun createConfettiPiece(color: Int): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(12, 8)
            setBackgroundColor(color)
            rotation = Random.nextFloat() * 360f
        }
    }
    
    /**
     * Animate bouncing confetti
     */
    private fun animateBouncingConfetti(confetti: View, root: ViewGroup) {
        val startX = Random.nextFloat() * root.width
        val startY = -50f
        val endY = root.height + 50f
        
        confetti.x = startX
        confetti.y = startY
        
        // Create bouncing motion with gravity
        val bounceInterpolator = BounceInterpolator()
        
        val fallAnimator = ObjectAnimator.ofFloat(confetti, "y", startY, endY).apply {
            duration = Random.nextLong(2000L, 4000L)
            interpolator = AccelerateInterpolator()
        }
        
        val rotationAnimator = ObjectAnimator.ofFloat(confetti, "rotation", 0f, Random.nextFloat() * 720f + 360f).apply {
            duration = fallAnimator.duration
            interpolator = LinearInterpolator()
        }
        
        val swayAnimator = ObjectAnimator.ofFloat(confetti, "x", startX, startX + Random.nextFloat() * 200f - 100f).apply {
            duration = fallAnimator.duration
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val animatorSet = AnimatorSet().apply {
            playTogether(fallAnimator, rotationAnimator, swayAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    particleContainer?.removeView(confetti)
                }
            })
        }
        
        activeAnimators.add(animatorSet)
        animatorSet.start()
    }
    
    /**
     * Create minimal particle accent
     */
    private fun createMinimalParticleAccent(root: ViewGroup, level: CelebrationLevel) {
        val particleCount = minOf(getParticleCountForLevel(level), 10)
        
        repeat(particleCount) { i ->
            val particle = createParticle(Color.parseColor("#FFFFFF"))
            particleContainer?.addView(particle)
            
            val centerX = root.width / 2f
            val centerY = root.height / 2f
            
            particle.x = centerX
            particle.y = centerY
            
            val endX = centerX + Random.nextFloat() * 200f - 100f
            val endY = centerY + Random.nextFloat() * 200f - 100f
            
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(particle, "x", centerX, endX),
                    ObjectAnimator.ofFloat(particle, "y", centerY, endY),
                    ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(particle, "scaleX", 1f, 0.5f),
                    ObjectAnimator.ofFloat(particle, "scaleY", 1f, 0.5f)
                )
                duration = 2000L
                startDelay = i * 100L
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        particleContainer?.removeView(particle)
                    }
                })
            }
            
            activeAnimators.add(animator)
            animator.start()
        }
    }
    
    /**
     * Create animation containers
     */
    private fun createAnimationContainers() {
        rootView?.let { root ->
            // Particle container (behind content)
            particleContainer = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
            }
            root.addView(particleContainer, 0)
            
            // Overlay container (in front of content)
            overlayContainer = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
            }
            root.addView(overlayContainer)
        }
    }
    
    /**
     * Create particle view
     */
    private fun createParticle(color: Int): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(10, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }
    }
    
    /**
     * Animate burst particle
     */
    private fun animateBurstParticle(particle: View, startX: Float, startY: Float, endX: Float, endY: Float) {
        particle.x = startX
        particle.y = startY
        
        val animator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(particle, "x", startX, endX),
                ObjectAnimator.ofFloat(particle, "y", startY, endY),
                ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(particle, "scaleX", 1f, 2f, 0.5f),
                ObjectAnimator.ofFloat(particle, "scaleY", 1f, 2f, 0.5f)
            )
            duration = 1500L
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    particleContainer?.removeView(particle)
                }
            })
        }
        
        activeAnimators.add(animator)
        animator.start()
    }
    
    /**
     * Animate shower particle
     */
    private fun animateShowerParticle(particle: View, startX: Float, startY: Float, endY: Float) {
        particle.x = startX
        particle.y = startY
        
        val animator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(particle, "y", startY, endY),
                ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(particle, "rotation", 0f, Random.nextFloat() * 360f)
            )
            duration = Random.nextLong(1000L, 3000L)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    particleContainer?.removeView(particle)
                }
            })
        }
        
        activeAnimators.add(animator)
        animator.start()
    }
    
    /**
     * Get particle count based on celebration level
     */
    private fun getParticleCountForLevel(level: CelebrationLevel): Int {
        return when (level) {
            CelebrationLevel.LEGENDARY -> EPIC_PARTICLES
            CelebrationLevel.EPIC -> HEAVY_PARTICLES
            CelebrationLevel.EXCELLENCE -> MEDIUM_PARTICLES
            CelebrationLevel.GREAT -> LIGHT_PARTICLES
            CelebrationLevel.GOOD -> LIGHT_PARTICLES
            CelebrationLevel.ENCOURAGING -> MICRO_PARTICLES
        }
    }
    
    /**
     * Get duration based on celebration level
     */
    private fun getDurationForLevel(level: CelebrationLevel): Long {
        return when (level) {
            CelebrationLevel.LEGENDARY -> EPIC_DURATION
            CelebrationLevel.EPIC -> LONG_DURATION
            CelebrationLevel.EXCELLENCE -> MEDIUM_DURATION
            CelebrationLevel.GREAT -> MEDIUM_DURATION
            CelebrationLevel.GOOD -> SHORT_DURATION
            CelebrationLevel.ENCOURAGING -> MICRO_DURATION
        }
    }
    
    /**
     * Get burst color
     */
    private fun getBurstColor(index: Int): Int {
        val colors = arrayOf(
            Color.parseColor("#FF6B35"),
            Color.parseColor("#F7931E"),
            Color.parseColor("#FFD23F"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7")
        )
        return colors[index % colors.size]
    }
    
    /**
     * Get shower color
     */
    private fun getShowerColor(): Int {
        val colors = arrayOf(
            Color.parseColor("#FFD700"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8C00"),
            Color.parseColor("#FF7F50")
        )
        return colors[Random.nextInt(colors.size)]
    }
    
    /**
     * Get wave color
     */
    private fun getWaveColor(index: Int): Int {
        val colors = arrayOf(
            Color.parseColor("#FF6B35"),
            Color.parseColor("#F7931E"),
            Color.parseColor("#FFD23F"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4")
        )
        return colors[index % colors.size]
    }
    
    /**
     * Trigger haptic feedback
     */
    private fun triggerHapticFeedback(level: CelebrationLevel) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        
        if (vibrator?.hasVibrator() == true) {
            val pattern = when (level) {
                CelebrationLevel.LEGENDARY -> longArrayOf(0, 100, 50, 100, 50, 200)
                CelebrationLevel.EPIC -> longArrayOf(0, 50, 25, 100, 25, 150)
                CelebrationLevel.EXCELLENCE -> longArrayOf(0, 50, 25, 50)
                CelebrationLevel.GREAT -> longArrayOf(0, 100)
                CelebrationLevel.GOOD -> longArrayOf(0, 50)
                CelebrationLevel.ENCOURAGING -> longArrayOf(0, 25)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
    
    /**
     * Update user personality
     */
    fun updatePersonality(personality: CelebrationPersonality) {
        this.userPersonality = personality
    }
    
    /**
     * Update celebration preferences
     */
    fun updatePreferences(intensity: Float, enableHaptics: Boolean, enableSound: Boolean) {
        this.preferredIntensity = intensity
        this.enableHaptics = enableHaptics
        this.enableSound = enableSound
    }
    
    /**
     * Stop all animations
     */
    fun stopAllAnimations() {
        isAnimating = false
        
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
        
        particleContainer?.removeAllViews()
        overlayContainer?.removeAllViews()
    }
    
    /**
     * Cleanup resources
     */
    private fun cleanup() {
        isAnimating = false
        particleContainer?.removeAllViews()
        overlayContainer?.removeAllViews()
    }
    
    /**
     * Full cleanup when component is destroyed
     */
    fun destroy() {
        stopAllAnimations()
        coroutineScope.cancel()
        rootView = null
        particleContainer = null
        overlayContainer = null
    }
    
    // Placeholder methods for additional animation types
    private suspend fun animateTextDynamically(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for dynamic text animations
    }
    
    private fun createScreenShake(root: ViewGroup) {
        // Implementation for screen shake effect
    }
    
    private fun createElegantFadeTransitions(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for elegant fade transitions
    }
    
    private fun createSophisticatedGradients(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for sophisticated gradients
    }
    
    private fun createGentleScaling(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for gentle scaling animations
    }
    
    private fun createRainbowParticles(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for rainbow particle effects
    }
    
    private fun createBouncyTextAnimations(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for bouncy text animations
    }
    
    private fun createSoundWaveVisualization(root: ViewGroup) {
        // Implementation for sound wave visualization
    }
    
    private fun createFunRotationEffects(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for fun rotation effects
    }
    
    private fun createEmojiRain(root: ViewGroup, type: CelebrationType) {
        // Implementation for emoji rain effect
    }
    
    private fun createCleanFadeIn(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for clean fade-in effect
    }
    
    private fun createSubtleColorHighlight(root: ViewGroup) {
        // Implementation for subtle color highlight
    }
    
    private fun createSimpleScaling(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for simple scaling animation
    }
    
    private fun createCleanTypographyAnimation(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for clean typography animation
    }
    
    private fun createTraditionalFireworks(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for traditional fireworks
    }
    
    private fun createGoldenTrophyAnimation(root: ViewGroup, type: CelebrationType) {
        // Implementation for golden trophy animation
    }
    
    private fun createClassicConfettiFall(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for classic confetti fall
    }
    
    private fun createRegalColorScheme(root: ViewGroup) {
        // Implementation for regal color scheme
    }
    
    private fun createTraditionalTextReveal(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for traditional text reveal
    }
    
    private fun createDigitalParticleMatrix(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for digital particle matrix
    }
    
    private fun createHolographicEffects(root: ViewGroup) {
        // Implementation for holographic effects
    }
    
    private fun createNeonGlowAnimations(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for neon glow animations
    }
    
    private fun createTechStyleTextEffects(root: ViewGroup, elements: List<PersonalizedElement>) {
        // Implementation for tech-style text effects
    }
    
    private fun createCircuitPatternOverlay(root: ViewGroup) {
        // Implementation for circuit pattern overlay
    }
    
    private fun createLaserBeamEffects(root: ViewGroup, level: CelebrationLevel) {
        // Implementation for laser beam effects
    }
}

// Celebration personality types
enum class CelebrationPersonality {
    DYNAMIC,      // High-energy, explosive
    ELEGANT,      // Smooth, sophisticated
    PLAYFUL,      // Fun, bouncy, colorful
    MINIMALIST,   // Clean, simple, focused
    CLASSIC,      // Traditional, timeless
    FUTURISTIC    // Modern, tech-inspired
}