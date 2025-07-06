"""
Main FastAPI application for the SwingSync AI backend.

This module sets up and runs the FastAPI web server, providing an API endpoint
to receive golf swing data, process it through the analysis pipeline (KPI extraction,
fault detection, and AI feedback generation), and return the comprehensive analysis.

Key Features:
- **API Endpoint (`/analyze_swing/`):** Accepts POST requests with swing data
  (pose keypoints, P-System classification, etc.) conforming to the
  `SwingVideoAnalysisInputModel`.
- **Data Validation:** Uses Pydantic models for automatic request and response
  data validation and serialization.
- **Analysis Pipeline Orchestration:** Calls functions from other modules
  (`kpi_extraction`, `fault_detection`, `feedback_generation`) to perform
  the full swing analysis.
- **Automatic API Documentation:** Provides interactive API documentation
  (Swagger UI at `/docs` and ReDoc at `/redoc`) for easy testing and integration.

Dependencies:
- `fastapi`: For building the API.
- `uvicorn`: For running the ASGI server.
- Project-specific modules: `data_structures`, `kpi_extraction`, `fault_detection`,
  `feedback_generation`.

Environment Variables:
- `GEMINI_API_KEY`: Required by `feedback_generation.py` for interacting with the
  Google Gemini API. Must be set in the environment where the FastAPI server runs.
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any

# Import core logic modules
from data_structures import (
    # SwingVideoAnalysisInput as SwingVideoAnalysisInputTypedDict, # Keep for reference
    PoseKeypoint as PoseKeypointTypedDict,
    FramePoseData as FramePoseDataTypedDict,
    PSystemPhase as PSystemPhaseTypedDict,
    SwingAnalysisFeedback, # This is already a TypedDict, FastAPI handles it for response
    LLMGeneratedTip, # Also a TypedDict for response
    DetectedFault as DetectedFaultTypedDict # Also for response
)
from kpi_extraction import extract_all_kpis
from fault_detection import check_swing_faults
from feedback_generation import generate_swing_analysis_feedback

# --- Pydantic Models for Request Validation ---
# These mirror the TypedDicts from data_structures.py for FastAPI's validation

class PoseKeypointModel(BaseModel):
    x: float
    y: float
    z: float
    visibility: Optional[float] = None

class FramePoseDataModel(BaseModel):
    # Using Dict[str, PoseKeypointModel] for dynamic keypoint names
    # Based on Pydantic docs, for arbitrary key names, this is one way.
    # Or define all possible keypoints as Optional fields if they are fixed.
    # For now, allowing any string key to map to a PoseKeypointModel.
    keypoints: Dict[str, PoseKeypointModel] = Field(..., alias="keypoints") # Ensure this field name is used in JSON

    # If you expect a fixed set of keypoints, define them like this:
    # nose: Optional[PoseKeypointModel] = None
    # left_shoulder: Optional[PoseKeypointModel] = None
    # ... and so on for all ~33 keypoints. This is more explicit.
    # For this example, using a dictionary is more flexible if keypoint names vary.
    # However, our kpi_extraction module EXPECTS specific names.
    # So, it's better to be explicit if possible, or validate expected keys exist.

    # Reverting to a simple Dict[str, PoseKeypointModel] as FramePoseData is Dict[str, PoseKeypoint]
    # Pydantic will expect a dictionary where keys are strings and values are PoseKeypointModel-like dicts.
    # This means the input JSON for a frame should be: {"nose": {"x":0,...}, "left_shoulder":{...}}
    # Let's make it a direct mapping for simplicity matching TypedDict
    # This requires the input JSON to be a dictionary of keypoint objects.
    # e.g. {"frames": [ {"left_shoulder": {"x":0,...}, ... }, ... ] }

    # Pydantic handles Dict[str, ModelType] well.
    # So, FramePoseData will be a dictionary where keys are keypoint names (strings)
    # and values are PoseKeypointModel instances.
    # This means the input `frames` list will contain dictionaries like:
    # { "nose": {"x":0,...}, "left_shoulder": {"x":0,...}, ... }
    # This matches our FramePoseData = Dict[str, PoseKeypoint] TypedDict.

    # No specific fields needed here if we treat it as Dict[str, PoseKeypointModel]
    # The validation will happen on the SwingVideoAnalysisInputModel's 'frames' field.


class PSystemPhaseModel(BaseModel):
    phase_name: str
    start_frame_index: int
    end_frame_index: int

class SwingVideoAnalysisInputModel(BaseModel):
    session_id: str
    user_id: str
    club_used: str
    # Each item in 'frames' should be a dictionary matching FramePoseData structure
    frames: List[Dict[str, PoseKeypointModel]] # List of {keypoint_name: PoseKeypointModel}
    p_system_classification: List[PSystemPhaseModel]
    video_fps: float

    # Convert back to TypedDict for internal use if necessary, though not strictly needed
    # as functions can often work with Pydantic models via .dict() or attribute access.
    # For this example, we'll convert to the TypedDicts our modules expect.
    def to_typed_dict(self) -> Dict[str, Any]: # Simplified to Dict for broader compatibility
        # Pydantic's .model_dump() is the modern way (previously .dict())
        return self.model_dump()


# --- FastAPI App ---
app = FastAPI(
    title="SwingSync AI API",
    description="API for analyzing golf swings and providing AI-powered feedback.",
    version="0.1.0"
)

@app.post("/analyze_swing/", response_model=SwingAnalysisFeedback)
async def analyze_swing_endpoint(swing_input_model: SwingVideoAnalysisInputModel):
    """
    Receives swing video analysis input, processes it through the AI pipeline,
    and returns structured feedback including LLM-generated tips.
    """
    try:
        # Convert Pydantic model to the TypedDict format our internal functions expect.
        # This is a bit of manual conversion now.
        # Alternatively, internal functions could be adapted to use Pydantic models.

        swing_input_dict = swing_input_model.to_typed_dict()

        # The `frames` field needs to be a List[FramePoseDataTypedDict]
        # FramePoseDataTypedDict is Dict[str, PoseKeypointTypedDict]
        # swing_input_model.frames is List[Dict[str, PoseKeypointModel]]
        # Pydantic model_dump handles nested models correctly.

        # 1. Extract KPIs
        # Ensure that the input to extract_all_kpis matches SwingVideoAnalysisInputTypedDict
        # The model_dump() should produce a dictionary compatible with the TypedDict.
        kpis = extract_all_kpis(swing_input_dict)

        # 2. Detect Faults
        faults = check_swing_faults(swing_input_dict, kpis)

        # 3. Generate Feedback (potentially calls Gemini API)
        # Ensure GEMINI_API_KEY is set in the environment where this API runs
        feedback_result = generate_swing_analysis_feedback(swing_input_dict, faults)

        return feedback_result

    except Exception as e:
        # Log the exception for server-side debugging
        print(f"Error during swing analysis: {e}")
        # Optionally, include more details in the error if it's safe to expose
        raise HTTPException(status_code=500, detail=f"An error occurred during analysis: {str(e)}")

@app.get("/", summary="Root endpoint for health check or basic info.")
async def read_root():
    return {
        "message": "Welcome to SwingSync AI API. Use the /analyze_swing/ endpoint to submit data.",
        "documentation": ["/docs", "/redoc"]
    }

# To run this application:
# 1. Ensure FastAPI and Uvicorn are installed: pip install fastapi uvicorn
# 2. Set the GEMINI_API_KEY environment variable: export GEMINI_API_KEY="YOUR_API_KEY"
# 3. Run Uvicorn: uvicorn main:app --reload
#    (from the directory containing main.py)
# 4. Access the API at http://127.0.0.1:8000
# 5. Access interactive API documentation at http://127.0.0.1:8000/docs

if __name__ == "__main__":
    # This block is for direct execution (though uvicorn is preferred for dev)
    # import uvicorn
    # uvicorn.run(app, host="0.0.0.0", port=8000)
    print("To run this FastAPI app, use Uvicorn: `uvicorn main:app --reload`")
    print("Ensure GEMINI_API_KEY environment variable is set if using feedback generation.")

```
