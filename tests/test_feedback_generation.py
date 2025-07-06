import unittest
from unittest.mock import patch, MagicMock
import os
import sys

# Adjust path to import from parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from feedback_generation import (
    format_prompt,
    generate_feedback_for_fault,
    generate_swing_analysis_feedback,
    LLM_PROMPT_TEMPLATES,
    API_KEY_ENV_VAR
)
from data_structures import DetectedFault, SwingVideoAnalysisInput, LLMGeneratedTip, SwingAnalysisFeedback
from tests.test_data_factory import get_insufficient_shoulder_turn_swing_input # For swing_input context

# A sample fault for testing prompt formatting and feedback generation
SAMPLE_FAULT_SHOULDER_TURN: DetectedFault = {
    "fault_id": "INSUFFICIENT_SHOULDER_TURN_P4",
    "fault_name": "Insufficient Shoulder Turn at Top of Backswing",
    "p_positions_implicated": ["P4"],
    "description": "The amount of shoulder rotation at the top of the backswing (P4) is less than optimal.",
    "kpi_deviations": [{
        "kpi_name": "Shoulder Rotation at P4 (relative to Address)",
        "observed_value": "45.0 degrees",
        "ideal_value_or_range": "Ideal: greater than or equal to 80.0 degrees",
        "p_position": "P4"
    }],
    "llm_prompt_template_key": "INSUFFICIENT_SHOULDER_TURN_P4_PROMPT",
    "severity": 0.7
}


class TestFeedbackGenerationFormatting(unittest.TestCase):

    def test_format_prompt(self):
        swing_input = get_insufficient_shoulder_turn_swing_input() # club_used is "7-Iron" by default
        swing_input['club_used'] = "Driver" # Override for test

        prompt_template = LLM_PROMPT_TEMPLATES[SAMPLE_FAULT_SHOULDER_TURN['llm_prompt_template_key']]

        formatted = format_prompt(prompt_template, SAMPLE_FAULT_SHOULDER_TURN, swing_input)

        self.assertIn("Driver", formatted) # Check club_used
        self.assertIn("Insufficient Shoulder Turn at Top of Backswing", formatted) # fault_name
        self.assertIn("45.0 degrees", formatted) # observed_value
        self.assertIn("greater than or equal to 80.0 degrees", formatted) # ideal_range (cleaned)
        self.assertIn("P4", formatted) # p_position
        self.assertIn(SAMPLE_FAULT_SHOULDER_TURN['description'], formatted) # description


