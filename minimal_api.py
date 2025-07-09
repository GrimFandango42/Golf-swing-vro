#!/usr/bin/env python3
"""
SwingSync AI - Minimal API for Mobile
=====================================
A lightweight version that works without heavy dependencies
"""

from fastapi import FastAPI, HTTPException, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel
from datetime import datetime
import os
import json
import socket
import base64
import asyncio

# Initialize FastAPI app
app = FastAPI(
    title="SwingSync AI - Mobile API",
    description="Professional Golf Coaching powered by AI",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all for mobile demo
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Models
class SwingAnalysisRequest(BaseModel):
    video_url: str
    user_id: str = "demo_user"

class VideoStreamFrame(BaseModel):
    frame_data: str  # base64 encoded frame
    timestamp: float
    user_id: str

class SwingAnalysisResponse(BaseModel):
    session_id: str
    overall_score: float
    x_factor: float
    tempo_score: float
    balance_score: float
    power_rating: str
    key_insights: list[str]
    coaching_feedback: str
    timestamp: datetime

# Get local IP
def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "localhost"

# Routes
@app.get("/", response_class=HTMLResponse)
async def root():
    """Landing page"""
    return """
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>SwingSync AI</title>
        <style>
            body {
                font-family: -apple-system, sans-serif;
                background: linear-gradient(135deg, #2E7D32, #1976D2);
                color: white;
                text-align: center;
                padding: 20px;
                margin: 0;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background: rgba(255,255,255,0.1);
                border-radius: 20px;
                padding: 30px;
                backdrop-filter: blur(10px);
            }
            h1 { font-size: 2.5em; margin-bottom: 10px; }
            .subtitle { font-size: 1.2em; opacity: 0.9; margin-bottom: 30px; }
            .status {
                background: rgba(76, 175, 80, 0.3);
                border-radius: 15px;
                padding: 20px;
                margin: 20px 0;
                border-left: 4px solid #4CAF50;
            }
            .button {
                background: #FFB300;
                color: #1976D2;
                padding: 15px 30px;
                border-radius: 25px;
                text-decoration: none;
                font-weight: bold;
                display: inline-block;
                margin: 20px 10px;
                transition: transform 0.2s;
            }
            .button:hover { transform: scale(1.05); }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🏌️ SwingSync AI</h1>
            <p class="subtitle">Professional Golf Coaching - Mobile API</p>
            
            <div class="status">
                <h3>✅ API Status: Online</h3>
                <p>Your AI golf coach is ready!</p>
            </div>
            
            <a href="/docs" class="button">📖 API Documentation</a>
            <a href="/health" class="button">🩺 System Health</a>
            <a href="/demo" class="button">🎯 Try Demo</a>
            <a href="/upload" class="button">📹 Upload Video</a>
        </div>
    </body>
    </html>
    """

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0",
        "device_ip": get_local_ip(),
        "features": [
            "AI Swing Analysis",
            "X-Factor Calculation", 
            "Voice Coaching",
            "Progress Tracking"
        ]
    }

@app.post("/api/analyze", response_model=SwingAnalysisResponse)
async def analyze_swing(request: SwingAnalysisRequest):
    """Analyze golf swing (demo mode)"""
    # Demo response with simulated variation
    import random
    
    scores = {
        "overall": round(random.uniform(75, 95), 1),
        "x_factor": round(random.uniform(35, 55), 1),
        "tempo": round(random.uniform(80, 95), 1),
        "balance": round(random.uniform(70, 90), 1)
    }
    
    power_ratings = ["Good", "Very Good", "Excellent", "Outstanding"]
    power_rating = random.choice(power_ratings)
    
    insights = [
        "Good X-Factor separation for power generation",
        "Solid tempo consistency throughout swing",
        "Minor adjustment needed in follow-through balance",
        "Strong weight transfer mechanics",
        "Excellent club path control"
    ]
    
    return SwingAnalysisResponse(
        session_id=f"demo_{datetime.now().timestamp()}",
        overall_score=scores["overall"],
        x_factor=scores["x_factor"],
        tempo_score=scores["tempo"],
        balance_score=scores["balance"],
        power_rating=power_rating,
        key_insights=random.sample(insights, 3),
        coaching_feedback=f"Nice swing! Your X-Factor of {scores['x_factor']}° shows good power potential. Your tempo score of {scores['tempo']}% indicates consistent rhythm. Keep focusing on that smooth transition!",
        timestamp=datetime.now()
    )

@app.post("/api/upload-video")
async def upload_video(file: UploadFile = File(...)):
    """Upload video file for analysis"""
    if not file.filename.endswith(('.mp4', '.mov', '.avi')):
        raise HTTPException(status_code=400, detail="Only video files (.mp4, .mov, .avi) are supported")
    
    # Save uploaded file
    upload_dir = "uploads"
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, f"{datetime.now().timestamp()}_{file.filename}")
    
    with open(file_path, "wb") as buffer:
        content = await file.read()
        buffer.write(content)
    
    # TODO: Process video with MediaPipe when dependencies are available
    # For now, return demo analysis
    return {
        "message": "Video uploaded successfully",
        "file_path": file_path,
        "file_size": len(content),
        "status": "processing",
        "note": "Real video analysis will be available once MediaPipe dependencies are installed"
    }

