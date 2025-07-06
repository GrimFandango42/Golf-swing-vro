"""
Module for AI-Powered Feedback Generation using the Google Gemini API.

This module takes the swing faults detected by `fault_detection.py`,
constructs detailed prompts, and interfaces with the Google Gemini API
to generate personalized coaching feedback. The feedback typically includes
an explanation of the fault, actionable tips, and suggested drills.

Key Components:
- `LLM_PROMPT_TEMPLATES`: A dictionary of prompt templates. Each template is
  designed for a specific fault type and guides the LLM to produce relevant
  and structured coaching advice. Placeholders in the templates are filled
  with specific data from the detected fault and swing analysis.
- `generate_feedback_for_fault()`: Generates feedback for a single fault by
  formatting the appropriate prompt and calling the Gemini API.
- `generate_swing_analysis_feedback()`: Orchestrates the feedback generation
  process, potentially prioritizing faults and compiling the overall feedback
  package.

API Key Management:
This module requires the `GEMINI_API_KEY` environment variable to be set
for authenticating with the Google Gemini API. If the key is not found,
API calls will be skipped, and placeholder feedback will be returned.

External Dependencies:
- `google-generativeai`: The official Google Python SDK for Gemini.
  (Install with: `pip install google-generativeai`)
"""
import os
import google.generativeai as genai
from typing import List, Dict, Optional, Any

from data_structures import (
    SwingVideoAnalysisInput,
    DetectedFault,
    LLMGeneratedTip,
    SwingAnalysisFeedback,
    # LLMFeedbackRequest # This might be used if we batch requests differently
)

# --- Environment Variable for API Key ---
# The user must set the GEMINI_API_KEY environment variable.
# Example: export GEMINI_API_KEY="YOUR_API_KEY"
API_KEY_ENV_VAR = "GEMINI_API_KEY"

# --- LLM Prompt Template Store ---
# Maps llm_prompt_template_key (from FaultDiagnosisMatrixEntry) to prompt strings.
# Placeholders like {fault_name}, {observed_value}, {ideal_range}, {club_used},
# {description}, {p_position} will be filled from the DetectedFault and SwingVideoAnalysisInput.

