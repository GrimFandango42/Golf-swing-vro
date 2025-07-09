package com.golfswing.vro.pixel.coaching

import com.golfswing.vro.pixel.coaching.ProfessionalCoachingPrompts.*
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

/**
 * Comprehensive tests for AI coaching prompt quality
 * Tests professional golf coaching standards and PGA methodology
 */
@RunWith(JUnit4::class)
class ProfessionalCoachingPromptsTest {

    private lateinit var testPoseData: String
    private lateinit var testAnalysisResult: String

    @Before
    fun setUp() {
        testPoseData = """
            Swing Phase: BACKSWING
            Shoulder Angle: 35°
            Hip Angle: 15°
            X-Factor: 40°
            Balance: 0.85
            Tempo: 3.2
        """.trimIndent()

        testAnalysisResult = """
            Primary Issue: Excessive hip rotation in backswing
            Secondary Issue: Early wrist release
            Positive: Good balance and tempo
        """.trimIndent()
    }

    @Test
    fun testProfessionalSwingAnalysisPromptStructure() {
        val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.BACKSWING,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )

        // Test prompt structure
        assertTrue("Prompt should contain context section", prompt.contains("CONTEXT:"))
        assertTrue("Prompt should contain analysis framework", prompt.contains("ANALYSIS FRAMEWORK:"))
        assertTrue("Prompt should contain fundamental checks", prompt.contains("FUNDAMENTAL CHECKS:"))
        assertTrue("Prompt should contain response requirements", prompt.contains("RESPONSE REQUIREMENTS:"))
        assertTrue("Prompt should contain example format", prompt.contains("EXAMPLE RESPONSE FORMAT:"))
        
        // Test content quality
        assertTrue("Prompt should mention PGA certification", prompt.contains("PGA-certified"))
        assertTrue("Prompt should include swing phase", prompt.contains("BACKSWING"))
        assertTrue("Prompt should include skill level", prompt.contains("INTERMEDIATE"))
        assertTrue("Prompt should include club type", prompt.contains("MID_IRON"))
        