@app.post("/api/stream-frame")
async def stream_frame(frame: VideoStreamFrame):
    """Process streaming video frame"""
    # TODO: Real-time frame processing with MediaPipe
    # For now, return demo response
    return {
        "frame_processed": True,
        "timestamp": frame.timestamp,
        "user_id": frame.user_id,
        "pose_detected": True,
        "kpis": {
            "posture_score": round(random.uniform(75, 95), 1),
            "alignment_score": round(random.uniform(70, 90), 1)
        },
        "note": "Real pose detection will be available once MediaPipe dependencies are installed"
    }

@app.get("/demo", response_class=HTMLResponse)
async def demo_interface():
    """Interactive demo interface"""
    return """
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>SwingSync AI Demo</title>
        <style>
            body {
                font-family: -apple-system, sans-serif;
                background: linear-gradient(135deg, #2E7D32, #1976D2);
                color: white;
                padding: 20px;
                margin: 0;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background: rgba(255,255,255,0.1);
                border-radius: 20px;
                padding: 30px;
                backdrop-filter: blur(10px);
            }
            h1 { text-align: center; font-size: 2em; margin-bottom: 20px; }
            .demo-button {
                background: #FFB300;
                color: #1976D2;
                padding: 20px;
                border: none;
                border-radius: 25px;
                font-size: 1.2em;
                font-weight: bold;
                width: 100%;
                cursor: pointer;
                margin: 20px 0;
                transition: transform 0.2s;
            }
            .demo-button:hover { transform: scale(1.05); }
            .result {
                background: rgba(255, 193, 7, 0.2);
                border-radius: 15px;
                padding: 20px;
                margin: 20px 0;
                display: none;
            }
            .score {
                font-size: 3em;
                font-weight: bold;
                color: #4CAF50;
                text-align: center;
                margin: 20px 0;
            }
            .metric {
                display: flex;
                justify-content: space-between;
                padding: 10px 0;
                border-bottom: 1px solid rgba(255,255,255,0.2);
            }
            .insight {
                background: rgba(33, 150, 243, 0.2);
                padding: 15px;
                border-radius: 10px;
                margin: 10px 0;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🏌️ SwingSync AI Demo</h1>
            
            <button class="demo-button" onclick="analyzeSwing()">
                📹 Analyze Demo Swing
            </button>
            
            <div id="result" class="result">
                <h2>🎯 Analysis Results</h2>
                <div class="score" id="score">87.5%</div>
                <p style="text-align: center;"><strong>Overall Swing Score</strong></p>
                
                <div class="metric">
                    <span>X-Factor (Power):</span>
                    <span><strong id="xfactor">45.2°</strong></span>
                </div>
                <div class="metric">
                    <span>Tempo Score:</span>
                    <span><strong id="tempo">92.1%</strong></span>
                </div>
                <div class="metric">
                    <span>Balance Score:</span>
                    <span><strong id="balance">84.6%</strong></span>
                </div>
                <div class="metric">
                    <span>Power Rating:</span>
                    <span><strong id="power">Excellent</strong></span>
                </div>
                
                <h3>🔍 Key Insights:</h3>
                <div id="insights"></div>
                
                <h3>🗣️ AI Coach Says:</h3>
                <div class="insight" id="coaching"></div>
            </div>
        </div>
        
        <script>
            async function analyzeSwing() {
                const button = document.querySelector('.demo-button');
                button.textContent = '⏳ Analyzing...';
                button.disabled = true;
                
                try {
                    const response = await fetch('/api/analyze', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            video_url: 'demo_video.mp4',
                            user_id: 'demo_user'
                        })
                    });
                    
                    const data = await response.json();
                    
                    // Update UI with results
                    document.getElementById('score').textContent = data.overall_score + '%';
                    document.getElementById('xfactor').textContent = data.x_factor + '°';
                    document.getElementById('tempo').textContent = data.tempo_score + '%';
                    document.getElementById('balance').textContent = data.balance_score + '%';
                    document.getElementById('power').textContent = data.power_rating;
                    
                    // Add insights
                    const insightsDiv = document.getElementById('insights');
                    insightsDiv.innerHTML = data.key_insights
                        .map(insight => `<div class="insight">• ${insight}</div>`)
                        .join('');
                    
                    // Add coaching feedback
                    document.getElementById('coaching').textContent = data.coaching_feedback;
                    
                    // Show results
                    document.getElementById('result').style.display = 'block';
                    
                } catch (error) {
                    alert('Demo analysis error: ' + error.message);
                } finally {
                    button.textContent = '📹 Analyze Another Swing';
                    button.disabled = false;
                }
            }
        </script>
    </body>
    </html>
    """