LLM_PROMPT_TEMPLATES: Dict[str, str] = {
    "IMPROPER_HIP_HINGE_P1_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} has an issue at address (P1).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their hip hinge angle (spine from vertical) is {observed_value}, but it should ideally be {ideal_range}.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is this specific hip hinge issue a problem for their golf swing? (max 2-3 sentences)
    2. Actionable Tip: What is one simple thought or adjustment they can make to correct this? (max 2 sentences)
    3. Drill: Describe one specific drill they can practice to feel and improve their hip hinge. (max 3-4 sentences, be very clear about setup and execution)

    Keep your tone encouraging, supportive, and professional. Avoid overly technical jargon where possible.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,

    "IMPROPER_KNEE_FLEX_P1_LEAD_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} has an issue with their lead leg at address (P1).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their lead knee flexion is {observed_value}, ideally it should be {ideal_range}.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is this specific lead knee flex issue a problem? (max 2-3 sentences)
    2. Actionable Tip: What is one simple adjustment they can make? (max 2 sentences)
    3. Drill: Describe one specific drill to practice correct lead knee flex. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,

    "IMPROPER_KNEE_FLEX_P1_TRAIL_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} has an issue with their trail leg at address (P1).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their trail knee flexion is {observed_value}, ideally it should be {ideal_range}.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is this specific trail knee flex issue a problem? (max 2-3 sentences)
    2. Actionable Tip: What is one simple adjustment they can make? (max 2 sentences)
    3. Drill: Describe one specific drill to practice correct trail knee flex. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,

    "INSUFFICIENT_SHOULDER_TURN_P4_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} has an issue at the top of their backswing (P4).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their shoulder turn is only {observed_value}, but it should be {ideal_range} for good power and sequencing.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is an insufficient shoulder turn detrimental to their swing? (max 2-3 sentences)
    2. Actionable Tip: What is one key thought or feeling to help them complete their shoulder turn? (max 2 sentences)
    3. Drill: Describe one specific drill to improve their shoulder rotation and achieve a fuller turn. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,

    "CUPPED_WRIST_P4_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} has an issue at the top of their backswing (P4).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their lead wrist angle is {observed_value}, indicating a cupped wrist. Ideally, it should be {ideal_range} (flat to slightly bowed).

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: How can a cupped lead wrist negatively affect the clubface and shot outcome (e.g., slice, loss of power)? (max 2-3 sentences)
    2. Actionable Tip: What is a simple tip or feel to help them achieve a flatter or slightly bowed lead wrist at the top? (max 2 sentences)
    3. Drill: Describe one specific drill to practice the correct lead wrist position. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,
    "IMPROPER_WEIGHT_DIST_P1_IRONS_PROMPT": """
    You are an expert golf coach. A golfer using an iron ({club_used}) has an issue with their weight distribution at address (P1).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their weight on the lead foot is estimated at {observed_value}. For irons, it should be balanced, around {ideal_range}.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is correct weight distribution at address important when using an iron? (max 2-3 sentences)
    2. Actionable Tip: What is one simple tip to help them achieve the proper 50/50 balance? (max 2 sentences)
    3. Drill: Describe one specific drill to feel the correct weight distribution for iron shots. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,
    # Placeholder for a potential Driver-specific weight distribution prompt
    # "IMPROPER_WEIGHT_DIST_P1_DRIVER_PROMPT": """... similar structure ...""",

    "HIP_SWAY_BACKSWING_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} appears to have a hip sway during their backswing (leading to P4).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their lateral hip movement away from the target is measured at {observed_value}, which is considered excessive. Ideally, hips should rotate more centrally.

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: Why is hip sway a problem, and how does it affect consistency and power? (max 2-3 sentences)
    2. Actionable Tip: What is one key thought or feeling to prevent swaying and promote rotation? (max 2 sentences)
    3. Drill: Describe one specific drill to help them feel centered rotation and reduce sway. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """,

    "REVERSE_SPINE_ANGLE_P4_PROMPT": """
    You are an expert golf coach. A golfer using a {club_used} may have a reverse spine angle at the top of their backswing (P4).
    Fault Name: {fault_name}.
    Detailed Description from Rulebook: {description}
    Observation: Their upper body tilt towards the target is measured at {observed_value}. A reverse spine angle occurs if this tilt is excessive (e.g. > 10 degrees).

    Please provide the following in a clear, easy-to-understand manner:
    1. Explanation: What is a reverse spine angle, and why is it detrimental to the swing (power, consistency, injury risk)? (max 2-3 sentences)
    2. Actionable Tip: What is one feel or setup key to help maintain proper spine tilt away from the target? (max 2 sentences)
    3. Drill: Describe one specific drill to practice maintaining the correct spine angle throughout the backswing. (max 3-4 sentences)

    Keep your tone encouraging, supportive, and professional.
    Structure your response with headings: "Explanation:", "Actionable Tip:", and "Drill:".
    """
}

# --- Gemini API Interaction ---

# Configure the Gemini API client
try:
    gemini_api_key = os.environ.get(API_KEY_ENV_VAR)
    if not gemini_api_key:
        print(f"Warning: Gemini API key not found in environment variable {API_KEY_ENV_VAR}. Feedback generation will be skipped.")
        genai.configure(api_key="DUMMY_KEY_SO_CODE_DOESNT_CRASH") # Won't work but prevents crash
    else:
        genai.configure(api_key=gemini_api_key)
except Exception as e:
    print(f"Error configuring Gemini API: {e}. Feedback generation may fail.")


# Safety settings for Gemini model
# Adjust these as needed based on the desired strictness.
SAFETY_SETTINGS = [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
]

# Generation configuration
GENERATION_CONFIG = {
    "temperature": 0.7, # Controls randomness. Lower for more deterministic, higher for more creative.
    "top_p": 1.0,
    "top_k": 32, # Consider adjusting based on desired output style
    "max_output_tokens": 512, # Max length of generated response
}


def format_prompt(
    prompt_template: str,
    fault: DetectedFault,
    swing_input: SwingVideoAnalysisInput
) -> str:
    """Fills placeholders in the prompt template with actual data."""

    # Extract relevant data, providing defaults for robustness
    kpi_dev = fault['kpi_deviations'][0] if fault['kpi_deviations'] else {}

    observed_value = kpi_dev.get('observed_value', "N/A")
    ideal_range = kpi_dev.get('ideal_value_or_range', "N/A").replace("Ideal: ", "") # Clean up "Ideal: " prefix
    p_position = kpi_dev.get('p_position', fault['p_positions_implicated'][0] if fault['p_positions_implicated'] else "N/A")

    return prompt_template.format(
        fault_name=fault.get('fault_name', "Unknown Fault"),
        description=fault.get('description', "No description."),
        observed_value=observed_value,
        ideal_range=ideal_range,
        club_used=swing_input.get('club_used', "unknown club"),
        p_position=p_position,
        # Add more placeholders as needed by templates
    )

