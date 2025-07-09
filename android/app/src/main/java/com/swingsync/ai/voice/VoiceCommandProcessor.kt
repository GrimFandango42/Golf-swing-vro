package com.swingsync.ai.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced Voice Command Processor for Golf-Specific Natural Language Understanding
 * 
 * Features:
 * - Golf-specific vocabulary and context understanding
 * - Natural language processing for conversational commands
 * - Intent classification with confidence scoring
 * - Parameter extraction (club types, distances, positions)
 * - Context-aware command interpretation
 * - Multi-turn conversation support
 * - Synonym and variation handling
 * - Correction and clarification handling
 * 
 * Understands commands like:
 * - "Start recording my driver swing"
 * - "How's my backswing looking?"
 * - "Give me tips for my iron shots"
 * - "Show me my swing plane"
 * - "Record a few practice swings"
 * - "What should I work on today?"
 */
@Singleton
class VoiceCommandProcessor @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "VoiceCommandProcessor"
        
        // Confidence thresholds
        private const val HIGH_CONFIDENCE = 0.8f
        private const val MEDIUM_CONFIDENCE = 0.6f
        private const val LOW_CONFIDENCE = 0.4f
        
        // Context tracking
        private const val CONTEXT_TIMEOUT_MS = 30000L // 30 seconds
        
        // Golf-specific vocabulary
        private val GOLF_CLUBS = mapOf(
            "driver" to listOf("driver", "big stick", "1 wood", "one wood"),
            "fairway_wood" to listOf("fairway wood", "3 wood", "5 wood", "wood"),
            "hybrid" to listOf("hybrid", "rescue", "utility"),
            "iron" to listOf("iron", "irons", "3 iron", "4 iron", "5 iron", "6 iron", "7 iron", "8 iron", "9 iron"),
            "wedge" to listOf("wedge", "pitching wedge", "sand wedge", "lob wedge", "gap wedge"),
            "putter" to listOf("putter", "putting", "putt")
        )
        
        private val SWING_PHASES = mapOf(
            "address" to listOf("address", "setup", "stance", "starting position"),
            "backswing" to listOf("backswing", "back swing", "takeaway", "going back"),
            "downswing" to listOf("downswing", "down swing", "coming down"),
            "impact" to listOf("impact", "contact", "hitting", "strike"),
            "follow_through" to listOf("follow through", "finish", "followthrough")
        )
        
        private val GOLF_METRICS = mapOf(
            "swing_speed" to listOf("swing speed", "club speed", "clubhead speed", "speed"),
            "tempo" to listOf("tempo", "rhythm", "timing", "pace"),
            "balance" to listOf("balance", "stability", "posture"),
            "swing_plane" to listOf("swing plane", "plane", "swing path", "path"),
            "angle" to listOf("angle", "angles", "position"),
            "power" to listOf("power", "strength", "force")
        )
        
        private val FEEDBACK_TYPES = mapOf(
            "analysis" to listOf("analysis", "analyze", "review", "check", "look at"),
            "tips" to listOf("tips", "advice", "help", "suggestions", "improve"),
            "comparison" to listOf("compare", "comparison", "difference", "versus"),
            "progress" to listOf("progress", "improvement", "getting better", "tracking")
        )
    }

    // State management
    private val _processingState = MutableStateFlow(ProcessingState.IDLE)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()
    
    private val _conversationContext = MutableStateFlow(ConversationContext())
    val conversationContext: StateFlow<ConversationContext> = _conversationContext.asStateFlow()

    // NLP Components
    private val intentClassifier = IntentClassifier()
    private val parameterExtractor = ParameterExtractor()
    private val contextManager = ContextManager()
    private val confidenceCalculator = ConfidenceCalculator()
    private val commandValidator = CommandValidator()

    /**
     * Process voice command with advanced NLP
     */
    fun processCommand(text: String): VoiceCommand {
        _processingState.value = ProcessingState.PROCESSING
        
        try {
            Log.d(TAG, "Processing command: $text")
            
            // Normalize and preprocess text
            val normalizedText = preprocessText(text)
            
            // Extract intent
            val intent = intentClassifier.classifyIntent(normalizedText)
            
            // Extract parameters
            val parameters = parameterExtractor.extractParameters(normalizedText, intent)
            
            // Update context
            contextManager.updateContext(intent, parameters)
            
            // Calculate confidence
            val confidence = confidenceCalculator.calculateConfidence(
                normalizedText, intent, parameters
            )
            
            // Validate command
            val validationResult = commandValidator.validateCommand(intent, parameters)
            
            // Create command object
            val command = VoiceCommand(
                type = intent.type,
                intent = intent,
                parameters = parameters,
                confidence = confidence,
                originalText = text,
                normalizedText = normalizedText,
                context = _conversationContext.value,
                validationResult = validationResult
            )
            
            _lastCommand.value = command
            _processingState.value = ProcessingState.COMPLETE
            
            Log.d(TAG, "Command processed: ${command.type} (confidence: ${command.confidence})")
            
            return command
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            _processingState.value = ProcessingState.ERROR
            
            return VoiceCommand(
                type = VoiceCommandType.ERROR,
                intent = CommandIntent(VoiceCommandType.ERROR, 0f),
                parameters = emptyMap(),
                confidence = 0f,
                originalText = text,
                normalizedText = text,
                context = _conversationContext.value,
                validationResult = ValidationResult.ERROR
            )
        }
    }

    /**
     * Handle follow-up questions and clarifications
     */
    fun handleFollowUp(text: String): VoiceCommand {
        val context = _conversationContext.value
        
        // Check if this is a clarification
        if (isclarification(text)) {
            return processClarification(text, context)
        }
        
        // Check if this is a continuation
        if (isContinuation(text)) {
            return processContinuation(text, context)
        }
        
        // Process as new command
        return processCommand(text)
    }

    /**
     * Get suggested commands based on context
     */
    fun getSuggestedCommands(): List<String> {
        val context = _conversationContext.value
        
        return when (context.lastIntent?.type) {
            VoiceCommandType.START_PRACTICE -> listOf(
                "Record my driver swing",
                "Start with putting practice",
                "Let's work on my irons"
            )
            VoiceCommandType.ANALYZE_SWING -> listOf(
                "What should I improve?",
                "How's my tempo?",
                "Check my swing plane"
            )
            VoiceCommandType.GET_TIPS -> listOf(
                "Show me an example",
                "Give me a drill",
                "What's my biggest issue?"
            )
            else -> listOf(
                "Start practice session",
                "Analyze my swing",
                "Give me tips",
                "Record a swing"
            )
        }
    }

    /**
     * Preprocess text for analysis
     */
    private fun preprocessText(text: String): String {
        return text.trim()
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .replace("[^a-zA-Z0-9\\s]".toRegex(), "")
    }

    /**
     * Check if text is a clarification
     */
    private fun isclarification(text: String): Boolean {
        val clarificationWords = listOf("yes", "no", "correct", "wrong", "right", "exactly", "that's right")
        return clarificationWords.any { text.lowercase().contains(it) }
    }

    /**
     * Check if text is a continuation
     */
    private fun isContinuation(text: String): Boolean {
        val continuationWords = listOf("and", "also", "plus", "then", "next", "after that")
        return continuationWords.any { text.lowercase().startsWith(it) }
    }

    /**
     * Process clarification
     */
    private fun processClarification(text: String, context: ConversationContext): VoiceCommand {
        // Handle yes/no responses
        val isPositive = listOf("yes", "correct", "right", "exactly").any { 
            text.lowercase().contains(it) 
        }
        
        return VoiceCommand(
            type = VoiceCommandType.CLARIFICATION,
            intent = CommandIntent(VoiceCommandType.CLARIFICATION, 0.9f),
            parameters = mapOf("confirmation" to isPositive),
            confidence = 0.9f,
            originalText = text,
            normalizedText = text,
            context = context,
            validationResult = ValidationResult.VALID
        )
    }

    /**
     * Process continuation
     */
    private fun processContinuation(text: String, context: ConversationContext): VoiceCommand {
        // Process as new command but maintain context
        val command = processCommand(text)
        return command.copy(
            type = VoiceCommandType.CONTINUATION,
            context = context.copy(isContinuation = true)
        )
    }

    /**
     * Reset conversation context
     */
    fun resetContext() {
        _conversationContext.value = ConversationContext()
        contextManager.reset()
    }

    /**
     * Get processing statistics
     */
    fun getProcessingStats(): Map<String, Any> {
        return mapOf(
            "totalCommands" to contextManager.totalCommands,
            "successfulCommands" to contextManager.successfulCommands,
            "averageConfidence" to contextManager.averageConfidence,
            "mostCommonIntent" to contextManager.mostCommonIntent,
            "contextSwitches" to contextManager.contextSwitches
        )
    }
}

