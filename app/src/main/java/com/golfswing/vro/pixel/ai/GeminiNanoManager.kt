package com.golfswing.vro.pixel.ai

import android.content.Context
import com.google.ai.edge.aicore.Content
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoManager @Inject constructor(
    private val context: Context
) {
    private val model: GenerativeModel by lazy {
        generativeModel("gemini-nano")
    }
    
    // Professional golf instruction expertise levels
    enum class SkillLevel {
        BEGINNER,
        INTERMEDIATE, 
        ADVANCED,
        PROFESSIONAL
    }
    
    // Golf club types for contextual coaching
    enum class ClubType {
        DRIVER,
        FAIRWAY_WOOD,
        HYBRID,
        LONG_IRON,
        MID_IRON,
        SHORT_IRON,
        WEDGE,
        PUTTER
    }
    
    // Golf swing situations for contextual feedback
    enum class SwingSituation {
        DRIVING_RANGE,
        COURSE_PLAY,
        PRACTICE_SESSION,
        LESSON,
        COMPETITION
    }

    /**
     * Analyze golf swing pose data using Gemini Nano
     * Processes pose landmarks and provides instant coaching feedback
     */
    suspend fun analyzeSwingPose(
        poseData: String,
        swingPhase: String,
        previousAnalysis: String? = null
    ): Flow<String> = flow {
        try {
            val prompt = buildSwingAnalysisPrompt(poseData, swingPhase, previousAnalysis)
            
            val response = model.generateContent(
                Content.Builder()
                    .text(prompt)
                    .build()
            )
            
            emit(response.text ?: "Unable to analyze swing at this time")
        } catch (e: Exception) {
            emit("Analysis error: ${e.message}")
        }
    }

    /**
     * Generate real-time coaching tips based on swing analysis
     */
    suspend fun generateCoachingTips(
        analysisResult: String,
        userSkillLevel: SkillLevel = SkillLevel.BEGINNER,
        clubType: ClubType? = null,
        swingSituation: SwingSituation = SwingSituation.PRACTICE_SESSION
    ): Flow<String> = flow {
        try {
            val prompt = buildProfessionalCoachingPrompt(analysisResult, userSkillLevel, clubType, swingSituation)
            
            val response = model.generateContent(
                Content.Builder()
                    .text(prompt)
                    .build()
            )
            
            emit(response.text ?: "No coaching tips available")
        } catch (e: Exception) {
            emit("Coaching error: ${e.message}")
        }
    }

    /**
     * Analyze swing tempo and rhythm
     */
    suspend fun analyzeSwingTempo(
        frameTimestamps: List<Long>,
        swingPhases: List<String>
    ): Flow<String> = flow {
        try {
            val tempoData = buildTempoAnalysisData(frameTimestamps, swingPhases)
            val prompt = buildTempoAnalysisPrompt(tempoData)
            
            val response = model.generateContent(
                Content.Builder()
                    .text(prompt)
                    .build()
            )
            
            emit(response.text ?: "Unable to analyze swing tempo")
        } catch (e: Exception) {
            emit("Tempo analysis error: ${e.message}")
        }
    }

    /**
     * Generate personalized practice drills based on PGA teaching methodology
     */
    suspend fun generatePracticeDrills(
        swingFaults: List<String>,
        userGoals: String,
        skillLevel: SkillLevel = SkillLevel.BEGINNER,
        clubType: ClubType? = null
    ): Flow<String> = flow {
        try {
            val prompt = buildProfessionalPracticeDrillPrompt(swingFaults, userGoals, skillLevel, clubType)
            
            val response = model.generateContent(
                Content.Builder()
                    .text(prompt)
                    .build()
            )
            
            emit(response.text ?: "No practice drills available")
        } catch (e: Exception) {
            emit("Practice drill error: ${e.message}")
        }
    }
    
    /**
     * Generate progress tracking insights with professional metrics
     */
    suspend fun generateProgressInsights(
        previousMetrics: Map<String, Float>,
        currentMetrics: Map<String, Float>,
        timeFrame: String,
        skillLevel: SkillLevel
    ): Flow<String> = flow {
        try {
            val prompt = buildProgressAnalysisPrompt(previousMetrics, currentMetrics, timeFrame, skillLevel)
            
            val response = model.generateContent(
                Content.Builder()
                    .text(prompt)
                    .build()
            )
            
            emit(response.text ?: "No progress insights available")
        } catch (e: Exception) {
            emit("Progress analysis error: ${e.message}")
        }
    }

    private fun buildSwingAnalysisPrompt(
        poseData: String,
        swingPhase: String,
        previousAnalysis: String?,
        skillLevel: SkillLevel = SkillLevel.BEGINNER,
        clubType: ClubType? = null
    ): String {
        val skillLevelContext = when (skillLevel) {
            SkillLevel.BEGINNER -> "Focus on fundamental setup, grip, and basic swing mechanics. Use simple, clear language."
            SkillLevel.INTERMEDIATE -> "Address intermediate concepts like swing plane, weight transfer, and tempo. Provide more detailed technical feedback."
            SkillLevel.ADVANCED -> "Analyze advanced biomechanics, kinematic sequence, and fine-tuning adjustments. Use professional terminology."
            SkillLevel.PROFESSIONAL -> "Provide tour-level analysis focusing on consistency, power optimization, and competitive performance factors."
        }
        
        val clubContext = clubType?.let { club ->
            when (club) {
                ClubType.DRIVER -> "Driver swing: Focus on wide arc, upward angle of attack, and maximum power transfer."
                ClubType.FAIRWAY_WOOD -> "Fairway wood: Emphasize sweeping motion, ball-first contact, and controlled power."
                ClubType.HYBRID -> "Hybrid swing: Combine iron and wood techniques for versatile ball-striking."
                ClubType.LONG_IRON -> "Long iron: Focus on ball-first contact, proper shaft lean, and consistent strike."
                ClubType.MID_IRON -> "Mid iron: Emphasize precision, divot after ball, and distance control."
                ClubType.SHORT_IRON -> "Short iron: Focus on accuracy, spin control, and consistent ball-turf interaction."
                ClubType.WEDGE -> "Wedge play: Emphasize feel, trajectory control, and precise distance management."
                ClubType.PUTTER -> "Putting stroke: Focus on tempo, face angle, and consistent roll."
            }
        } ?: "General swing analysis"
        
        return """
        You are a PGA-certified golf instructor with expertise in biomechanics and swing analysis.
        
        INSTRUCTION LEVEL: $skillLevelContext
        CLUB CONTEXT: $clubContext
        
        Current swing phase: $swingPhase
        Biomechanical data: $poseData
        ${previousAnalysis?.let { "Previous observation: $it" } ?: ""}
        
        Analyze using PGA teaching methodology focusing on:
        1. Setup fundamentals (grip, stance, alignment, posture)
        2. Swing plane and club path
        3. Kinematic sequence and weight transfer
        4. Impact position and follow-through
        5. Tempo and rhythm
        
        Provide professional feedback prioritizing the most critical swing element.
        Use appropriate technical terminology for skill level.
        Keep response under 100 words with actionable coaching cues.
        """.trimIndent()
    }

    private fun buildProfessionalCoachingPrompt(
        analysisResult: String,
        userSkillLevel: SkillLevel,
        clubType: ClubType?,
        swingSituation: SwingSituation
    ): String {
        val skillContext = when (userSkillLevel) {
            SkillLevel.BEGINNER -> "New to golf - focus on fundamentals, simple feels, and building confidence. Avoid technical jargon."
            SkillLevel.INTERMEDIATE -> "Developing player - ready for swing plane concepts, weight transfer, and tempo work. Use clear technical terms."
            SkillLevel.ADVANCED -> "Experienced player - can handle biomechanical concepts, kinematic sequence, and advanced drills. Use professional terminology."
            SkillLevel.PROFESSIONAL -> "Elite player - focus on fine-tuning, consistency under pressure, and competitive optimization."
        }
        
        val situationContext = when (swingSituation) {
            SwingSituation.DRIVING_RANGE -> "Practice environment - focus on repetition, feel development, and swing changes."
            SwingSituation.COURSE_PLAY -> "On-course situation - emphasize course management, shot selection, and immediate fixes."
            SwingSituation.PRACTICE_SESSION -> "Structured practice - work on specific improvements and skill development."
            SwingSituation.LESSON -> "Lesson environment - detailed instruction, concept explanation, and progressive learning."
            SwingSituation.COMPETITION -> "Competition setting - focus on rhythm, confidence, and executing learned skills."
        }
        
        val clubSpecificGuidance = clubType?.let { club ->
            when (club) {
                ClubType.DRIVER -> "Driver coaching: Focus on tee height, ball position, upward angle of attack, and power generation."
                ClubType.FAIRWAY_WOOD -> "Fairway wood guidance: Emphasize sweeping action, ball position, and clean contact."
                ClubType.HYBRID -> "Hybrid instruction: Combine iron and wood techniques for versatile shot-making."
                ClubType.LONG_IRON -> "Long iron coaching: Focus on ball-first contact, proper angle of attack, and distance control."
                ClubType.MID_IRON -> "Mid iron guidance: Emphasize precision, consistent divot pattern, and distance management."
                ClubType.SHORT_IRON -> "Short iron instruction: Focus on accuracy, spin control, and scoring precision."
                ClubType.WEDGE -> "Wedge coaching: Emphasize feel, trajectory control, and short game finesse."
                ClubType.PUTTER -> "Putting instruction: Focus on alignment, tempo, and consistent roll."
            }
        } ?: "General swing coaching"
        
        return """
        You are a PGA Master Professional with 20+ years of teaching experience.
        
        PLAYER PROFILE: $skillContext
        SITUATION: $situationContext
        CLUB FOCUS: $clubSpecificGuidance
        
        SWING ANALYSIS: $analysisResult
        
        Provide 2-3 prioritized coaching points that:
        1. Address the primary swing issue with appropriate technical depth
        2. Match the player's skill level and learning capacity
        3. Are immediately actionable in the current situation
        4. Build on golf fundamentals and proven teaching methods
        5. Include specific feels, positions, or drills when appropriate
        
        Use PGA teaching methodology with positive reinforcement.
        Include one primary focus and two supporting points.
        Keep each point under 35 words with clear, actionable language.
        """.trimIndent()
    }

    private fun buildTempoAnalysisData(
        frameTimestamps: List<Long>,
        swingPhases: List<String>
    ): String {
        val tempoData = StringBuilder()
        
        for (i in swingPhases.indices) {
            val timestamp = frameTimestamps.getOrNull(i) ?: 0L
            val phase = swingPhases[i]
            tempoData.append("$phase: ${timestamp}ms\n")
        }
        
        return tempoData.toString()
    }

    private fun buildTempoAnalysisPrompt(tempoData: String): String {
        return """
        You are analyzing golf swing tempo and rhythm.
        
        Swing phase timing data:
        $tempoData
        
        Analyze the tempo and provide feedback on:
        1. Overall swing speed
        2. Transition timing
        3. Rhythm consistency
        4. Recommendations for improvement
        
        Keep response concise and actionable.
        """.trimIndent()
    }

    private fun buildProfessionalPracticeDrillPrompt(
        swingFaults: List<String>,
        userGoals: String,
        skillLevel: SkillLevel,
        clubType: ClubType?
    ): String {
        val faultsList = swingFaults.joinToString(", ")
        
        val skillBasedDrillComplexity = when (skillLevel) {
            SkillLevel.BEGINNER -> "Simple, fundamental drills focusing on basic positions and feels. Use alignment aids and basic feedback."
            SkillLevel.INTERMEDIATE -> "Progressive drills with multiple components. Include swing plane and tempo work."
            SkillLevel.ADVANCED -> "Complex drills targeting specific biomechanical improvements. Include pressure and timing elements."
            SkillLevel.PROFESSIONAL -> "Advanced training drills with performance metrics and competitive pressure simulation."
        }
        
        val clubSpecificDrills = clubType?.let { club ->
            when (club) {
                ClubType.DRIVER -> "Driver-specific drills: Focus on tee work, angle of attack, and power development."
                ClubType.FAIRWAY_WOOD -> "Fairway wood drills: Emphasize sweeping motion and clean contact patterns."
                ClubType.HYBRID -> "Hybrid drills: Combine iron and wood techniques for versatile shot-making."
                ClubType.LONG_IRON -> "Long iron drills: Focus on ball-first contact and consistent strike patterns."
                ClubType.MID_IRON -> "Mid iron drills: Emphasize precision and distance control."
                ClubType.SHORT_IRON -> "Short iron drills: Focus on accuracy and spin control."
                ClubType.WEDGE -> "Wedge drills: Emphasize touch, trajectory control, and short game development."
                ClubType.PUTTER -> "Putting drills: Focus on alignment, tempo, and consistent roll."
            }
        } ?: "General swing drills"
        
        return """
        You are a PGA Master Professional designing practice drills based on proven teaching methodology.
        
        STUDENT PROFILE: $skillBasedDrillComplexity
        CLUB FOCUS: $clubSpecificDrills
        
        IDENTIFIED SWING FAULTS: $faultsList
        PLAYER GOALS: $userGoals
        
        Design 3 progressive practice drills following PGA teaching principles:
        
        DRILL REQUIREMENTS:
        1. Target primary swing faults with specific correction focus
        2. Appropriate for skill level and available practice time
        3. Include measurable success indicators
        4. Build from simple to complex movements
        5. Can be practiced solo with minimal equipment
        
        For each drill provide:
        - Title and primary objective
        - Step-by-step instructions
        - Repetition guidelines (sets/reps)
        - Key checkpoints and success indicators
        - Progressive difficulty variations
        - Common mistakes to avoid
        
        Use proven PGA teaching drills and methodologies.
        Keep each drill description under 75 words with clear, actionable steps.
        """.trimIndent()
    }
    
    private fun buildProgressAnalysisPrompt(
        previousMetrics: Map<String, Float>,
        currentMetrics: Map<String, Float>,
        timeFrame: String,
        skillLevel: SkillLevel
    ): String {
        val metricsComparison = StringBuilder()
        
        previousMetrics.forEach { (metric, previousValue) ->
            val currentValue = currentMetrics[metric] ?: 0f
            val change = currentValue - previousValue
            val percentChange = if (previousValue != 0f) (change / previousValue) * 100 else 0f
            metricsComparison.append("$metric: $previousValue → $currentValue (${if (change >= 0) "+" else ""}${String.format("%.1f", percentChange)}%)\n")
        }
        
        val skillLevelExpectations = when (skillLevel) {
            SkillLevel.BEGINNER -> "Focus on fundamental improvements and consistency building. Expect gradual progress in basic metrics."
            SkillLevel.INTERMEDIATE -> "Look for refinement in swing mechanics and developing consistency. Moderate improvement rates expected."
            SkillLevel.ADVANCED -> "Analyze fine-tuning improvements and performance optimization. Smaller but significant gains expected."
            SkillLevel.PROFESSIONAL -> "Evaluate competitive performance metrics and consistency under pressure. Minimal but crucial improvements."
        }
        
        return """
        You are a PGA Master Professional analyzing player progress using data-driven insights.
        
        SKILL LEVEL CONTEXT: $skillLevelExpectations
        TIME FRAME: $timeFrame
        
        PERFORMANCE METRICS COMPARISON:
        $metricsComparison
        
        Provide professional progress analysis including:
        1. Overall improvement trend assessment
        2. Strongest areas of development
        3. Areas needing continued focus
        4. Specific achievements to celebrate
        5. Realistic next milestones
        6. Recommended practice priorities
        
        Use PGA teaching methodology to:
        - Identify meaningful progress indicators
        - Set appropriate expectations for skill level
        - Provide motivation through achievement recognition
        - Guide future practice and lesson planning
        
        Keep analysis under 150 words with positive, professional tone.
        """.trimIndent()
    }
}