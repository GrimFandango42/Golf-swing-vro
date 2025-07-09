package com.golfswing.vro.pixel.coaching

import com.golfswing.vro.pixel.ai.GeminiNanoManager
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contextual feedback system providing club-specific and situation-specific coaching
 * Adapts feedback based on club type, playing conditions, and situational context
 */
@Singleton
class ContextualFeedbackSystem @Inject constructor() {
    
    /**
     * Contextual feedback data structure
     */
    data class ContextualFeedback(
        val primaryFeedback: String,
        val clubSpecificTips: List<String>,
        val situationalGuidance: List<String>,
        val conditionConsiderations: List<String>,
        val adaptiveCoaching: String,
        val visualCues: List<String>,
        val feelCues: List<String>,
        val commonMistakes: List<String>,
        val advancedTips: List<String>
    )
    
    /**
     * Context parameters for feedback generation
     */
    data class FeedbackContext(
        val clubType: GeminiNanoManager.ClubType,
        val swingPhase: SwingPhase,
        val playingSituation: PlayingSituation,
        val weatherConditions: WeatherConditions?,
        val lieCondition: LieCondition,
        val targetDistance: Float,
        val pressureLevel: PressureLevel,
        val playerConfidence: Float,
        val recentPerformance: RecentPerformance
    )
    
    /**
     * Playing situation contexts
     */
    enum class PlayingSituation {
        DRIVING_RANGE,
        PRACTICE_GREEN,
        FIRST_TEE,
        FAIRWAY_SHOT,
        APPROACH_SHOT,
        RECOVERY_SHOT,
        BUNKER_SHOT,
        PUTTING,
        PRESSURE_SITUATION,
        TOURNAMENT_PLAY
    }
    
    /**
     * Weather conditions affecting play
     */
    data class WeatherConditions(
        val windSpeed: Float,
        val windDirection: WindDirection,
        val temperature: Float,
        val humidity: Float,
        val pressure: Float
    )
    
    enum class WindDirection {
        HEADWIND, TAILWIND, CROSSWIND_LEFT, CROSSWIND_RIGHT, CALM
    }
    
    /**
     * Lie conditions
     */
    enum class LieCondition {
        PERFECT_LIE,
        TIGHT_LIE,
        FLUFFY_LIE,
        UPHILL_LIE,
        DOWNHILL_LIE,
        SIDEHILL_ABOVE,
        SIDEHILL_BELOW,
        DIVOT,
        ROUGH,
        DEEP_ROUGH,
        SAND,
        HARDPAN
    }
    
    /**
     * Pressure level assessment
     */
    enum class PressureLevel {
        LOW,        // Practice, casual rounds
        MEDIUM,     // Club tournaments, match play
        HIGH,       // Important competitions
        EXTREME     // Professional tournament conditions
    }
    
    /**
     * Recent performance tracking
     */
    data class RecentPerformance(
        val lastFiveShots: List<ShotResult>,
        val overallTrend: TrendDirection,
        val confidenceLevel: Float,
        val strugglingAreas: List<String>
    )
    
    data class ShotResult(
        val quality: Float,
        val accuracy: Float,
        val distance: Float,
        val clubUsed: GeminiNanoManager.ClubType,
        val situation: PlayingSituation
    )
    
    enum class TrendDirection {
        IMPROVING, STABLE, DECLINING
    }
    
    /**
     * Generate contextual feedback based on comprehensive situation analysis
     */
    fun generateContextualFeedback(
        baseAnalysis: String,
        context: FeedbackContext,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): ContextualFeedback {
        val clubSpecificTips = generateClubSpecificTips(context.clubType, context.swingPhase, skillLevel)
        val situationalGuidance = generateSituationalGuidance(context.playingSituation, context.pressureLevel)
        val conditionConsiderations = generateConditionConsiderations(context.weatherConditions, context.lieCondition)
        val adaptiveCoaching = generateAdaptiveCoaching(baseAnalysis, context, skillLevel)
        val visualCues = generateVisualCues(context.clubType, context.swingPhase, skillLevel)
        val feelCues = generateFeelCues(context.clubType, context.swingPhase, skillLevel)
        val commonMistakes = generateCommonMistakes(context.clubType, context.playingSituation)
        val advancedTips = generateAdvancedTips(context.clubType, context.swingPhase, skillLevel)
        
        return ContextualFeedback(
            primaryFeedback = adaptiveCoaching,
            clubSpecificTips = clubSpecificTips,
            situationalGuidance = situationalGuidance,
            conditionConsiderations = conditionConsiderations,
            adaptiveCoaching = adaptiveCoaching,
            visualCues = visualCues,
            feelCues = feelCues,
            commonMistakes = commonMistakes,
            advancedTips = advancedTips
        )
    }
    