/**
 * Intent classification for golf commands
 */
class IntentClassifier {
    private val intentPatterns = mapOf(
        VoiceCommandType.START_PRACTICE to listOf(
            "start practice", "begin session", "let's practice", "start coaching",
            "begin workout", "start training", "let's get started"
        ),
        VoiceCommandType.END_PRACTICE to listOf(
            "end practice", "stop session", "finish up", "that's enough",
            "done for today", "wrap up", "stop coaching"
        ),
        VoiceCommandType.RECORD_SWING to listOf(
            "record swing", "capture swing", "film swing", "record me",
            "take a video", "start recording", "record my"
        ),
        VoiceCommandType.ANALYZE_SWING to listOf(
            "analyze swing", "check swing", "review swing", "how did i do",
            "analyze that", "what do you think", "check my form"
        ),
        VoiceCommandType.GET_TIPS to listOf(
            "give tips", "need help", "what should i work on", "any advice",
            "how to improve", "help me with", "suggestions"
        ),
        VoiceCommandType.SHOW_METRICS to listOf(
            "show metrics", "show stats", "check my numbers", "what's my speed",
            "show swing speed", "display data", "show me my"
        ),
        VoiceCommandType.COMPARE_SWINGS to listOf(
            "compare swings", "compare to", "show difference", "versus",
            "compare with", "how does this compare"
        ),
        VoiceCommandType.SETTINGS to listOf(
            "change settings", "adjust", "modify", "settings",
            "change voice", "slower", "faster", "volume"
        ),
        VoiceCommandType.HELP to listOf(
            "help", "what can you do", "commands", "instructions",
            "how to use", "guide me", "what are my options"
        )
    )

