package com.swingsync.ai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.swingsync.ai.R
import com.swingsync.ai.data.model.*
import com.swingsync.ai.ui.theme.*
import com.swingsync.ai.visualization.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * VisualizationScreen - Main screen for beautiful swing visualization and analysis
 * 
 * Features:
 * - Tab-based navigation between different visualization types
 * - Swing comparison with 3D rendering
 * - Progress tracking with beautiful animations
 * - Heat maps for fault analysis
 * - Interactive charts for data exploration
 * - Export and sharing capabilities
 * - Customizable visualization settings
 */
class VisualizationScreen : Fragment() {
    
    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var containerLayout: FrameLayout
    private lateinit var settingsButton: ImageButton
    private lateinit var shareButton: FloatingActionButton
    private lateinit var playButton: FloatingActionButton
    
    // Visualization Components
    private lateinit var swingComparisonView: SwingComparisonView
    private lateinit var progressVisualization: ProgressVisualization
    private lateinit var heatMapGenerator: HeatMapGenerator
    private lateinit var interactiveCharts: InteractiveCharts
    
    // Data
    private var currentSession: RecordingSession? = null
    private var comparisonSession: RecordingSession? = null
    private var swingSessions = mutableListOf<SwingSession>()
    private var selectedTab = 0
    private var isPlaying = false
    
    // Settings
    private var visualizationSettings = VisualizationSettings()
    
    data class SwingSession(
        val sessionId: String,
        val timestamp: Long,
        val score: Float,
        val poseData: List<FramePoseData>,
        val analysis: SwingAnalysisFeedback,
        val clubUsed: String
    )
    
    data class VisualizationSettings(
        val showTrails: Boolean = true,
        val showSkeleton: Boolean = true,
        val showPhaseMarkers: Boolean = true,
        val animationSpeed: Float = 1.0f,
        val comparisonMode: SwingComparisonRenderer.ComparisonMode = SwingComparisonRenderer.ComparisonMode.SIDE_BY_SIDE,
        val colorScheme: String = "golf_theme",
        val exportQuality: String = "high"
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)
        
        initializeViews(view)
        setupTabs()
        setupVisualizationComponents()
        setupClickListeners()
        loadSampleData()
        
