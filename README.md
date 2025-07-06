# SwingSync AI

Golf swing analysis platform with real-time biomechanical analysis and AI-powered coaching feedback.

## Overview

SwingSync AI processes 3D pose estimation data from golf swings to identify biomechanical faults and generate personalized coaching recommendations. The platform supports real-time streaming analysis, user management, and conversational AI coaching interfaces.

## Core Features

- **Biomechanical Analysis**: 31 KPI calculations across 10 swing positions (P1-P10)
- **Fault Detection**: Club-specific rules for Driver, Iron, and Wedge analysis
- **AI Coaching**: Integration with Google Gemini 2.5 Flash for feedback generation
- **Real-time Streaming**: WebSocket-based live analysis with sub-100ms latency
- **User Management**: Authentication, profiles, and progress tracking
- **Conversational Interface**: Voice-based coaching with multiple personality modes

## Architecture

```
┌─────────────────────────────────────────┐
│ FastAPI Application (main.py)           │
├─────────────────────────────────────────┤
│ Real-time Streaming (WebSocket)         │
│ ├── Connection Manager                  │
│ ├── Live Analysis Pipeline              │
│ └── Adaptive Quality Controls           │
├─────────────────────────────────────────┤
│ Analysis Engine                         │
│ ├── KPI Extraction                     │
│ ├── Fault Detection                    │
│ └── AI Feedback Generation             │
├─────────────────────────────────────────┤
│ Data Layer                              │
│ ├── PostgreSQL Database                │
│ ├── Redis Caching                      │
│ └── User Management                    │
└─────────────────────────────────────────┘
```

## Technical Specifications

### Performance
- API Response Time: P95 < 200ms
- Real-time Analysis: Sub-100ms frame processing
- Concurrent Users: 1000+ WebSocket connections
- Memory Usage: <500MB per session
- Database Queries: P95 < 50ms

### Technology Stack
- **Backend**: FastAPI 0.104+, Python 3.8+
- **Database**: PostgreSQL 13+, Redis 6+
- **AI Integration**: Google Gemini 2.5 Flash, OpenAI GPT-4
- **Real-time**: WebSocket, asyncio
- **Authentication**: JWT with bcrypt

## Installation

### Prerequisites
- Python 3.8+
- PostgreSQL 13+ (or SQLite for development)
- Redis 6+
- Google Gemini API key

### Setup
```bash
# Clone repository
git clone https://github.com/GrimFandango42/Golf-swing-vro.git
cd Golf-swing-vro

# Create virtual environment
python -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-streaming.txt

# Configure environment
cp .env.example .env
# Edit .env with your API keys and database URLs

# Initialize database
python migrate.py init

# Start application
uvicorn main:app --reload
```

## API Endpoints

### Authentication
- `POST /auth/register` - User registration
- `POST /auth/login` - User authentication
- `GET /auth/me` - Get user profile

### Analysis
- `POST /analyze_swing/` - Analyze swing data
- `GET /user/sessions` - Get user session history
- `GET /user/analytics` - Get performance analytics

### Real-time
- `WS /api/v1/stream/ws/{user_id}` - Real-time analysis
- `WS /api/v1/stream/ws/coaching/{session_id}` - Live coaching

## Data Structures

### Input Format
```python
{
    "session_id": "string",
    "user_id": "string", 
    "club_used": "driver|iron|wedge",
    "frames": [
        {
            "keypoint_name": {
                "x": float,
                "y": float,
                "z": float,
                "visibility": float
            }
        }
    ],
    "p_system_classification": [
        {
            "phase_name": "P1-P10",
            "start_frame_index": int,
            "end_frame_index": int
        }
    ],
    "video_fps": float
}
```

### Output Format
```python
{
    "session_id": "string",
    "summary_of_findings": "string",
    "detailed_feedback": [
        {
            "fault_name": "string",
            "explanation": "string",
            "corrective_tip": "string",
            "drill_recommendation": "string"
        }
    ],
    "raw_detected_faults": [
        {
            "fault_name": "string",
            "severity": float,
            "kpi_deviations": []
        }
    ]
}
```

## Development

### Project Structure
```
├── core/
│   ├── data_structures.py      # Data models and TypedDicts
│   ├── kpi_extraction.py       # Biomechanical calculations
│   ├── fault_detection.py      # Rule-based fault detection
│   └── feedback_generation.py  # AI coaching integration
├── streaming/
│   ├── websocket_manager.py    # Connection management
│   ├── streaming_endpoints.py  # WebSocket API
│   └── live_analysis.py        # Real-time processing
├── data/
│   ├── database.py             # Database models
│   ├── user_management.py      # Authentication
│   └── analytics.py            # Performance tracking
├── conversational_coaching/    # Voice interface
├── tests/                      # Test suite
└── main.py                     # FastAPI application
```

### Testing
```bash
# Run all tests
pytest tests/ -v

# Run performance tests
pytest tests/test_performance.py -v

# Run streaming tests  
pytest tests/test_streaming.py -v

# Generate coverage report
pytest --cov=./ --cov-report=html
```

### Code Quality
```bash
# Format code
black .
isort .

# Type checking
mypy .

# Security scan
bandit -r .
```

## Deployment

### Docker
```bash
# Build image
docker build -t swingsync-ai .

# Run container
docker run -p 8000:8000 swingsync-ai
```

### Kubernetes
```bash
# Deploy to cluster
kubectl apply -f k8s/

# Check status
kubectl get pods -n swingsync-ai
```

### Environment Variables
```bash
# Required
DATABASE_URL=postgresql://user:password@localhost/swingsync
GEMINI_API_KEY=your_gemini_api_key

# Optional
REDIS_URL=redis://localhost:6379
OPENAI_API_KEY=your_openai_api_key
SECRET_KEY=your_secret_key
```

## Configuration

### Database Schema
- Users, user profiles, and preferences
- Swing sessions and analysis results
- Biomechanical KPIs and detected faults
- Progress tracking and goals

### Real-time Configuration
```python
STREAMING_CONFIG = {
    "max_connections": 1000,
    "target_latency_ms": 100,
    "frame_buffer_size": 10,
    "quality_adaptation": True
}
```

## Monitoring

### Health Checks
- `GET /health` - Basic health status
- `GET /health/detailed` - System diagnostics

### Metrics
- API response times and error rates
- WebSocket connection counts and latency
- Database query performance
- AI model response times

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit pull request

### Development Standards
- Follow PEP 8 style guidelines
- Add type hints for all functions
- Include docstrings for public APIs
- Write tests for new functionality
- Update documentation as needed

## License

MIT License - see LICENSE file for details.

## Support

- API Documentation: http://localhost:8000/docs
- Issues: GitHub Issues
- Email: support@swingsync.ai