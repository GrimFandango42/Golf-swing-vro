package com.swingsync.ai.visualization

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.swingsync.ai.data.model.BiomechanicalKPI
import com.swingsync.ai.data.model.DetectedFault
import com.swingsync.ai.data.model.GolfSwingPhase
import com.swingsync.ai.ui.theme.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * ProgressVisualization - Beautiful progress tracking and improvement visualization
 * 
 * Features:
 * - Progress timeline with smooth animations
 * - Skill radar charts showing multi-dimensional progress
 * - Milestone achievements with celebration animations
 * - Trend analysis with predictive insights
 * - Comparison with ideal performance
 * - Shareable progress reports
 */
class ProgressVisualization @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Data structures
    data class SwingSession(
        val sessionId: String,
        val timestamp: Long,
        val score: Float,
        val kpis: List<BiomechanicalKPI>,
        val faults: List<DetectedFault>,
        val clubUsed: String,
        val improvements: List<String> = emptyList()
    )
    
    data class ProgressMetric(
        val name: String,
        val currentValue: Float,
        val targetValue: Float,
        val historicalValues: List<Float>,
        val unit: String,
        val color: Int
    )
    
    data class Milestone(
        val id: String,
        val title: String,
        val description: String,
        val targetValue: Float,
        val currentValue: Float,
        val isAchieved: Boolean,
        val achievedDate: Long? = null,
        val icon: Int? = null
    )
    
    // Paint objects for drawing
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Data
    private var sessions = mutableListOf<SwingSession>()
    private var progressMetrics = mutableListOf<ProgressMetric>()
    private var milestones = mutableListOf<Milestone>()
    private var selectedTimeRange = TimeRange.LAST_30_DAYS
    private var selectedMetric = "Overall Score"
    
    // Animation
    private var animationProgress = 0f
    private var isAnimating = false
    private val animationDuration = 2000L
    
    // Layout
    private var chartRect = RectF()
    private var legendRect = RectF()
    private var timelineRect = RectF()
    private var metricsRect = RectF()
    
    // Colors
    private val primaryColor = Color.parseColor("#4CAF50")
    private val secondaryColor = Color.parseColor("#2196F3")
    private val accentColor = Color.parseColor("#FFC107")
    private val improvementColor = Color.parseColor("#8BC34A")
    private val warningColor = Color.parseColor("#FF9800")
    private val errorColor = Color.parseColor("#F44336")
    
    enum class TimeRange(val days: Int, val displayName: String) {
        LAST_7_DAYS(7, "Last 7 Days"),
        LAST_30_DAYS(30, "Last 30 Days"),
        LAST_90_DAYS(90, "Last 3 Months"),
        LAST_YEAR(365, "Last Year"),
        ALL_TIME(Int.MAX_VALUE, "All Time")
    }
    
    enum class ChartType {
        LINE_CHART,
        RADAR_CHART,
        PROGRESS_BARS,
        TIMELINE,
        HEATMAP
    }
    
    init {
        setupPaints()
        setupSampleData()
    }
    
    private fun setupPaints() {
        backgroundPaint.color = Color.parseColor("#FAFAFA")
        backgroundPaint.style = Paint.Style.FILL
        
        gridPaint.color = Color.parseColor("#E0E0E0")
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        
        progressPaint.color = primaryColor
        progressPaint.style = Paint.Style.FILL
        progressPaint.strokeWidth = 4f
        
        textPaint.color = Color.parseColor("#212121")
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        
        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    
    private fun setupSampleData() {
        // Sample progress metrics
        progressMetrics.add(
            ProgressMetric(
                "Overall Score",
                85f,
                90f,
                listOf(65f, 68f, 72f, 75f, 78f, 82f, 85f),
                "points",
                primaryColor
            )
        )
        
        progressMetrics.add(
            ProgressMetric(
                "Swing Consistency",
                78f,
                85f,
                listOf(60f, 63f, 67f, 70f, 74f, 76f, 78f),
                "%",
                secondaryColor
            )
        )
        
        progressMetrics.add(
            ProgressMetric(
                "Impact Position",
                82f,
                88f,
                listOf(70f, 73f, 76f, 78f, 80f, 81f, 82f),
                "degrees",
                accentColor
            )
        )
        
        // Sample milestones
        milestones.add(
            Milestone(
                "consistency_80",
                "Consistency Champion",
                "Achieve 80% swing consistency",
                80f,
                78f,
                false
            )
        )
        
        milestones.add(
            Milestone(
                "score_90",
                "Excellence Award",
                "Reach overall score of 90",
                90f,
                85f,
                false
            )
        )
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val padding = 40f
        val chartHeight = h * 0.6f
        val legendHeight = h * 0.15f
        val timelineHeight = h * 0.25f
        
        chartRect.set(padding, padding, w - padding, chartHeight)
        legendRect.set(padding, chartHeight + 20f, w - padding, chartHeight + legendHeight)
        timelineRect.set(padding, h - timelineHeight, w - padding, h - padding)
        metricsRect.set(padding, legendRect.bottom + 20f, w - padding, timelineRect.top - 20f)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw main chart
        drawProgressChart(canvas)
        
        // Draw legend
        drawLegend(canvas)
        
        // Draw metrics overview
        drawMetricsOverview(canvas)
        
        // Draw timeline
        drawTimeline(canvas)
        
        // Draw milestones
        drawMilestones(canvas)
    }
    
    private fun drawProgressChart(canvas: Canvas) {
        if (progressMetrics.isEmpty()) return
        
        val metric = progressMetrics.find { it.name == selectedMetric } ?: progressMetrics[0]
        val values = metric.historicalValues
        
        if (values.isEmpty()) return
        
        // Draw chart background
        canvas.drawRoundRect(chartRect, 16f, 16f, backgroundPaint)
        
        // Draw grid
        drawGrid(canvas, chartRect)
        
        // Draw progress line with animation
        drawProgressLine(canvas, chartRect, values, metric.color)
        
        // Draw data points
        drawDataPoints(canvas, chartRect, values, metric.color)
        
        // Draw trend line
        drawTrendLine(canvas, chartRect, values)
        
        // Draw target line
        drawTargetLine(canvas, chartRect, metric.targetValue)
    }
    
    private fun drawGrid(canvas: Canvas, rect: RectF) {
        // Horizontal grid lines
        for (i in 0..10) {
            val y = rect.top + (rect.height() / 10f) * i
            canvas.drawLine(rect.left, y, rect.right, y, gridPaint)
        }
        
        // Vertical grid lines
        for (i in 0..6) {
            val x = rect.left + (rect.width() / 6f) * i
            canvas.drawLine(x, rect.top, x, rect.bottom, gridPaint)
        }
    }
    
    private fun drawProgressLine(canvas: Canvas, rect: RectF, values: List<Float>, color: Int) {
        if (values.size < 2) return
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.strokeCap = Paint.Cap.ROUND
        
        val path = Path()
        val stepX = rect.width() / (values.size - 1)
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        for (i in values.indices) {
            val x = rect.left + i * stepX
            val y = rect.bottom - ((values[i] - minValue) / range) * rect.height()
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                // Create smooth curves
                val prevX = rect.left + (i - 1) * stepX
                val prevY = rect.bottom - ((values[i - 1] - minValue) / range) * rect.height()
                val controlX1 = prevX + stepX / 3
                val controlY1 = prevY
                val controlX2 = x - stepX / 3
                val controlY2 = y
                
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }
        
        // Apply animation
        val animatedPath = Path()
        val measure = PathMeasure(path, false)
        val length = measure.length
        val animatedLength = length * animationProgress
        
        measure.getSegment(0f, animatedLength, animatedPath, true)
        canvas.drawPath(animatedPath, paint)
        
        // Draw gradient fill
        if (animationProgress > 0.5f) {
            drawGradientFill(canvas, rect, values, color)
        }
    }
    
    private fun drawGradientFill(canvas: Canvas, rect: RectF, values: List<Float>, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Create gradient
        val gradient = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            intArrayOf(
                Color.argb(80, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(20, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        
        paint.shader = gradient
        
        // Create fill path
        val fillPath = Path()
        val stepX = rect.width() / (values.size - 1)
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        fillPath.moveTo(rect.left, rect.bottom)
        
        for (i in values.indices) {
            val x = rect.left + i * stepX
            val y = rect.bottom - ((values[i] - minValue) / range) * rect.height()
            if (i == 0) {
                fillPath.lineTo(x, y)
            } else {
                val prevX = rect.left + (i - 1) * stepX
                val prevY = rect.bottom - ((values[i - 1] - minValue) / range) * rect.height()
                val controlX1 = prevX + stepX / 3
                val controlY1 = prevY
                val controlX2 = x - stepX / 3
                val controlY2 = y
                
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }
        
        fillPath.lineTo(rect.right, rect.bottom)
        fillPath.close()
        
        canvas.drawPath(fillPath, paint)
    }
    
    private fun drawDataPoints(canvas: Canvas, rect: RectF, values: List<Float>, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.FILL
        
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint.color = Color.argb(60, 0, 0, 0)
        
        val stepX = rect.width() / (values.size - 1)
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        for (i in values.indices) {
            val x = rect.left + i * stepX
            val y = rect.bottom - ((values[i] - minValue) / range) * rect.height()
            
            val progress = if (i < values.size * animationProgress) 1f else 0f
            val radius = 8f * progress
            
            if (progress > 0) {
                // Draw shadow
                canvas.drawCircle(x + 2f, y + 2f, radius, shadowPaint)
                
                // Draw point
                canvas.drawCircle(x, y, radius, paint)
                
                // Draw inner highlight
                val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                highlightPaint.color = Color.WHITE
                canvas.drawCircle(x, y, radius * 0.4f, highlightPaint)
            }
        }
    }
    
    private fun drawTrendLine(canvas: Canvas, rect: RectF, values: List<Float>) {
        if (values.size < 3) return
        
        // Calculate linear regression
        val n = values.size
        val sumX = (0 until n).sum()
        val sumY = values.sum()
        val sumXY = (0 until n).map { i -> i * values[i] }.sum()
        val sumX2 = (0 until n).map { i -> i * i }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        
        // Draw trend line
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(150, 255, 193, 7) // Semi-transparent amber
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        val startY = rect.bottom - ((intercept - minValue) / range) * rect.height()
        val endY = rect.bottom - ((slope * (n - 1) + intercept - minValue) / range) * rect.height()
        
        canvas.drawLine(rect.left, startY, rect.right, endY, paint)
    }
    
    private fun drawTargetLine(canvas: Canvas, rect: RectF, targetValue: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(200, 76, 175, 80) // Semi-transparent green
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        
        val metric = progressMetrics.find { it.name == selectedMetric } ?: return
        val values = metric.historicalValues
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        val y = rect.bottom - ((targetValue - minValue) / range) * rect.height()
        canvas.drawLine(rect.left, y, rect.right, y, paint)
        
        // Draw target label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.parseColor("#4CAF50")
        labelPaint.textSize = 28f
        labelPaint.typeface = Typeface.DEFAULT_BOLD
        
        val text = "Target: ${targetValue.toInt()}"
        canvas.drawText(text, rect.right - 120f, y - 10f, labelPaint)
    }
    
    private fun drawLegend(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 28f
        paint.typeface = Typeface.DEFAULT_BOLD
        
        var x = legendRect.left
        val y = legendRect.centerY()
        
        for (metric in progressMetrics) {
            // Draw color indicator
            val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            colorPaint.color = metric.color
            canvas.drawCircle(x + 10f, y, 8f, colorPaint)
            
            // Draw label
            paint.color = if (metric.name == selectedMetric) Color.parseColor("#212121") else Color.parseColor("#757575")
            canvas.drawText(metric.name, x + 30f, y + 8f, paint)
            
            x += paint.measureText(metric.name) + 80f
        }
    }
    
    private fun drawMetricsOverview(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 24f
        
        val cardWidth = (metricsRect.width() - 40f) / 3f
        val cardHeight = metricsRect.height() - 20f
        
        for (i in progressMetrics.indices) {
            val metric = progressMetrics[i]
            val cardX = metricsRect.left + i * (cardWidth + 20f)
            val cardRect = RectF(cardX, metricsRect.top + 10f, cardX + cardWidth, metricsRect.bottom - 10f)
            
            // Draw card background
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            cardPaint.color = Color.WHITE
            cardPaint.setShadowLayer(6f, 0f, 3f, Color.argb(30, 0, 0, 0))
            canvas.drawRoundRect(cardRect, 12f, 12f, cardPaint)
            
            // Draw metric content
            drawMetricCard(canvas, cardRect, metric)
        }
    }
    
    private fun drawMetricCard(canvas: Canvas, rect: RectF, metric: ProgressMetric) {
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        
        // Draw title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.parseColor("#424242")
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(metric.name, centerX, rect.top + 30f, titlePaint)
        
        // Draw current value
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        valuePaint.color = metric.color
        valuePaint.textSize = 32f
        valuePaint.textAlign = Paint.Align.CENTER
        valuePaint.typeface = Typeface.DEFAULT_BOLD
        
        val valueText = "${metric.currentValue.toInt()}${metric.unit}"
        canvas.drawText(valueText, centerX, centerY + 5f, valuePaint)
        
        // Draw progress bar
        val progressRect = RectF(
            rect.left + 20f,
            centerY + 25f,
            rect.right - 20f,
            centerY + 35f
        )
        
        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = Color.parseColor("#E0E0E0")
        canvas.drawRoundRect(progressRect, 5f, 5f, bgPaint)
        
        // Progress
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint.color = metric.color
        
        val progress = (metric.currentValue / metric.targetValue).coerceIn(0f, 1f)
        val progressWidth = progressRect.width() * progress * animationProgress
        
        val fillRect = RectF(
            progressRect.left,
            progressRect.top,
            progressRect.left + progressWidth,
            progressRect.bottom
        )
        
        canvas.drawRoundRect(fillRect, 5f, 5f, progressPaint)
        
        // Draw improvement indicator
        if (metric.historicalValues.size >= 2) {
            val recent = metric.historicalValues.takeLast(2)
            val improvement = recent[1] - recent[0]
            
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            arrowPaint.color = if (improvement > 0) improvementColor else warningColor
            arrowPaint.textSize = 16f
            arrowPaint.textAlign = Paint.Align.CENTER
            
            val arrowText = if (improvement > 0) "↑ +${improvement.toInt()}" else "↓ ${improvement.toInt()}"
            canvas.drawText(arrowText, centerX, rect.bottom - 15f, arrowPaint)
        }
    }
    
    private fun drawTimeline(canvas: Canvas) {
        if (sessions.isEmpty()) return
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 3f
        
        // Draw timeline base
        val timelineY = timelineRect.centerY()
        canvas.drawLine(timelineRect.left, timelineY, timelineRect.right, timelineY, paint)
        
        // Draw sessions
        val timelineWidth = timelineRect.width()
        val timeRange = getTimeRangeMillis()
        val currentTime = System.currentTimeMillis()
        
        for (session in sessions.takeLast(10)) {
            val timeDiff = currentTime - session.timestamp
            if (timeDiff <= timeRange) {
                val x = timelineRect.right - (timeDiff.toFloat() / timeRange) * timelineWidth
                drawSessionMarker(canvas, x, timelineY, session)
            }
        }
    }
    
    private fun drawSessionMarker(canvas: Canvas, x: Float, y: Float, session: SwingSession) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = getSessionColor(session.score)
        
        // Draw marker
        canvas.drawCircle(x, y, 12f, paint)
        
        // Draw score
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        scorePaint.color = Color.WHITE
        scorePaint.textSize = 18f
        scorePaint.textAlign = Paint.Align.CENTER
        scorePaint.typeface = Typeface.DEFAULT_BOLD
        
        canvas.drawText(session.score.toInt().toString(), x, y + 6f, scorePaint)
        
        // Draw date below
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        datePaint.color = Color.parseColor("#757575")
        datePaint.textSize = 12f
        datePaint.textAlign = Paint.Align.CENTER
        
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val dateText = dateFormat.format(Date(session.timestamp))
        canvas.drawText(dateText, x, y + 35f, datePaint)
    }
    
    private fun getSessionColor(score: Float): Int {
        return when {
            score >= 85f -> Color.parseColor("#4CAF50") // Green
            score >= 70f -> Color.parseColor("#FFC107") // Amber
            else -> Color.parseColor("#FF5722") // Red
        }
    }
    
    private fun drawMilestones(canvas: Canvas) {
        // Draw milestone indicators on the side
        val milestoneX = width - 60f
        var milestoneY = 100f
        
        for (milestone in milestones) {
            drawMilestone(canvas, milestoneX, milestoneY, milestone)
            milestoneY += 80f
        }
    }
    
    private fun drawMilestone(canvas: Canvas, x: Float, y: Float, milestone: Milestone) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = if (milestone.isAchieved) Color.parseColor("#4CAF50") else Color.parseColor("#E0E0E0")
        
        // Draw milestone circle
        canvas.drawCircle(x, y, 20f, paint)
        
        // Draw progress ring
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint.color = Color.parseColor("#2196F3")
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = 4f
        
        val progress = (milestone.currentValue / milestone.targetValue).coerceIn(0f, 1f)
        val sweepAngle = 360f * progress * animationProgress
        
        val rect = RectF(x - 25f, y - 25f, x + 25f, y + 25f)
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
        
        // Draw checkmark if achieved
        if (milestone.isAchieved) {
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            checkPaint.color = Color.WHITE
            checkPaint.strokeWidth = 3f
            checkPaint.style = Paint.Style.STROKE
            checkPaint.strokeCap = Paint.Cap.ROUND
            
            // Draw checkmark
            canvas.drawLine(x - 8f, y, x - 3f, y + 5f, checkPaint)
            canvas.drawLine(x - 3f, y + 5f, x + 8f, y - 5f, checkPaint)
        }
    }
    
    private fun getTimeRangeMillis(): Long {
        return when (selectedTimeRange) {
            TimeRange.LAST_7_DAYS -> 7 * 24 * 60 * 60 * 1000L
            TimeRange.LAST_30_DAYS -> 30 * 24 * 60 * 60 * 1000L
            TimeRange.LAST_90_DAYS -> 90 * 24 * 60 * 60 * 1000L
            TimeRange.LAST_YEAR -> 365 * 24 * 60 * 60 * 1000L
            TimeRange.ALL_TIME -> Long.MAX_VALUE
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
        
        animation.duration = animationDuration
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
    
    fun addSession(session: SwingSession) {
        sessions.add(session)
        updateProgressMetrics()
        invalidate()
    }
    
    fun setSelectedMetric(metricName: String) {
        selectedMetric = metricName
        invalidate()
    }
    
    fun setTimeRange(timeRange: TimeRange) {
        selectedTimeRange = timeRange
        invalidate()
    }
    
    private fun updateProgressMetrics() {
        // Update metrics based on new session data
        // This would calculate new values based on recent sessions
    }
    
    fun exportChart(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
    
    // Public API methods
    fun setProgressMetrics(metrics: List<ProgressMetric>) {
        progressMetrics.clear()
        progressMetrics.addAll(metrics)
        invalidate()
    }
    
    fun updateMilestones(milestones: List<Milestone>) {
        this.milestones.clear()
        this.milestones.addAll(milestones)
        invalidate()
    }
    
    fun celebrateAchievement(milestone: Milestone) {
        // Trigger celebration animation
        // This would show a special animation when a milestone is achieved
    }
}