@app.get("/upload", response_class=HTMLResponse)
async def upload_interface():
    """Video upload interface"""
    return """
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>SwingSync AI - Upload Video</title>
        <style>
            body {
                font-family: -apple-system, sans-serif;
                background: linear-gradient(135deg, #2E7D32, #1976D2);
                color: white;
                padding: 20px;
                margin: 0;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background: rgba(255,255,255,0.1);
                border-radius: 20px;
                padding: 30px;
                backdrop-filter: blur(10px);
            }
            h1 { text-align: center; font-size: 2em; margin-bottom: 20px; }
            .upload-area {
                border: 2px dashed #FFB300;
                border-radius: 15px;
                padding: 40px;
                text-align: center;
                margin: 20px 0;
                transition: all 0.3s;
            }
            .upload-area:hover { background: rgba(255, 179, 0, 0.1); }
            .upload-area.dragover { background: rgba(255, 179, 0, 0.2); }
            input[type="file"] {
                display: none;
            }
            .upload-button {
                background: #FFB300;
                color: #1976D2;
                padding: 15px 30px;
                border: none;
                border-radius: 25px;
                font-weight: bold;
                cursor: pointer;
                font-size: 1.1em;
            }
            .progress {
                width: 100%;
                height: 10px;
                background: rgba(255,255,255,0.2);
                border-radius: 5px;
                margin: 20px 0;
                overflow: hidden;
                display: none;
            }
            .progress-bar {
                height: 100%;
                background: #4CAF50;
                width: 0%;
                transition: width 0.3s;
            }
            .result {
                background: rgba(76, 175, 80, 0.2);
                border-radius: 15px;
                padding: 20px;
                margin: 20px 0;
                display: none;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>📹 Upload Golf Swing Video</h1>
            
            <div class="upload-area" id="uploadArea">
                <h3>📱 Drop video here or click to select</h3>
                <p>Supports: .mp4, .mov, .avi</p>
                <input type="file" id="fileInput" accept=".mp4,.mov,.avi">
                <button class="upload-button" onclick="document.getElementById('fileInput').click()">
                    Choose Video File
                </button>
            </div>
            
            <div class="progress" id="progress">
                <div class="progress-bar" id="progressBar"></div>
            </div>
            
            <div id="result" class="result"></div>
            
            <p style="text-align: center; margin-top: 30px; opacity: 0.8;">
                📝 Note: Real video analysis with MediaPipe pose detection will be available once dependencies are fully installed. Current version provides upload capability and demo analysis.
            </p>
        </div>
        
        <script>
            const uploadArea = document.getElementById('uploadArea');
            const fileInput = document.getElementById('fileInput');
            const progress = document.getElementById('progress');
            const progressBar = document.getElementById('progressBar');
            const result = document.getElementById('result');
            
            // Drag and drop functionality
            uploadArea.addEventListener('dragover', (e) => {
                e.preventDefault();
                uploadArea.classList.add('dragover');
            });
            
            uploadArea.addEventListener('dragleave', () => {
                uploadArea.classList.remove('dragover');
            });
            
            uploadArea.addEventListener('drop', (e) => {
                e.preventDefault();
                uploadArea.classList.remove('dragover');
                const files = e.dataTransfer.files;
                if (files.length > 0) {
                    uploadFile(files[0]);
                }
            });
            
            fileInput.addEventListener('change', (e) => {
                if (e.target.files.length > 0) {
                    uploadFile(e.target.files[0]);
                }
            });
            
            async function uploadFile(file) {
                const formData = new FormData();
                formData.append('file', file);
                
                progress.style.display = 'block';
                result.style.display = 'none';
                
                try {
                    const response = await fetch('/api/upload-video', {
                        method: 'POST',
                        body: formData
                    });
                    
                    const data = await response.json();
                    
                    progressBar.style.width = '100%';
                    
                    setTimeout(() => {
                        progress.style.display = 'none';
                        result.style.display = 'block';
                        result.innerHTML = `
                            <h3>✅ Upload Successful!</h3>
                            <p><strong>File:</strong> ${file.name}</p>
                            <p><strong>Size:</strong> ${(data.file_size / 1024 / 1024).toFixed(2)} MB</p>
                            <p><strong>Status:</strong> ${data.status}</p>
                            <p><strong>Note:</strong> ${data.note}</p>
                        `;
                    }, 500);
                    
                } catch (error) {
                    progress.style.display = 'none';
                    result.style.display = 'block';
                    result.innerHTML = `
                        <h3>❌ Upload Failed</h3>
                        <p>Error: ${error.message}</p>
                    `;
                }
            }
        </script>
    </body>
    </html>
    """

@app.get("/api/sessions")
async def get_sessions():
    """Get demo sessions"""
    return {
        "sessions": [
            {
                "id": "demo_001",
                "date": "2025-01-08",
                "score": 87.5,
                "status": "completed"
            },
            {
                "id": "demo_002", 
                "date": "2025-01-07",
                "score": 85.2,
                "status": "completed"
            }
        ]
    }

if __name__ == "__main__":
    import uvicorn
    print("🏌️ Starting SwingSync AI Minimal API...")
    print(f"📱 Access at: http://{get_local_ip()}:8000")
    uvicorn.run(app, host="0.0.0.0", port=8000)