def generate_feedback_for_fault(
    fault: DetectedFault,
    swing_input: SwingVideoAnalysisInput,
    model_name: str = "gemini-1.5-flash-latest" # Using flash for speed and cost
) -> Optional[LLMGeneratedTip]:
    """
    Generates coaching feedback for a single detected fault using the Gemini API.
    """
    if not os.environ.get(API_KEY_ENV_VAR):
        print("Skipping Gemini API call: API key not configured.")
        return LLMGeneratedTip(
            explanation="Gemini API key not configured. Feedback generation skipped.",
            tip="Please set the GEMINI_API_KEY environment variable.",
            drill_suggestion=None
        )

    prompt_template_key = fault.get('llm_prompt_template_key')
    if not prompt_template_key or prompt_template_key not in LLM_PROMPT_TEMPLATES:
        print(f"Warning: LLM prompt template key '{prompt_template_key}' not found for fault '{fault['fault_id']}'.")
        return None

    prompt_template = LLM_PROMPT_TEMPLATES[prompt_template_key]
    formatted_prompt = format_prompt(prompt_template, fault, swing_input)

    # print(f"\n--- Sending Prompt to Gemini for Fault: {fault['fault_name']} ---")
    # print(formatted_prompt)
    # print("--- End of Prompt ---")

    try:
        model = genai.GenerativeModel(
            model_name=model_name,
            safety_settings=SAFETY_SETTINGS,
            generation_config=GENERATION_CONFIG
            )
        response = model.generate_content(formatted_prompt)

        # print("\n--- Gemini Response ---")
        # print(response.text) # print(response.parts[0].text)
        # print("--- End of Gemini Response ---")

        # Basic parsing assuming response structure is: Explanation\nTip\nDrill
        # This is highly dependent on the prompt asking for this structure.
        # A more robust parsing might involve regex or asking LLM for JSON.
        parts = response.text.strip().split('\n')
        explanation = parts[0] if len(parts) > 0 else "Could not parse explanation."
        tip = parts[1] if len(parts) > 1 else "Could not parse tip."
        drill = parts[2] if len(parts) > 2 else None # Drill is optional

        # A more robust way is to look for keywords if the LLM is cooperative
        explanation_text = "Could not parse explanation."
        tip_text = "Could not parse tip."
        drill_text = None

        current_section = None
        parsed_sections = {"1": "", "2": "", "3": ""}

        for line in response.text.strip().split('\n'):
            line = line.strip()
            if not line: continue

            if line.startswith("1.") or line.lower().startswith("explanation:"):
                current_section = "1"
                parsed_sections[current_section] += line.replace("1.", "").replace("explanation:", "").strip() + " "
            elif line.startswith("2.") or line.lower().startswith("tip:"):
                current_section = "2"
                parsed_sections[current_section] += line.replace("2.", "").replace("tip:", "").strip() + " "
            elif line.startswith("3.") or line.lower().startswith("drill:"):
                current_section = "3"
                parsed_sections[current_section] += line.replace("3.", "").replace("drill:", "").strip() + " "
            elif current_section:
                parsed_sections[current_section] += line + " "

        explanation_text = parsed_sections["1"].strip() or explanation_text
        tip_text = parsed_sections["2"].strip() or tip_text
        drill_text = parsed_sections["3"].strip() or None


        return LLMGeneratedTip(
            explanation=explanation_text,
            tip=tip_text,
            drill_suggestion=drill_text
        )

    except Exception as e:
        print(f"Error calling Gemini API for fault '{fault['fault_id']}': {e}")
        if "API key not valid" in str(e):
             print("Please ensure your GEMINI_API_KEY is correct and has permissions.")
        return LLMGeneratedTip(
            explanation=f"Error generating feedback via LLM: {type(e).__name__}",
            tip="Could not connect to or parse response from the AI model.",
            drill_suggestion=None
        )


