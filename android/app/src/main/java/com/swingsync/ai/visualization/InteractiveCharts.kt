package com.swingsync.ai.visualization

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet
import com.swingsync.ai.data.model.*
import com.swingsync.ai.ui.theme.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * InteractiveCharts - Beautiful interactive charts for golf swing analysis
 * 
 * Features:
 * - Radar charts for multi-dimensional skill analysis
 * - Line charts for progress tracking
 * - Bar charts for session comparisons
 * - Scatter plots for correlation analysis
 * - Touch interactions with smooth animations
 * - Zoom and pan capabilities
 * - Data point highlighting and details
 * - Export capabilities
 */
class InteractiveCharts @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Data structures
    data class ChartDataPoint(
        val x: Float,
        val y: Float,
        val value: Float,
        val label: String,
        val timestamp: Long = System.currentTimeMillis(),
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class ChartSeries(
        val name: String,
        val data: List<ChartDataPoint>,
        val color: Int,
        val isVisible: Boolean = true,
        val lineStyle: LineStyle = LineStyle.SOLID,
        val fillArea: Boolean = false
    )
    
    data class RadarMetric(
        val name: String,
        val value: Float,
        val maxValue: Float,
        val unit: String,
        val color: Int,
        val target: Float? = null
    )
    
    data class ComparisonData(
        val current: List<RadarMetric>,
        val previous: List<RadarMetric>,
        val ideal: List<RadarMetric>
    )
    
    // Touch handling
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private var panX = 0f
    private var panY = 0f
    private var selectedDataPoint: ChartDataPoint? = null
    private var highlightedSeries: String? = null
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Chart data
    private var chartType = ChartType.LINE
    private var chartSeries = mutableListOf<ChartSeries>()
    private var radarMetrics = mutableListOf<RadarMetric>()
    private var comparisonData: ComparisonData? = null
    private var showLegend = true
    private var showGrid = true
    private var showDataPoints = true
    private var showAnimations = true
    
    // Animation
    private var animationProgress = 0f
    private var isAnimating = false
    private val animationDuration = 2000L
    
    // Layout
    private var chartRect = RectF()
    private var legendRect = RectF()
    private var titleRect = RectF()
    private var detailsRect = RectF()
    
    // Chart settings
    private var xAxisMin = 0f
    private var xAxisMax = 100f
    private var yAxisMin = 0f
    private var yAxisMax = 100f
    private var gridStepX = 10f
    private var gridStepY = 10f
    
    enum class ChartType {
        LINE,
        RADAR,
        BAR,
        SCATTER,
        AREA,
        COMPARISON
    }
    
    enum class LineStyle {
        SOLID,
        DASHED,
        DOTTED
    }
    
    enum class InteractionMode {
        NONE,
        ZOOM,
        PAN,
        SELECT,
        HIGHLIGHT
    }
    
    init {
        setupPaints()
        setupGestureDetectors()
        setupSampleData()
    }
    
    private fun setupPaints() {
        backgroundPaint.color = Color.parseColor("#FAFAFA")
        backgroundPaint.style = Paint.Style.FILL
        
        gridPaint.color = Color.parseColor("#E0E0E0")
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        
        axisPaint.color = Color.parseColor("#424242")
        axisPaint.style = Paint.Style.STROKE
        axisPaint.strokeWidth = 2f
        
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 4f
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeJoin = Paint.Join.ROUND
        
        fillPaint.style = Paint.Style.FILL
        
        textPaint.color = Color.parseColor("#212121")
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.DEFAULT
        
        highlightPaint.color = Color.parseColor("#FF9800")
        highlightPaint.style = Paint.Style.STROKE
        highlightPaint.strokeWidth = 6f
        
        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    
    private fun setupGestureDetectors() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                handleTap(e.x, e.y)
                return true
            }
            
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                panX -= distanceX
                panY -= distanceY
                invalidate()
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                resetZoomAndPan()
                return true
            }
        })
        
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
                invalidate()
                return true
            }
        })
    }
    
    private fun setupSampleData() {
        // Sample line chart data
        val sampleData1 = listOf(
            ChartDataPoint(0f, 0f, 65f, "Session 1"),
            ChartDataPoint(1f, 0f, 68f, "Session 2"),
            ChartDataPoint(2f, 0f, 72f, "Session 3"),
            ChartDataPoint(3f, 0f, 75f, "Session 4"),
            ChartDataPoint(4f, 0f, 78f, "Session 5"),
            ChartDataPoint(5f, 0f, 82f, "Session 6"),
            ChartDataPoint(6f, 0f, 85f, "Session 7")
        )
        
        chartSeries.add(
            ChartSeries(
                "Overall Score",
                sampleData1,
                Color.parseColor("#4CAF50"),
                fillArea = true
            )
        )
        
        // Sample radar metrics
        radarMetrics.addAll(
            listOf(
                RadarMetric("Stance", 85f, 100f, "%", Color.parseColor("#4CAF50"), 90f),
                RadarMetric("Backswing", 78f, 100f, "%", Color.parseColor("#2196F3"), 85f),
                RadarMetric("Downswing", 82f, 100f, "%", Color.parseColor("#FF9800"), 88f),
                RadarMetric("Impact", 88f, 100f, "%", Color.parseColor("#9C27B0"), 92f),
                RadarMetric("Follow Through", 75f, 100f, "%", Color.parseColor("#FF5722"), 80f),
                RadarMetric("Tempo", 80f, 100f, "%", Color.parseColor("#795548"), 85f)
            )
        )
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val padding = 60f
        val legendHeight = if (showLegend) 60f else 0f
        val titleHeight = 50f
        val detailsWidth = 200f
        
        titleRect.set(padding, padding, w - padding, padding + titleHeight)
        
        when (chartType) {
            ChartType.RADAR -> {
                val size = min(w - padding * 2, h - padding * 2 - legendHeight - titleHeight)
                val centerX = w / 2f
                val centerY = (h - legendHeight + titleHeight) / 2f
                
                chartRect.set(
                    centerX - size / 2,
                    centerY - size / 2,
                    centerX + size / 2,
                    centerY + size / 2
                )
            }
            else -> {
                chartRect.set(
                    padding,
                    padding + titleHeight + 20f,
                    w - padding - detailsWidth,
                    h - padding - legendHeight
                )
                
                detailsRect.set(
                    w - detailsWidth,
                    padding + titleHeight + 20f,
                    w - padding,
                    h - padding - legendHeight
                )
            }
        }
        
        if (showLegend) {
            legendRect.set(
                padding,
                h - legendHeight,
                w - padding,
                h - padding
            )
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply transformations
        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        
        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw title
        drawTitle(canvas)
        
        // Draw chart based on type
        when (chartType) {
            ChartType.LINE -> drawLineChart(canvas)
            ChartType.RADAR -> drawRadarChart(canvas)
            ChartType.BAR -> drawBarChart(canvas)
            ChartType.SCATTER -> drawScatterPlot(canvas)
            ChartType.AREA -> drawAreaChart(canvas)
            ChartType.COMPARISON -> drawComparisonChart(canvas)
        }
        
        // Draw legend
        if (showLegend) {
            drawLegend(canvas)
        }
        
        // Draw details panel
        drawDetailsPanel(canvas)
        
        canvas.restore()
    }
    
    private fun drawTitle(canvas: Canvas) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.parseColor("#212121")
        titlePaint.textSize = 32f
        titlePaint.typeface = Typeface.DEFAULT_BOLD
        titlePaint.textAlign = Paint.Align.CENTER
        
        val title = when (chartType) {
            ChartType.LINE -> "Progress Over Time"
            ChartType.RADAR -> "Skill Assessment"
            ChartType.BAR -> "Session Comparison"
            ChartType.SCATTER -> "Correlation Analysis"
            ChartType.AREA -> "Performance Trends"
            ChartType.COMPARISON -> "Before vs After"
        }
        
        canvas.drawText(title, titleRect.centerX(), titleRect.centerY() + 10f, titlePaint)
    }
    
    private fun drawLineChart(canvas: Canvas) {
        // Draw grid
        if (showGrid) {
            drawGrid(canvas)
        }
        
        // Draw axes
        drawAxes(canvas)
        
        // Draw data series
        for (series in chartSeries) {
            if (series.isVisible) {
                drawLineSeries(canvas, series)
            }
        }
        
        // Draw data points
        if (showDataPoints) {
            for (series in chartSeries) {
                if (series.isVisible) {
                    drawDataPoints(canvas, series)
                }
            }
        }
        
        // Draw highlight
        selectedDataPoint?.let { point ->
            drawHighlight(canvas, point)
        }
    }
    
    private fun drawGrid(canvas: Canvas) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = Color.parseColor("#E8E8E8")
        gridPaint.strokeWidth = 1f
        
        // Vertical grid lines
        val xSteps = ((xAxisMax - xAxisMin) / gridStepX).toInt()
        for (i in 0..xSteps) {
            val x = chartRect.left + (i * gridStepX / (xAxisMax - xAxisMin)) * chartRect.width()
            canvas.drawLine(x, chartRect.top, x, chartRect.bottom, gridPaint)
        }
        
        // Horizontal grid lines
        val ySteps = ((yAxisMax - yAxisMin) / gridStepY).toInt()
        for (i in 0..ySteps) {
            val y = chartRect.bottom - (i * gridStepY / (yAxisMax - yAxisMin)) * chartRect.height()
            canvas.drawLine(chartRect.left, y, chartRect.right, y, gridPaint)
        }
    }
    
    private fun drawAxes(canvas: Canvas) {
        // X-axis
        canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)
        
        // Y-axis
        canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint)
        
        // X-axis labels
        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        xLabelPaint.color = Color.parseColor("#424242")
        xLabelPaint.textSize = 20f
        xLabelPaint.textAlign = Paint.Align.CENTER
        
        val xSteps = ((xAxisMax - xAxisMin) / gridStepX).toInt()
        for (i in 0..xSteps) {
            val x = chartRect.left + (i * gridStepX / (xAxisMax - xAxisMin)) * chartRect.width()
            val value = xAxisMin + i * gridStepX
            canvas.drawText(value.toInt().toString(), x, chartRect.bottom + 30f, xLabelPaint)
        }
        
        // Y-axis labels
        val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        yLabelPaint.color = Color.parseColor("#424242")
        yLabelPaint.textSize = 20f
        yLabelPaint.textAlign = Paint.Align.RIGHT
        
        val ySteps = ((yAxisMax - yAxisMin) / gridStepY).toInt()
        for (i in 0..ySteps) {
            val y = chartRect.bottom - (i * gridStepY / (yAxisMax - yAxisMin)) * chartRect.height()
            val value = yAxisMin + i * gridStepY
            canvas.drawText(value.toInt().toString(), chartRect.left - 15f, y + 6f, yLabelPaint)
        }
    }
    
    private fun drawLineSeries(canvas: Canvas, series: ChartSeries) {
        if (series.data.size < 2) return
        
        linePaint.color = series.color
        linePaint.strokeWidth = if (series.name == highlightedSeries) 6f else 4f
        
        // Set line style
        when (series.lineStyle) {
            LineStyle.SOLID -> linePaint.pathEffect = null
            LineStyle.DASHED -> linePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
            LineStyle.DOTTED -> linePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        
        val path = Path()
        var isFirst = true
        
        for (point in series.data) {
            val x = chartRect.left + (point.x / (xAxisMax - xAxisMin)) * chartRect.width()
            val y = chartRect.bottom - (point.value / (yAxisMax - yAxisMin)) * chartRect.height()
            
            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Apply animation
        if (showAnimations && animationProgress < 1f) {
            val animatedPath = Path()
            val measure = PathMeasure(path, false)
            val length = measure.length
            val animatedLength = length * animationProgress
            
            measure.getSegment(0f, animatedLength, animatedPath, true)
            canvas.drawPath(animatedPath, linePaint)
        } else {
            canvas.drawPath(path, linePaint)
        }
        
        // Draw fill area
        if (series.fillArea) {
            drawFillArea(canvas, series)
        }
    }
    
    private fun drawFillArea(canvas: Canvas, series: ChartSeries) {
        if (series.data.size < 2) return
        
        fillPaint.color = Color.argb(80, 
            Color.red(series.color), 
            Color.green(series.color), 
            Color.blue(series.color)
        )
        
        val fillPath = Path()
        fillPath.moveTo(chartRect.left, chartRect.bottom)
        
        for (point in series.data) {
            val x = chartRect.left + (point.x / (xAxisMax - xAxisMin)) * chartRect.width()
            val y = chartRect.bottom - (point.value / (yAxisMax - yAxisMin)) * chartRect.height()
            fillPath.lineTo(x, y)
        }
        
        fillPath.lineTo(chartRect.right, chartRect.bottom)
        fillPath.close()
        
        canvas.drawPath(fillPath, fillPaint)
    }
    
    private fun drawDataPoints(canvas: Canvas, series: ChartSeries) {
        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pointPaint.color = series.color
        pointPaint.style = Paint.Style.FILL
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3f
        
        for (point in series.data) {
            val x = chartRect.left + (point.x / (xAxisMax - xAxisMin)) * chartRect.width()
            val y = chartRect.bottom - (point.value / (yAxisMax - yAxisMin)) * chartRect.height()
            
            val radius = if (point == selectedDataPoint) 12f else 8f
            val animatedRadius = radius * animationProgress
            
            if (animatedRadius > 0) {
                // Draw shadow
                canvas.drawCircle(x + 2f, y + 2f, animatedRadius, shadowPaint)
                
                // Draw point
                canvas.drawCircle(x, y, animatedRadius, pointPaint)
                canvas.drawCircle(x, y, animatedRadius, strokePaint)
            }
        }
    }
    
    private fun drawHighlight(canvas: Canvas, point: ChartDataPoint) {
        val x = chartRect.left + (point.x / (xAxisMax - xAxisMin)) * chartRect.width()
        val y = chartRect.bottom - (point.value / (yAxisMax - yAxisMin)) * chartRect.height()
        
        // Draw highlight circle
        canvas.drawCircle(x, y, 15f, highlightPaint)
        
        // Draw value label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.parseColor("#212121")
        labelPaint.textSize = 24f
        labelPaint.typeface = Typeface.DEFAULT_BOLD
        labelPaint.textAlign = Paint.Align.CENTER
        
        val labelBg = Paint(Paint.ANTI_ALIAS_FLAG)
        labelBg.color = Color.parseColor("#FFFFFF")
        labelBg.style = Paint.Style.FILL
        labelBg.setShadowLayer(6f, 0f, 3f, Color.argb(50, 0, 0, 0))
        
        val labelText = "${point.value.toInt()}"
        val labelWidth = labelPaint.measureText(labelText) + 20f
        val labelHeight = 40f
        
        val labelRect = RectF(
            x - labelWidth / 2,
            y - labelHeight - 20f,
            x + labelWidth / 2,
            y - 20f
        )
        
        canvas.drawRoundRect(labelRect, 8f, 8f, labelBg)
        canvas.drawText(labelText, x, y - 30f, labelPaint)
    }
    
    private fun drawRadarChart(canvas: Canvas) {
        if (radarMetrics.isEmpty()) return
        
        val centerX = chartRect.centerX()
        val centerY = chartRect.centerY()
        val radius = min(chartRect.width(), chartRect.height()) / 2f - 40f
        
        // Draw radar grid
        drawRadarGrid(canvas, centerX, centerY, radius)
        
        // Draw radar data
        drawRadarData(canvas, centerX, centerY, radius)
        
        // Draw radar labels
        drawRadarLabels(canvas, centerX, centerY, radius)
    }
    
    private fun drawRadarGrid(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = Color.parseColor("#E0E0E0")
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        
        // Draw concentric circles
        for (i in 1..5) {
            val r = radius * i / 5f
            canvas.drawCircle(centerX, centerY, r, gridPaint)
        }
        
        // Draw radial lines
        val angleStep = 360f / radarMetrics.size
        for (i in radarMetrics.indices) {
            val angle = i * angleStep * PI / 180f
            val endX = centerX + radius * cos(angle).toFloat()
            val endY = centerY + radius * sin(angle).toFloat()
            
            canvas.drawLine(centerX, centerY, endX, endY, gridPaint)
        }
    }
    
    private fun drawRadarData(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val dataPath = Path()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.color = Color.argb(80, 76, 175, 80)
        fillPaint.style = Paint.Style.FILL
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.color = Color.parseColor("#4CAF50")
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 4f
        
        val angleStep = 360f / radarMetrics.size
        var isFirst = true
        
        for (i in radarMetrics.indices) {
            val metric = radarMetrics[i]
            val angle = i * angleStep * PI / 180f - PI / 2f // Start from top
            val normalizedValue = metric.value / metric.maxValue
            val r = radius * normalizedValue * animationProgress
            
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            
            if (isFirst) {
                dataPath.moveTo(x, y)
                isFirst = false
            } else {
                dataPath.lineTo(x, y)
            }
        }
        
        dataPath.close()
        
        // Draw fill
        canvas.drawPath(dataPath, fillPaint)
        
        // Draw stroke
        canvas.drawPath(dataPath, strokePaint)
        
        // Draw data points
        for (i in radarMetrics.indices) {
            val metric = radarMetrics[i]
            val angle = i * angleStep * PI / 180f - PI / 2f
            val normalizedValue = metric.value / metric.maxValue
            val r = radius * normalizedValue * animationProgress
            
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            
            // Draw point
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            pointPaint.color = metric.color
            pointPaint.style = Paint.Style.FILL
            
            canvas.drawCircle(x, y, 8f, pointPaint)
            
            // Draw target if available
            metric.target?.let { target ->
                val targetNormalized = target / metric.maxValue
                val targetR = radius * targetNormalized
                val targetX = centerX + targetR * cos(angle).toFloat()
                val targetY = centerY + targetR * sin(angle).toFloat()
                
                val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                targetPaint.color = Color.parseColor("#FF9800")
                targetPaint.style = Paint.Style.STROKE
                targetPaint.strokeWidth = 3f
                
                canvas.drawCircle(targetX, targetY, 6f, targetPaint)
            }
        }
    }
    
    private fun drawRadarLabels(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.parseColor("#424242")
        labelPaint.textSize = 20f
        labelPaint.typeface = Typeface.DEFAULT_BOLD
        
        val angleStep = 360f / radarMetrics.size
        
        for (i in radarMetrics.indices) {
            val metric = radarMetrics[i]
            val angle = i * angleStep * PI / 180f - PI / 2f
            val labelRadius = radius + 30f
            
            val x = centerX + labelRadius * cos(angle).toFloat()
            val y = centerY + labelRadius * sin(angle).toFloat()
            
            // Adjust text alignment based on position
            labelPaint.textAlign = when {
                x < centerX - 10f -> Paint.Align.RIGHT
                x > centerX + 10f -> Paint.Align.LEFT
                else -> Paint.Align.CENTER
            }
            
            canvas.drawText(metric.name, x, y + 6f, labelPaint)
            
            // Draw value
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            valuePaint.color = metric.color
            valuePaint.textSize = 16f
            valuePaint.textAlign = labelPaint.textAlign
            
            val valueText = "${metric.value.toInt()}${metric.unit}"
            canvas.drawText(valueText, x, y + 25f, valuePaint)
        }
    }
    
    private fun drawBarChart(canvas: Canvas) {
        // Implementation for bar chart
        // Similar structure to line chart but with bars
    }
    
    private fun drawScatterPlot(canvas: Canvas) {
        // Implementation for scatter plot
        // Draw points without connecting lines
    }
    
    private fun drawAreaChart(canvas: Canvas) {
        // Implementation for area chart
        // Similar to line chart but with filled areas
    }
    
    private fun drawComparisonChart(canvas: Canvas) {
        comparisonData?.let { data ->
            // Draw comparison between current, previous, and ideal
            drawComparisonRadar(canvas, data)
        }
    }
    
    private fun drawComparisonRadar(canvas: Canvas, data: ComparisonData) {
        val centerX = chartRect.centerX()
        val centerY = chartRect.centerY()
        val radius = min(chartRect.width(), chartRect.height()) / 2f - 40f
        
        // Draw grid
        drawRadarGrid(canvas, centerX, centerY, radius)
        
        // Draw ideal (target) data
        drawRadarSeries(canvas, centerX, centerY, radius, data.ideal, Color.parseColor("#4CAF50"), 0.3f)
        
        // Draw previous data
        drawRadarSeries(canvas, centerX, centerY, radius, data.previous, Color.parseColor("#FF9800"), 0.5f)
        
        // Draw current data
        drawRadarSeries(canvas, centerX, centerY, radius, data.current, Color.parseColor("#2196F3"), 0.8f)
    }
    
    private fun drawRadarSeries(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, 
                               metrics: List<RadarMetric>, color: Int, alpha: Float) {
        val path = Path()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.color = Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))
        fillPaint.style = Paint.Style.FILL
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.color = color
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3f
        
        val angleStep = 360f / metrics.size
        var isFirst = true
        
        for (i in metrics.indices) {
            val metric = metrics[i]
            val angle = i * angleStep * PI / 180f - PI / 2f
            val normalizedValue = metric.value / metric.maxValue
            val r = radius * normalizedValue * animationProgress
            
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            
            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }
        
        path.close()
        
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }
    
    private fun drawLegend(canvas: Canvas) {
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendPaint.textSize = 18f
        legendPaint.color = Color.parseColor("#424242")
        
        var x = legendRect.left + 20f
        val y = legendRect.centerY()
        
        for (series in chartSeries) {
            if (series.isVisible) {
                // Draw color indicator
                val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                colorPaint.color = series.color
                canvas.drawCircle(x, y, 8f, colorPaint)
                
                // Draw label
                canvas.drawText(series.name, x + 20f, y + 6f, legendPaint)
                
                x += legendPaint.measureText(series.name) + 60f
            }
        }
    }
    
    private fun drawDetailsPanel(canvas: Canvas) {
        selectedDataPoint?.let { point ->
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            bgPaint.color = Color.parseColor("#FFFFFF")
            bgPaint.style = Paint.Style.FILL
            bgPaint.setShadowLayer(8f, 0f, 4f, Color.argb(50, 0, 0, 0))
            
            canvas.drawRoundRect(detailsRect, 12f, 12f, bgPaint)
            
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            titlePaint.color = Color.parseColor("#212121")
            titlePaint.textSize = 20f
            titlePaint.typeface = Typeface.DEFAULT_BOLD
            
            val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            contentPaint.color = Color.parseColor("#424242")
            contentPaint.textSize = 16f
            
            var textY = detailsRect.top + 30f
            
            canvas.drawText("Details", detailsRect.left + 20f, textY, titlePaint)
            textY += 40f
            
            canvas.drawText("Value: ${point.value.toInt()}", detailsRect.left + 20f, textY, contentPaint)
            textY += 25f
            
            canvas.drawText("Label: ${point.label}", detailsRect.left + 20f, textY, contentPaint)
            textY += 25f
            
            // Draw metadata
            for ((key, value) in point.metadata) {
                canvas.drawText("$key: $value", detailsRect.left + 20f, textY, contentPaint)
                textY += 25f
            }
        }
    }
    
    private fun handleTap(x: Float, y: Float) {
        // Check if tap is on a data point
        for (series in chartSeries) {
            for (point in series.data) {
                val pointX = chartRect.left + (point.x / (xAxisMax - xAxisMin)) * chartRect.width()
                val pointY = chartRect.bottom - (point.value / (yAxisMax - yAxisMin)) * chartRect.height()
                
                val distance = sqrt((x - pointX).pow(2) + (y - pointY).pow(2))
                if (distance < 30f) {
                    selectedDataPoint = point
                    invalidate()
                    return
                }
            }
        }
        
        // Clear selection if no point was tapped
        selectedDataPoint = null
        invalidate()
    }
    
    private fun resetZoomAndPan() {
        scaleFactor = 1.0f
        panX = 0f
        panY = 0f
        invalidate()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
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
    
    // Public API methods
    fun setChartType(type: ChartType) {
        chartType = type
        requestLayout()
        invalidate()
    }
    
    fun setChartSeries(series: List<ChartSeries>) {
        chartSeries.clear()
        chartSeries.addAll(series)
        calculateAxisRanges()
        invalidate()
    }
    
    fun setRadarMetrics(metrics: List<RadarMetric>) {
        radarMetrics.clear()
        radarMetrics.addAll(metrics)
        invalidate()
    }
    
    fun setComparisonData(data: ComparisonData) {
        comparisonData = data
        invalidate()
    }
    
    fun setHighlightedSeries(seriesName: String?) {
        highlightedSeries = seriesName
        invalidate()
    }
    
    fun setShowLegend(show: Boolean) {
        showLegend = show
        requestLayout()
        invalidate()
    }
    
    fun setShowGrid(show: Boolean) {
        showGrid = show
        invalidate()
    }
    
    fun setShowDataPoints(show: Boolean) {
        showDataPoints = show
        invalidate()
    }
    
    fun setShowAnimations(show: Boolean) {
        showAnimations = show
    }
    
    fun exportChart(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
    
    private fun calculateAxisRanges() {
        if (chartSeries.isEmpty()) return
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        for (series in chartSeries) {
            for (point in series.data) {
                minX = min(minX, point.x)
                maxX = max(maxX, point.x)
                minY = min(minY, point.value)
                maxY = max(maxY, point.value)
            }
        }
        
        // Add padding
        val xPadding = (maxX - minX) * 0.1f
        val yPadding = (maxY - minY) * 0.1f
        
        xAxisMin = minX - xPadding
        xAxisMax = maxX + xPadding
        yAxisMin = minY - yPadding
        yAxisMax = maxY + yPadding
        
        // Calculate grid steps
        gridStepX = (xAxisMax - xAxisMin) / 10f
        gridStepY = (yAxisMax - yAxisMin) / 10f
    }
}