    fun classifyIntent(text: String): CommandIntent {
        var bestMatch = CommandIntent(VoiceCommandType.CONVERSATION, 0f)
        
        for ((intentType, patterns) in intentPatterns) {
            for (pattern in patterns) {
                val similarity = calculateSimilarity(text, pattern)
                if (similarity > bestMatch.confidence) {
                    bestMatch = CommandIntent(intentType, similarity)
                }
            }
        }
        
        return bestMatch
    }

    private fun calculateSimilarity(text: String, pattern: String): Float {
        val textWords = text.split(" ")
        val patternWords = pattern.split(" ")
        
        var matchCount = 0
        for (word in patternWords) {
            if (textWords.any { it.contains(word) || word.contains(it) }) {
                matchCount++
            }
        }
        
        return matchCount.toFloat() / patternWords.size
    }
}

/**
 * Parameter extraction for golf commands
 */
class ParameterExtractor {
    fun extractParameters(text: String, intent: CommandIntent): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()
        
        // Extract club type
        val club = extractClub(text)
        if (club != null) {
            parameters["club"] = club
        }
        
        // Extract swing phase
        val phase = extractSwingPhase(text)
        if (phase != null) {
            parameters["swing_phase"] = phase
        }
        
        // Extract metrics
        val metrics = extractMetrics(text)
        if (metrics.isNotEmpty()) {
            parameters["metrics"] = metrics
        }
        
        // Extract numbers
        val numbers = extractNumbers(text)
        if (numbers.isNotEmpty()) {
            parameters["numbers"] = numbers
        }
        
        // Extract feedback type
        val feedbackType = extractFeedbackType(text)
        if (feedbackType != null) {
            parameters["feedback_type"] = feedbackType
        }
        
        return parameters
    }

    private fun extractClub(text: String): String? {
        for ((clubType, variations) in GOLF_CLUBS) {
            for (variation in variations) {
                if (text.contains(variation)) {
                    return clubType
                }
            }
        }
        return null
    }

    private fun extractSwingPhase(text: String): String? {
        for ((phase, variations) in SWING_PHASES) {
            for (variation in variations) {
                if (text.contains(variation)) {
                    return phase
                }
            }
        }
        return null
    }

    private fun extractMetrics(text: String): List<String> {
        val metrics = mutableListOf<String>()
        for ((metric, variations) in GOLF_METRICS) {
            for (variation in variations) {
                if (text.contains(variation)) {
                    metrics.add(metric)
                }
            }
        }
        return metrics
    }

    private fun extractNumbers(text: String): List<Int> {
        val numberPattern = Pattern.compile("\\d+")
        val matcher = numberPattern.matcher(text)
        val numbers = mutableListOf<Int>()
        
        while (matcher.find()) {
            numbers.add(matcher.group().toInt())
        }
        
        return numbers
    }

    private fun extractFeedbackType(text: String): String? {
        for ((type, variations) in FEEDBACK_TYPES) {
            for (variation in variations) {
                if (text.contains(variation)) {
                    return type
                }
            }
        }
        return null
    }
}

