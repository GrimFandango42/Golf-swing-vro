# SwingSync AI - Golf Swing Analysis Backend

SwingSync AI is a Python-based backend system designed to analyze golf swings using computer vision data (pose estimation keypoints) and provide AI-generated coaching feedback via the Google Gemini API.

This project forms the core server-side logic for a golf swing coaching application. It processes skeletal data from a swing video, identifies biomechanical faults, and generates personalized tips and drills.

## Project Structure

The project is organized into the following main Python modules:

-   `data_structures.py`: Defines the core data TypedDicts and Pydantic models used throughout the application for representing pose data, swing phases, KPIs, faults, and API responses.
-   `kpi_extraction.py`: Contains functions to calculate various biomechanical Key Performance Indicators (KPIs) from the input 3D pose data (e.g., joint angles, rotations, positions).
-   `fault_detection.py`: Implements the logic for detecting common golf swing faults by comparing extracted KPIs against a predefined `FAULT_DIAGNOSIS_MATRIX`.
-   `feedback_generation.py`: Responsible for constructing prompts based on detected faults and interacting with the Google Gemini API to generate natural language coaching feedback (explanations, tips, and drills).
-   `main.py`: A FastAPI application that exposes an API endpoint (`/analyze_swing/`) to receive swing data, orchestrate the analysis pipeline, and return the generated feedback.
-   `tests/`: Contains unit tests and a `test_data_factory.py` for generating mock swing data for testing purposes.

## Setup and Installation

1.  **Clone the repository (if applicable).**

2.  **Create a virtual environment (recommended):**
    ```bash
    python -m venv venv
    source venv/bin/activate  # On Windows: venv\Scripts\activate
    ```

3.  **Install dependencies:**
    The primary dependencies are FastAPI, Uvicorn, NumPy, and Google Generative AI.
    ```bash
    pip install fastapi uvicorn numpy google-generativeai
    ```
    (A `requirements.txt` file would typically be provided for `pip install -r requirements.txt`)

4.  **Set Environment Variables:**
    To use the feedback generation feature, you need a Google Gemini API key.
    ```bash
    export GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
    ```
    Replace `"YOUR_GEMINI_API_KEY"` with your actual key.

## Running the Application

1.  **Start the FastAPI server using Uvicorn:**
    From the project root directory (where `main.py` is located):
    ```bash
    uvicorn main:app --reload
    ```
    The `--reload` flag enables auto-reloading during development.

2.  **Access the API:**
    -   The API will be available at `http://127.0.0.1:8000`.
    -   Interactive API documentation (Swagger UI) is at `http://127.0.0.1:8000/docs`.
    -   Alternative documentation (ReDoc) is at `http://127.0.0.1:8000/redoc`.

    You can use the `/docs` interface to send test JSON payloads to the `/analyze_swing/` endpoint. Refer to `data_structures.py` or the Pydantic models in `main.py` (`SwingVideoAnalysisInputModel`) for the expected request body structure.

## Running Tests

Unit tests are located in the `tests/` directory and use Python's `unittest` module.

1.  **Navigate to the project root directory.**
2.  **Run all tests:**
    ```bash
    python -m unittest discover tests
    ```
3.  **Run a specific test file:**
    ```bash
    python -m unittest tests.test_kpi_extraction
    # or python tests/test_kpi_extraction.py if tests folder is not a package
    ```

## How it Works (Analysis Pipeline)

1.  **Input:** The `/analyze_swing/` API endpoint receives a JSON payload containing:
    *   User and session information.
    *   A sequence of frames, each with 3D pose keypoints for the golfer.
    *   P-System classification (timing of P1-P10 swing phases).
    *   Video FPS.

2.  **KPI Extraction (`kpi_extraction.py`):**
    *   Relevant biomechanical KPIs (angles, rotations, etc.) are calculated for key swing positions (P1, P4, etc.) from the pose data.

3.  **Fault Detection (`fault_detection.py`):**
    *   The extracted KPIs are compared against predefined rules and ideal ranges in the `FAULT_DIAGNOSIS_MATRIX`.
    *   If deviations are found, `DetectedFault` objects are created.

4.  **Feedback Generation (`feedback_generation.py`):**
    *   For significant detected faults, prompts are constructed using templates from `LLM_PROMPT_TEMPLATES`.
    *   These prompts are sent to the Google Gemini API.
    *   The LLM's response (containing explanations, tips, and drills) is parsed.

5.  **Output:** The API returns a `SwingAnalysisFeedback` JSON object containing:
    *   A summary of findings.
    *   Detailed LLM-generated tips for the primary fault(s).
    *   A list of all raw detected faults.

## Further Development

-   Implement calculation for all placeholder KPIs.
-   Expand the `FAULT_DIAGNOSIS_MATRIX` with more rules.
-   Refine `LLM_PROMPT_TEMPLATES` for higher quality feedback.
-   Enhance severity calculation for faults.
-   Develop more sophisticated fault prioritization.
-   Integrate with a mobile front-end application.
-   Consider moving matrix definitions and prompt templates to external configuration files (e.g., JSON, YAML).
```
