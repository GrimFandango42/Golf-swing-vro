package com.golfswing.vro.pixel.coaching

import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase

/**
 * Professional golf coaching prompts based on PGA teaching standards
 * Provides scientifically-backed coaching language and terminology
 */
object ProfessionalCoachingPrompts {
    
    /**
     * Club-specific coaching contexts
     */
    enum class ClubType {
        DRIVER, FAIRWAY_WOOD, HYBRID, LONG_IRON, MID_IRON, SHORT_IRON, WEDGE, PUTTER
    }
    
    /**
     * Skill level definitions based on handicap ranges
     */
    enum class SkillLevel(val handicapRange: String, val description: String) {
        BEGINNER("25+", "New to golf, focus on fundamentals"),
        INTERMEDIATE("10-24", "Developing consistency, refining technique"),
        ADVANCED("0-9", "Fine-tuning performance, course management"),
        PROFESSIONAL("Plus", "Tour-level precision and consistency")
    }
    
    /**
     * Situational contexts for coaching
     */
    enum class SituationalContext {
        DRIVING_RANGE, COURSE_PLAY, PRACTICE_SESSION, TOURNAMENT_PREP, INJURY_RECOVERY
    }
    
    /**
     * Generate comprehensive swing analysis prompt with PGA standards
     */
    fun buildProfessionalSwingAnalysisPrompt(
        poseData: String,
        swingPhase: SwingPhase,
        skillLevel: SkillLevel,
        clubType: ClubType,
        previousAnalysis: String? = null
    ): String {
        val phaseSpecificInstructions = getPhaseSpecificInstructions(swingPhase, skillLevel, clubType)
        val fundamentalChecks = getFundamentalChecks(swingPhase, skillLevel)
        
        return """
        You are a PGA-certified golf instructor providing real-time swing analysis.
        
        CONTEXT:
        - Swing Phase: ${swingPhase.name}
        - Skill Level: ${skillLevel.description}
        - Club Type: ${clubType.name}
        - Pose Data: $poseData
        ${previousAnalysis?.let { "- Previous Analysis: $it" } ?: ""}
        
        ANALYSIS FRAMEWORK:
        $phaseSpecificInstructions
        
        FUNDAMENTAL CHECKS:
        $fundamentalChecks
        
        RESPONSE REQUIREMENTS:
        1. Use professional golf terminology (grip, stance, alignment, plane, etc.)
        2. Prioritize the most impactful correction for this skill level
        3. Provide ONE specific, actionable correction
        4. Include a brief "feel" cue appropriate for the skill level
        5. Mention the expected outcome of the correction
        6. Keep response under 80 words
        7. Use encouraging, professional coaching tone
        
        EXAMPLE RESPONSE FORMAT:
        "Your [specific observation]. Focus on [specific correction] by [actionable instruction]. 
        Feel [sensory cue]. This will help you [expected outcome]."
        """.trimIndent()
    }
    
    /**
     * Generate coaching tips with skill level adaptation
     */
    fun buildSkillAdaptedCoachingPrompt(
        analysisResult: String,
        skillLevel: SkillLevel,
        clubType: ClubType,
        situationalContext: SituationalContext
    ): String {
        val skillSpecificFocus = getSkillSpecificFocus(skillLevel)
        val practiceEnvironment = getPracticeEnvironmentGuidance(situationalContext)
        
        return """
        You are providing personalized coaching based on PGA teaching methodology.
        
        STUDENT PROFILE:
        - Skill Level: ${skillLevel.description}
        - Club: ${clubType.name}
        - Context: ${situationalContext.name}
        - Analysis: $analysisResult
        
        SKILL-SPECIFIC FOCUS:
        $skillSpecificFocus
        
        PRACTICE ENVIRONMENT:
        $practiceEnvironment
        
        COACHING GUIDELINES:
        1. Provide 2-3 progressive coaching points
        2. Use terminology appropriate for skill level
        3. Include specific practice methods
        4. Mention common mistakes to avoid
        5. Suggest measurable improvement indicators
        
        RESPONSE FORMAT:
        - Primary Focus: [Main improvement area]
        - Key Adjustment: [Specific technique correction]
        - Practice Method: [How to practice this]
        - Success Indicator: [How to know it's working]
        
        Keep each section under 25 words.
        """.trimIndent()
    }
    