/**
 * Context management for conversation tracking
 */
class ContextManager {
    var totalCommands = 0
    var successfulCommands = 0
    var averageConfidence = 0f
    var mostCommonIntent = VoiceCommandType.CONVERSATION
    var contextSwitches = 0
    
    private var lastContext: ConversationContext? = null
    private val intentCounts = mutableMapOf<VoiceCommandType, Int>()
    private val confidenceSum = mutableMapOf<VoiceCommandType, Float>()

    fun updateContext(intent: CommandIntent, parameters: Map<String, Any>) {
        totalCommands++
        
        if (intent.confidence > 0.5f) {
            successfulCommands++
        }
        
        // Update intent statistics
        intentCounts[intent.type] = intentCounts.getOrDefault(intent.type, 0) + 1
        confidenceSum[intent.type] = confidenceSum.getOrDefault(intent.type, 0f) + intent.confidence
        
        // Update most common intent
        mostCommonIntent = intentCounts.maxByOrNull { it.value }?.key ?: VoiceCommandType.CONVERSATION
        
        // Update average confidence
        averageConfidence = confidenceSum.values.sum() / totalCommands
    }

    fun reset() {
        lastContext = null
        contextSwitches = 0
    }
}

/**
 * Confidence calculation for commands
 */
class ConfidenceCalculator {
    fun calculateConfidence(
        text: String, 
        intent: CommandIntent, 
        parameters: Map<String, Any>
    ): Float {
        var confidence = intent.confidence
        
        // Boost confidence for specific parameters
        if (parameters.containsKey("club")) {
            confidence += 0.1f
        }
        
        if (parameters.containsKey("swing_phase")) {
            confidence += 0.1f
        }
        
        if (parameters.containsKey("metrics")) {
            confidence += 0.05f
        }
        
        // Penalize for very short commands
        if (text.length < 5) {
            confidence -= 0.2f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
}

/**
 * Command validation
 */
class CommandValidator {
    fun validateCommand(intent: CommandIntent, parameters: Map<String, Any>): ValidationResult {
        return when (intent.type) {
            VoiceCommandType.RECORD_SWING -> {
                if (parameters.containsKey("club") || intent.confidence > 0.7f) {
                    ValidationResult.VALID
                } else {
                    ValidationResult.NEEDS_CLARIFICATION
                }
            }
            VoiceCommandType.ANALYZE_SWING -> {
                ValidationResult.VALID
            }
            VoiceCommandType.GET_TIPS -> {
                ValidationResult.VALID
            }
            VoiceCommandType.ERROR -> {
                ValidationResult.ERROR
            }
            else -> {
                if (intent.confidence > 0.5f) {
                    ValidationResult.VALID
                } else {
                    ValidationResult.NEEDS_CLARIFICATION
                }
            }
        }
    }
}

/**
 * Enhanced Voice Command data class
 */
data class VoiceCommand(
    val type: VoiceCommandType,
    val intent: CommandIntent,
    val parameters: Map<String, Any>,
    val confidence: Float,
    val originalText: String,
    val normalizedText: String,
    val context: ConversationContext,
    val validationResult: ValidationResult
)

/**
 * Command intent with confidence
 */
data class CommandIntent(
    val type: VoiceCommandType,
    val confidence: Float
)

/**
 * Conversation context tracking
 */
data class ConversationContext(
    val lastIntent: CommandIntent? = null,
    val lastParameters: Map<String, Any> = emptyMap(),
    val conversationId: String = System.currentTimeMillis().toString(),
    val turnCount: Int = 0,
    val isContinuation: Boolean = false,
    val needsClarification: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enhanced command types
 */
enum class VoiceCommandType {
    START_PRACTICE,
    END_PRACTICE,
    RECORD_SWING,
    ANALYZE_SWING,
    GET_TIPS,
    SHOW_METRICS,
    COMPARE_SWINGS,
    SETTINGS,
    HELP,
    CONVERSATION,
    CLARIFICATION,
    CONTINUATION,
    ERROR
}

/**
 * Command validation results
 */
enum class ValidationResult {
    VALID,
    NEEDS_CLARIFICATION,
    ERROR
}

/**
 * Processing states
 */
enum class ProcessingState {
    IDLE,
    PROCESSING,
    COMPLETE,
    ERROR
}