        // Test professional language requirements
        assertTrue("Prompt should require professional terminology", 
            prompt.contains("professional golf terminology"))
        assertTrue("Prompt should limit response length", prompt.contains("80 words"))
        assertTrue("Prompt should require encouraging tone", 
            prompt.contains("encouraging, professional coaching tone"))
    }

    @Test
    fun testSkillLevelAdaptation() {
        // Test beginner level prompt
        val beginnerPrompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.ADDRESS,
            SkillLevel.BEGINNER,
            ClubType.DRIVER
        )
        
        assertTrue("Beginner prompt should focus on comfort", beginnerPrompt.contains("comfort"))
        assertTrue("Beginner prompt should mention fundamentals", beginnerPrompt.contains("fundamentals"))
        
        // Test professional level prompt
        val professionalPrompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.DOWNSWING,
            SkillLevel.PROFESSIONAL,
            ClubType.WEDGE
        )
        
        assertTrue("Professional prompt should mention efficiency", professionalPrompt.contains("efficiency"))
        assertTrue("Professional prompt should mention optimization", professionalPrompt.contains("optimization"))
        
        // Test content differences
        assertNotEquals("Beginner and professional prompts should differ", beginnerPrompt, professionalPrompt)
    }

    @Test
    fun testSwingPhaseSpecificInstructions() {
        // Test each swing phase has specific instructions
        val phases = listOf(
            SwingPhase.ADDRESS,
            SwingPhase.BACKSWING,
            SwingPhase.DOWNSWING,
            SwingPhase.IMPACT,
            SwingPhase.FOLLOW_THROUGH
        )
        
        for (phase in phases) {
            val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
                testPoseData,
                phase,
                SkillLevel.INTERMEDIATE,
                ClubType.MID_IRON
            )
            
            assertTrue("Each phase should have specific instructions", 
                prompt.contains(phase.name))
            assertTrue("Instructions should be phase-appropriate", 
                prompt.length > 500) // Ensure substantial content
        }
    }

    @Test
    fun testClubTypeSpecificGuidance() {
        val clubTypes = listOf(
            ClubType.DRIVER,
            ClubType.FAIRWAY_WOOD,
            ClubType.MID_IRON,
            ClubType.WEDGE,
            ClubType.PUTTER
        )
        
        for (clubType in clubTypes) {
            val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
                testPoseData,
                SwingPhase.BACKSWING,
                SkillLevel.INTERMEDIATE,
                clubType
            )
            
            assertTrue("Each club type should be mentioned", 
                prompt.contains(clubType.name))
        }
    }

    @Test
    fun testSkillAdaptedCoachingPromptQuality() {
        val prompt = ProfessionalCoachingPrompts.buildSkillAdaptedCoachingPrompt(
            testAnalysisResult,
            SkillLevel.ADVANCED,
            ClubType.LONG_IRON,
            SituationalContext.DRIVING_RANGE
        )
        
        // Test structure
        assertTrue("Should contain student profile", prompt.contains("STUDENT PROFILE:"))
        assertTrue("Should contain skill-specific focus", prompt.contains("SKILL-SPECIFIC FOCUS:"))
        assertTrue("Should contain practice environment", prompt.contains("PRACTICE ENVIRONMENT:"))
        assertTrue("Should contain coaching guidelines", prompt.contains("COACHING GUIDELINES:"))
        assertTrue("Should contain response format", prompt.contains("RESPONSE FORMAT:"))
        
        // Test content quality
        assertTrue("Should mention PGA methodology", prompt.contains("PGA teaching methodology"))
        assertTrue("Should include skill level", prompt.contains("ADVANCED"))
        assertTrue("Should include club type", prompt.contains("LONG_IRON"))
        assertTrue("Should include context", prompt.contains("DRIVING_RANGE"))
        
        // Test specific requirements
        assertTrue("Should ask for 2-3 coaching points", prompt.contains("2-3 progressive coaching points"))
        assertTrue("Should mention practice methods", prompt.contains("practice methods"))
        assertTrue("Should mention improvement indicators", prompt.contains("improvement indicators"))
    }

    @Test
    fun testPracticeDrillPromptComprehensiveness() {
        val swingFaults = listOf("Over the top", "Early extension", "Chicken wing")
        
        val prompt = ProfessionalCoachingPrompts.buildPracticeDrillPrompt(
            swingFaults,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON,
            30,
            "Driving range with alignment sticks"
        )
        
        // Test structure
        assertTrue("Should contain session parameters", prompt.contains("SESSION PARAMETERS:"))
        assertTrue("Should contain drill complexity", prompt.contains("DRILL COMPLEXITY LEVEL:"))
        assertTrue("Should contain time structure", prompt.contains("TIME STRUCTURE:"))
        assertTrue("Should contain drill requirements", prompt.contains("DRILL REQUIREMENTS:"))
        assertTrue("Should contain response format", prompt.contains("RESPONSE FORMAT"))
        
        // Test content
        assertTrue("Should mention PGA progressions", prompt.contains("PGA teaching progressions"))
        assertTrue("Should include swing faults", prompt.contains("Over the top"))
        assertTrue("Should include time allocation", prompt.contains("30 minutes"))
        assertTrue("Should include environment", prompt.contains("Driving range"))
        
        // Test drill requirements
        assertTrue("Should address critical fault first", prompt.contains("critical fault first"))
        assertTrue("Should provide 3-4 drills", prompt.contains("3-4 progressive drills"))
        assertTrue("Should include rep counts", prompt.contains("rep counts"))
        assertTrue("Should mention success criteria", prompt.contains("success criteria"))
    }

    @Test
    fun testTempoAnalysisPromptAccuracy() {
        val tempoData = """
            Backswing Time: 0.8s
            Downswing Time: 0.3s
            Tempo Ratio: 2.67:1
            Consistency: 85%
        """.trimIndent()
        
        val prompt = ProfessionalCoachingPrompts.buildTempoAnalysisPrompt(
            tempoData,
            SkillLevel.ADVANCED,
            ClubType.DRIVER
        )
        
        // Test structure
        assertTrue("Should contain tempo data", prompt.contains("TEMPO DATA:"))
        assertTrue("Should contain reference standards", prompt.contains("REFERENCE STANDARDS:"))
        assertTrue("Should contain tempo training methods", prompt.contains("TEMPO TRAINING METHODS:"))
        assertTrue("Should contain analysis requirements", prompt.contains("ANALYSIS REQUIREMENTS:"))
        assertTrue("Should contain response format", prompt.contains("RESPONSE FORMAT:"))
        
        // Test content
        assertTrue("Should mention PGA tempo principles", prompt.contains("PGA tempo training principles"))
        assertTrue("Should include driver tempo", prompt.contains("DRIVER"))
        assertTrue("Should include advanced training", prompt.contains("ADVANCED"))
        
        // Test requirements
        assertTrue("Should compare to ideal ranges", prompt.contains("ideal ranges"))
        assertTrue("Should identify inconsistencies", prompt.contains("tempo inconsistencies"))
        assertTrue("Should suggest training methods", prompt.contains("tempo training methods"))
    }

    @Test
    fun testPromptPersonalizationBySkillLevel() {
        val skillLevels = listOf(
            SkillLevel.BEGINNER,
            SkillLevel.INTERMEDIATE,
            SkillLevel.ADVANCED,
            SkillLevel.PROFESSIONAL
        )
        
        val prompts = skillLevels.map { skillLevel ->
            ProfessionalCoachingPrompts.buildSkillAdaptedCoachingPrompt(
                testAnalysisResult,
                skillLevel,
                ClubType.MID_IRON,
                SituationalContext.PRACTICE_SESSION
            )
        }
        
        // Test that each skill level gets different content
        for (i in 0 until prompts.size - 1) {
            for (j in i + 1 until prompts.size) {
                assertNotEquals("Skill level ${skillLevels[i]} should differ from ${skillLevels[j]}", 
                    prompts[i], prompts[j])
            }
        }
        
        // Test specific skill level characteristics
        assertTrue("Beginner should focus on fundamentals", 
            prompts[0].contains("fundamental"))
        assertTrue("Professional should mention performance", 
            prompts[3].contains("performance"))
    }

    @Test
    fun testSituationalContextAdaptation() {
        val contexts = listOf(
            SituationalContext.DRIVING_RANGE,
            SituationalContext.COURSE_PLAY,
            SituationalContext.PRACTICE_SESSION,
            SituationalContext.TOURNAMENT_PREP
        )
        
        for (context in contexts) {
            val prompt = ProfessionalCoachingPrompts.buildSkillAdaptedCoachingPrompt(
                testAnalysisResult,
                SkillLevel.INTERMEDIATE,
                ClubType.MID_IRON,
                context
            )
            
            assertTrue("Context should be mentioned", prompt.contains(context.name))
            
            // Test context-specific content
            when (context) {
                SituationalContext.DRIVING_RANGE -> {
                    assertTrue("Range should mention technique", prompt.contains("technique"))
                }
                SituationalContext.COURSE_PLAY -> {
                    assertTrue("Course should mention management", prompt.contains("management"))
                }
                SituationalContext.TOURNAMENT_PREP -> {
                    assertTrue("Tournament should mention competitive", prompt.contains("competitive"))
                }
                else -> {
                    assertTrue("Context should have specific guidance", prompt.length > 500)
                }
            }
        }
    }

    @Test
    fun testCoachingLanguageQuality() {
        val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.IMPACT,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )
        
        // Test professional terminology requirements
        val professionalTerms = listOf(
            "grip", "stance", "alignment", "plane", "sequence", "impact", "follow-through"
        )
        
        var termsFound = 0
        for (term in professionalTerms) {
            if (prompt.contains(term, ignoreCase = true)) {
                termsFound++
            }
        }
        
        assertTrue("Should contain professional golf terminology", termsFound >= 3)
        
        // Test coaching structure
        assertTrue("Should provide actionable instruction", prompt.contains("actionable"))
        assertTrue("Should include feel cues", prompt.contains("feel"))
        assertTrue("Should mention expected outcome", prompt.contains("expected outcome"))
    }

    @Test
    fun testPromptLengthAndConciseness() {
        val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.BACKSWING,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )
        
        // Test word count requirement
        assertTrue("Should specify 80 word limit", prompt.contains("80 words"))
        
        // Test section length requirements for skill-adapted prompts
        val skillPrompt = ProfessionalCoachingPrompts.buildSkillAdaptedCoachingPrompt(
            testAnalysisResult,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON,
            SituationalContext.PRACTICE_SESSION
        )
        
        assertTrue("Should specify 25 words per section", skillPrompt.contains("25 words"))
    }

    @Test
    fun testTempoStandardsAccuracy() {
        val clubTypes = listOf(
            ClubType.DRIVER,
            ClubType.MID_IRON,
            ClubType.WEDGE,
            ClubType.PUTTER
        )
        
        for (clubType in clubTypes) {
            val prompt = ProfessionalCoachingPrompts.buildTempoAnalysisPrompt(
                "Test tempo data",
                SkillLevel.INTERMEDIATE,
                clubType
            )
            
            // Test that tempo standards are club-specific
            assertTrue("Should contain club-specific tempo standards", 
                prompt.contains(clubType.name))
            assertTrue("Should mention backswing time", prompt.contains("Backswing:"))
            assertTrue("Should mention downswing time", prompt.contains("Downswing:"))
        }
    }

    @Test
    fun testDrillComplexityProgression() {
        val skillLevels = listOf(SkillLevel.BEGINNER, SkillLevel.ADVANCED)
        val swingFaults = listOf("Slice", "Pull")
        
        val beginnerPrompt = ProfessionalCoachingPrompts.buildPracticeDrillPrompt(
            swingFaults,
            SkillLevel.BEGINNER,
            ClubType.MID_IRON,
            20,
            "Indoor simulator"
        )
        
        val advancedPrompt = ProfessionalCoachingPrompts.buildPracticeDrillPrompt(
            swingFaults,
            SkillLevel.ADVANCED,
            ClubType.MID_IRON,
            20,
            "Indoor simulator"
        )
        
        // Test complexity differences
        assertTrue("Beginner should mention simple drills", beginnerPrompt.contains("Simple"))
        assertTrue("Advanced should mention complex drills", advancedPrompt.contains("Complex"))
        
        assertTrue("Beginner should focus on safety", beginnerPrompt.contains("Safety"))
        assertTrue("Advanced should mention precision", advancedPrompt.contains("precision"))
    }

    @Test
    fun testTimeBasedStructureLogic() {
        val timeDurations = listOf(15, 30, 60, 90)
        
        for (duration in timeDurations) {
            val prompt = ProfessionalCoachingPrompts.buildPracticeDrillPrompt(
                listOf("Over the top"),
                SkillLevel.INTERMEDIATE,
                ClubType.MID_IRON,
                duration,
                "Driving range"
            )
            
            assertTrue("Should mention duration", prompt.contains("$duration minutes"))
            assertTrue("Should contain time structure", prompt.contains("TIME STRUCTURE:"))
            
            // Test that longer sessions have more components
            if (duration >= 60) {
                assertTrue("Longer sessions should have assessment", prompt.contains("assessment"))
            }
        }
    }

    @Test
    fun testCoachingPromptConsistency() {
        // Test that similar inputs produce consistent outputs
        val prompt1 = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.BACKSWING,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )
        
        val prompt2 = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.BACKSWING,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )
        
        assertEquals("Same inputs should produce same prompts", prompt1, prompt2)
    }

    @Test
    fun testPGAStandardsCompliance() {
        val prompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            testPoseData,
            SwingPhase.IMPACT,
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON
        )
        
        // Test PGA methodology requirements
        assertTrue("Should mention PGA standards", prompt.contains("PGA"))
        assertTrue("Should use professional coaching tone", prompt.contains("professional coaching tone"))
        assertTrue("Should prioritize safety", prompt.contains("Focus on") || prompt.contains("safety"))
        
        // Test educational approach
        assertTrue("Should be educational", prompt.contains("specific") && prompt.contains("actionable"))
        assertTrue("Should provide clear guidance", prompt.contains("instruction") || prompt.contains("correction"))
    }

    @Test
    fun testErrorHandlingAndEdgeCases() {
        // Test with minimal data
        val minimalPrompt = ProfessionalCoachingPrompts.buildProfessionalSwingAnalysisPrompt(
            "Basic pose data",
            SwingPhase.SETUP,
            SkillLevel.BEGINNER,
            ClubType.DRIVER
        )
        
        assertNotNull("Should handle minimal data", minimalPrompt)
        assertTrue("Should still be comprehensive", minimalPrompt.length > 200)
        
        // Test with empty swing faults
        val emptyFaultsPrompt = ProfessionalCoachingPrompts.buildPracticeDrillPrompt(
            emptyList(),
            SkillLevel.INTERMEDIATE,
            ClubType.MID_IRON,
            30,
            "Driving range"
        )
        
        assertNotNull("Should handle empty faults", emptyFaultsPrompt)
        assertTrue("Should provide general guidance", emptyFaultsPrompt.length > 100)
    }
}