    /**
     * Generate practice drill recommendations
     */
    fun buildPracticeDrillPrompt(
        swingFaults: List<String>,
        skillLevel: SkillLevel,
        clubType: ClubType,
        availableTime: Int,
        practiceEnvironment: String
    ): String {
        val drillComplexity = getDrillComplexity(skillLevel)
        val timeBasedStructure = getTimeBasedStructure(availableTime)
        
        return """
        You are designing a practice session based on PGA teaching progressions.
        
        SESSION PARAMETERS:
        - Skill Level: ${skillLevel.description}
        - Primary Faults: ${swingFaults.joinToString(", ")}
        - Club Focus: ${clubType.name}
        - Available Time: $availableTime minutes
        - Environment: $practiceEnvironment
        
        DRILL COMPLEXITY LEVEL:
        $drillComplexity
        
        TIME STRUCTURE:
        $timeBasedStructure
        
        DRILL REQUIREMENTS:
        1. Address the most critical fault first
        2. Provide 3-4 progressive drills
        3. Include setup, execution, and success criteria
        4. Suggest rep counts and rest periods
        5. Include feedback mechanisms
        6. Mention common execution errors
        
        RESPONSE FORMAT for each drill:
        - Drill Name: [Descriptive name]
        - Purpose: [What it corrects]
        - Setup: [How to prepare]
        - Execution: [Step-by-step process]
        - Reps: [Number of repetitions]
        - Success Criteria: [How to know it's working]
        - Progression: [How to make it harder]
        
        Keep each section concise but complete.
        """.trimIndent()
    }
    
    /**
     * Generate tempo and rhythm analysis prompt
     */
    fun buildTempoAnalysisPrompt(
        tempoData: String,
        skillLevel: SkillLevel,
        clubType: ClubType
    ): String {
        val idealTempo = getIdealTempoForClub(clubType)
        val tempoTraining = getTempoTrainingMethods(skillLevel)
        
        return """
        You are analyzing swing tempo using PGA tempo training principles.
        
        TEMPO DATA:
        $tempoData
        
        REFERENCE STANDARDS:
        - Ideal Tempo for ${clubType.name}: $idealTempo
        - Training Level: ${skillLevel.description}
        
        TEMPO TRAINING METHODS:
        $tempoTraining
        
        ANALYSIS REQUIREMENTS:
        1. Compare actual tempo to ideal ranges
        2. Identify tempo inconsistencies
        3. Suggest tempo training methods
        4. Provide rhythm improvement strategies
        5. Include timing checkpoints
        
        RESPONSE FORMAT:
        - Tempo Assessment: [Current vs ideal]
        - Main Issue: [Primary tempo problem]
        - Training Method: [Specific tempo drill]
        - Rhythm Cue: [Mental timing aid]
        - Progress Metric: [How to measure improvement]
        
        Keep response professional and actionable.
        """.trimIndent()
    }
    
