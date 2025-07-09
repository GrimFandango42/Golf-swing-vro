package com.swingsync.ai.social

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.swingsync.ai.R
import com.swingsync.ai.achievements.Achievement
import com.swingsync.ai.celebration.ShareableContent
import com.swingsync.ai.detection.BestSwingEvent
import com.swingsync.ai.detection.ImprovementEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * ShareGraphicsGenerator - Creates beautiful graphics for social media sharing
 * 
 * This system automatically generates stunning, branded graphics for users to share
 * their golf achievements, improvements, and best swings on social media platforms.
 * 
 * Features:
 * - Branded achievement cards
 * - Progress visualization graphics
 * - Streak celebration banners
 * - Personal best announcements
 * - Improvement progress charts
 * - Custom swing analysis summaries
 * - Social media optimized formats
 * - Personalized color schemes
 * - Dynamic text overlays
 * - Professional template designs
 * 
 * Graphics are optimized for various social platforms (Instagram, Twitter, Facebook, etc.)
 * and include proper branding, typography, and visual hierarchy.
 */
@Singleton
class ShareGraphicsGenerator @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ShareGraphicsGenerator"
        
        // Standard social media dimensions
        const val INSTAGRAM_SQUARE_WIDTH = 1080
        const val INSTAGRAM_SQUARE_HEIGHT = 1080
        const val INSTAGRAM_STORY_WIDTH = 1080
        const val INSTAGRAM_STORY_HEIGHT = 1920
        const val TWITTER_CARD_WIDTH = 1200
        const val TWITTER_CARD_HEIGHT = 675
        const val FACEBOOK_POST_WIDTH = 1200
        const val FACEBOOK_POST_HEIGHT = 630
        
        // Design constants
        private const val CORNER_RADIUS = 24f
        private const val PADDING = 60f
        private const val TITLE_SIZE = 72f
        private const val SUBTITLE_SIZE = 48f
        private const val BODY_SIZE = 36f
        private const val CAPTION_SIZE = 28f
        
        // Color palette
        private val PRIMARY_COLOR = Color.parseColor("#FF6B35")
        private val SECONDARY_COLOR = Color.parseColor("#F7931E")
        private val ACCENT_COLOR = Color.parseColor("#FFD23F")
        private val BACKGROUND_COLOR = Color.parseColor("#1A1A1A")
        private val TEXT_COLOR = Color.WHITE
        private val SECONDARY_TEXT_COLOR = Color.parseColor("#CCCCCC")
        private val SUCCESS_COLOR = Color.parseColor("#4CAF50")
        private val IMPROVEMENT_COLOR = Color.parseColor("#2196F3")
        private val ACHIEVEMENT_COLOR = Color.parseColor("#FF9800")
    }
    
    // Paint objects for drawing
    private val titlePaint = Paint().apply {
        color = TEXT_COLOR
        textSize = TITLE_SIZE
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val subtitlePaint = Paint().apply {
        color = SECONDARY_TEXT_COLOR
        textSize = SUBTITLE_SIZE
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val bodyPaint = Paint().apply {
        color = TEXT_COLOR
        textSize = BODY_SIZE
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val captionPaint = Paint().apply {
        color = SECONDARY_TEXT_COLOR
        textSize = CAPTION_SIZE
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    /**
     * Generate share graphic for best swing achievement
     */
    suspend fun generateBestSwingGraphic(
        event: BestSwingEvent,
        format: SocialMediaFormat = SocialMediaFormat.INSTAGRAM_SQUARE
    ): ShareGraphic = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Generating best swing graphic for ${event.sessionId}")
        
        val dimensions = getDimensions(format)
        val bitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawGradientBackground(canvas, dimensions, PRIMARY_COLOR, SECONDARY_COLOR)
        
        // Draw decorative elements
        drawDecorativeElements(canvas, dimensions, "best_swing")
        
        // Draw main content
        val centerX = dimensions.width / 2f
        val centerY = dimensions.height / 2f
        
        // Title
        val title = when {
            event.isPersonalBest -> "🏆 NEW PERSONAL BEST!"
            event.isConsistentExcellence -> "🎯 CONSISTENT EXCELLENCE!"
            event.isTopPercentile -> "💎 EXCEPTIONAL SWING!"
            else -> "⭐ GREAT SWING!"
        }
        
        canvas.drawText(title, centerX, centerY - 200f, titlePaint)
        
        // Score display
        val scoreText = "${(event.score * 100).toInt()}%"
        val scorePaint = Paint(titlePaint).apply {
            textSize = 120f
            color = ACCENT_COLOR
        }
        canvas.drawText(scoreText, centerX, centerY, scorePaint)
        
        // Strengths
        val strengthsText = "Strengths: ${event.strengths.joinToString(", ")}"
        drawMultilineText(canvas, strengthsText, centerX, centerY + 120f, bodyPaint, dimensions.width - 120f)
        
        // Date
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(event.timestamp))
        canvas.drawText(dateText, centerX, dimensions.height - 100f, captionPaint)
        
        // Branding
        drawBranding(canvas, dimensions)
        
        // Save to file
        val filePath = saveBitmapToFile(bitmap, "best_swing_${event.sessionId}")
        
        ShareGraphic(
            filePath = filePath,
            bitmap = bitmap,
            format = format,
            contentType = ShareContentType.BEST_SWING,
            metadata = mapOf(
                "event_id" to event.sessionId,
                "score" to event.score,
                "quality" to event.qualityLevel.name
            )
        )
    }
    
    /**
     * Generate share graphic for improvement achievement
     */
    suspend fun generateImprovementGraphic(
        event: ImprovementEvent,
        format: SocialMediaFormat = SocialMediaFormat.INSTAGRAM_SQUARE
    ): ShareGraphic = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Generating improvement graphic for ${event.userId}")
        
        val dimensions = getDimensions(format)
        val bitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawGradientBackground(canvas, dimensions, IMPROVEMENT_COLOR, Color.parseColor("#1976D2"))
        
        // Draw decorative elements
        drawDecorativeElements(canvas, dimensions, "improvement")
        
        val centerX = dimensions.width / 2f
        val centerY = dimensions.height / 2f
        
        // Title
        canvas.drawText("🚀 AMAZING PROGRESS!", centerX, centerY - 250f, titlePaint)
        
        // Improvement percentage
        val improvementText = "${(event.improvementAmount * 100).toInt()}%"
        val improvementPaint = Paint(titlePaint).apply {
            textSize = 140f
            color = ACCENT_COLOR
        }
        canvas.drawText(improvementText, centerX, centerY - 50f, improvementPaint)
        
        // Improvement area
        val areaText = "IMPROVEMENT IN ${event.improvementArea.uppercase()}"
        canvas.drawText(areaText, centerX, centerY + 80f, bodyPaint)
        
        // Progress visualization
        drawProgressChart(canvas, centerX, centerY + 180f, event.oldAverage, event.newAverage)
        
        // Date
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(event.timestamp))
        canvas.drawText(dateText, centerX, dimensions.height - 100f, captionPaint)
        
        // Branding
        drawBranding(canvas, dimensions)
        
        val filePath = saveBitmapToFile(bitmap, "improvement_${event.userId}_${event.timestamp}")
        
        ShareGraphic(
            filePath = filePath,
            bitmap = bitmap,
            format = format,
            contentType = ShareContentType.IMPROVEMENT,
            metadata = mapOf(
                "user_id" to event.userId,
                "improvement" to event.improvementAmount,
                "area" to event.improvementArea
            )
        )
    }
    
    /**
     * Generate share graphic for achievement milestone
     */
    suspend fun generateAchievementGraphic(
        achievement: Achievement,
        format: SocialMediaFormat = SocialMediaFormat.INSTAGRAM_SQUARE
    ): ShareGraphic = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Generating achievement graphic for ${achievement.id}")
        
        val dimensions = getDimensions(format)
        val bitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background based on rarity
        val colors = getAchievementColors(achievement.rarity)
        drawGradientBackground(canvas, dimensions, colors.first, colors.second)
        
        // Draw decorative elements
        drawDecorativeElements(canvas, dimensions, "achievement")
        
        val centerX = dimensions.width / 2f
        val centerY = dimensions.height / 2f
        
        // Title
        canvas.drawText("🏆 ACHIEVEMENT UNLOCKED!", centerX, centerY - 300f, titlePaint)
        
        // Achievement icon/badge
        drawAchievementBadge(canvas, centerX, centerY - 150f, achievement.rarity)
        
        // Achievement name
        val namePaint = Paint(titlePaint).apply {
            textSize = 64f
            color = ACCENT_COLOR
        }
        canvas.drawText(achievement.name.uppercase(), centerX, centerY + 50f, namePaint)
        
        // Achievement description
        drawMultilineText(canvas, achievement.description, centerX, centerY + 150f, bodyPaint, dimensions.width - 120f)
        
        // Rarity indicator
        val rarityText = "${achievement.rarity.uppercase()} ACHIEVEMENT"
        val rarityPaint = Paint(captionPaint).apply {
            color = colors.first
        }
        canvas.drawText(rarityText, centerX, centerY + 250f, rarityPaint)
        
        // Date
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateText, centerX, dimensions.height - 100f, captionPaint)
        
        // Branding
        drawBranding(canvas, dimensions)
        
        val filePath = saveBitmapToFile(bitmap, "achievement_${achievement.id}")
        
        ShareGraphic(
            filePath = filePath,
            bitmap = bitmap,
            format = format,
            contentType = ShareContentType.ACHIEVEMENT,
            metadata = mapOf(
                "achievement_id" to achievement.id,
                "achievement_name" to achievement.name,
                "rarity" to achievement.rarity
            )
        )
    }
    
    /**
     * Generate share graphic for streak celebration
     */
    suspend fun generateStreakGraphic(
        streakCount: Int,
        streakType: String,
        format: SocialMediaFormat = SocialMediaFormat.INSTAGRAM_SQUARE
    ): ShareGraphic = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Generating streak graphic for $streakCount $streakType")
        
        val dimensions = getDimensions(format)
        val bitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawGradientBackground(canvas, dimensions, Color.parseColor("#FF5722"), Color.parseColor("#FF9800"))
        
        // Draw decorative elements
        drawDecorativeElements(canvas, dimensions, "streak")
        
        val centerX = dimensions.width / 2f
        val centerY = dimensions.height / 2f
        
        // Title
        canvas.drawText("🔥 ON FIRE!", centerX, centerY - 250f, titlePaint)
        
        // Streak count
        val streakText = streakCount.toString()
        val streakPaint = Paint(titlePaint).apply {
            textSize = 180f
            color = ACCENT_COLOR
        }
        canvas.drawText(streakText, centerX, centerY - 50f, streakPaint)
        
        // Streak type
        val typeText = "${streakType.uppercase()} STREAK"
        canvas.drawText(typeText, centerX, centerY + 80f, bodyPaint)
        
        // Draw flame effects
        drawFlameEffects(canvas, centerX, centerY + 180f, streakCount)
        
        // Date
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateText, centerX, dimensions.height - 100f, captionPaint)
        
        // Branding
        drawBranding(canvas, dimensions)
        
        val filePath = saveBitmapToFile(bitmap, "streak_${streakType}_${streakCount}")
        
        ShareGraphic(
            filePath = filePath,
            bitmap = bitmap,
            format = format,
            contentType = ShareContentType.STREAK,
            metadata = mapOf(
                "streak_count" to streakCount,
                "streak_type" to streakType
            )
        )
    }
    
    /**
     * Generate custom share graphic with provided content
     */
    suspend fun generateCustomGraphic(
        content: ShareableContent,
        format: SocialMediaFormat = SocialMediaFormat.INSTAGRAM_SQUARE
    ): ShareGraphic = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Generating custom graphic")
        
        val dimensions = getDimensions(format)
        val bitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawGradientBackground(canvas, dimensions, PRIMARY_COLOR, SECONDARY_COLOR)
        
        // Draw decorative elements
        drawDecorativeElements(canvas, dimensions, "custom")
        
        val centerX = dimensions.width / 2f
        val centerY = dimensions.height / 2f
        
        // Title
        canvas.drawText(content.title, centerX, centerY - 150f, titlePaint)
        
        // Description
        drawMultilineText(canvas, content.description, centerX, centerY, bodyPaint, dimensions.width - 120f)
        
        // Hashtags
        val hashtagText = content.hashtags.joinToString(" ")
        canvas.drawText(hashtagText, centerX, centerY + 150f, captionPaint)
        
        // Date
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateText, centerX, dimensions.height - 100f, captionPaint)
        
        // Branding
        drawBranding(canvas, dimensions)
        
        val filePath = saveBitmapToFile(bitmap, "custom_${System.currentTimeMillis()}")
        
        ShareGraphic(
            filePath = filePath,
            bitmap = bitmap,
            format = format,
            contentType = ShareContentType.CUSTOM,
            metadata = mapOf(
                "title" to content.title,
                "description" to content.description
            )
        )
    }
    
    /**
     * Draw gradient background
     */
    private fun drawGradientBackground(
        canvas: Canvas,
        dimensions: Dimensions,
        startColor: Int,
        endColor: Int
    ) {
        val gradient = LinearGradient(
            0f, 0f, dimensions.width.toFloat(), dimensions.height.toFloat(),
            startColor, endColor, Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
        }
        
        canvas.drawRect(0f, 0f, dimensions.width.toFloat(), dimensions.height.toFloat(), paint)
    }
    
    /**
     * Draw decorative elements
     */
    private fun drawDecorativeElements(canvas: Canvas, dimensions: Dimensions, type: String) {
        when (type) {
            "best_swing" -> drawBestSwingDecorations(canvas, dimensions)
            "improvement" -> drawImprovementDecorations(canvas, dimensions)
            "achievement" -> drawAchievementDecorations(canvas, dimensions)
            "streak" -> drawStreakDecorations(canvas, dimensions)
            "custom" -> drawCustomDecorations(canvas, dimensions)
        }
    }
    
    /**
     * Draw best swing decorations
     */
    private fun drawBestSwingDecorations(canvas: Canvas, dimensions: Dimensions) {
        val paint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        // Draw golf ball pattern
        repeat(5) { i ->
            val x = (dimensions.width * 0.1f) + (i * dimensions.width * 0.2f)
            val y = dimensions.height * 0.1f
            canvas.drawCircle(x, y, 20f, paint)
        }
        
        // Draw swing arc
        val arcPaint = Paint().apply {
            color = Color.argb(50, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        
        val rect = RectF(
            dimensions.width * 0.1f,
            dimensions.height * 0.7f,
            dimensions.width * 0.9f,
            dimensions.height * 0.9f
        )
        canvas.drawArc(rect, 180f, 180f, false, arcPaint)
    }
    
    /**
     * Draw improvement decorations
     */
    private fun drawImprovementDecorations(canvas: Canvas, dimensions: Dimensions) {
        val paint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        
        // Draw upward arrows
        repeat(3) { i ->
            val x = dimensions.width * 0.15f + (i * dimensions.width * 0.35f)
            val y = dimensions.height * 0.15f
            drawArrow(canvas, x, y, paint)
        }
    }
    
    /**
     * Draw achievement decorations
     */
    private fun drawAchievementDecorations(canvas: Canvas, dimensions: Dimensions) {
        val paint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
            style = Paint.Style.FILL
        }
        
        // Draw stars
        repeat(8) { i ->
            val angle = (i * 45f) * Math.PI / 180f
            val radius = dimensions.width * 0.35f
            val x = dimensions.width / 2f + (radius * Math.cos(angle)).toFloat()
            val y = dimensions.height / 2f + (radius * Math.sin(angle)).toFloat()
            drawStar(canvas, x, y, 30f, paint)
        }
    }
    
    /**
     * Draw streak decorations
     */
    private fun drawStreakDecorations(canvas: Canvas, dimensions: Dimensions) {
        val paint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        
        // Draw flame patterns
        repeat(6) { i ->
            val x = dimensions.width * 0.1f + (i * dimensions.width * 0.16f)
            val y = dimensions.height * 0.85f
            drawFlame(canvas, x, y, paint)
        }
    }
    
    /**
     * Draw custom decorations
     */
    private fun drawCustomDecorations(canvas: Canvas, dimensions: Dimensions) {
        val paint = Paint().apply {
            color = Color.argb(25, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        // Draw grid pattern
        val gridSize = 80f
        for (x in 0 until (dimensions.width / gridSize).toInt()) {
            for (y in 0 until (dimensions.height / gridSize).toInt()) {
                val left = x * gridSize
                val top = y * gridSize
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint)
            }
        }
    }
    
    /**
     * Draw progress chart
     */
    private fun drawProgressChart(canvas: Canvas, centerX: Float, centerY: Float, oldValue: Float, newValue: Float) {
        val chartWidth = 300f
        val chartHeight = 80f
        val barHeight = 40f
        
        val oldPaint = Paint().apply {
            color = Color.argb(150, 255, 255, 255)
            style = Paint.Style.FILL
        }
        
        val newPaint = Paint().apply {
            color = SUCCESS_COLOR
            style = Paint.Style.FILL
        }
        
        val maxValue = maxOf(oldValue, newValue)
        val oldWidth = (oldValue / maxValue) * chartWidth
        val newWidth = (newValue / maxValue) * chartWidth
        
        // Draw old value bar
        canvas.drawRect(
            centerX - chartWidth / 2f,
            centerY - barHeight,
            centerX - chartWidth / 2f + oldWidth,
            centerY - barHeight / 2f,
            oldPaint
        )
        
        // Draw new value bar
        canvas.drawRect(
            centerX - chartWidth / 2f,
            centerY - barHeight / 2f,
            centerX - chartWidth / 2f + newWidth,
            centerY,
            newPaint
        )
        
        // Draw labels
        val labelPaint = Paint(captionPaint).apply {
            textAlign = Paint.Align.LEFT
        }
        
        canvas.drawText("Before: ${(oldValue * 100).toInt()}%", centerX - chartWidth / 2f, centerY + 30f, labelPaint)
        canvas.drawText("After: ${(newValue * 100).toInt()}%", centerX - chartWidth / 2f, centerY + 60f, labelPaint)
    }
    
    /**
     * Draw achievement badge
     */
    private fun drawAchievementBadge(canvas: Canvas, centerX: Float, centerY: Float, rarity: String) {
        val badgeSize = 100f
        val colors = getAchievementColors(rarity)
        
        val paint = Paint().apply {
            color = colors.first
            style = Paint.Style.FILL
        }
        
        // Draw badge background
        canvas.drawCircle(centerX, centerY, badgeSize, paint)
        
        // Draw badge border
        val borderPaint = Paint().apply {
            color = colors.second
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(centerX, centerY, badgeSize, borderPaint)
        
        // Draw rarity symbol
        val symbolPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val symbol = when (rarity) {
            "legendary" -> "👑"
            "epic" -> "💎"
            "rare" -> "⭐"
            "uncommon" -> "🔥"
            else -> "🏆"
        }
        
        canvas.drawText(symbol, centerX, centerY + 20f, symbolPaint)
    }
    
    /**
     * Draw flame effects
     */
    private fun drawFlameEffects(canvas: Canvas, centerX: Float, centerY: Float, intensity: Int) {
        val flameCount = min(intensity, 10)
        val paint = Paint().apply {
            color = Color.argb(100, 255, 165, 0)
            style = Paint.Style.FILL
        }
        
        repeat(flameCount) { i ->
            val x = centerX + (i - flameCount / 2) * 30f
            val y = centerY + (Math.random() * 20f).toFloat()
            drawFlame(canvas, x, y, paint)
        }
    }
    
    /**
     * Draw branding
     */
    private fun drawBranding(canvas: Canvas, dimensions: Dimensions) {
        val brandingPaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val brandText = "SwingSync AI"
        canvas.drawText(brandText, dimensions.width / 2f, dimensions.height - 40f, brandingPaint)
    }
    
    /**
     * Draw multiline text
     */
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            
            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        val lineHeight = paint.textSize * 1.2f
        val totalHeight = lines.size * lineHeight
        val startY = y - totalHeight / 2f
        
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, startY + (index + 1) * lineHeight, paint)
        }
    }
    
    /**
     * Draw arrow
     */
    private fun drawArrow(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x - 15f, y + 30f)
            lineTo(x - 7f, y + 30f)
            lineTo(x - 7f, y + 60f)
            lineTo(x + 7f, y + 60f)
            lineTo(x + 7f, y + 30f)
            lineTo(x + 15f, y + 30f)
            close()
        }
        
        canvas.drawPath(path, paint)
    }
    
    /**
     * Draw star
     */
    private fun drawStar(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val path = Path()
        val angle = Math.PI / 5.0
        
        for (i in 0..9) {
            val radius = if (i % 2 == 0) size else size / 2f
            val theta = i * angle
            val px = x + (radius * Math.cos(theta)).toFloat()
            val py = y + (radius * Math.sin(theta)).toFloat()
            
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        
        path.close()
        canvas.drawPath(path, paint)
    }
    
    /**
     * Draw flame
     */
    private fun drawFlame(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(x, y)
            cubicTo(x - 10f, y - 20f, x - 5f, y - 40f, x, y - 50f)
            cubicTo(x + 5f, y - 40f, x + 10f, y - 20f, x, y)
            close()
        }
        
        canvas.drawPath(path, paint)
    }
    
    /**
     * Get achievement colors based on rarity
     */
    private fun getAchievementColors(rarity: String): Pair<Int, Int> {
        return when (rarity) {
            "legendary" -> Pair(Color.parseColor("#FFD700"), Color.parseColor("#FFA500"))
            "epic" -> Pair(Color.parseColor("#9C27B0"), Color.parseColor("#7B1FA2"))
            "rare" -> Pair(Color.parseColor("#2196F3"), Color.parseColor("#1976D2"))
            "uncommon" -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"))
            else -> Pair(Color.parseColor("#757575"), Color.parseColor("#424242"))
        }
    }
    
    /**
     * Get dimensions for social media format
     */
    private fun getDimensions(format: SocialMediaFormat): Dimensions {
        return when (format) {
            SocialMediaFormat.INSTAGRAM_SQUARE -> Dimensions(INSTAGRAM_SQUARE_WIDTH, INSTAGRAM_SQUARE_HEIGHT)
            SocialMediaFormat.INSTAGRAM_STORY -> Dimensions(INSTAGRAM_STORY_WIDTH, INSTAGRAM_STORY_HEIGHT)
            SocialMediaFormat.TWITTER_CARD -> Dimensions(TWITTER_CARD_WIDTH, TWITTER_CARD_HEIGHT)
            SocialMediaFormat.FACEBOOK_POST -> Dimensions(FACEBOOK_POST_WIDTH, FACEBOOK_POST_HEIGHT)
        }
    }
    
    /**
     * Save bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, filename: String): String {
        val dir = File(context.cacheDir, "share_graphics")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val file = File(dir, "$filename.png")
        
        try {
            val outputStream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            Log.d(TAG, "Saved graphic to: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            return ""
        }
    }
    
    /**
     * Get bitmap as byte array
     */
    fun getBitmapAsByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Clean up cached graphics
     */
    fun cleanupCache() {
        val dir = File(context.cacheDir, "share_graphics")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }
}

// Data classes for share graphics
data class ShareGraphic(
    val filePath: String,
    val bitmap: Bitmap,
    val format: SocialMediaFormat,
    val contentType: ShareContentType,
    val metadata: Map<String, Any>
)

data class Dimensions(
    val width: Int,
    val height: Int
)

enum class SocialMediaFormat {
    INSTAGRAM_SQUARE,
    INSTAGRAM_STORY,
    TWITTER_CARD,
    FACEBOOK_POST
}

enum class ShareContentType {
    BEST_SWING,
    IMPROVEMENT,
    ACHIEVEMENT,
    STREAK,
    CUSTOM
}