    /**
     * Generate club-specific coaching tips
     */
    private fun generateClubSpecificTips(
        clubType: GeminiNanoManager.ClubType,
        swingPhase: SwingPhase,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): List<String> {
        val tips = mutableListOf<String>()
        
        when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: inside left heel for maximum power")
                        tips.add("Tee height: half ball above club crown")
                        tips.add("Stance: slightly wider than shoulders")
                        if (skillLevel != SkillLevelAdaptationSystem.SkillLevel.BEGINNER) {
                            tips.add("Spine tilt: slight tilt away from target")
                        }
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Wide arc for maximum clubhead speed")
                        tips.add("Full shoulder turn for power")
                        if (skillLevel == SkillLevelAdaptationSystem.SkillLevel.ADVANCED || skillLevel == SkillLevelAdaptationSystem.SkillLevel.PROFESSIONAL) {
                            tips.add("Maintain spine angle throughout turn")
                        }
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Attack angle: slightly upward for optimal launch")
                        tips.add("Release club fully for maximum distance")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Strike ball on upswing for optimal launch")
                        tips.add("Square clubface at impact")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("High finish for maximum power transfer")
                        tips.add("Balanced finish on front foot")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Hold balanced finish position")
                        tips.add("Club should finish over left shoulder")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: 2-3 inches inside left heel")
                        tips.add("Sweep the ball off the turf")
                        tips.add("Stance: shoulder-width apart")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Smooth, controlled takeaway")
                        tips.add("Full shoulder turn with stable base")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Shallow approach angle")
                        tips.add("Let the club do the work")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact with slight descending blow")
                        tips.add("Trust the loft of the club")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Smooth acceleration through impact")
                        tips.add("Extended follow-through")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Balanced finish position")
                        tips.add("Weight on front foot")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.LONG_IRON -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: 2 inches inside left heel")
                        tips.add("Hands slightly ahead of ball")
                        tips.add("Athletic posture with slight knee flex")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Controlled tempo for accuracy")
                        tips.add("Maintain spine angle")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Steep angle of attack")
                        tips.add("Compress the ball against turf")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact essential")
                        tips.add("Hands ahead of clubhead")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Take divot after ball contact")
                        tips.add("Accelerate through impact")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Balanced finish with weight forward")
                        tips.add("Club finishes high")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.SHORT_IRON -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: center of stance")
                        tips.add("Hands well ahead of ball")
                        tips.add("Weight slightly favoring front foot")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Controlled, compact swing")
                        tips.add("Focus on accuracy over power")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Steep downward attack angle")
                        tips.add("Compress ball for spin")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact with divot after")
                        tips.add("Square clubface for accuracy")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Accelerate through impact zone")
                        tips.add("Good divot indicates proper contact")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Controlled finish position")
                        tips.add("Weight completely on front foot")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.WEDGE -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: center to back of stance")
                        tips.add("Hands well ahead of ball")
                        tips.add("Open stance for better rotation")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Controlled, rhythmic tempo")
                        tips.add("Maintain wrist angles")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Steep angle of attack for spin")
                        tips.add("Accelerate through impact")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact essential")
                        tips.add("Open clubface for height and spin")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("High finish for trajectory")
                        tips.add("Maintain acceleration through ball")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("High, controlled finish")
                        tips.add("Weight fully on front foot")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.PUTTER -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Eyes directly over ball")
                        tips.add("Shoulders square to target line")
                        tips.add("Light grip pressure")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Pendulum motion from shoulders")
                        tips.add("No wrist action")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Smooth acceleration")
                        tips.add("Keep head still")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Square putter face")
                        tips.add("Solid contact on sweet spot")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Follow through toward target")
                        tips.add("Maintain tempo")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Hold finish position")
                        tips.add("Watch ball roll")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.HYBRID -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: slightly left of center")
                        tips.add("Combine iron and wood setup")
                        tips.add("Confidence in club selection")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Smooth, controlled tempo")
                        tips.add("Full shoulder turn")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Shallow angle of attack")
                        tips.add("Let club head release")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact")
                        tips.add("Trust the club's design")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Smooth acceleration")
                        tips.add("Extended follow-through")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Balanced finish")
                        tips.add("Weight on front foot")
                    }
                }
            }
            
            GeminiNanoManager.ClubType.MID_IRON -> {
                when (swingPhase) {
                    SwingPhase.ADDRESS -> {
                        tips.add("Ball position: just left of center")
                        tips.add("Hands slightly ahead of ball")
                        tips.add("Balanced stance")
                    }
                    SwingPhase.BACKSWING -> {
                        tips.add("Controlled tempo")
                        tips.add("Good shoulder turn")
                    }
                    SwingPhase.DOWNSWING -> {
                        tips.add("Descending blow")
                        tips.add("Compress the ball")
                    }
                    SwingPhase.IMPACT -> {
                        tips.add("Ball-first contact")
                        tips.add("Square clubface")
                    }
                    SwingPhase.FOLLOW_THROUGH -> {
                        tips.add("Accelerate through impact")
                        tips.add("Take divot after ball")
                    }
                    SwingPhase.FINISH -> {
                        tips.add("Balanced finish")
                        tips.add("Weight forward")
                    }
                }
            }
        }
        
        return tips.take(3)
    }
    
    /**
     * Generate situational guidance
     */
    private fun generateSituationalGuidance(
        situation: PlayingSituation,
        pressureLevel: PressureLevel
    ): List<String> {
        val guidance = mutableListOf<String>()
        
        when (situation) {
            PlayingSituation.DRIVING_RANGE -> {
                guidance.add("Focus on technique over results")
                guidance.add("Practice with specific targets")
                guidance.add("Work on feel and rhythm")
            }
            PlayingSituation.FIRST_TEE -> {
                guidance.add("Use your most reliable club")
                guidance.add("Focus on smooth tempo")
                guidance.add("Commit to your shot")
            }
            PlayingSituation.FAIRWAY_SHOT -> {
                guidance.add("Assess lie conditions carefully")
                guidance.add("Choose appropriate club for distance")
                guidance.add("Focus on solid contact")
            }
            PlayingSituation.APPROACH_SHOT -> {
                guidance.add("Consider pin position")
                guidance.add("Plan for safe landing area")
                guidance.add("Control distance precisely")
            }
            PlayingSituation.RECOVERY_SHOT -> {
                guidance.add("Play conservatively")
                guidance.add("Get back in play safely")
                guidance.add("Minimize risk of penalty")
            }
            PlayingSituation.BUNKER_SHOT -> {
                guidance.add("Open clubface and stance")
                guidance.add("Hit sand behind ball")
                guidance.add("Accelerate through sand")
            }
            PlayingSituation.PUTTING -> {
                guidance.add("Read green slope and grain")
                guidance.add("Focus on speed control")
                guidance.add("Trust your read")
            }
            PlayingSituation.PRESSURE_SITUATION -> {
                guidance.add("Stick to your routine")
                guidance.add("Focus on process, not outcome")
                guidance.add("Trust your preparation")
            }
            PlayingSituation.TOURNAMENT_PLAY -> {
                guidance.add("Play within your abilities")
                guidance.add("Stay patient and composed")
                guidance.add("Execute your game plan")
            }
            PlayingSituation.PRACTICE_GREEN -> {
                guidance.add("Practice different speeds")
                guidance.add("Work on short putts")
                guidance.add("Develop feel for green")
            }
        }
        
        // Add pressure-specific guidance
        when (pressureLevel) {
            PressureLevel.HIGH, PressureLevel.EXTREME -> {
                guidance.add("Take extra time to settle")
                guidance.add("Focus on breathing and rhythm")
                guidance.add("Trust your natural swing")
            }
            PressureLevel.MEDIUM -> {
                guidance.add("Maintain normal routine")
                guidance.add("Stay committed to shot")
            }
            PressureLevel.LOW -> {
                guidance.add("Good opportunity to try new techniques")
                guidance.add("Focus on improvement")
            }
        }
        
        return guidance.take(4)
    }
    
    /**
     * Generate condition considerations
     */
    private fun generateConditionConsiderations(
        weather: WeatherConditions?,
        lie: LieCondition
    ): List<String> {
        val considerations = mutableListOf<String>()
        
        // Weather considerations
        weather?.let { w ->
            when (w.windDirection) {
                WindDirection.HEADWIND -> {
                    considerations.add("Take more club for distance")
                    considerations.add("Keep ball flight lower")
                    considerations.add("Swing with control, not force")
                }
                WindDirection.TAILWIND -> {
                    considerations.add("Take less club than normal")
                    considerations.add("Ball will carry farther")
                    considerations.add("Be careful of overshooting")
                }
                WindDirection.CROSSWIND_LEFT -> {
                    considerations.add("Aim right of target")
                    considerations.add("Ball will curve left")
                    considerations.add("Allow for wind drift")
                }
                WindDirection.CROSSWIND_RIGHT -> {
                    considerations.add("Aim left of target")
                    considerations.add("Ball will curve right")
                    considerations.add("Allow for wind drift")
                }
                WindDirection.CALM -> {
                    considerations.add("Ideal conditions for accuracy")
                    considerations.add("Focus on precise execution")
                }
            }
            
            if (w.temperature < 10f) {
                considerations.add("Ball won't carry as far in cold")
                considerations.add("Take extra club for distance")
            }
            
            if (w.humidity > 80f) {
                considerations.add("Ball may not carry as far")
                considerations.add("Grip may become slippery")
            }
        }
        
        // Lie considerations
        when (lie) {
            LieCondition.PERFECT_LIE -> {
                considerations.add("Great lie for full shot")
                considerations.add("Execute normal swing")
            }
            LieCondition.TIGHT_LIE -> {
                considerations.add("Ball-first contact essential")
                considerations.add("Avoid hitting behind ball")
                considerations.add("Make clean, crisp contact")
            }
            LieCondition.FLUFFY_LIE -> {
                considerations.add("Ball sitting up nicely")
                considerations.add("Avoid hitting too far behind")
                considerations.add("Trust your normal swing")
            }
            LieCondition.UPHILL_LIE -> {
                considerations.add("Ball will fly higher")
                considerations.add("Take less club")
                considerations.add("Adjust stance to slope")
            }
            LieCondition.DOWNHILL_LIE -> {
                considerations.add("Ball will fly lower")
                considerations.add("Take more club")
                considerations.add("Swing with slope")
            }
            LieCondition.SIDEHILL_ABOVE -> {
                considerations.add("Ball will curve left")
                considerations.add("Aim right of target")
                considerations.add("Choke down on grip")
            }
            LieCondition.SIDEHILL_BELOW -> {
                considerations.add("Ball will curve right")
                considerations.add("Aim left of target")
                considerations.add("Bend knees more")
            }
            LieCondition.DIVOT -> {
                considerations.add("Ball-first contact crucial")
                considerations.add("Take one more club")
                considerations.add("Steep angle of attack")
            }
            LieCondition.ROUGH -> {
                considerations.add("Grass will grab clubface")
                considerations.add("Take more club")
                considerations.add("Firm grip pressure")
            }
            LieCondition.DEEP_ROUGH -> {
                considerations.add("Focus on getting out")
                considerations.add("Take lofted club")
                considerations.add("Swing aggressively")
            }
            LieCondition.SAND -> {
                considerations.add("Open clubface")
                considerations.add("Hit sand behind ball")
                considerations.add("Accelerate through sand")
            }
            LieCondition.HARDPAN -> {
                considerations.add("Clean contact essential")
                considerations.add("Ball will come out low")
                considerations.add("Avoid bouncing club")
            }
        }
        
        return considerations.take(3)
    }
    
    /**
     * Generate adaptive coaching based on context
     */
    private fun generateAdaptiveCoaching(
        baseAnalysis: String,
        context: FeedbackContext,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): String {
        val contextualModifier = when (context.playingSituation) {
            PlayingSituation.PRESSURE_SITUATION -> "Under pressure, focus on your fundamentals. "
            PlayingSituation.TOURNAMENT_PLAY -> "In competition, trust your preparation. "
            PlayingSituation.DRIVING_RANGE -> "In practice, this is a great opportunity to work on technique. "
            PlayingSituation.RECOVERY_SHOT -> "For recovery, prioritize getting back in play. "
            else -> ""
        }
        
        val confidenceAdjuster = when {
            context.playerConfidence < 0.3f -> "Build confidence with a smooth, controlled swing. "
            context.playerConfidence > 0.8f -> "You're feeling good - trust your instincts. "
            else -> ""
        }
        
        val performanceAdjuster = when (context.recentPerformance.overallTrend) {
            TrendDirection.IMPROVING -> "You're on a roll - keep the momentum going. "
            TrendDirection.DECLINING -> "Stay patient and focus on your process. "
            TrendDirection.STABLE -> "Consistency is key - maintain your rhythm. "
        }
        
        return "$contextualModifier$confidenceAdjuster$performanceAdjuster$baseAnalysis"
    }
    
    /**
     * Generate visual cues
     */
    private fun generateVisualCues(
        clubType: GeminiNanoManager.ClubType,
        swingPhase: SwingPhase,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): List<String> {
        val cues = mutableListOf<String>()
        
        when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> {
                cues.add("Imagine sweeping ball off tee")
                cues.add("Visualize wide, powerful arc")
                cues.add("Picture ball launching high")
            }
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> {
                cues.add("See ball being swept off turf")
                cues.add("Imagine smooth, controlled tempo")
                cues.add("Visualize ball trajectory")
            }
            GeminiNanoManager.ClubType.LONG_IRON -> {
                cues.add("Picture ball-first contact")
                cues.add("See divot after ball")
                cues.add("Imagine penetrating ball flight")
            }
            GeminiNanoManager.ClubType.SHORT_IRON -> {
                cues.add("Visualize compressing ball")
                cues.add("See ball spinning toward target")
                cues.add("Picture high, soft landing")
            }
            GeminiNanoManager.ClubType.WEDGE -> {
                cues.add("Imagine ball floating high")
                cues.add("See ball landing softly")
                cues.add("Picture spin stopping ball")
            }
            GeminiNanoManager.ClubType.PUTTER -> {
                cues.add("Visualize ball rolling on line")
                cues.add("See ball dropping in hole")
                cues.add("Picture smooth pendulum motion")
            }
            else -> {
                cues.add("Visualize solid contact")
                cues.add("See ball flying to target")
                cues.add("Picture balanced finish")
            }
        }
        
        return cues.take(2)
    }
    
    /**
     * Generate feel cues
     */
    private fun generateFeelCues(
        clubType: GeminiNanoManager.ClubType,
        swingPhase: SwingPhase,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): List<String> {
        val cues = mutableListOf<String>()
        
        when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> {
                cues.add("Feel the power building in backswing")
                cues.add("Sense the clubhead releasing")
                cues.add("Feel balanced finish")
            }
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> {
                cues.add("Feel smooth, sweeping motion")
                cues.add("Sense club gliding through grass")
                cues.add("Feel controlled power")
            }
            GeminiNanoManager.ClubType.LONG_IRON -> {
                cues.add("Feel ball compression")
                cues.add("Sense crisp, clean contact")
                cues.add("Feel divot after ball")
            }
            GeminiNanoManager.ClubType.SHORT_IRON -> {
                cues.add("Feel ball pinching against ground")
                cues.add("Sense aggressive ball-first contact")
                cues.add("Feel spin on ball")
            }
            GeminiNanoManager.ClubType.WEDGE -> {
                cues.add("Feel soft hands through impact")
                cues.add("Sense loft lifting ball")
                cues.add("Feel controlled acceleration")
            }
            GeminiNanoManager.ClubType.PUTTER -> {
                cues.add("Feel pendulum rhythm")
                cues.add("Sense smooth stroke")
                cues.add("Feel ball rolling off putter")
            }
            else -> {
                cues.add("Feel solid contact")
                cues.add("Sense smooth tempo")
                cues.add("Feel balanced finish")
            }
        }
        
        return cues.take(2)
    }
    
    /**
     * Generate common mistakes for club and situation
     */
    private fun generateCommonMistakes(
        clubType: GeminiNanoManager.ClubType,
        situation: PlayingSituation
    ): List<String> {
        val mistakes = mutableListOf<String>()
        
        when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> {
                mistakes.add("Trying to hit too hard")
                mistakes.add("Ball position too far back")
                mistakes.add("Hitting down on ball")
            }
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> {
                mistakes.add("Trying to lift ball")
                mistakes.add("Hitting too far behind ball")
                mistakes.add("Lack of confidence")
            }
            GeminiNanoManager.ClubType.LONG_IRON -> {
                mistakes.add("Not hitting ball first")
                mistakes.add("Trying to help ball up")
                mistakes.add("Insufficient commitment")
            }
            GeminiNanoManager.ClubType.SHORT_IRON -> {
                mistakes.add("Deceleration through impact")
                mistakes.add("Trying to steer ball")
                mistakes.add("Not taking divot")
            }
            GeminiNanoManager.ClubType.WEDGE -> {
                mistakes.add("Flipping wrists at impact")
                mistakes.add("Deceleration")
                mistakes.add("Inconsistent setup")
            }
            GeminiNanoManager.ClubType.PUTTER -> {
                mistakes.add("Moving head during stroke")
                mistakes.add("Inconsistent tempo")
                mistakes.add("Poor alignment")
            }
            else -> {
                mistakes.add("Poor setup position")
                mistakes.add("Inconsistent tempo")
                mistakes.add("Lack of commitment")
            }
        }
        
        return mistakes.take(2)
    }
    
    /**
     * Generate advanced tips for higher skill levels
     */
    private fun generateAdvancedTips(
        clubType: GeminiNanoManager.ClubType,
        swingPhase: SwingPhase,
        skillLevel: SkillLevelAdaptationSystem.SkillLevel
    ): List<String> {
        if (skillLevel == SkillLevelAdaptationSystem.SkillLevel.BEGINNER) {
            return emptyList()
        }
        
        val tips = mutableListOf<String>()
        
        when (clubType) {
            GeminiNanoManager.ClubType.DRIVER -> {
                tips.add("Optimize attack angle for launch conditions")
                tips.add("Work on path and face angle relationship")
                tips.add("Develop speed-control balance")
            }
            GeminiNanoManager.ClubType.FAIRWAY_WOOD -> {
                tips.add("Practice different trajectory shots")
                tips.add("Work on course-specific distances")
                tips.add("Master tight lie technique")
            }
            GeminiNanoManager.ClubType.LONG_IRON -> {
                tips.add("Develop consistent strike pattern")
                tips.add("Master trajectory control")
                tips.add("Work on spin rate optimization")
            }
            GeminiNanoManager.ClubType.SHORT_IRON -> {
                tips.add("Focus on precision distance control")
                tips.add("Master spin rate variations")
                tips.add("Develop shot shaping ability")
            }
            GeminiNanoManager.ClubType.WEDGE -> {
                tips.add("Master multiple shot techniques")
                tips.add("Develop feel for different lies")
                tips.add("Work on trajectory and spin control")
            }
            GeminiNanoManager.ClubType.PUTTER -> {
                tips.add("Develop green reading skills")
                tips.add("Master speed control variations")
                tips.add("Work on pressure putting")
            }
            else -> {
                tips.add("Focus on consistency optimization")
                tips.add("Develop situational awareness")
                tips.add("Master pressure performance")
            }
        }
        
        return tips.take(2)
    }
}