class TestFeedbackGenerationGeminiAPIMocking(unittest.TestCase):

    def setUp(self):
        # Ensure API key is set for tests that might reach the actual API call path
        # (though we mock it, some initial configuration might happen)
        self.original_api_key = os.environ.get(API_KEY_ENV_VAR)
        os.environ[API_KEY_ENV_VAR] = "TEST_API_KEY_MOCK" # Set a dummy key

        # It's important to patch where the object is *looked up*, not where it's defined.
        # feedback_generation imports genai, so we patch 'feedback_generation.genai'
        self.patcher = patch('feedback_generation.genai.GenerativeModel')
        self.mock_generative_model_class = self.patcher.start()

        # Configure the mock class to return a mock instance
        self.mock_model_instance = MagicMock()
        self.mock_generative_model_class.return_value = self.mock_model_instance

    def tearDown(self):
        self.patcher.stop()
        if self.original_api_key is not None:
            os.environ[API_KEY_ENV_VAR] = self.original_api_key
        else:
            del os.environ[API_KEY_ENV_VAR]

    def test_generate_feedback_for_fault_successful_call(self):
        swing_input = get_insufficient_shoulder_turn_swing_input()

        # Mock the response from model.generate_content()
        mock_api_response = MagicMock()
        mock_api_response.text = (
            "1. This is the explanation.\n"
            "2. This is the tip.\n"
            "3. This is the drill."
        )
        self.mock_model_instance.generate_content.return_value = mock_api_response

        result_tip = generate_feedback_for_fault(SAMPLE_FAULT_SHOULDER_TURN, swing_input)

        self.mock_generative_model_class.assert_called_once() # Check if GenerativeModel was instantiated
        self.mock_model_instance.generate_content.assert_called_once() # Check if generate_content was called

        # Verify the prompt contains expected elements (already tested more deeply in TestFeedbackGenerationFormatting)
        sent_prompt = self.mock_model_instance.generate_content.call_args[0][0]
        self.assertIn("Insufficient Shoulder Turn", sent_prompt)

        self.assertIsNotNone(result_tip)
        self.assertEqual(result_tip['explanation'], "This is the explanation.")
        self.assertEqual(result_tip['tip'], "This is the tip.")
        self.assertEqual(result_tip['drill_suggestion'], "This is the drill.")

    def test_generate_feedback_for_fault_api_error(self):
        swing_input = get_insufficient_shoulder_turn_swing_input()

        self.mock_model_instance.generate_content.side_effect = Exception("Simulated API Error")

        result_tip = generate_feedback_for_fault(SAMPLE_FAULT_SHOULDER_TURN, swing_input)

        self.assertIsNotNone(result_tip)
        self.assertIn("Error generating feedback via LLM", result_tip['explanation'])
        self.assertIn("Simulated API Error", result_tip['explanation'])


    def test_generate_feedback_no_api_key(self):
        # Temporarily remove API key for this specific test
        current_key = os.environ.pop(API_KEY_ENV_VAR, None)

        swing_input = get_insufficient_shoulder_turn_swing_input()
        result_tip = generate_feedback_for_fault(SAMPLE_FAULT_SHOULDER_TURN, swing_input)

        self.assertIsNotNone(result_tip)
        self.assertIn("Gemini API key not configured", result_tip['explanation'])
        self.mock_model_instance.generate_content.assert_not_called() # API should not be called

        # Restore key if it was there
        if current_key:
            os.environ[API_KEY_ENV_VAR] = current_key


    def test_generate_swing_analysis_feedback_orchestration(self):
        swing_input = get_insufficient_shoulder_turn_swing_input()
        detected_faults = [SAMPLE_FAULT_SHOULDER_TURN] # Only one fault for simplicity

        # Mock the response for the specific fault being processed
        mock_api_response = MagicMock()
        mock_api_response.text = "1. Explanation for shoulder turn.\n2. Tip for shoulder turn.\n3. Drill for shoulder turn."
        self.mock_model_instance.generate_content.return_value = mock_api_response

        full_feedback = generate_swing_analysis_feedback(swing_input, detected_faults)

        self.assertIsInstance(full_feedback, SwingAnalysisFeedback)
        self.assertIn("Insufficient Shoulder Turn", full_feedback['summary_of_findings'])
        self.assertEqual(len(full_feedback['detailed_feedback']), 1)

        tip_info = full_feedback['detailed_feedback'][0]
        self.assertEqual(tip_info['explanation'], "Explanation for shoulder turn.")
        self.assertEqual(tip_info['tip'], "Tip for shoulder turn.")
        self.assertEqual(tip_info['drill_suggestion'], "Drill for shoulder turn.")

        self.assertEqual(full_feedback['raw_detected_faults'], detected_faults)

    def test_generate_swing_analysis_feedback_no_faults(self):
        swing_input = get_insufficient_shoulder_turn_swing_input() # Content doesn't matter much here
        detected_faults = []

        full_feedback = generate_swing_analysis_feedback(swing_input, detected_faults)

        self.assertIn("No major faults detected", full_feedback['summary_of_findings'])
        self.assertEqual(len(full_feedback['detailed_feedback']), 0)
        self.mock_model_instance.generate_content.assert_not_called()


if __name__ == '__main__':
    unittest.main()
```