    private fun getPhaseSpecificInstructions(
        swingPhase: SwingPhase,
        skillLevel: SkillLevel,
        clubType: ClubType
    ): String {
        return when (swingPhase) {
            SwingPhase.ADDRESS -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Check grip pressure (light but secure), stance width (shoulder-width for ${clubType.name}), 
                        ball position, and posture fundamentals. Focus on comfort and balance.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Verify grip neutrality, stance precision, alignment to target, spine angle, 
                        and weight distribution. Check pre-shot routine consistency.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Assess setup precision, club face alignment, body alignment relationships, 
                        pressure distribution, and mental preparation state.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Analyze setup efficiency, micro-adjustments for conditions, 
                        optimal pressure points, and competitive readiness indicators.
                    """.trimIndent()
                }
            }
            SwingPhase.BACKSWING -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Monitor takeaway path, wrist hinge timing, shoulder turn, and maintaining balance. 
                        Focus on smooth, controlled motion.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Check swing plane consistency, hip-shoulder separation, wrist cock timing, 
                        and maintaining spine angle.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Analyze swing plane precision, X-factor creation, club shaft position, 
                        and dynamic balance maintenance.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Assess swing plane efficiency, load optimization, clubface control, 
                        and kinetic chain sequencing.
                    """.trimIndent()
                }
            }
            SwingPhase.DOWNSWING -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Focus on sequence (hips first, then shoulders), maintaining balance, 
                        and smooth acceleration through impact.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Check transition sequence, hip clearing, arm/club slot, and maintaining 
                        lag angle approaching impact.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Analyze kinetic chain efficiency, club delivery angle, impact position 
                        preparation, and power transfer optimization.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Assess transition efficiency, optimal club delivery, impact dynamics, 
                        and maximum power transfer with control.
                    """.trimIndent()
                }
            }
            SwingPhase.IMPACT -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Check head position (steady), balance, and basic impact position. 
                        Focus on solid contact over power.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Verify impact alignments, weight transfer completion, shaft lean, 
                        and clubface square at impact.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Analyze impact dynamics, attack angle optimization, face control, 
                        and post-impact position quality.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Assess impact efficiency, optimal attack angle, precise face control, 
                        and energy transfer maximization.
                    """.trimIndent()
                }
            }
            SwingPhase.FOLLOW_THROUGH -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Ensure complete follow-through, balanced finish position, and smooth 
                        deceleration. Focus on finishing in balance.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Check extension through impact, rotation completion, balanced finish, 
                        and club position at completion.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Analyze post-impact extension, rotation efficiency, finish balance, 
                        and swing completion quality.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Assess follow-through efficiency, finish position optimization, 
                        and swing completion under pressure.
                    """.trimIndent()
                }
            }
            SwingPhase.FINISH -> {
                when (skillLevel) {
                    SkillLevel.BEGINNER -> """
                        Confirm balanced finish position, weight on front foot, and complete 
                        body rotation. Focus on control and balance.
                    """.trimIndent()
                    SkillLevel.INTERMEDIATE -> """
                        Verify finish position quality, balance maintenance, and body position 
                        indicating proper swing completion.
                    """.trimIndent()
                    SkillLevel.ADVANCED -> """
                        Analyze finish position efficiency, dynamic balance, and swing 
                        completion indicating optimal technique.
                    """.trimIndent()
                    SkillLevel.PROFESSIONAL -> """
                        Assess finish position precision, balance under pressure, and swing 
                        completion indicating maximum performance.
                    """.trimIndent()
                }
            }
        }
    }
    
    private fun getFundamentalChecks(swingPhase: SwingPhase, skillLevel: SkillLevel): String {
        val basicChecks = when (swingPhase) {
            SwingPhase.ADDRESS -> "Grip, stance, alignment, posture, balance"
            SwingPhase.BACKSWING -> "Takeaway path, turn sequence, swing plane, balance"
            SwingPhase.DOWNSWING -> "Transition, sequence, lag, approach angle"
            SwingPhase.IMPACT -> "Contact quality, face position, weight transfer"
            SwingPhase.FOLLOW_THROUGH -> "Extension, rotation, balance"
            SwingPhase.FINISH -> "Balance, position, completion"
        }
        
        return if (skillLevel == SkillLevel.BEGINNER) {
            "Focus on basic fundamentals: $basicChecks"
        } else {
            "Assess technical precision in: $basicChecks"
        }
    }
    
    private fun getSkillSpecificFocus(skillLevel: SkillLevel): String {
        return when (skillLevel) {
            SkillLevel.BEGINNER -> """
                - Emphasize fundamental setup and grip
                - Focus on balance and basic swing motion
                - Prioritize contact quality over distance
                - Use simple, clear language
                - Avoid technical jargon
            """.trimIndent()
            SkillLevel.INTERMEDIATE -> """
                - Refine swing plane and sequence
                - Improve consistency and ball-striking
                - Introduce course management concepts
                - Use moderate technical terminology
                - Focus on repeatable technique
            """.trimIndent()
            SkillLevel.ADVANCED -> """
                - Fine-tune swing mechanics
                - Optimize power and control balance
                - Develop shot-shaping abilities
                - Use advanced technical language
                - Focus on performance optimization
            """.trimIndent()
            SkillLevel.PROFESSIONAL -> """
                - Maximize efficiency and consistency
                - Optimize for competitive performance
                - Develop pressure-situation skills
                - Use precise technical terminology
                - Focus on marginal gains
            """.trimIndent()
        }
    }
    
    private fun getPracticeEnvironmentGuidance(context: SituationalContext): String {
        return when (context) {
            SituationalContext.DRIVING_RANGE -> """
                - Focus on technique refinement
                - Use alignment aids and training tools
                - Practice with specific targets
                - Work on swing feels and positions
            """.trimIndent()
            SituationalContext.COURSE_PLAY -> """
                - Emphasize course management
                - Focus on shot selection
                - Consider lie and conditions
                - Prioritize smart play over perfection
            """.trimIndent()
            SituationalContext.PRACTICE_SESSION -> """
                - Structured skill development
                - Progressive drill sequences
                - Measurement and feedback
                - Deliberate practice principles
            """.trimIndent()
            SituationalContext.TOURNAMENT_PREP -> """
                - Simulate competitive conditions
                - Focus on mental preparation
                - Practice pre-shot routines
                - Develop confidence and consistency
            """.trimIndent()
            SituationalContext.INJURY_RECOVERY -> """
                - Emphasize gradual progression
                - Focus on safe movement patterns
                - Avoid aggressive positions
                - Prioritize health over performance
            """.trimIndent()
        }
    }
    
    private fun getDrillComplexity(skillLevel: SkillLevel): String {
        return when (skillLevel) {
            SkillLevel.BEGINNER -> """
                - Simple, single-focus drills
                - Basic movement patterns
                - Immediate feedback mechanisms
                - Safety-first approach
            """.trimIndent()
            SkillLevel.INTERMEDIATE -> """
                - Multi-component drills
                - Progressive skill building
                - Coordination challenges
                - Performance measurement
            """.trimIndent()
            SkillLevel.ADVANCED -> """
                - Complex, integrated drills
                - Pressure simulation
                - Fine motor control
                - Competitive elements
            """.trimIndent()
            SkillLevel.PROFESSIONAL -> """
                - High-precision drills
                - Performance optimization
                - Competition simulation
                - Marginal gain focus
            """.trimIndent()
        }
    }
    
    private fun getTimeBasedStructure(availableTime: Int): String {
        return when {
            availableTime <= 15 -> """
                - 5 min warm-up
                - 8 min main drill
                - 2 min integration
            """.trimIndent()
            availableTime <= 30 -> """
                - 5 min warm-up
                - 20 min progressive drills
                - 5 min validation swings
            """.trimIndent()
            availableTime <= 60 -> """
                - 10 min warm-up
                - 35 min skill development
                - 10 min integration
                - 5 min assessment
            """.trimIndent()
            else -> """
                - 15 min warm-up
                - 45 min progressive skill work
                - 15 min game simulation
                - 10 min assessment and planning
            """.trimIndent()
        }
    }
    
    private fun getIdealTempoForClub(clubType: ClubType): String {
        return when (clubType) {
            ClubType.DRIVER -> "Backswing: 0.8-1.0s, Downswing: 0.3-0.4s"
            ClubType.FAIRWAY_WOOD -> "Backswing: 0.7-0.9s, Downswing: 0.3-0.4s"
            ClubType.HYBRID -> "Backswing: 0.6-0.8s, Downswing: 0.25-0.35s"
            ClubType.LONG_IRON -> "Backswing: 0.6-0.8s, Downswing: 0.25-0.35s"
            ClubType.MID_IRON -> "Backswing: 0.5-0.7s, Downswing: 0.2-0.3s"
            ClubType.SHORT_IRON -> "Backswing: 0.4-0.6s, Downswing: 0.2-0.3s"
            ClubType.WEDGE -> "Backswing: 0.3-0.5s, Downswing: 0.15-0.25s"
            ClubType.PUTTER -> "Backswing: 0.2-0.4s, Downswing: 0.1-0.2s"
        }
    }
    
    private fun getTempoTrainingMethods(skillLevel: SkillLevel): String {
        return when (skillLevel) {
            SkillLevel.BEGINNER -> """
                - Count-based tempo (1-2-3 rhythm)
                - Slow motion swings
                - Metronome training
                - "Swoosh" drills
            """.trimIndent()
            SkillLevel.INTERMEDIATE -> """
                - Tour tempo ratios (3:1)
                - Rhythm training with clubs
                - Variable tempo practice
                - Pressure tempo maintenance
            """.trimIndent()
            SkillLevel.ADVANCED -> """
                - Precise tempo ratios
                - Situational tempo control
                - Advanced rhythm training
                - Competitive tempo consistency
            """.trimIndent()
            SkillLevel.PROFESSIONAL -> """
                - Optimal tempo efficiency
                - Pressure tempo mastery
                - Micro-tempo adjustments
                - Performance tempo optimization
            """.trimIndent()
        }
    }
}