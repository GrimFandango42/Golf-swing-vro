package com.swingsync.ai.onboarding

import com.swingsync.ai.data.UserProfile
import com.swingsync.ai.ml.SwingPredictor
import com.swingsync.ai.utils.GolfKnowledge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart profile generator that creates personalized user profiles
 * from minimal fun questions and intelligent inference
 */
@Singleton
class SmartProfileGenerator @Inject constructor(
    private val swingPredictor: SwingPredictor,
    private val golfKnowledge: GolfKnowledge
) {

    /**
     * Generate a smart user profile from answers
     */
    fun generateSmartProfile(answers: OnboardingWizard.ProfileAnswers): UserProfile {
        val baseProfile = createBaseProfile(answers)
        val enhancedProfile = enhanceWithIntelligence(baseProfile, answers)
        val personalizedProfile = addPersonalization(enhancedProfile, answers)
        
        return personalizedProfile
    }

    /**
     * Create base profile from direct answers
     */
    private fun createBaseProfile(answers: OnboardingWizard.ProfileAnswers): UserProfile {
        val experienceLevel = mapExperienceLevel(answers.experienceLevel)
        val handicap = estimateHandicap(answers.experienceLevel, answers.practiceFrequency)
        val playerType = determinePlayerType(answers.mainGoal, answers.experienceLevel)
        
        return UserProfile(
            id = generateProfileId(),
            name = generateFunName(answers),
            experienceLevel = experienceLevel,
            handicap = handicap,
            playerType = playerType,
            favoriteClub = answers.favoriteClub,
            practiceFrequency = answers.practiceFrequency,
            mainGoal = answers.mainGoal,
            strengthAreas = identifyStrengthAreas(answers),
            improvementAreas = identifyImprovementAreas(answers),
            personalityTraits = inferPersonalityTraits(answers),
            preferences = generateSmartPreferences(answers),
            motivationStyle = determineMotivationStyle(answers),
            learningStyle = inferLearningStyle(answers),
            createdAt = System.currentTimeMillis(),
            isComplete = true
        )
    }

    /**
     * Enhance profile with AI intelligence
     */
    private fun enhanceWithIntelligence(profile: UserProfile, answers: OnboardingWizard.ProfileAnswers): UserProfile {
        val predictedSwingCharacteristics = swingPredictor.predictSwingCharacteristics(profile)
        val commonChallenges = golfKnowledge.getCommonChallenges(profile.experienceLevel)
        val recommendedDrills = golfKnowledge.getRecommendedDrills(profile)
        val equipmentSuggestions = golfKnowledge.getEquipmentSuggestions(profile)
        
        return profile.copy(
            predictedSwingSpeed = predictedSwingCharacteristics.averageSpeed,
            predictedSwingTempo = predictedSwingCharacteristics.tempo,
            commonChallenges = commonChallenges,
            recommendedDrills = recommendedDrills,
            equipmentSuggestions = equipmentSuggestions,
            aiInsights = generateAIInsights(profile, answers)
        )
    }

    /**
     * Add personalization touches
     */
    private fun addPersonalization(profile: UserProfile, answers: OnboardingWizard.ProfileAnswers): UserProfile {
        val personalizedGoals = createPersonalizedGoals(profile, answers)
        val customizedSettings = createCustomizedSettings(profile)
        val tailoredTips = generateTailoredTips(profile)
        
        return profile.copy(
            personalizedGoals = personalizedGoals,
            customizedSettings = customizedSettings,
            tailoredTips = tailoredTips,
            profileCompleteness = calculateProfileCompleteness(profile),
            funFact = generateFunFact(profile, answers)
        )
    }

    /**
     * Map experience level from user-friendly terms
     */
    private fun mapExperienceLevel(experienceLevel: String): UserProfile.ExperienceLevel {
        return when (experienceLevel.lowercase()) {
            "just started", "beginner", "new to golf" -> UserProfile.ExperienceLevel.BEGINNER
            "weekend warrior", "recreational", "casual player" -> UserProfile.ExperienceLevel.INTERMEDIATE
            "serious player", "competitive", "advanced" -> UserProfile.ExperienceLevel.ADVANCED
            "professional", "expert", "pro" -> UserProfile.ExperienceLevel.EXPERT
            else -> UserProfile.ExperienceLevel.INTERMEDIATE
        }
    }

    /**
     * Estimate handicap based on experience and practice
     */
    private fun estimateHandicap(experienceLevel: String, practiceFrequency: String): Float {
        val baseHandicap = when (experienceLevel.lowercase()) {
            "just started", "beginner" -> 28f
            "weekend warrior", "recreational" -> 18f
            "serious player", "competitive" -> 8f
            "professional", "expert" -> 2f
            else -> 18f
        }
        
        val practiceModifier = when (practiceFrequency.lowercase()) {
            "daily" -> -5f
            "weekly" -> -2f
            "monthly" -> 2f
            else -> 0f
        }
        
        return (baseHandicap + practiceModifier).coerceIn(0f, 36f)
    }

    /**
     * Determine player type from goals and experience
     */
    private fun determinePlayerType(mainGoal: String, experienceLevel: String): String {
        return when {
            mainGoal.contains("fun", ignoreCase = true) -> "Fun Player"
            mainGoal.contains("scores", ignoreCase = true) -> "Score Improver"
            mainGoal.contains("pro", ignoreCase = true) -> "Aspiring Pro"
            mainGoal.contains("compete", ignoreCase = true) -> "Competitor"
            experienceLevel.contains("beginner", ignoreCase = true) -> "Learning Enthusiast"
            else -> "Golf Enthusiast"
        }
    }

    /**
     * Identify strength areas from profile
     */
    private fun identifyStrengthAreas(answers: OnboardingWizard.ProfileAnswers): List<String> {
        val strengths = mutableListOf<String>()
        
        when (answers.favoriteClub.lowercase()) {
            "driver" -> strengths.add("Power")
            "irons" -> strengths.add("Accuracy")
            "putter" -> strengths.add("Short Game")
            "wedges" -> strengths.add("Touch")
        }
        
        when (answers.practiceFrequency.lowercase()) {
            "daily" -> strengths.add("Dedication")
            "weekly" -> strengths.add("Consistency")
        }
        
        if (strengths.isEmpty()) {
            strengths.add("Enthusiasm")
        }
        
        return strengths
    }

    /**
     * Identify improvement areas
     */
    private fun identifyImprovementAreas(answers: OnboardingWizard.ProfileAnswers): List<String> {
        val improvements = mutableListOf<String>()
        
        when (answers.experienceLevel.lowercase()) {
            "just started", "beginner" -> {
                improvements.addAll(listOf("Swing Fundamentals", "Course Management", "Rules"))
            }
            "weekend warrior", "recreational" -> {
                improvements.addAll(listOf("Consistency", "Short Game", "Mental Game"))
            }
            "serious player", "competitive" -> {
                improvements.addAll(listOf("Fine-tuning", "Course Strategy", "Pressure Performance"))
            }
        }
        
        return improvements
    }

    /**
     * Infer personality traits from answers
     */
    private fun inferPersonalityTraits(answers: OnboardingWizard.ProfileAnswers): List<String> {
        val traits = mutableListOf<String>()
        
        when (answers.mainGoal.lowercase()) {
            "have fun" -> traits.addAll(listOf("Relaxed", "Enjoys the game", "Social"))
            "lower scores" -> traits.addAll(listOf("Goal-oriented", "Competitive", "Analytical"))
            "go pro" -> traits.addAll(listOf("Ambitious", "Dedicated", "Perfectionist"))
        }
        
        when (answers.practiceFrequency.lowercase()) {
            "daily" -> traits.add("Disciplined")
            "weekly" -> traits.add("Committed")
            "monthly" -> traits.add("Casual")
        }
        
        return traits.distinct()
    }

    /**
     * Generate smart preferences based on profile
     */
    private fun generateSmartPreferences(answers: OnboardingWizard.ProfileAnswers): UserProfile.Preferences {
        return UserProfile.Preferences(
            analysisDetail = when (answers.experienceLevel.lowercase()) {
                "just started", "beginner" -> "Simple"
                "weekend warrior", "recreational" -> "Balanced"
                "serious player", "competitive" -> "Detailed"
                else -> "Balanced"
            },
            feedbackStyle = when (answers.mainGoal.lowercase()) {
                "have fun" -> "Encouraging"
                "lower scores" -> "Direct"
                "go pro" -> "Technical"
                else -> "Balanced"
            },
            practiceReminders = answers.practiceFrequency.lowercase() != "daily",
            videoAutoSave = true,
            shareProgress = when (answers.mainGoal.lowercase()) {
                "have fun" -> true
                "go pro" -> true
                else -> false
            },
            preferredUnits = "Imperial" // Default, can be auto-detected from locale
        )
    }

    /**
     * Determine motivation style
     */
    private fun determineMotivationStyle(answers: OnboardingWizard.ProfileAnswers): String {
        return when {
            answers.mainGoal.contains("fun", ignoreCase = true) -> "Enjoyment-focused"
            answers.mainGoal.contains("scores", ignoreCase = true) -> "Achievement-focused"
            answers.mainGoal.contains("pro", ignoreCase = true) -> "Mastery-focused"
            else -> "Progress-focused"
        }
    }

    /**
     * Infer learning style
     */
    private fun inferLearningStyle(answers: OnboardingWizard.ProfileAnswers): String {
        return when (answers.experienceLevel.lowercase()) {
            "just started", "beginner" -> "Visual learner"
            "weekend warrior", "recreational" -> "Hands-on learner"
            "serious player", "competitive" -> "Analytical learner"
            else -> "Balanced learner"
        }
    }

    /**
     * Generate AI insights
     */
    private fun generateAIInsights(profile: UserProfile, answers: OnboardingWizard.ProfileAnswers): List<String> {
        val insights = mutableListOf<String>()
        
        insights.add("Your ${profile.favoriteClub} preference suggests you enjoy ${getClubCharacteristic(profile.favoriteClub)}")
        insights.add("With ${profile.practiceFrequency.lowercase()} practice, you're likely to see improvements in ${getImprovementTimeline(profile.practiceFrequency)}")
        insights.add("Your ${profile.playerType} profile indicates you'll benefit most from ${getRecommendedFocus(profile.playerType)}")
        
        return insights
    }

    /**
     * Create personalized goals
     */
    private fun createPersonalizedGoals(profile: UserProfile, answers: OnboardingWizard.ProfileAnswers): List<String> {
        val goals = mutableListOf<String>()
        
        when (profile.experienceLevel) {
            UserProfile.ExperienceLevel.BEGINNER -> {
                goals.addAll(listOf(
                    "Make solid contact with the ball",
                    "Learn proper grip and stance",
                    "Hit 3 out of 5 balls straight"
                ))
            }
            UserProfile.ExperienceLevel.INTERMEDIATE -> {
                goals.addAll(listOf(
                    "Improve consistency by 15%",
                    "Lower handicap by 3 strokes",
                    "Master favorite club (${profile.favoriteClub})"
                ))
            }
            UserProfile.ExperienceLevel.ADVANCED -> {
                goals.addAll(listOf(
                    "Fine-tune swing mechanics",
                    "Achieve single-digit handicap",
                    "Develop course management skills"
                ))
            }
            UserProfile.ExperienceLevel.EXPERT -> {
                goals.addAll(listOf(
                    "Optimize swing for maximum efficiency",
                    "Achieve scratch handicap",
                    "Master all weather conditions"
                ))
            }
        }
        
        return goals
    }

    /**
     * Create customized settings
     */
    private fun createCustomizedSettings(profile: UserProfile): UserProfile.Settings {
        return UserProfile.Settings(
            analysisSpeed = when (profile.experienceLevel) {
                UserProfile.ExperienceLevel.BEGINNER -> "Slow"
                UserProfile.ExperienceLevel.INTERMEDIATE -> "Normal"
                UserProfile.ExperienceLevel.ADVANCED -> "Fast"
                UserProfile.ExperienceLevel.EXPERT -> "Instant"
            },
            measurementPrecision = when (profile.experienceLevel) {
                UserProfile.ExperienceLevel.BEGINNER -> "Basic"
                UserProfile.ExperienceLevel.INTERMEDIATE -> "Standard"
                UserProfile.ExperienceLevel.ADVANCED -> "Precise"
                UserProfile.ExperienceLevel.EXPERT -> "Maximum"
            },
            autoAnalysis = profile.practiceFrequency.lowercase() == "daily",
            backgroundProcessing = true,
            cloudSync = true
        )
    }

    /**
     * Generate tailored tips
     */
    private fun generateTailoredTips(profile: UserProfile): List<String> {
        val tips = mutableListOf<String>()
        
        tips.add("Focus on your ${profile.improvementAreas.firstOrNull() ?: "swing fundamentals"} for fastest improvement")
        tips.add("Your ${profile.favoriteClub} skills can be transferred to other clubs")
        tips.add("Practice ${profile.practiceFrequency.lowercase()} sessions work best when focused on specific skills")
        
        return tips
    }

    /**
     * Calculate profile completeness
     */
    private fun calculateProfileCompleteness(profile: UserProfile): Float {
        var completeness = 0.7f // Base completeness from onboarding
        
        if (profile.strengthAreas.isNotEmpty()) completeness += 0.1f
        if (profile.improvementAreas.isNotEmpty()) completeness += 0.1f
        if (profile.personalityTraits.isNotEmpty()) completeness += 0.1f
        
        return completeness.coerceIn(0f, 1f)
    }

    /**
     * Generate a fun fact about the user
     */
    private fun generateFunFact(profile: UserProfile, answers: OnboardingWizard.ProfileAnswers): String {
        val facts = listOf(
            "You're one of ${getPercentage(profile.experienceLevel)}% of golfers at your level!",
            "Your ${profile.favoriteClub} preference is shared by ${getClubPopularity(profile.favoriteClub)}% of players",
            "With ${profile.practiceFrequency.lowercase()} practice, you're in the top ${getPracticePercentile(profile.practiceFrequency)}% of dedicated players",
            "Your ${profile.playerType} personality makes you ${getPersonalityAdvantage(profile.playerType)}"
        )
        
        return facts.random()
    }

    /**
     * Generate a fun profile name
     */
    private fun generateFunName(answers: OnboardingWizard.ProfileAnswers): String {
        val prefixes = when (answers.experienceLevel.lowercase()) {
            "just started", "beginner" -> listOf("Rising", "Aspiring", "Eager")
            "weekend warrior", "recreational" -> listOf("Weekend", "Steady", "Reliable")
            "serious player", "competitive" -> listOf("Serious", "Focused", "Dedicated")
            else -> listOf("Passionate", "Skilled", "Committed")
        }
        
        val suffixes = when (answers.favoriteClub.lowercase()) {
            "driver" -> listOf("Driver", "Bomber", "Power Player")
            "irons" -> listOf("Iron Master", "Precision Player", "Accuracy Ace")
            "putter" -> listOf("Putter", "Short Game Specialist", "Clutch Player")
            else -> listOf("Golfer", "Player", "Enthusiast")
        }
        
        return "${prefixes.random()} ${suffixes.random()}"
    }

    /**
     * Generate unique profile ID
     */
    private fun generateProfileId(): String {
        return "profile_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // Helper methods for generating insights
    private fun getClubCharacteristic(club: String): String {
        return when (club.lowercase()) {
            "driver" -> "power and distance"
            "irons" -> "precision and control"
            "putter" -> "finesse and touch"
            "wedges" -> "creativity and short game"
            else -> "versatility"
        }
    }

    private fun getImprovementTimeline(frequency: String): String {
        return when (frequency.lowercase()) {
            "daily" -> "2-3 weeks"
            "weekly" -> "1-2 months"
            "monthly" -> "3-4 months"
            else -> "2-3 months"
        }
    }

    private fun getRecommendedFocus(playerType: String): String {
        return when (playerType.lowercase()) {
            "fun player" -> "enjoying the game and basic fundamentals"
            "score improver" -> "consistency and course management"
            "aspiring pro" -> "advanced techniques and mental game"
            "competitor" -> "pressure situations and strategic play"
            else -> "balanced skill development"
        }
    }

    private fun getPercentage(level: UserProfile.ExperienceLevel): Int {
        return when (level) {
            UserProfile.ExperienceLevel.BEGINNER -> 40
            UserProfile.ExperienceLevel.INTERMEDIATE -> 45
            UserProfile.ExperienceLevel.ADVANCED -> 13
            UserProfile.ExperienceLevel.EXPERT -> 2
        }
    }

    private fun getClubPopularity(club: String): Int {
        return when (club.lowercase()) {
            "driver" -> 35
            "irons" -> 40
            "putter" -> 20
            "wedges" -> 5
            else -> 25
        }
    }

    private fun getPracticePercentile(frequency: String): Int {
        return when (frequency.lowercase()) {
            "daily" -> 5
            "weekly" -> 25
            "monthly" -> 70
            else -> 50
        }
    }

    private fun getPersonalityAdvantage(playerType: String): String {
        return when (playerType.lowercase()) {
            "fun player" -> "naturally relaxed under pressure"
            "score improver" -> "highly motivated to practice"
            "aspiring pro" -> "detail-oriented and focused"
            "competitor" -> "thrives in challenging situations"
            else -> "well-balanced in your approach"
        }
    }
}