def generate_swing_analysis_feedback(
    swing_input: SwingVideoAnalysisInput,
    detected_faults: List[DetectedFault]
) -> SwingAnalysisFeedback:
    """
    Generates overall swing analysis feedback, including LLM-generated tips
    for detected faults.
    """
    llm_tips: List[LLMGeneratedTip] = []

    if not detected_faults:
        summary = "No major faults detected with the current analysis rules! Keep up the good work."
    else:
        # Simple prioritization: process the fault with the highest severity, if available.
        # Otherwise, just process the first one.
        # This avoids overwhelming the user (and API calls during testing).
        fault_to_process = None
        if detected_faults:
            sorted_faults = sorted(
                [f for f in detected_faults if f.get('severity') is not None],
                key=lambda f: f['severity'],
                reverse=True
            )
            if sorted_faults:
                fault_to_process = sorted_faults[0]
            else: # No faults with severity, take the first one
                fault_to_process = detected_faults[0]

        if fault_to_process:
            print(f"Generating feedback for primary fault: {fault_to_process['fault_name']}")
            tip = generate_feedback_for_fault(fault_to_process, swing_input)
            if tip:
                llm_tips.append(tip)
            summary = f"Found a key area for improvement: {fault_to_process['fault_name']}. See details below."
        else: # Should not happen if detected_faults is not empty
            summary = "Faults were detected, but could not select a primary one for detailed feedback."


    # In a full version, might generate a summary from LLM too, or combine tips.
    return SwingAnalysisFeedback(
        session_id=swing_input['session_id'],
        summary_of_findings=summary,
        detailed_feedback=llm_tips,
        raw_detected_faults=detected_faults,
        visualisation_annotations=None # Placeholder
    )


if __name__ == '__main__':
    # Requires FAULT_DIAGNOSIS_MATRIX and check_swing_faults from fault_detection
    # and extract_all_kpis from kpi_extraction
    from fault_detection import check_swing_faults, FAULT_DIAGNOSIS_MATRIX
    from kpi_extraction import extract_all_kpis, PLACEHOLDER_LEAD_WRIST_ANGLE_P4

    # Use the faulty swing data from fault_detection's test
    # (Assuming fault_detection.py is in the same directory or PYTHONPATH includes it)
    # To avoid circular dependency if fault_detection imports this, we might need to restructure or pass matrix

    # --- Create dummy SwingVideoAnalysisInput for testing (copied from fault_detection test) ---
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
        "session_id": "test_feedback_gen_001",
        "user_id": "test_user_feedback",
        "club_used": "Driver", # Test with Driver
        "frames": p1_frames_faulty + p4_frames_faulty,
        "p_system_classification": [
            {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 10},
            {"phase_name": "P4", "start_frame_index": 11, "end_frame_index": 20}
        ],
        "video_fps": 60.0
    }

    print("--- Testing Feedback Generation ---")
    if not os.environ.get(API_KEY_ENV_VAR):
        print(f"SKIPPING TEST: Environment variable {API_KEY_ENV_VAR} is not set.")
    else:
        print("Extracting KPIs for faulty swing...")
        kpis_from_faulty_swing = extract_all_kpis(sample_swing_faulty_input)

        # Manually add placeholder KPI for "Cupped Wrist"
        from data_structures import BiomechanicalKPI # Ensure BiomechanicalKPI is available
        placeholder_cupped_wrist_kpi = BiomechanicalKPI(
            p_position="P4", kpi_name=PLACEHOLDER_LEAD_WRIST_ANGLE_P4, value=25.0, unit="degrees",
            ideal_range=(-5.0, 5.0), notes="Placeholder KPI for cupped wrist."
        )
        kpis_from_faulty_swing.append(placeholder_cupped_wrist_kpi)

        print("Checking for faults...")
        faults = check_swing_faults(sample_swing_faulty_input, kpis_from_faulty_swing)

        if faults:
            print(f"Detected {len(faults)} faults. Generating feedback for the primary one...")
            full_feedback = generate_swing_analysis_feedback(sample_swing_faulty_input, faults)

            print("\n--- Generated Swing Analysis Feedback ---")
            print(f"Session ID: {full_feedback['session_id']}")
            print(f"Summary: {full_feedback['summary_of_findings']}")
            if full_feedback['detailed_feedback']:
                for tip_info in full_feedback['detailed_feedback']:
                    print("\n  --- Detailed Tip ---")
                    print(f"  Explanation: {tip_info['explanation']}")
                    print(f"  Tip: {tip_info['tip']}")
                    if tip_info['drill_suggestion']:
                        print(f"  Drill: {tip_info['drill_suggestion']}")
            else:
                print("  No detailed LLM feedback was generated.")

            print("\n  --- Raw Detected Faults ---")
            for i,f in enumerate(full_feedback['raw_detected_faults']):
                print(f"  Fault {i+1}: {f['fault_name']} (Severity: {f.get('severity', 'N/A')})")
                for dev in f['kpi_deviations']:
                     print(f"    - {dev['kpi_name']}: {dev['observed_value']} (Ideal: {dev['ideal_value_or_range'].replace('Ideal: ','')})")

        else:
            print("No faults detected, so no feedback to generate.")
            full_feedback = generate_swing_analysis_feedback(sample_swing_faulty_input, [])
            print(f"Summary: {full_feedback['summary_of_findings']}")

```
