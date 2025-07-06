"""
Enhanced AI-Powered Feedback Generation using Google Gemini 2.5 Flash API.

This module takes the swing faults detected by `fault_detection.py`,
constructs sophisticated prompts, and interfaces with the Google Gemini 2.5 Flash API
to generate personalized coaching feedback with real-time analysis capabilities.

Key Enhancements for Gemini 2.5 Flash:
- Advanced prompt engineering with dynamic context adaptation
- Multi-fault scenario handling with priority-based feedback
- Real-time streaming responses for faster feedback delivery
- Enhanced drill recommendations with specific progression steps
- Improved error handling and retry mechanisms
- Context-aware prompt generation based on user skill level

Key Components:
- `ENHANCED_PROMPT_TEMPLATES`: Sophisticated prompt templates leveraging Gemini 2.5's
  improved reasoning capabilities for more nuanced coaching advice
- `DynamicPromptGenerator`: Class for generating context-aware prompts based on fault
  severity, user context, and swing analysis data
- `StreamingFeedbackGenerator`: Class for handling real-time feedback generation
- `generate_multi_fault_feedback()`: Handles complex scenarios with multiple faults
- `generate_swing_analysis_feedback()`: Orchestrates enhanced feedback generation

API Key Management:
This module requires the `GEMINI_API_KEY` environment variable to be set
for authenticating with the Google Gemini 2.5 Flash API.

External Dependencies:
- `google-generativeai`: The official Google Python SDK for Gemini.
  (Install with: `pip install google-generativeai`)
"""
import os
import asyncio
import json
import time
from typing import List, Dict, Optional, Any, AsyncGenerator, Union
from dataclasses import dataclass, field
from enum import Enum
import google.generativeai as genai

from data_structures import (
    SwingVideoAnalysisInput,
    DetectedFault,
    LLMGeneratedTip,
    SwingAnalysisFeedback,
)

# --- Environment Variable for API Key ---
API_KEY_ENV_VAR = "GEMINI_API_KEY"

# --- Enhanced Configuration for Gemini 2.5 Flash ---
class FeedbackMode(Enum):
    QUICK = "quick"           # Fast, concise feedback
    DETAILED = "detailed"     # Comprehensive analysis
    STREAMING = "streaming"   # Real-time response
    MULTI_FAULT = "multi_fault"  # Multiple fault analysis