        return view
    }
    
    private fun initializeViews(view: View) {
        tabLayout = view.findViewById(R.id.tab_layout)
        containerLayout = view.findViewById(R.id.container_layout)
        settingsButton = view.findViewById(R.id.settings_button)
        shareButton = view.findViewById(R.id.share_button)
        playButton = view.findViewById(R.id.play_button)
    }
    
    private fun setupTabs() {
        val tabs = listOf(
            "Swing Comparison",
            "Progress",
            "Heat Map",
            "Analytics"
        )
        
        tabs.forEach { tabName ->
            tabLayout.addTab(tabLayout.newTab().setText(tabName))
        }
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    selectedTab = it.position
                    switchVisualizationTab(it.position)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupVisualizationComponents() {
        // Initialize swing comparison view
        swingComparisonView = SwingComparisonView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
        }
        
        // Initialize progress visualization
        progressVisualization = ProgressVisualization(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        
        // Initialize heat map generator
        heatMapGenerator = HeatMapGenerator(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        
        // Initialize interactive charts
        interactiveCharts = InteractiveCharts(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        
        // Add all views to container
        containerLayout.addView(swingComparisonView)
        containerLayout.addView(progressVisualization)
        containerLayout.addView(heatMapGenerator)
        containerLayout.addView(interactiveCharts)
    }
    
    private fun setupClickListeners() {
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
        
        shareButton.setOnClickListener {
            shareVisualization()
        }
        
        playButton.setOnClickListener {
            togglePlayback()
        }
    }
    
    private fun switchVisualizationTab(position: Int) {
        // Hide all views
        swingComparisonView.visibility = View.GONE
        progressVisualization.visibility = View.GONE
        heatMapGenerator.visibility = View.GONE
        interactiveCharts.visibility = View.GONE
        
        // Show selected view and start animation
        when (position) {
            0 -> {
                swingComparisonView.visibility = View.VISIBLE
                swingComparisonView.startAnimation()
                playButton.show()
            }
            1 -> {
                progressVisualization.visibility = View.VISIBLE
                progressVisualization.startAnimation()
                playButton.hide()
            }
            2 -> {
                heatMapGenerator.visibility = View.VISIBLE
                heatMapGenerator.startAnimation()
                playButton.hide()
            }
            3 -> {
                interactiveCharts.visibility = View.VISIBLE
                interactiveCharts.startAnimation()
                playButton.hide()
            }
        }
    }
    
    private fun loadSampleData() {
        lifecycleScope.launch {
            // Load sample swing sessions
            loadSampleSwingSessions()
            
            // Setup initial data for each visualization
            setupSwingComparisonData()
            setupProgressData()
            setupHeatMapData()
            setupChartsData()
        }
    }
    
    private fun loadSampleSwingSessions() {
        val sessions = listOf(
            createSampleSession("session_1", "Driver", 85f, 7),
            createSampleSession("session_2", "7-Iron", 78f, 14),
            createSampleSession("session_3", "Driver", 88f, 21),
            createSampleSession("session_4", "Putter", 92f, 28)
        )
        
        swingSessions.addAll(sessions)
        
        // Set current and comparison sessions
        currentSession = convertToRecordingSession(sessions[0])
        comparisonSession = convertToRecordingSession(sessions[2])
    }
    
    private fun createSampleSession(id: String, club: String, score: Float, daysAgo: Int): SwingSession {
        val timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        val poseData = generateSamplePoseData()
        val analysis = generateSampleAnalysis(score)
        
        return SwingSession(id, timestamp, score, poseData, analysis, club)
    }
    
    private fun generateSamplePoseData(): List<FramePoseData> {
        // Generate sample pose data for a golf swing
        val frames = mutableListOf<FramePoseData>()
        val swingPhases = 60 // 60 frames for a complete swing
        
        for (i in 0 until swingPhases) {
            val progress = i.toFloat() / swingPhases
            val frameData = mutableMapOf<String, PoseKeypoint>()
            
            // Generate keypoints for major body parts
            val bodyParts = listOf(
                "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
                "left_wrist", "right_wrist", "left_hip", "right_hip",
                "left_knee", "right_knee", "left_ankle", "right_ankle"
            )
            
            bodyParts.forEach { part ->
                val keypoint = generateKeypointForPart(part, progress)
                frameData[part] = keypoint
            }
            
            frames.add(frameData)
        }
        
        return frames
    }
    
    private fun generateKeypointForPart(part: String, progress: Float): PoseKeypoint {
        // Generate realistic golf swing motion for each body part
        val baseX = when {
            part.contains("left") -> 0.3f
            part.contains("right") -> 0.7f
            else -> 0.5f
        }
        
        val baseY = when {
            part.contains("shoulder") -> 0.3f
            part.contains("elbow") -> 0.4f
            part.contains("wrist") -> 0.5f
            part.contains("hip") -> 0.6f
            part.contains("knee") -> 0.8f
            part.contains("ankle") -> 0.95f
            else -> 0.5f
        }
        
        // Add swing motion variation
        val swingPhase = (progress * 2 * Math.PI).toFloat()
        val swingOffset = Math.sin(swingPhase.toDouble()).toFloat() * 0.1f
        
        return PoseKeypoint(
            x = baseX + swingOffset,
            y = baseY + swingOffset * 0.5f,
            z = 0f,
            visibility = 0.9f
        )
    }
    
    private fun generateSampleAnalysis(score: Float): SwingAnalysisFeedback {
        val faults = listOf(
            DetectedFault(
                "posture_001",
                "Setup Posture",
                listOf("P1", "P2"),
                "Maintain proper spine angle throughout the swing",
                emptyList(),
                "posture_tips",
                0.6f
            ),
            DetectedFault(
                "swing_plane_001",
                "Swing Plane",
                listOf("P4", "P5", "P6"),
                "Keep the club on the correct swing plane",
                emptyList(),
                "swing_plane_tips",
                0.4f
            )
        )
        
        val tips = listOf(
            LLMGeneratedTip(
                "Your swing shows good tempo and rhythm",
                "Focus on maintaining consistent spine angle",
                "Practice the setup drill for 10 minutes daily"
            ),
            LLMGeneratedTip(
                "Impact position is improving",
                "Work on weight transfer through the swing",
                "Use the weight shift drill with medicine ball"
            )
        )
        
        return SwingAnalysisFeedback(
            sessionId = "session_${System.currentTimeMillis()}",
            summaryOfFindings = "Overall swing quality is good with minor adjustments needed",
            detailedFeedback = tips,
            rawDetectedFaults = faults
        )
    }
    
    private fun convertToRecordingSession(swingSession: SwingSession): RecordingSession {
        val poseResults = swingSession.poseData.mapIndexed { index, frameData ->
            PoseDetectionResult(
                keypoints = frameData,
                confidence = 0.9f,
                timestamp = swingSession.timestamp + index * 33L, // 30 FPS
                frameIndex = index
            )
        }
        
        return RecordingSession(
            sessionId = swingSession.sessionId,
            userId = "user_123",
            clubUsed = swingSession.clubUsed,
            startTime = swingSession.timestamp,
            endTime = swingSession.timestamp + poseResults.size * 33L,
            fps = 30f,
            totalFrames = poseResults.size,
            poseDetectionResults = poseResults,
            isRecording = false
        )
    }
    
    private fun setupSwingComparisonData() {
        currentSession?.let { current ->
            swingComparisonView.setCurrentSwing(current.poseDetectionResults.map { it.keypoints })
        }
        
        comparisonSession?.let { comparison ->
            swingComparisonView.setComparisonSwing(comparison.poseDetectionResults.map { it.keypoints })
        }
        
        // Apply settings
        swingComparisonView.setComparisonMode(visualizationSettings.comparisonMode)
        swingComparisonView.setShowTrails(visualizationSettings.showTrails)
        swingComparisonView.setShowSkeleton(visualizationSettings.showSkeleton)
        swingComparisonView.setShowPhaseMarkers(visualizationSettings.showPhaseMarkers)
        swingComparisonView.setAnimationSpeed(visualizationSettings.animationSpeed)
    }
    
    private fun setupProgressData() {
        val progressMetrics = listOf(
            ProgressVisualization.ProgressMetric(
                "Overall Score",
                85f,
                90f,
                swingSessions.map { it.score },
                "points",
                android.graphics.Color.parseColor("#4CAF50")
            ),
            ProgressVisualization.ProgressMetric(
                "Consistency",
                78f,
                85f,
                listOf(60f, 65f, 70f, 75f, 78f),
                "%",
                android.graphics.Color.parseColor("#2196F3")
            ),
            ProgressVisualization.ProgressMetric(
                "Power",
                82f,
                88f,
                listOf(70f, 75f, 78f, 80f, 82f),
                "mph",
                android.graphics.Color.parseColor("#FF9800")
            )
        )
        
        val milestones = listOf(
            ProgressVisualization.Milestone(
                "consistency_80",
                "Consistency Champion",
                "Achieve 80% swing consistency",
                80f,
                78f,
                false
            ),
            ProgressVisualization.Milestone(
                "score_90",
                "Excellence Award",
                "Reach overall score of 90",
                90f,
                85f,
                false
            )
        )
        
        progressVisualization.setProgressMetrics(progressMetrics)
        progressVisualization.updateMilestones(milestones)
    }
    
    private fun setupHeatMapData() {
        // Generate heat map data from swing sessions
        val faultHeatMaps = heatMapGenerator.generateHeatMapFromSessions(swingSessions)
        
        faultHeatMaps.forEach { heatMap ->
            heatMapGenerator.addFaultHeatMap(heatMap)
        }
        
        heatMapGenerator.setHeatMapType(HeatMapGenerator.HeatMapType.BODY_FAULTS)
        heatMapGenerator.setShowLegend(true)
        heatMapGenerator.setShowBodyOutline(true)
    }
    
    private fun setupChartsData() {
        // Setup line chart for progress
        val progressData = swingSessions.mapIndexed { index, session ->
            InteractiveCharts.ChartDataPoint(
                x = index.toFloat(),
                y = 0f,
                value = session.score,
                label = "Session ${index + 1}",
                timestamp = session.timestamp
            )
        }
        
        val chartSeries = listOf(
            InteractiveCharts.ChartSeries(
                "Progress",
                progressData,
                android.graphics.Color.parseColor("#4CAF50"),
                fillArea = true
            )
        )
        
        // Setup radar chart for skill assessment
        val radarMetrics = listOf(
            InteractiveCharts.RadarMetric("Stance", 85f, 100f, "%", android.graphics.Color.parseColor("#4CAF50"), 90f),
            InteractiveCharts.RadarMetric("Backswing", 78f, 100f, "%", android.graphics.Color.parseColor("#2196F3"), 85f),
            InteractiveCharts.RadarMetric("Downswing", 82f, 100f, "%", android.graphics.Color.parseColor("#FF9800"), 88f),
            InteractiveCharts.RadarMetric("Impact", 88f, 100f, "%", android.graphics.Color.parseColor("#9C27B0"), 92f),
            InteractiveCharts.RadarMetric("Follow Through", 75f, 100f, "%", android.graphics.Color.parseColor("#FF5722"), 80f)
        )
        
        interactiveCharts.setChartType(InteractiveCharts.ChartType.LINE)
        interactiveCharts.setChartSeries(chartSeries)
        interactiveCharts.setRadarMetrics(radarMetrics)
        interactiveCharts.setShowLegend(true)
        interactiveCharts.setShowGrid(true)
        interactiveCharts.setShowDataPoints(true)
    }
    
    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_visualization_settings, null)
        
        // Setup settings controls
        val showTrailsSwitch = view.findViewById<Switch>(R.id.show_trails_switch)
        val showSkeletonSwitch = view.findViewById<Switch>(R.id.show_skeleton_switch)
        val showPhaseMarkersSwitch = view.findViewById<Switch>(R.id.show_phase_markers_switch)
        val animationSpeedSlider = view.findViewById<SeekBar>(R.id.animation_speed_slider)
        val comparisonModeSpinner = view.findViewById<Spinner>(R.id.comparison_mode_spinner)
        val colorSchemeSpinner = view.findViewById<Spinner>(R.id.color_scheme_spinner)
        val exportQualitySpinner = view.findViewById<Spinner>(R.id.export_quality_spinner)
        
        // Set current values
        showTrailsSwitch.isChecked = visualizationSettings.showTrails
        showSkeletonSwitch.isChecked = visualizationSettings.showSkeleton
        showPhaseMarkersSwitch.isChecked = visualizationSettings.showPhaseMarkers
        animationSpeedSlider.progress = (visualizationSettings.animationSpeed * 100).toInt()
        
        // Setup spinners
        setupSpinner(comparisonModeSpinner, arrayOf("Side by Side", "Overlay", "Split Screen"))
        setupSpinner(colorSchemeSpinner, arrayOf("Golf Theme", "High Contrast", "Colorblind Friendly"))
        setupSpinner(exportQualitySpinner, arrayOf("High", "Medium", "Low"))
        
        // Setup listeners
        showTrailsSwitch.setOnCheckedChangeListener { _, isChecked ->
            visualizationSettings = visualizationSettings.copy(showTrails = isChecked)
            applySettings()
        }
        
        showSkeletonSwitch.setOnCheckedChangeListener { _, isChecked ->
            visualizationSettings = visualizationSettings.copy(showSkeleton = isChecked)
            applySettings()
        }
        
        showPhaseMarkersSwitch.setOnCheckedChangeListener { _, isChecked ->
            visualizationSettings = visualizationSettings.copy(showPhaseMarkers = isChecked)
            applySettings()
        }
        
        animationSpeedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                visualizationSettings = visualizationSettings.copy(animationSpeed = progress / 100f)
                applySettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun setupSpinner(spinner: Spinner, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
    
    private fun applySettings() {
        swingComparisonView.setShowTrails(visualizationSettings.showTrails)
        swingComparisonView.setShowSkeleton(visualizationSettings.showSkeleton)
        swingComparisonView.setShowPhaseMarkers(visualizationSettings.showPhaseMarkers)
        swingComparisonView.setAnimationSpeed(visualizationSettings.animationSpeed)
        swingComparisonView.setComparisonMode(visualizationSettings.comparisonMode)
    }
    
    private fun shareVisualization() {
        lifecycleScope.launch {
            val bitmap = when (selectedTab) {
                0 -> swingComparisonView.exportFrame()
                1 -> progressVisualization.exportChart()
                2 -> heatMapGenerator.exportHeatMap()
                3 -> interactiveCharts.exportChart()
                else -> null
            }
            
            bitmap?.let { 
                shareVisualizationBitmap(it)
            }
        }
    }
    
    private fun shareVisualizationBitmap(bitmap: Bitmap) {
        // Implementation for sharing the visualization
        // This would save the bitmap and create a share intent
    }
    
    private fun togglePlayback() {
        isPlaying = !isPlaying
        
        if (isPlaying) {
            swingComparisonView.startAnimation()
            playButton.setImageResource(R.drawable.ic_pause)
        } else {
            swingComparisonView.stopAnimation()
            playButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }
    
    // Custom SwingComparisonView wrapper to integrate with the OpenGL renderer
    inner class SwingComparisonView(context: Context) : FrameLayout(context) {
        private lateinit var glSurfaceView: GLSurfaceView
        private lateinit var renderer: SwingComparisonRenderer
        
        init {
            setupGLSurfaceView()
        }
        
        private fun setupGLSurfaceView() {
            glSurfaceView = GLSurfaceView(context)
            glSurfaceView.setEGLContextClientVersion(3)
            
            renderer = SwingComparisonRenderer(context) { bitmap ->
                // Handle frame capture for export
            }
            
            glSurfaceView.setRenderer(renderer)
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            
            addView(glSurfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        
        fun setCurrentSwing(swingData: List<FramePoseData>) {
            renderer.setCurrentSwing(swingData)
        }
        
        fun setComparisonSwing(swingData: List<FramePoseData>) {
            renderer.setComparisonSwing(swingData)
        }
        
        fun setComparisonMode(mode: SwingComparisonRenderer.ComparisonMode) {
            renderer.setComparisonMode(mode)
        }
        
        fun setShowTrails(show: Boolean) {
            renderer.setShowTrails(show)
        }
        
        fun setShowSkeleton(show: Boolean) {
            renderer.setShowSkeleton(show)
        }
        
        fun setShowPhaseMarkers(show: Boolean) {
            renderer.setShowPhaseMarkers(show)
        }
        
        fun setAnimationSpeed(speed: Float) {
            renderer.setAnimationSpeed(speed)
        }
        
        fun startAnimation() {
            renderer.startAnimation()
        }
        
        fun stopAnimation() {
            renderer.stopAnimation()
        }
        
        fun exportFrame(): Bitmap? {
            return renderer.exportFrame()
        }
        
        override fun onTouchEvent(event: MotionEvent): Boolean {
            return renderer.handleTouchEvent(event)
        }
    }
    
    companion object {
        fun newInstance(): VisualizationScreen {
            return VisualizationScreen()
        }
    }
}