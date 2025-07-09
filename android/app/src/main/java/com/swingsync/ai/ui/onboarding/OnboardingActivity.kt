package com.swingsync.ai.ui.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.swingsync.ai.R
import com.swingsync.ai.onboarding.OnboardingWizard
import com.swingsync.ai.ui.MainActivity
import com.swingsync.ai.ui.components.MagicButton
import com.swingsync.ai.ui.components.ParticleSystem
import com.swingsync.ai.utils.HapticFeedback
import com.swingsync.ai.utils.SoundEffects
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Magical onboarding activity that creates a delightful first-time experience
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingWizard by viewModels()
    
    @Inject
    lateinit var hapticFeedback: HapticFeedback
    
    @Inject
    lateinit var soundEffects: SoundEffects

    // UI Components
    private lateinit var rootLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var magicAnimation: LottieAnimationView
    private lateinit var contentContainer: LinearLayout
    private lateinit var actionButton: MagicButton
    private lateinit var skipButton: Button
    private lateinit var particleSystem: ParticleSystem

    // Question UI components
    private lateinit var questionCard: MaterialCardView
    private lateinit var questionTitle: TextView
    private lateinit var experienceChips: ChipGroup
    private lateinit var goalChips: ChipGroup
    private lateinit var clubChips: ChipGroup
    private lateinit var frequencyChips: ChipGroup

    // Camera setup components
    private lateinit var cameraPreview: View
    private lateinit var guidanceText: TextView
    private lateinit var guidanceOverlay: ImageView

    // Sample analysis components
    private lateinit var analysisAnimation: LottieAnimationView
    private lateinit var analysisResults: LinearLayout
    private lateinit var improvementsList: LinearLayout

    private var currentAnswers = OnboardingWizard.ProfileAnswers(
        experienceLevel = "",
        mainGoal = "",
        favoriteClub = "",
        practiceFrequency = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        initializeViews()
        setupObservers()
        setupClickListeners()
        startMagicalJourney()
    }

    private fun initializeViews() {
        rootLayout = findViewById(R.id.root_layout)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
        titleText = findViewById(R.id.title_text)
        subtitleText = findViewById(R.id.subtitle_text)
        magicAnimation = findViewById(R.id.magic_animation)
        contentContainer = findViewById(R.id.content_container)
        actionButton = findViewById(R.id.action_button)
        skipButton = findViewById(R.id.skip_button)
        
        // Initialize particle system
        particleSystem = ParticleSystem(this)
        rootLayout.addView(particleSystem)
        
        // Initialize question components
        questionCard = findViewById(R.id.question_card)
        questionTitle = findViewById(R.id.question_title)
        experienceChips = findViewById(R.id.experience_chips)
        goalChips = findViewById(R.id.goal_chips)
        clubChips = findViewById(R.id.club_chips)
        frequencyChips = findViewById(R.id.frequency_chips)
        
        // Initialize camera components
        cameraPreview = findViewById(R.id.camera_preview)
        guidanceText = findViewById(R.id.guidance_text)
        guidanceOverlay = findViewById(R.id.guidance_overlay)
        
        // Initialize analysis components
        analysisAnimation = findViewById(R.id.analysis_animation)
        analysisResults = findViewById(R.id.analysis_results)
        improvementsList = findViewById(R.id.improvements_list)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                handleStateChange(state)
            }
        }
        
        lifecycleScope.launch {
            viewModel.progress.collect { progress ->
                updateProgress(progress)
            }
        }
        
        lifecycleScope.launch {
            viewModel.magicMoments.collect { moment ->
                showMagicMoment(moment)
            }
        }
        
        lifecycleScope.launch {
            viewModel.setupTime.collect { time ->
                if (time > 0) {
                    showSetupTime(time)
                }
            }
        }
    }

    private fun setupClickListeners() {
        actionButton.setOnClickListener {
            handleActionButtonClick()
        }
        
        skipButton.setOnClickListener {
            viewModel.skipToApp()
        }
        
        // Setup chip listeners
        setupChipListeners()
    }

    private fun setupChipListeners() {
        experienceChips.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val chip = findViewById<Chip>(checkedId)
                currentAnswers = currentAnswers.copy(experienceLevel = chip.text.toString())
                hapticFeedback.tap()
                checkAnswersComplete()
            }
        }
        
        goalChips.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val chip = findViewById<Chip>(checkedId)
                currentAnswers = currentAnswers.copy(mainGoal = chip.text.toString())
                hapticFeedback.tap()
                checkAnswersComplete()
            }
        }
        
        clubChips.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val chip = findViewById<Chip>(checkedId)
                currentAnswers = currentAnswers.copy(favoriteClub = chip.text.toString())
                hapticFeedback.tap()
                checkAnswersComplete()
            }
        }
        
        frequencyChips.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val chip = findViewById<Chip>(checkedId)
                currentAnswers = currentAnswers.copy(practiceFrequency = chip.text.toString())
                hapticFeedback.tap()
                checkAnswersComplete()
            }
        }
    }

    private fun startMagicalJourney() {
        particleSystem.start()
        viewModel.startMagicalJourney(this)
    }

    private fun handleStateChange(state: OnboardingWizard.OnboardingState) {
        when (state) {
            is OnboardingWizard.OnboardingState.Welcome -> showWelcomeScreen()
            is OnboardingWizard.OnboardingState.AutoDetecting -> showAutoDetectingScreen()
            is OnboardingWizard.OnboardingState.QuickQuestions -> showQuickQuestionsScreen()
            is OnboardingWizard.OnboardingState.CameraSetup -> showCameraSetupScreen()
            is OnboardingWizard.OnboardingState.SampleAnalysis -> showSampleAnalysisScreen()
            is OnboardingWizard.OnboardingState.Complete -> showCompleteScreen()
            is OnboardingWizard.OnboardingState.Error -> showErrorScreen(state.message)
        }
    }

    private fun showWelcomeScreen() {
        titleText.text = "Welcome to SwingSync AI"
        subtitleText.text = "Let's get you set up for golf greatness!"
        
        magicAnimation.setAnimation(R.raw.welcome_animation)
        magicAnimation.playAnimation()
        
        actionButton.text = "Let's Start!"
        actionButton.isVisible = true
        skipButton.isVisible = true
        
        // Hide other components
        questionCard.isVisible = false
        cameraPreview.isVisible = false
        analysisAnimation.isVisible = false
        
        soundEffects.playWelcome()
    }

    private fun showAutoDetectingScreen() {
        titleText.text = "Setting Up Your Magic"
        subtitleText.text = "Configuring everything automatically..."
        
        magicAnimation.setAnimation(R.raw.auto_setup_animation)
        magicAnimation.playAnimation()
        
        actionButton.isVisible = false
        skipButton.isVisible = false
        
        // Start sparkle effects
        particleSystem.startSparkles()
        
        soundEffects.playMagic()
    }

    private fun showQuickQuestionsScreen() {
        titleText.text = "Quick Questions"
        subtitleText.text = "Help me personalize your experience"
        
        magicAnimation.setAnimation(R.raw.questions_animation)
        magicAnimation.playAnimation()
        
        questionCard.isVisible = true
        actionButton.text = "Create My Profile"
        actionButton.isVisible = false // Will show when answers are complete
        skipButton.isVisible = true
        
        animateQuestionCard()
        soundEffects.playQuestion()
    }

    private fun showCameraSetupScreen() {
        titleText.text = "Camera Setup"
        subtitleText.text = "Let's position your camera perfectly"
        
        cameraPreview.isVisible = true
        guidanceText.isVisible = true
        guidanceOverlay.isVisible = true
        
        // Hide other components
        questionCard.isVisible = false
        magicAnimation.isVisible = false
        
        startCameraGuidance()
        soundEffects.playCamera()
    }

    private fun showSampleAnalysisScreen() {
        titleText.text = "Sample Analysis"
        subtitleText.text = "Watch me analyze a professional swing"
        
        analysisAnimation.setAnimation(R.raw.analysis_animation)
        analysisAnimation.playAnimation()
        analysisAnimation.isVisible = true
        
        // Hide other components
        cameraPreview.isVisible = false
        guidanceText.isVisible = false
        guidanceOverlay.isVisible = false
        
        startSampleAnalysis()
        soundEffects.playAnalysis()
    }

    private fun showCompleteScreen() {
        titleText.text = "You're All Set!"
        subtitleText.text = "Ready to analyze your swing"
        
        magicAnimation.setAnimation(R.raw.success_animation)
        magicAnimation.playAnimation()
        magicAnimation.isVisible = true
        
        actionButton.text = "Start Analyzing"
        actionButton.isVisible = true
        skipButton.isVisible = false
        
        // Hide other components
        analysisAnimation.isVisible = false
        analysisResults.isVisible = false
        
        particleSystem.startCelebration()
        soundEffects.playSuccess()
        hapticFeedback.celebrate()
    }

    private fun showErrorScreen(message: String) {
        titleText.text = "Oops!"
        subtitleText.text = message
        
        magicAnimation.setAnimation(R.raw.error_animation)
        magicAnimation.playAnimation()
        
        actionButton.text = "Try Again"
        actionButton.isVisible = true
        skipButton.isVisible = true
        
        soundEffects.playError()
        hapticFeedback.error()
    }

    private fun handleActionButtonClick() {
        when (viewModel.state.value) {
            is OnboardingWizard.OnboardingState.Welcome -> {
                // Action button click is handled by the wizard automatically
                hapticFeedback.tap()
            }
            is OnboardingWizard.OnboardingState.QuickQuestions -> {
                if (areAnswersComplete()) {
                    viewModel.generateProfileFromAnswers(currentAnswers)
                    hapticFeedback.success()
                }
            }
            is OnboardingWizard.OnboardingState.Complete -> {
                finishOnboarding()
            }
            is OnboardingWizard.OnboardingState.Error -> {
                startMagicalJourney()
            }
            else -> {
                // Handle other states
            }
        }
    }

    private fun updateProgress(progress: Float) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", (progress * 100).toInt())
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        progressText.text = "${(progress * 100).toInt()}%"
    }

    private fun showMagicMoment(moment: OnboardingWizard.MagicMoment) {
        val magicText = TextView(this).apply {
            text = moment.message
            textSize = 16f
            setTextColor(getColor(R.color.magic_text))
            alpha = 0f
        }
        
        rootLayout.addView(magicText)
        
        val animation = OnboardingWizard.createMagicAnimation(magicText, moment.type)
        animation.start()
        
        lifecycleScope.launch {
            delay(moment.duration)
            rootLayout.removeView(magicText)
        }
        
        // Play appropriate sound
        when (moment.type) {
            OnboardingWizard.MagicType.SPARKLE -> soundEffects.playSparkle()
            OnboardingWizard.MagicType.SUCCESS -> soundEffects.playSuccess()
            OnboardingWizard.MagicType.INSIGHT -> soundEffects.playInsight()
            OnboardingWizard.MagicType.CELEBRATION -> soundEffects.playCelebration()
            OnboardingWizard.MagicType.TIPS -> soundEffects.playTip()
        }
    }

    private fun showSetupTime(time: Long) {
        val timeText = "Setup completed in ${time / 1000} seconds!"
        subtitleText.text = timeText
        
        // Animate the achievement
        val scaleX = ObjectAnimator.ofFloat(subtitleText, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(subtitleText, "scaleY", 1f, 1.1f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 600
            start()
        }
    }

    private fun animateQuestionCard() {
        questionCard.alpha = 0f
        questionCard.translationY = 100f
        
        questionCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun checkAnswersComplete() {
        if (areAnswersComplete()) {
            actionButton.isVisible = true
            actionButton.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun areAnswersComplete(): Boolean {
        return currentAnswers.experienceLevel.isNotEmpty() &&
               currentAnswers.mainGoal.isNotEmpty() &&
               currentAnswers.favoriteClub.isNotEmpty() &&
               currentAnswers.practiceFrequency.isNotEmpty()
    }

    private fun startCameraGuidance() {
        lifecycleScope.launch {
            viewModel.getCameraGuidance().collect { guidance ->
                guidanceText.text = guidance.instruction
                
                // Update guidance overlay based on visual cue
                updateGuidanceOverlay(guidance.visualCue)
                
                if (guidance.isOptimal) {
                    hapticFeedback.success()
                } else {
                    hapticFeedback.tap()
                }
            }
        }
    }

    private fun updateGuidanceOverlay(visualCue: CameraPositioningGuide.VisualCue) {
        // Update the overlay based on the visual cue
        when (visualCue.type) {
            CameraPositioningGuide.CueType.CIRCLE_OVERLAY -> {
                guidanceOverlay.setImageResource(R.drawable.circle_overlay)
            }
            CameraPositioningGuide.CueType.ARROW_DIRECTION -> {
                guidanceOverlay.setImageResource(R.drawable.arrow_overlay)
            }
            CameraPositioningGuide.CueType.GRID_LINES -> {
                guidanceOverlay.setImageResource(R.drawable.grid_overlay)
            }
            CameraPositioningGuide.CueType.SILHOUETTE_GUIDE -> {
                guidanceOverlay.setImageResource(R.drawable.silhouette_overlay)
            }
            CameraPositioningGuide.CueType.DISTANCE_INDICATOR -> {
                guidanceOverlay.setImageResource(R.drawable.distance_overlay)
            }
        }
        
        // Animate the overlay
        val animation = when (visualCue.animation) {
            CameraPositioningGuide.AnimationType.PULSE -> createPulseAnimation(guidanceOverlay)
            CameraPositioningGuide.AnimationType.BOUNCE -> createBounceAnimation(guidanceOverlay)
            CameraPositioningGuide.AnimationType.FADE -> createFadeAnimation(guidanceOverlay)
            CameraPositioningGuide.AnimationType.SLIDE -> createSlideAnimation(guidanceOverlay)
            CameraPositioningGuide.AnimationType.ROTATE -> createRotateAnimation(guidanceOverlay)
        }
        
        animation.start()
    }

    private fun startSampleAnalysis() {
        lifecycleScope.launch {
            delay(2000) // Let animation play
            
            // Show analysis results
            analysisResults.isVisible = true
            improvementsList.isVisible = true
            
            // Animate results appearance
            val results = listOf(
                "Swing speed: 95 mph",
                "Tempo: Excellent",
                "Club path: Slightly inside-out",
                "Face angle: 2° closed"
            )
            
            results.forEachIndexed { index, result ->
                delay(500)
                addAnalysisResult(result, index)
            }
            
            delay(1000)
            
            // Show improvements
            val improvements = listOf(
                "Improve follow-through for 5% more distance",
                "Adjust grip for better face control",
                "Work on weight transfer timing"
            )
            
            improvements.forEachIndexed { index, improvement ->
                delay(600)
                addImprovement(improvement, index)
            }
        }
    }

    private fun addAnalysisResult(result: String, index: Int) {
        val resultView = TextView(this).apply {
            text = result
            textSize = 14f
            setTextColor(getColor(R.color.analysis_text))
            alpha = 0f
            translationX = 50f
        }
        
        analysisResults.addView(resultView)
        
        resultView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay((index * 100).toLong())
            .start()
    }

    private fun addImprovement(improvement: String, index: Int) {
        val improvementView = TextView(this).apply {
            text = "• $improvement"
            textSize = 14f
            setTextColor(getColor(R.color.improvement_text))
            alpha = 0f
            translationX = -50f
        }
        
        improvementsList.addView(improvementView)
        
        improvementView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay((index * 100).toLong())
            .start()
    }

    private fun finishOnboarding() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        
        // Custom transition
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // Animation helpers
    private fun createPulseAnimation(view: View): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    private fun createBounceAnimation(view: View): AnimatorSet {
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 0f)
        
        return AnimatorSet().apply {
            play(translateY)
            duration = 600
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    private fun createFadeAnimation(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f)
        
        return AnimatorSet().apply {
            play(alpha)
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    private fun createSlideAnimation(view: View): AnimatorSet {
        val translateX = ObjectAnimator.ofFloat(view, "translationX", -10f, 10f, -10f)
        
        return AnimatorSet().apply {
            play(translateX)
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    private fun createRotateAnimation(view: View): AnimatorSet {
        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        
        return AnimatorSet().apply {
            play(rotation)
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        particleSystem.stop()
    }
}