class UserSkillLevel(Enum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"
    PROFESSIONAL = "professional"

@dataclass
class FeedbackContext:
    """Enhanced context for dynamic prompt generation"""
    user_skill_level: UserSkillLevel = UserSkillLevel.INTERMEDIATE
    feedback_mode: FeedbackMode = FeedbackMode.DETAILED
    session_history: List[str] = field(default_factory=list)
    priority_focus: Optional[str] = None  # e.g., "power", "accuracy", "consistency"
    user_preferences: Dict[str, Any] = field(default_factory=dict)

# --- Enhanced Prompt Templates for Gemini 2.5 Flash ---
ENHANCED_PROMPT_TEMPLATES: Dict[str, str] = {
    "IMPROPER_HIP_HINGE_P1_PROMPT": """
    You are a world-class golf instructor with expertise in biomechanics and personalized coaching. 
    
    CONTEXT:
    - Player skill level: {skill_level}
    - Club: {club_used}
    - Fault: {fault_name}
    - P-Position: {p_position}
    
    BIOMECHANICAL ANALYSIS:
    {description}
    
    MEASUREMENT DATA:
    - Observed hip hinge angle: {observed_value}
    - Optimal range: {ideal_range}
    - Deviation severity: {severity_level}
    
    COACHING TASK:
    Provide expert analysis tailored to a {skill_level} player. Consider how this fault impacts their entire kinetic chain and shot patterns.
    
    Structure your response as follows:
    
    **ROOT CAUSE ANALYSIS:**
    Explain the biomechanical issue and its impact on the swing sequence, ball flight, and potential injury risk.
    
    **IMMEDIATE FIX:**
    One precise, actionable adjustment they can implement right now.
    
    **PROGRESSIVE DRILL SEQUENCE:**
    1. Foundation drill (static position)
    2. Dynamic movement drill
    3. Integration drill with ball
    
    **SUCCESS METRICS:**
    Specific checkpoints to measure improvement.
    
    **COMMON MISTAKES TO AVOID:**
    Anticipate and address typical overcorrections.
    
    Adapt your technical language and detail level to match a {skill_level} player's understanding.
    """,

    "IMPROPER_KNEE_FLEX_P1_LEAD_PROMPT": """
    You are an elite golf performance coach specializing in setup fundamentals and athletic posture.
    
    PLAYER PROFILE:
    - Skill Level: {skill_level}
    - Equipment: {club_used}
    - Primary Issue: {fault_name} at {p_position}
    
    TECHNICAL ASSESSMENT:
    {description}
    
    MEASUREMENT ANALYSIS:
    - Current lead knee flexion: {observed_value}
    - Target range: {ideal_range}
    - Impact on stability: {severity_assessment}
    
    EXPERT COACHING RESPONSE:
    
    **ATHLETIC FOUNDATION:**
    Explain how proper knee flexion creates a stable, powerful platform for the swing.
    
    **PRECISION ADJUSTMENT:**
    Specific body positioning cue that creates immediate improvement.
    
    **MOTOR LEARNING SEQUENCE:**
    1. Awareness drill (feel the difference)
    2. Stability challenge drill
    3. Dynamic transition drill
    4. Pressure pattern validation
    
    **PERFORMANCE IMPACT:**
    How this correction affects power transfer, balance, and shot consistency.
    
    **INTEGRATION STRATEGY:**
    How to maintain this position throughout the swing phases.
    
    Customize complexity and terminology for {skill_level} understanding.
    """,

    "IMPROPER_KNEE_FLEX_P1_TRAIL_PROMPT": """
    You are a master golf instructor with deep expertise in athletic posture and swing mechanics.
    
    ASSESSMENT PARAMETERS:
    - Player Level: {skill_level}
    - Club Selection: {club_used}
    - Technical Fault: {fault_name}
    - Swing Phase: {p_position}
    
    BIOMECHANICAL FINDINGS:
    {description}
    
    QUANTITATIVE DATA:
    - Trail knee flexion measurement: {observed_value}
    - Optimal positioning: {ideal_range}
    - Compensatory patterns detected: {related_issues}
    
    COMPREHENSIVE COACHING PLAN:
    
    **KINETIC CHAIN ANALYSIS:**
    Detail how trail leg positioning affects hip rotation, weight transfer, and swing plane.
    
    **IMMEDIATE CORRECTION:**
    One powerful setup adjustment with instant feedback mechanism.
    
    **SYSTEMATIC IMPROVEMENT PROTOCOL:**
    1. Postural awareness drill
    2. Dynamic loading pattern drill
    3. Rotation efficiency drill
    4. Power transfer validation
    
    **COMPENSATION PREVENTION:**
    Address likely adjustments that could create new problems.
    
    **LONG-TERM DEVELOPMENT:**
    Progressive challenges to maintain improvement under pressure.
    
    Match your instruction style to {skill_level} learning preferences and technical capacity.
    """,

    "INSUFFICIENT_SHOULDER_TURN_P4_PROMPT": """
    You are a renowned swing coach specializing in power generation and rotational mechanics.
    
    SWING ANALYSIS CONTEXT:
    - Player Expertise: {skill_level}
    - Club: {club_used}
    - Critical Issue: {fault_name}
    - Backswing Phase: {p_position}
    
    ROTATIONAL MECHANICS ASSESSMENT:
    {description}
    
    TURN EFFICIENCY DATA:
    - Current shoulder rotation: {observed_value}
    - Power-optimal range: {ideal_range}
    - Energy storage deficit: {power_loss_estimate}
    
    ADVANCED COACHING INTERVENTION:
    
    **POWER PHYSICS:**
    Explain the relationship between shoulder turn, elastic energy storage, and clubhead speed.
    
    **BREAKTHROUGH TECHNIQUE:**
    One transformative feel or trigger that unlocks fuller rotation.
    
    **MULTI-PHASE TRAINING SYSTEM:**
    1. Mobility assessment and preparation
    2. Turn amplitude training (no club)
    3. Loaded turn with resistance
    4. Speed development drill
    5. Pressure and timing integration
    
    **INDIVIDUAL LIMITATIONS ANALYSIS:**
    Address potential physical restrictions and workarounds.
    
    **PERFORMANCE METRICS:**
    Measurable improvements in distance, accuracy, and consistency.
    
    **ADVANCED CONCEPTS:**
    {skill_level}-appropriate discussion of X-factor, dynamic loading, and sequence optimization.
    
    Calibrate technical depth to {skill_level} comprehension and goals.
    """,

    "CUPPED_WRIST_P4_PROMPT": """
    You are an expert golf instructor and clubface control specialist with deep knowledge of impact dynamics.
    
    TECHNICAL PROFILE:
    - Student Level: {skill_level}
    - Equipment: {club_used}
    - Primary Concern: {fault_name}
    - Critical Position: {p_position}
    
    CLUBFACE CONTROL ANALYSIS:
    {description}
    
    WRIST POSITION DATA:
    - Lead wrist angle: {observed_value}
    - Neutral to strong range: {ideal_range}
    - Clubface deviation: {face_angle_impact}
    
    MASTER CLASS INSTRUCTION:
    
    **IMPACT DYNAMICS:**
    Detailed explanation of how wrist position controls clubface, attack angle, and ball flight laws.
    
    **BREAKTHROUGH ADJUSTMENT:**
    Powerful grip and wrist position cue that creates immediate clubface control.
    
    **PROGRESSIVE MASTERY SEQUENCE:**
    1. Static position training with feedback
    2. Slow-motion rehearsal drill
    3. Impact bag training
    4. Ball striking validation
    5. Pressure situation testing
    
    **SHOT PATTERN TRANSFORMATION:**
    Specific improvements in ball flight, distance control, and shot shaping ability.
    
    **MAINTENANCE PROTOCOL:**
    Daily exercises to reinforce proper wrist action and prevent regression.
    
    **ADVANCED APPLICATIONS:**
    {skill_level}-specific discussion of shaft lean, compression, and shot creativity.
    
    Tailor instruction complexity and metaphors to {skill_level} experience and learning style.
    """,

    "IMPROPER_WEIGHT_DIST_P1_IRONS_PROMPT": """
    You are a precision golf instructor specializing in setup fundamentals and ball-striking excellence.
    
    SETUP ANALYSIS FRAMEWORK:
    - Player Classification: {skill_level}
    - Iron Selection: {club_used}
    - Balance Issue: {fault_name}
    - Address Position: {p_position}
    
    WEIGHT DISTRIBUTION SCIENCE:
    {description}
    
    PRESSURE MAPPING DATA:
    - Current weight distribution: {observed_value}
    - Optimal balance point: {ideal_range}
    - Stability index: {stability_rating}
    
    EXPERT BALANCE COACHING:
    
    **ATHLETIC FOUNDATION:**
    Explain how proper weight distribution creates consistent impact conditions and shot control.
    
    **INSTANT CALIBRATION:**
    Precise setup adjustment with immediate balance feedback.
    
    **BALANCE MASTERY PROGRESSION:**
    1. Static pressure awareness drill
    2. Dynamic stability challenge
    3. Transition timing drill
    4. Impact efficiency validation
    5. Course condition adaptation
    
    **IMPACT OPTIMIZATION:**
    How proper setup balance creates descending blow, clean contact, and distance control.
    
    **ENVIRONMENTAL ADAPTATION:**
    Adjustments for different lies, slopes, and course conditions.
    
    **CONSISTENCY PROTOCOLS:**
    Pre-shot routine elements that ensure repeatable setup balance.
    
    Adapt instruction detail and metaphors to {skill_level} understanding and playing goals.
    """,

    "HIP_SWAY_BACKSWING_PROMPT": """
    You are a master golf instructor and movement specialist focusing on swing efficiency and power transfer.
    
    MOVEMENT ANALYSIS CONTEXT:
    - Student Profile: {skill_level}
    - Club: {club_used}
    - Movement Fault: {fault_name}
    - Backswing Dynamics: Movement from P1 to P4
    
    KINEMATIC ASSESSMENT:
    {description}
    
    LATERAL MOVEMENT DATA:
    - Hip displacement: {observed_value}
    - Efficient rotation threshold: {ideal_range}
    - Power loss coefficient: {efficiency_rating}
    
    MOVEMENT CORRECTION MASTERY:
    
    **BIOMECHANICAL FOUNDATION:**
    Explain how hip sway disrupts the kinetic chain, reduces coil, and affects timing.
    
    **TRANSFORMATION KEY:**
    One powerful movement pattern that eliminates sway and maximizes rotation.
    
    **MOTOR PATTERN RECONSTRUCTION:**
    1. Center awareness training
    2. Rotational isolation drill
    3. Resistance training for stability
    4. Dynamic coil development
    5. Speed and power integration
    
    **ATHLETIC PERFORMANCE GAINS:**
    Improvements in power, accuracy, and swing consistency from proper hip action.
    
    **PRESSURE PATTERN OPTIMIZATION:**
    Ground force utilization for maximum energy transfer.
    
    **COURSE APPLICATION:**
    Maintaining centered rotation under competitive pressure and varying conditions.
    
    Scale technical complexity and training intensity to {skill_level} physical capacity and goals.
    """,

    "REVERSE_SPINE_ANGLE_P4_PROMPT": """
    You are an elite golf instructor and sports medicine specialist with expertise in safe, powerful swing mechanics.
    
    POSTURAL ANALYSIS FRAMEWORK:
    - Player Development Level: {skill_level}
    - Equipment: {club_used}
    - Postural Fault: {fault_name}
    - Critical Position: {p_position}
    
    SPINE MECHANICS EVALUATION:
    {description}
    
    POSTURAL MEASUREMENT:
    - Spine angle deviation: {observed_value}
    - Safe, powerful range: {ideal_range}
    - Injury risk assessment: {safety_rating}
    
    COMPREHENSIVE SPINE ANGLE CORRECTION:
    
    **SAFETY AND PERFORMANCE:**
    Detailed explanation of injury risks and performance limitations from reverse spine angle.
    
    **CORE CORRECTION:**
    Fundamental postural adjustment that creates safe, powerful positions.
    
    **SYSTEMATIC REHABILITATION:**
    1. Postural awareness and mobility
    2. Core stability training
    3. Loaded position practice
    4. Dynamic movement integration
    5. Strength and conditioning support
    
    **PERFORMANCE TRANSFORMATION:**
    How proper spine angle improves power transfer, consistency, and longevity.
    
    **INJURY PREVENTION:**
    Long-term strategies for maintaining healthy swing mechanics.
    
    **ADVANCED BIOMECHANICS:**
    {skill_level}-appropriate discussion of spinal loading, rotational stress, and athletic positioning.
    
    Customize rehabilitation approach and progression to {skill_level} physical condition and training commitment.
    """
}

# --- Multi-Fault Analysis Prompt ---
MULTI_FAULT_ANALYSIS_PROMPT = """
You are a master golf instructor analyzing a complex swing pattern with multiple technical issues.

COMPREHENSIVE SWING ASSESSMENT:
Player Level: {skill_level}
Club: {club_used}
Number of detected faults: {fault_count}

FAULT PRIORITY MATRIX:
{fault_details}

EXPERT MULTI-FAULT STRATEGY:

**ROOT CAUSE HIERARCHY:**
Identify the primary fault driving the compensatory patterns.

**SEQUENTIAL CORRECTION PLAN:**
1. Foundation fix (address the root cause first)
2. Progressive corrections (logical order of improvement)
3. Integration phase (combining corrections)

**PRACTICE PRIORITIES:**
Which elements to focus on first for maximum improvement with minimal confusion.

**EXPECTED PROGRESSION:**
Timeline and milestones for systematic improvement.

**COMPENSATION MANAGEMENT:**
How to prevent new faults while correcting existing ones.

Provide clear priorities suitable for {skill_level} player development approach.
"""

# --- Dynamic Prompt Generator ---
class DynamicPromptGenerator:
    """Generates context-aware prompts based on fault severity and user context"""
    
    def __init__(self, context: FeedbackContext):
        self.context = context
    
    def assess_severity_level(self, fault: DetectedFault) -> str:
        """Assess fault severity for prompt customization"""
        severity = fault.get('severity', 0)
        if severity >= 8:
            return "critical"
        elif severity >= 6:
            return "significant"
        elif severity >= 4:
            return "moderate"
        else:
            return "minor"
    
    def generate_context_variables(self, fault: DetectedFault, swing_input: SwingVideoAnalysisInput) -> Dict[str, str]:
        """Generate enhanced context variables for prompt formatting"""
        kpi_dev = fault['kpi_deviations'][0] if fault['kpi_deviations'] else {}
        
        base_vars = {
            'fault_name': fault.get('fault_name', "Unknown Fault"),
            'description': fault.get('description', "No description available."),
            'observed_value': kpi_dev.get('observed_value', "N/A"),
            'ideal_range': kpi_dev.get('ideal_value_or_range', "N/A").replace("Ideal: ", ""),
            'club_used': swing_input.get('club_used', "unknown club"),
            'p_position': kpi_dev.get('p_position', fault['p_positions_implicated'][0] if fault['p_positions_implicated'] else "N/A"),
            'skill_level': self.context.user_skill_level.value,
        }
        
        # Enhanced context variables
        severity_level = self.assess_severity_level(fault)
        base_vars.update({
            'severity_level': severity_level,
            'severity_assessment': f"{severity_level} impact on swing performance",
            'related_issues': self._identify_related_issues(fault),
            'power_loss_estimate': self._estimate_power_impact(fault),
            'face_angle_impact': self._estimate_clubface_impact(fault),
            'stability_rating': self._assess_stability_impact(fault),
            'efficiency_rating': self._assess_efficiency_impact(fault),
            'safety_rating': self._assess_safety_impact(fault),
        })
        
        return base_vars
    
    def _identify_related_issues(self, fault: DetectedFault) -> str:
        """Identify potential compensatory patterns"""
        fault_name = fault.get('fault_name', '').lower()
        if 'hip' in fault_name:
            return "potential shoulder compensation, weight shift issues"
        elif 'knee' in fault_name:
            return "possible hip mobility restrictions, ankle stability"
        elif 'shoulder' in fault_name:
            return "likely arm swing compensation, timing issues"
        elif 'wrist' in fault_name:
            return "grip pressure problems, impact control issues"
        else:
            return "secondary swing adaptations likely"
    
    def _estimate_power_impact(self, fault: DetectedFault) -> str:
        """Estimate power loss from fault"""
        severity = fault.get('severity', 0)
        if severity >= 8:
            return "15-25% power reduction"
        elif severity >= 6:
            return "10-15% power reduction"
        elif severity >= 4:
            return "5-10% power reduction"
        else:
            return "minimal power impact"
    
    def _estimate_clubface_impact(self, fault: DetectedFault) -> str:
        """Estimate clubface control impact"""
        fault_name = fault.get('fault_name', '').lower()
        if 'wrist' in fault_name or 'cupped' in fault_name:
            return "3-8 degrees open tendency"
        elif 'grip' in fault_name:
            return "inconsistent face control"
        else:
            return "indirect face control effects"
    
    def _assess_stability_impact(self, fault: DetectedFault) -> str:
        """Assess impact on swing stability"""
        fault_name = fault.get('fault_name', '').lower()
        if any(word in fault_name for word in ['knee', 'hip', 'weight']):
            return "moderate to high stability compromise"
        else:
            return "minimal stability impact"
    
    def _assess_efficiency_impact(self, fault: DetectedFault) -> str:
        """Assess impact on swing efficiency"""
        severity = fault.get('severity', 0)
        return f"{min(severity * 10, 90)}% efficiency retention"
    
    def _assess_safety_impact(self, fault: DetectedFault) -> str:
        """Assess injury risk impact"""
        fault_name = fault.get('fault_name', '').lower()
        if 'reverse' in fault_name or 'spine' in fault_name:
            return "elevated injury risk - priority correction"
        elif 'sway' in fault_name:
            return "moderate back stress risk"
        else:
            return "low injury risk"

# --- Enhanced Gemini API Configuration ---
SAFETY_SETTINGS = [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
]

# Enhanced generation config for Gemini 2.5 Flash
GENERATION_CONFIG = {
    "temperature": 0.3,  # Lower for more consistent coaching advice
    "top_p": 0.95,
    "top_k": 40,
    "max_output_tokens": 1024,  # Increased for detailed feedback
}

# Streaming configuration
STREAMING_CONFIG = {
    "temperature": 0.4,
    "top_p": 0.9,
    "top_k": 32,
    "max_output_tokens": 512,
}

# --- Enhanced API Setup ---
try:
    gemini_api_key = os.environ.get(API_KEY_ENV_VAR)
    if not gemini_api_key:
        print(f"Warning: Gemini API key not found in environment variable {API_KEY_ENV_VAR}. Feedback generation will be skipped.")
        genai.configure(api_key="DUMMY_KEY_SO_CODE_DOESNT_CRASH")
    else:
        genai.configure(api_key=gemini_api_key)
        print("Gemini 2.5 Flash API configured successfully")
except Exception as e:
    print(f"Error configuring Gemini API: {e}. Feedback generation may fail.")

# --- Streaming Feedback Generator ---
class StreamingFeedbackGenerator:
    """Handles real-time streaming feedback generation"""
    
    def __init__(self, model_name: str = "gemini-2.5-flash-latest"):
        self.model_name = model_name
        self.model = None
        self._initialize_model()
    
    def _initialize_model(self):
        """Initialize the Gemini model with enhanced settings"""
        try:
            self.model = genai.GenerativeModel(
                model_name=self.model_name,
                safety_settings=SAFETY_SETTINGS,
                generation_config=STREAMING_CONFIG
            )
        except Exception as e:
            print(f"Error initializing streaming model: {e}")
    
    async def generate_streaming_feedback(
        self, 
        prompt: str, 
        callback: Optional[callable] = None
    ) -> AsyncGenerator[str, None]:
        """Generate streaming feedback with real-time updates"""
        if not self.model:
            yield "Error: Streaming model not initialized"
            return
        
        try:
            response = await self.model.generate_content_async(
                prompt,
                stream=True
            )
            
            accumulated_text = ""
            async for chunk in response:
                if chunk.text:
                    accumulated_text += chunk.text
                    if callback:
                        await callback(chunk.text)
                    yield chunk.text
                    
        except Exception as e:
            yield f"Streaming error: {str(e)}"

# --- Enhanced Feedback Generation Functions ---

def format_enhanced_prompt(
    prompt_template: str,
    fault: DetectedFault,
    swing_input: SwingVideoAnalysisInput,
    context: FeedbackContext
) -> str:
    """Enhanced prompt formatting with dynamic context variables"""
    generator = DynamicPromptGenerator(context)
    context_vars = generator.generate_context_variables(fault, swing_input)
    
    try:
        return prompt_template.format(**context_vars)
    except KeyError as e:
        print(f"Warning: Missing template variable {e}. Using fallback formatting.")
        # Fallback to basic formatting for compatibility
        return prompt_template.format(
            fault_name=fault.get('fault_name', "Unknown Fault"),
            description=fault.get('description', "No description available."),
            observed_value=context_vars.get('observed_value', "N/A"),
            ideal_range=context_vars.get('ideal_range', "N/A"),
            club_used=swing_input.get('club_used', "unknown club"),
            p_position=context_vars.get('p_position', "N/A"),
            skill_level=context.user_skill_level.value
        )

def parse_enhanced_response(response_text: str) -> LLMGeneratedTip:
    """Enhanced response parsing for structured feedback"""
    sections = {
        'explanation': '',
        'tip': '',
        'drill': ''
    }
    
    current_section = None
    lines = response_text.strip().split('\n')
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # Enhanced section detection
        line_lower = line.lower()
        if any(keyword in line_lower for keyword in ['root cause', 'analysis', 'explanation', 'foundation', 'biomechanical']):
            current_section = 'explanation'
            sections[current_section] += line.replace('**', '').replace('*', '') + ' '
        elif any(keyword in line_lower for keyword in ['immediate', 'fix', 'tip', 'adjustment', 'breakthrough']):
            current_section = 'tip'
            sections[current_section] += line.replace('**', '').replace('*', '') + ' '
        elif any(keyword in line_lower for keyword in ['drill', 'exercise', 'practice', 'training', 'sequence']):
            current_section = 'drill'
            sections[current_section] += line.replace('**', '').replace('*', '') + ' '
        elif current_section:
            sections[current_section] += line + ' '
    
    # Fallback parsing if structured parsing fails
    if not any(sections.values()):
        parts = response_text.split('\n\n')
        sections['explanation'] = parts[0] if len(parts) > 0 else "Analysis not available"
        sections['tip'] = parts[1] if len(parts) > 1 else "Tip not available"
        sections['drill'] = parts[2] if len(parts) > 2 else None
    
    return LLMGeneratedTip(
        explanation=sections['explanation'].strip() or "Analysis not available",
        tip=sections['tip'].strip() or "Tip not available",
        drill_suggestion=sections['drill'].strip() if sections['drill'].strip() else None
    )

def generate_feedback_for_fault(
    fault: DetectedFault,
    swing_input: SwingVideoAnalysisInput,
    context: Optional[FeedbackContext] = None,
    model_name: str = "gemini-2.5-flash-latest"  # Updated to Gemini 2.5 Flash
) -> Optional[LLMGeneratedTip]:
    """
    Enhanced feedback generation for a single fault using Gemini 2.5 Flash
    """
    if not os.environ.get(API_KEY_ENV_VAR):
        print("Skipping Gemini API call: API key not configured.")
        return LLMGeneratedTip(
            explanation="Gemini API key not configured. Feedback generation skipped.",
            tip="Please set the GEMINI_API_KEY environment variable.",
            drill_suggestion=None
        )
    
    if context is None:
        context = FeedbackContext()
    
    prompt_template_key = fault.get('llm_prompt_template_key')
    if not prompt_template_key or prompt_template_key not in ENHANCED_PROMPT_TEMPLATES:
        print(f"Warning: Enhanced prompt template key '{prompt_template_key}' not found for fault '{fault['fault_id']}'.")
        return None
    
    prompt_template = ENHANCED_PROMPT_TEMPLATES[prompt_template_key]
    formatted_prompt = format_enhanced_prompt(prompt_template, fault, swing_input, context)
    
    # Enhanced retry mechanism
    max_retries = 3
    retry_delay = 1
    
    for attempt in range(max_retries):
        try:
            model = genai.GenerativeModel(
                model_name=model_name,
                safety_settings=SAFETY_SETTINGS,
                generation_config=GENERATION_CONFIG
            )
            
            response = model.generate_content(formatted_prompt)
            
            if response.text:
                return parse_enhanced_response(response.text)
            else:
                print(f"Empty response from Gemini 2.5 Flash on attempt {attempt + 1}")
                
        except Exception as e:
            print(f"Error calling Gemini 2.5 Flash API on attempt {attempt + 1}: {e}")
            if attempt < max_retries - 1:
                time.sleep(retry_delay)
                retry_delay *= 2  # Exponential backoff
            
            if "quota exceeded" in str(e).lower():
                return LLMGeneratedTip(
                    explanation="API quota exceeded. Please try again later.",
                    tip="Consider upgrading your Gemini API plan for higher usage limits.",
                    drill_suggestion=None
                )
    
    return LLMGeneratedTip(
        explanation=f"Failed to generate feedback after {max_retries} attempts",
        tip="Please check your internet connection and API configuration.",
        drill_suggestion=None
    )

def generate_multi_fault_feedback(
    faults: List[DetectedFault],
    swing_input: SwingVideoAnalysisInput,
    context: Optional[FeedbackContext] = None,
    model_name: str = "gemini-2.5-flash-latest"
) -> Optional[LLMGeneratedTip]:
    """
    Generate comprehensive feedback for multiple faults using advanced analysis
    """
    if not faults or not os.environ.get(API_KEY_ENV_VAR):
        return None
    
    if context is None:
        context = FeedbackContext()
    
    # Prepare fault details for multi-fault analysis
    fault_details = []
    for i, fault in enumerate(faults, 1):
        severity = fault.get('severity', 0)
        kpi_dev = fault['kpi_deviations'][0] if fault['kpi_deviations'] else {}
        
        fault_detail = f"""
        {i}. {fault.get('fault_name', 'Unknown')} (Severity: {severity}/10)
           Position: {kpi_dev.get('p_position', 'N/A')}
           Measurement: {kpi_dev.get('observed_value', 'N/A')} vs Ideal: {kpi_dev.get('ideal_value_or_range', 'N/A')}
           Impact: {fault.get('description', 'No description')[:100]}...
        """
        fault_details.append(fault_detail)
    
    prompt = MULTI_FAULT_ANALYSIS_PROMPT.format(
        skill_level=context.user_skill_level.value,
        club_used=swing_input.get('club_used', 'unknown club'),
        fault_count=len(faults),
        fault_details='\n'.join(fault_details)
    )
    
    try:
        model = genai.GenerativeModel(
            model_name=model_name,
            safety_settings=SAFETY_SETTINGS,
            generation_config=GENERATION_CONFIG
        )
        
        response = model.generate_content(prompt)
        return parse_enhanced_response(response.text)
        
    except Exception as e:
        print(f"Error generating multi-fault feedback: {e}")
        return None

def generate_swing_analysis_feedback(
    swing_input: SwingVideoAnalysisInput,
    detected_faults: List[DetectedFault],
    context: Optional[FeedbackContext] = None
) -> SwingAnalysisFeedback:
    """
    Enhanced swing analysis feedback generation with Gemini 2.5 Flash
    """
    if context is None:
        context = FeedbackContext()
    
    llm_tips: List[LLMGeneratedTip] = []
    
    if not detected_faults:
        summary = "Excellent swing mechanics detected! Your fundamentals are solid. Continue to focus on consistency and fine-tuning."
    else:
        # Enhanced fault prioritization
        prioritized_faults = sorted(
            [f for f in detected_faults if f.get('severity') is not None],
            key=lambda f: f['severity'],
            reverse=True
        )
        
        if not prioritized_faults:
            prioritized_faults = detected_faults
        
        # Multi-fault handling based on context mode
        if context.feedback_mode == FeedbackMode.MULTI_FAULT and len(prioritized_faults) > 1:
            # Generate comprehensive multi-fault analysis
            multi_fault_tip = generate_multi_fault_feedback(
                prioritized_faults[:3],  # Limit to top 3 faults
                swing_input,
                context
            )
            if multi_fault_tip:
                llm_tips.append(multi_fault_tip)
            
            summary = f"Comprehensive analysis of {len(prioritized_faults)} swing elements. Focus on the systematic approach outlined below."
            
        else:
            # Single fault detailed analysis
            primary_fault = prioritized_faults[0]
            print(f"Generating enhanced feedback for primary fault: {primary_fault['fault_name']}")
            
            tip = generate_feedback_for_fault(primary_fault, swing_input, context)
            if tip:
                llm_tips.append(tip)
            
            severity_text = "critical" if primary_fault.get('severity', 0) >= 8 else "key"
            summary = f"Identified a {severity_text} area for improvement: {primary_fault['fault_name']}. Enhanced coaching guidance provided below."
    
    return SwingAnalysisFeedback(
        session_id=swing_input['session_id'],
        summary_of_findings=summary,
        detailed_feedback=llm_tips,
        raw_detected_faults=detected_faults,
        visualisation_annotations=None
    )

# --- Real-time Analysis Support ---
async def generate_realtime_feedback(
    swing_input: SwingVideoAnalysisInput,
    detected_faults: List[DetectedFault],
    callback: Optional[callable] = None
) -> SwingAnalysisFeedback:
    """
    Generate real-time streaming feedback for immediate coaching
    """
    if not detected_faults:
        return generate_swing_analysis_feedback(swing_input, detected_faults)
    
    context = FeedbackContext(feedback_mode=FeedbackMode.STREAMING)
    streaming_generator = StreamingFeedbackGenerator()
    
    primary_fault = max(detected_faults, key=lambda f: f.get('severity', 0))
    prompt_template_key = primary_fault.get('llm_prompt_template_key')
    
    if prompt_template_key and prompt_template_key in ENHANCED_PROMPT_TEMPLATES:
        prompt_template = ENHANCED_PROMPT_TEMPLATES[prompt_template_key]
        formatted_prompt = format_enhanced_prompt(prompt_template, primary_fault, swing_input, context)
        
        # Collect streaming response
        full_response = ""
        async for chunk in streaming_generator.generate_streaming_feedback(formatted_prompt, callback):
            full_response += chunk
        
        tip = parse_enhanced_response(full_response)
        
        return SwingAnalysisFeedback(
            session_id=swing_input['session_id'],
            summary_of_findings=f"Real-time analysis: {primary_fault['fault_name']}",
            detailed_feedback=[tip] if tip else [],
            raw_detected_faults=detected_faults,
            visualisation_annotations=None
        )
    
    # Fallback to standard generation
    return generate_swing_analysis_feedback(swing_input, detected_faults, context)

if __name__ == '__main__':
    # Enhanced testing with Gemini 2.5 Flash
    from fault_detection import check_swing_faults, FAULT_DIAGNOSIS_MATRIX
    from kpi_extraction import extract_all_kpis, PLACEHOLDER_LEAD_WRIST_ANGLE_P4
    
    # Create test data (same as before for compatibility)
    def _make_kp(x,y,z): return {"x":x, "y":y, "z":z, "visibility":1.0}
    
    p1_frames_faulty = []
    for _ in range(11):
        frame_data: Dict[str, Any] = {
            "left_shoulder": _make_kp(-0.2, 1.4, -0.3),
            "right_shoulder": _make_kp(0.2, 1.4, -0.3),
            "left_hip": _make_kp(-0.15, 0.9, 0), "right_hip": _make_kp(0.15, 0.9, 0),
            "left_knee": _make_kp(-0.18, 0.4, 0.05), "right_knee": _make_kp(0.18, 0.45, 0),
            "left_ankle": _make_kp(-0.25, 0.1, 0), "right_ankle": _make_kp(0.15, 0.1, 0),
        }
        p1_frames_faulty.append(frame_data)
    
    p4_frames_faulty = []
    for _ in range(10):
        frame_data_p4: Dict[str, Any] = {
            "left_shoulder": _make_kp(-0.07, 1.4, -0.19), "right_shoulder": _make_kp(0.07, 1.4, 0.19),
            "left_hip": _make_kp(-0.1, 0.9, -0.08), "right_hip": _make_kp(0.1, 0.9, -0.08),
            "left_knee": _make_kp(-0.18, 0.5, 0), "right_knee": _make_kp(0.18, 0.5, 0),
            "left_ankle": _make_kp(-0.2, 0.1, 0), "right_ankle": _make_kp(0.2, 0.1, 0),
            "left_wrist": _make_kp(-0.3, 1.5, -0.4),
        }
        p4_frames_faulty.append(frame_data_p4)

    sample_swing_faulty_input: SwingVideoAnalysisInput = {
        "session_id": "enhanced_feedback_test_001",
        "user_id": "test_user_enhanced",
        "club_used": "Driver",
        "frames": p1_frames_faulty + p4_frames_faulty,
        "p_system_classification": [
            {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
            {"phase_name": "P4", "start_frame_index": 11, "end_frame_index": 20}
        ],
        "video_fps": 60.0
    }

    print("--- Testing Enhanced Feedback Generation with Gemini 2.5 Flash ---")
    if not os.environ.get(API_KEY_ENV_VAR):
        print(f"SKIPPING TEST: Environment variable {API_KEY_ENV_VAR} is not set.")
    else:
        print("Extracting KPIs for enhanced analysis...")
        kpis_from_faulty_swing = extract_all_kpis(sample_swing_faulty_input)
        
        # Add placeholder KPI for testing
        from data_structures import BiomechanicalKPI
        placeholder_cupped_wrist_kpi = BiomechanicalKPI(
            p_position="P4", kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4, value=25.0, unit="degrees",
            ideal_range=(-5.0, 5.0), notes="Placeholder KPI for enhanced testing."
        )
        kpis_from_faulty_swing.append(placeholder_cupped_wrist_kpi)

        print("Checking for faults with enhanced analysis...")
        faults = check_swing_faults(sample_swing_faulty_input, kpis_from_faulty_swing)

        if faults:
            print(f"Detected {len(faults)} faults. Testing enhanced feedback modes...")
            
            # Test different feedback contexts
            contexts = [
                FeedbackContext(user_skill_level=UserSkillLevel.INTERMEDIATE, feedback_mode=FeedbackMode.DETAILED),
                FeedbackContext(user_skill_level=UserSkillLevel.ADVANCED, feedback_mode=FeedbackMode.MULTI_FAULT)
            ]
            
            for i, context in enumerate(contexts):
                print(f"\n--- Enhanced Feedback Test {i+1}: {context.user_skill_level.value} / {context.feedback_mode.value} ---")
                
                full_feedback = generate_swing_analysis_feedback(sample_swing_faulty_input, faults, context)
                
                print(f"Session ID: {full_feedback['session_id']}")
                print(f"Enhanced Summary: {full_feedback['summary_of_findings']}")
                
                if full_feedback['detailed_feedback']:
                    for j, tip_info in enumerate(full_feedback['detailed_feedback']):
                        print(f"\n  --- Enhanced Coaching Tip {j+1} ---")
                        print(f"  Analysis: {tip_info['explanation'][:200]}...")
                        print(f"  Action: {tip_info['tip'][:150]}...")
                        if tip_info['drill_suggestion']:
                            print(f"  Training: {tip_info['drill_suggestion'][:150]}...")
                else:
                    print("  No detailed feedback generated.")

        else:
            print("No faults detected for enhanced testing.")