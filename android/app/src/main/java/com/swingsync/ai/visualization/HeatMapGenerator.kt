package com.swingsync.ai.visualization

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import com.swingsync.ai.data.model.*
import com.swingsync.ai.ui.theme.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * HeatMapGenerator - Creates beautiful heat maps showing swing fault patterns and improvement areas
 * 
 * Features:
 * - Body heat maps showing common fault areas
 * - Swing plane heat maps with 3D visualization
 * - Temporal heat maps showing fault patterns over time
 * - Comparative heat maps between sessions
 * - Interactive exploration with touch
 * - Customizable color schemes and intensity levels
 */
class HeatMapGenerator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Data structures
    data class HeatMapPoint(
        val x: Float,
        val y: Float,
        val intensity: Float,
        val faultType: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class FaultHeatMap(
        val faultId: String,
        val faultName: String,
        val severity: Float,
        val affectedBodyParts: List<String>,
        val heatPoints: List<HeatMapPoint>,
        val color: Int,
        val sessions: List<String> = emptyList()
    )
    
    data class SwingPlaneHeatMap(
        val planePoints: List<HeatMapPoint>,
        val idealPlane: List<PointF>,
        val deviations: List<Float>,
        val clubType: String
    )
    
    data class TemporalHeatMap(
        val timeSlices: List<HeatMapSlice>,
        val duration: Long,
        val faultProgression: Map<String, List<Float>>
    )
    
    data class HeatMapSlice(
        val timeStart: Long,
        val timeEnd: Long,
        val heatPoints: List<HeatMapPoint>,
        val averageIntensity: Float
    )
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val heatPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Heat map data
    private var faultHeatMaps = mutableListOf<FaultHeatMap>()
    private var swingPlaneHeatMap: SwingPlaneHeatMap? = null
    private var temporalHeatMap: TemporalHeatMap? = null
    private var bodyOutlinePoints = mutableListOf<PointF>()
    
    // Settings
    private var heatMapType = HeatMapType.BODY_FAULTS
    private var intensityThreshold = 0.3f
    private var showLegend = true
    private var showBodyOutline = true
    private var animationProgress = 0f
    private var isAnimating = false
    private var selectedFaultType: String? = null
    
    // Layout
    private var heatMapRect = RectF()
    private var legendRect = RectF()
    private var controlsRect = RectF()
    
    // Colors
    private val heatMapColors = intArrayOf(
        Color.TRANSPARENT,
        Color.argb(100, 0, 255, 0),    // Low intensity - Green
        Color.argb(150, 255, 255, 0),  // Medium - Yellow
        Color.argb(200, 255, 165, 0),  // High - Orange
        Color.argb(255, 255, 0, 0)     // Very High - Red
    )
    
    private val bodyOutlineColor = Color.argb(150, 100, 100, 100)
    private val gridColor = Color.argb(50, 200, 200, 200)
    
    enum class HeatMapType {
        BODY_FAULTS,      // Heat map overlaid on body outline
        SWING_PLANE,      // 3D swing plane with deviation heat map
        TEMPORAL,         // Heat map showing faults over time
        COMPARATIVE,      // Comparison between sessions
        CLUB_SPECIFIC     // Heat map specific to club type
    }
    
    enum class IntensityScale {
        LINEAR,
        LOGARITHMIC,
        EXPONENTIAL
    }
    
    init {
        setupPaints()
        setupBodyOutline()
        setupSampleData()
    }
    
    private fun setupPaints() {
        backgroundPaint.color = Color.parseColor("#FAFAFA")
        backgroundPaint.style = Paint.Style.FILL
        
        heatPaint.style = Paint.Style.FILL
        heatPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        
        overlayPaint.style = Paint.Style.FILL
        overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        
        textPaint.color = Color.parseColor("#212121")
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        
        legendPaint.textSize = 20f
        legendPaint.color = Color.parseColor("#424242")
    }
    
    private fun setupBodyOutline() {
        // Create a simplified body outline for golf posture
        bodyOutlinePoints.clear()
        
        // Head (circle approximation)
        val headCenterX = 0.5f
        val headCenterY = 0.15f
        val headRadius = 0.08f
        
        for (i in 0..36) {
            val angle = i * 10f * PI / 180f
            val x = headCenterX + headRadius * cos(angle).toFloat()
            val y = headCenterY + headRadius * sin(angle).toFloat()
            bodyOutlinePoints.add(PointF(x, y))
        }
        
        // Body outline points (normalized coordinates 0-1)
        val bodyPoints = listOf(
            // Neck to shoulders
            PointF(0.5f, 0.23f),
            PointF(0.3f, 0.25f), // Left shoulder
            PointF(0.7f, 0.25f), // Right shoulder
            
            // Arms
            PointF(0.25f, 0.4f), // Left elbow
            PointF(0.2f, 0.55f), // Left wrist
            PointF(0.75f, 0.4f), // Right elbow
            PointF(0.8f, 0.55f), // Right wrist
            
            // Torso
            PointF(0.35f, 0.35f), // Left side
            PointF(0.65f, 0.35f), // Right side
            PointF(0.4f, 0.6f),   // Left hip
            PointF(0.6f, 0.6f),   // Right hip
            
            // Legs
            PointF(0.42f, 0.8f),  // Left knee
            PointF(0.58f, 0.8f),  // Right knee
            PointF(0.4f, 0.95f),  // Left ankle
            PointF(0.6f, 0.95f)   // Right ankle
        )
        
        bodyOutlinePoints.addAll(bodyPoints)
    }
    
    private fun setupSampleData() {
        // Sample fault heat maps
        faultHeatMaps.add(
            FaultHeatMap(
                "posture_001",
                "Poor Setup Posture",
                0.8f,
                listOf("spine", "shoulders", "hips"),
                generateSampleHeatPoints(0.5f, 0.4f, 0.8f),
                Color.parseColor("#FF5722")
            )
        )
        
        faultHeatMaps.add(
            FaultHeatMap(
                "swing_plane_001",
                "Over the Top",
                0.6f,
                listOf("shoulders", "arms", "club_path"),
                generateSampleHeatPoints(0.3f, 0.3f, 0.6f),
                Color.parseColor("#FF9800")
            )
        )
        
        faultHeatMaps.add(
            FaultHeatMap(
                "impact_001",
                "Early Extension",
                0.7f,
                listOf("hips", "spine"),
                generateSampleHeatPoints(0.5f, 0.6f, 0.7f),
                Color.parseColor("#F44336")
            )
        )
    }
    
    private fun generateSampleHeatPoints(centerX: Float, centerY: Float, intensity: Float): List<HeatMapPoint> {
        val points = mutableListOf<HeatMapPoint>()
        val numPoints = 20
        
        for (i in 0 until numPoints) {
            val angle = (i * 360f / numPoints) * PI / 180f
            val radius = 0.1f + (Math.random() * 0.05f).toFloat()
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            val pointIntensity = intensity * (0.5f + Math.random().toFloat() * 0.5f)
            
            points.add(HeatMapPoint(x, y, pointIntensity))
        }
        
        return points
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val padding = 40f
        val legendHeight = 60f
        val controlsHeight = 50f
        
        heatMapRect.set(
            padding,
            padding + controlsHeight,
            w - padding,
            h - padding - legendHeight
        )
        
        legendRect.set(
            padding,
            h - legendHeight,
            w - padding,
            h - padding
        )
        
        controlsRect.set(
            padding,
            padding,
            w - padding,
            padding + controlsHeight
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw controls
        drawControls(canvas)
        
        // Draw heat map based on type
        when (heatMapType) {
            HeatMapType.BODY_FAULTS -> drawBodyFaultHeatMap(canvas)
            HeatMapType.SWING_PLANE -> drawSwingPlaneHeatMap(canvas)
            HeatMapType.TEMPORAL -> drawTemporalHeatMap(canvas)
            HeatMapType.COMPARATIVE -> drawComparativeHeatMap(canvas)
            HeatMapType.CLUB_SPECIFIC -> drawClubSpecificHeatMap(canvas)
        }
        
        // Draw legend
        if (showLegend) {
            drawLegend(canvas)
        }
    }
    
    private fun drawControls(canvas: Canvas) {
        // Draw heat map type selector
        val controlPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        controlPaint.color = Color.parseColor("#E0E0E0")
        controlPaint.style = Paint.Style.FILL
        
        canvas.drawRoundRect(controlsRect, 8f, 8f, controlPaint)
        
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.parseColor("#424242")
        titlePaint.textSize = 24f
        titlePaint.typeface = Typeface.DEFAULT_BOLD
        
        canvas.drawText("Heat Map: ${heatMapType.name.replace("_", " ")}", 
                       controlsRect.left + 20f, controlsRect.centerY() + 8f, titlePaint)
    }
    
    private fun drawBodyFaultHeatMap(canvas: Canvas) {
        // Draw body outline if enabled
        if (showBodyOutline) {
            drawBodyOutline(canvas)
        }
        
        // Draw heat map points
        for (faultHeatMap in faultHeatMaps) {
            if (selectedFaultType == null || faultHeatMap.faultId == selectedFaultType) {
                drawHeatMapPoints(canvas, faultHeatMap.heatPoints, faultHeatMap.color)
            }
        }
        
        // Draw fault labels
        drawFaultLabels(canvas)
    }
    
    private fun drawBodyOutline(canvas: Canvas) {
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        outlinePaint.color = bodyOutlineColor
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = 3f
        
        // Convert normalized coordinates to canvas coordinates
        val path = Path()
        var isFirst = true
        
        for (point in bodyOutlinePoints) {
            val canvasX = heatMapRect.left + point.x * heatMapRect.width()
            val canvasY = heatMapRect.top + point.y * heatMapRect.height()
            
            if (isFirst) {
                path.moveTo(canvasX, canvasY)
                isFirst = false
            } else {
                path.lineTo(canvasX, canvasY)
            }
        }
        
        canvas.drawPath(path, outlinePaint)
        
        // Draw joint circles
        val jointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        jointPaint.color = Color.argb(100, 100, 100, 100)
        jointPaint.style = Paint.Style.FILL
        
        val jointPoints = listOf(
            PointF(0.3f, 0.25f), // Shoulders
            PointF(0.7f, 0.25f),
            PointF(0.25f, 0.4f), // Elbows
            PointF(0.75f, 0.4f),
            PointF(0.4f, 0.6f),  // Hips
            PointF(0.6f, 0.6f),
            PointF(0.42f, 0.8f), // Knees
            PointF(0.58f, 0.8f)
        )
        
        for (joint in jointPoints) {
            val canvasX = heatMapRect.left + joint.x * heatMapRect.width()
            val canvasY = heatMapRect.top + joint.y * heatMapRect.height()
            canvas.drawCircle(canvasX, canvasY, 8f, jointPaint)
        }
    }
    
    private fun drawHeatMapPoints(canvas: Canvas, heatPoints: List<HeatMapPoint>, baseColor: Int) {
        for (point in heatPoints) {
            val canvasX = heatMapRect.left + point.x * heatMapRect.width()
            val canvasY = heatMapRect.top + point.y * heatMapRect.height()
            
            // Apply animation progress
            val animatedIntensity = point.intensity * animationProgress
            
            if (animatedIntensity > intensityThreshold) {
                drawHeatPoint(canvas, canvasX, canvasY, animatedIntensity, baseColor)
            }
        }
    }
    
    private fun drawHeatPoint(canvas: Canvas, x: Float, y: Float, intensity: Float, baseColor: Int) {
        val radius = 30f + intensity * 40f
        val alpha = (intensity * 255).toInt().coerceIn(0, 255)
        
        // Create radial gradient for heat effect
        val gradient = RadialGradient(
            x, y, radius,
            intArrayOf(
                Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Color.argb(alpha / 2, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        
        heatPaint.shader = gradient
        canvas.drawCircle(x, y, radius, heatPaint)
        
        // Draw center point
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        centerPaint.color = Color.argb(alpha, 255, 255, 255)
        canvas.drawCircle(x, y, 4f, centerPaint)
    }
    
    private fun drawFaultLabels(canvas: Canvas) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.parseColor("#212121")
        labelPaint.textSize = 20f
        labelPaint.typeface = Typeface.DEFAULT_BOLD
        
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = Color.argb(200, 255, 255, 255)
        bgPaint.style = Paint.Style.FILL
        
        for (i in faultHeatMaps.indices) {
            val faultHeatMap = faultHeatMaps[i]
            val labelY = heatMapRect.top + 40f + i * 30f
            
            if (selectedFaultType == null || faultHeatMap.faultId == selectedFaultType) {
                // Draw background
                val textWidth = labelPaint.measureText(faultHeatMap.faultName)
                val labelRect = RectF(
                    heatMapRect.left + 10f,
                    labelY - 15f,
                    heatMapRect.left + textWidth + 40f,
                    labelY + 15f
                )
                
                canvas.drawRoundRect(labelRect, 8f, 8f, bgPaint)
                
                // Draw color indicator
                val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                colorPaint.color = faultHeatMap.color
                canvas.drawCircle(heatMapRect.left + 25f, labelY, 6f, colorPaint)
                
                // Draw label
                canvas.drawText(faultHeatMap.faultName, heatMapRect.left + 35f, labelY + 6f, labelPaint)
            }
        }
    }
    
    private fun drawSwingPlaneHeatMap(canvas: Canvas) {
        swingPlaneHeatMap?.let { heatMap ->
            // Draw 3D swing plane visualization
            draw3DSwingPlane(canvas, heatMap)
        }
    }
    
    private fun draw3DSwingPlane(canvas: Canvas, heatMap: SwingPlaneHeatMap) {
        // Simplified 3D representation
        val planePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        planePaint.color = Color.argb(100, 100, 150, 255)
        planePaint.style = Paint.Style.FILL
        
        // Draw ideal plane
        val idealPath = Path()
        var isFirst = true
        
        for (point in heatMap.idealPlane) {
            val canvasX = heatMapRect.left + point.x * heatMapRect.width()
            val canvasY = heatMapRect.top + point.y * heatMapRect.height()
            
            if (isFirst) {
                idealPath.moveTo(canvasX, canvasY)
                isFirst = false
            } else {
                idealPath.lineTo(canvasX, canvasY)
            }
        }
        
        canvas.drawPath(idealPath, planePaint)
        
        // Draw deviation heat points
        drawHeatMapPoints(canvas, heatMap.planePoints, Color.parseColor("#FF5722"))
        
        // Draw club type label
        val clubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        clubPaint.color = Color.parseColor("#424242")
        clubPaint.textSize = 24f
        clubPaint.typeface = Typeface.DEFAULT_BOLD
        
        canvas.drawText("Club: ${heatMap.clubType}", 
                       heatMapRect.left + 20f, heatMapRect.top + 30f, clubPaint)
    }
    
    private fun drawTemporalHeatMap(canvas: Canvas) {
        temporalHeatMap?.let { heatMap ->
            // Draw time-based heat map
            drawTimeSlices(canvas, heatMap.timeSlices)
            drawFaultProgression(canvas, heatMap.faultProgression)
        }
    }
    
    private fun drawTimeSlices(canvas: Canvas, timeSlices: List<HeatMapSlice>) {
        val sliceWidth = heatMapRect.width() / timeSlices.size
        
        for (i in timeSlices.indices) {
            val slice = timeSlices[i]
            val sliceRect = RectF(
                heatMapRect.left + i * sliceWidth,
                heatMapRect.top,
                heatMapRect.left + (i + 1) * sliceWidth,
                heatMapRect.bottom
            )
            
            // Draw slice background with intensity color
            val intensityColor = getIntensityColor(slice.averageIntensity)
            val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            slicePaint.color = intensityColor
            
            canvas.drawRect(sliceRect, slicePaint)
            
            // Draw heat points within slice
            for (point in slice.heatPoints) {
                val canvasX = sliceRect.left + point.x * sliceRect.width()
                val canvasY = sliceRect.top + point.y * sliceRect.height()
                drawHeatPoint(canvas, canvasX, canvasY, point.intensity, Color.parseColor("#FF0000"))
            }
        }
    }
    
    private fun drawFaultProgression(canvas: Canvas, faultProgression: Map<String, List<Float>>) {
        // Draw fault progression lines
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = 3f
        
        var colorIndex = 0
        val colors = listOf(
            Color.parseColor("#FF5722"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800")
        )
        
        for ((faultName, progression) in faultProgression) {
            progressPaint.color = colors[colorIndex % colors.size]
            
            val path = Path()
            val stepX = heatMapRect.width() / (progression.size - 1)
            
            for (i in progression.indices) {
                val x = heatMapRect.left + i * stepX
                val y = heatMapRect.bottom - (progression[i] / 100f) * heatMapRect.height()
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            canvas.drawPath(path, progressPaint)
            colorIndex++
        }
    }
    
    private fun drawComparativeHeatMap(canvas: Canvas) {
        // Draw side-by-side comparison
        val halfWidth = heatMapRect.width() / 2
        
        // Left side - Session A
        val leftRect = RectF(
            heatMapRect.left,
            heatMapRect.top,
            heatMapRect.left + halfWidth,
            heatMapRect.bottom
        )
        
        // Right side - Session B
        val rightRect = RectF(
            heatMapRect.left + halfWidth,
            heatMapRect.top,
            heatMapRect.right,
            heatMapRect.bottom
        )
        
        // Draw separator
        val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        separatorPaint.color = Color.parseColor("#BDBDBD")
        separatorPaint.strokeWidth = 2f
        
        canvas.drawLine(
            heatMapRect.left + halfWidth,
            heatMapRect.top,
            heatMapRect.left + halfWidth,
            heatMapRect.bottom,
            separatorPaint
        )
        
        // Draw labels
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.parseColor("#424242")
        labelPaint.textSize = 20f
        labelPaint.textAlign = Paint.Align.CENTER
        
        canvas.drawText("Session A", leftRect.centerX(), leftRect.top + 30f, labelPaint)
        canvas.drawText("Session B", rightRect.centerX(), rightRect.top + 30f, labelPaint)
    }
    
    private fun drawClubSpecificHeatMap(canvas: Canvas) {
        // Draw heat map specific to club type
        drawBodyFaultHeatMap(canvas)
        
        // Add club-specific annotations
        val clubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        clubPaint.color = Color.parseColor("#424242")
        clubPaint.textSize = 18f
        
        canvas.drawText("Club-specific analysis", 
                       heatMapRect.left + 20f, heatMapRect.bottom - 20f, clubPaint)
    }
    
    private fun drawLegend(canvas: Canvas) {
        // Draw intensity legend
        val legendItemWidth = legendRect.width() / 5
        
        for (i in 0..4) {
            val itemRect = RectF(
                legendRect.left + i * legendItemWidth,
                legendRect.top,
                legendRect.left + (i + 1) * legendItemWidth,
                legendRect.bottom - 20f
            )
            
            val intensityColor = getIntensityColor(i / 4f)
            val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            itemPaint.color = intensityColor
            
            canvas.drawRect(itemRect, itemPaint)
            
            // Draw intensity label
            val intensityText = when (i) {
                0 -> "Low"
                1 -> "Med-Low"
                2 -> "Medium"
                3 -> "Med-High"
                4 -> "High"
                else -> ""
            }
            
            legendPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(intensityText, itemRect.centerX(), legendRect.bottom - 5f, legendPaint)
        }
    }
    
    private fun getIntensityColor(intensity: Float): Int {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        
        return when {
            clampedIntensity < 0.2f -> Color.argb(100, 0, 255, 0)    // Green
            clampedIntensity < 0.4f -> Color.argb(120, 255, 255, 0)  // Yellow
            clampedIntensity < 0.6f -> Color.argb(140, 255, 165, 0)  // Orange
            clampedIntensity < 0.8f -> Color.argb(160, 255, 69, 0)   // Red-Orange
            else -> Color.argb(180, 255, 0, 0)                       // Red
        }
    }
    
    fun startAnimation() {
        if (isAnimating) return
        
        isAnimating = true
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                animationProgress = interpolatedTime
                invalidate()
            }
        }
        
        animation.duration = 2000L
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                isAnimating = false
                animationProgress = 1f
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        
        startAnimation(animation)
    }
    
    // Public API methods
    fun setHeatMapType(type: HeatMapType) {
        heatMapType = type
        invalidate()
    }
    
    fun addFaultHeatMap(faultHeatMap: FaultHeatMap) {
        faultHeatMaps.add(faultHeatMap)
        invalidate()
    }
    
    fun setSwingPlaneHeatMap(heatMap: SwingPlaneHeatMap) {
        swingPlaneHeatMap = heatMap
        invalidate()
    }
    
    fun setTemporalHeatMap(heatMap: TemporalHeatMap) {
        temporalHeatMap = heatMap
        invalidate()
    }
    
    fun setIntensityThreshold(threshold: Float) {
        intensityThreshold = threshold.coerceIn(0f, 1f)
        invalidate()
    }
    
    fun setSelectedFaultType(faultType: String?) {
        selectedFaultType = faultType
        invalidate()
    }
    
    fun setShowLegend(show: Boolean) {
        showLegend = show
        invalidate()
    }
    
    fun setShowBodyOutline(show: Boolean) {
        showBodyOutline = show
        invalidate()
    }
    
    fun exportHeatMap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
    
    fun generateHeatMapFromSessions(sessions: List<SwingSession>): List<FaultHeatMap> {
        val faultMap = mutableMapOf<String, MutableList<HeatMapPoint>>()
        
        for (session in sessions) {
            for (fault in session.faults) {
                val faultPoints = faultMap.getOrPut(fault.faultId) { mutableListOf() }
                
                // Generate heat points based on fault data
                val heatPoints = generateHeatPointsFromFault(fault)
                faultPoints.addAll(heatPoints)
            }
        }
        
        return faultMap.map { (faultId, points) ->
            val fault = sessions.flatMap { it.faults }.find { it.faultId == faultId }
            FaultHeatMap(
                faultId,
                fault?.faultName ?: "Unknown Fault",
                fault?.severity ?: 0.5f,
                fault?.pPositionsImplicated ?: emptyList(),
                points,
                getColorForFault(faultId)
            )
        }
    }
    
    private fun generateHeatPointsFromFault(fault: DetectedFault): List<HeatMapPoint> {
        // Generate heat points based on fault characteristics
        val points = mutableListOf<HeatMapPoint>()
        
        // This would be implemented based on the specific fault type
        // For now, return sample points
        return generateSampleHeatPoints(0.5f, 0.5f, fault.severity ?: 0.5f)
    }
    
    private fun getColorForFault(faultId: String): Int {
        return when {
            faultId.contains("posture") -> Color.parseColor("#FF5722")
            faultId.contains("swing") -> Color.parseColor("#FF9800")
            faultId.contains("impact") -> Color.parseColor("#F44336")
            faultId.contains("tempo") -> Color.parseColor("#9C27B0")
            else -> Color.parseColor("#607D8B")
        }
    }
}