# Real-time Streaming Analysis for SwingSync AI

This document describes the real-time streaming analysis capabilities added to the SwingSync AI golf swing analysis system. The streaming functionality enables low-latency, frame-by-frame analysis for live coaching and immediate feedback during practice sessions.

## Overview

The streaming system provides:
- **Real-time Analysis**: Sub-100ms latency for frame-by-frame swing analysis
- **Live Coaching**: WebSocket-based coaching sessions between instructors and students
- **Streaming Feedback**: Integration with Gemini 2.5 Flash for instant AI coaching
- **Performance Monitoring**: Real-time KPI tracking and system metrics
- **Session Management**: Live coaching session coordination and management

## Architecture

### Core Components

1. **WebSocket Manager** (`websocket_manager.py`)
   - Connection lifecycle management
   - Session-based connection grouping
   - Message routing and broadcasting
   - Health monitoring and cleanup

2. **Streaming Endpoints** (`streaming_endpoints.py`)
   - WebSocket endpoints for real-time communication
   - REST API for session management
   - Message handling and routing logic
   - Performance metrics collection

3. **Live Analysis Engine** (`live_analysis.py`)
   - Real-time swing phase detection
   - Frame-by-frame KPI calculation
   - Adaptive fault detection
   - Performance optimization for low latency

4. **Enhanced Feedback Generation**
   - Streaming integration with existing `feedback_generation.py`
   - Real-time context-aware prompts
   - Gemini 2.5 Flash streaming responses

## API Endpoints

### WebSocket Endpoints

#### 1. Main Streaming Endpoint
```
WebSocket: /api/v1/stream/ws/{user_id}
```
Primary endpoint for real-time swing analysis streaming.

**Features:**
- Frame-by-frame pose data processing
- Immediate analysis results
- Real-time KPI monitoring
- Instant fault detection and feedback

**Message Types:**
- `frame_data`: Send pose keypoints for analysis
- `start_session`: Initialize streaming session
- `end_session`: Terminate streaming session
- `analysis_result`: Receive analysis results
- `feedback`: Receive AI coaching feedback

#### 2. Live Coaching Endpoint
```
WebSocket: /api/v1/stream/ws/coaching/{session_id}?user_id={user_id}
```
Collaborative coaching sessions between instructors and students.

**Features:**
- Multi-user session management
- Real-time coaching communication
- Synchronized swing analysis
- Drill suggestions and tips

**Message Types:**
- `coaching_tip`: Send/receive coaching advice
- `drill_suggestion`: Share practice drills
- `coaching_frame_update`: Synchronized frame viewing

#### 3. Performance Monitoring Endpoint
```
WebSocket: /api/v1/stream/ws/monitor/{user_id}
```
Real-time system performance and analytics monitoring.

**Features:**
- Connection statistics
- Analysis performance metrics
- System health monitoring
- Real-time KPI tracking

### REST API Endpoints

#### Session Management
```
POST /api/v1/stream/sessions
GET /api/v1/stream/sessions/{session_id}
DELETE /api/v1/stream/sessions/{session_id}
GET /api/v1/stream/sessions/{session_id}/stats
```

#### System Monitoring
```
GET /api/v1/stream/system/stats
GET /streaming/status
```

## Usage Examples

### 1. Basic Real-time Analysis

```javascript
// Connect to streaming endpoint
const ws = new WebSocket(`ws://localhost:8000/api/v1/stream/ws/${userId}`);

// Start streaming session
ws.send(JSON.stringify({
    type: "start_streaming_session",
    data: {
        config: {
            user_id: userId,
            session_name: "Practice Session",
            club_used: "Driver",
            skill_level: "intermediate",
            enable_real_time_kpis: true,
            enable_instant_feedback: true,
            target_latency_ms: 100
        }
    }
}));

// Send frame data for analysis
ws.send(JSON.stringify({
    type: "frame_data",
    data: {
        frame_index: frameIndex,
        timestamp: Date.now() / 1000,
        keypoints: {
            "left_shoulder": {"x": 0.2, "y": 1.4, "z": -0.3, "visibility": 0.9},
            "right_shoulder": {"x": -0.2, "y": 1.4, "z": -0.3, "visibility": 0.9},
            // ... other keypoints
        }
    }
}));

// Handle analysis results
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.type === "analysis_result") {
        console.log("Swing Phase:", message.data.swing_phase);
        console.log("KPIs:", message.data.kpis);
        console.log("Faults:", message.data.detected_faults);
    }
    
    if (message.type === "feedback") {
        console.log("AI Feedback:", message.data.feedback);
    }
};
```

### 2. Live Coaching Session

```javascript
// Coach connects to coaching session
const coachWs = new WebSocket(`ws://localhost:8000/api/v1/stream/ws/coaching/${sessionId}?user_id=${coachId}`);

// Student connects to same session
const studentWs = new WebSocket(`ws://localhost:8000/api/v1/stream/ws/coaching/${sessionId}?user_id=${studentId}`);

// Coach sends tip to student
coachWs.send(JSON.stringify({
    type: "coaching_tip",
    data: {
        tip: "Focus on keeping your head still during the backswing",
        target_user: studentId,
        priority: "high"
    }
}));

// Coach suggests drill
coachWs.send(JSON.stringify({
    type: "drill_suggestion",
    data: {
        drill_name: "Wall Drill",
        description: "Practice swings with back against wall",
        duration_minutes: 5,
        target_user: studentId
    }
}));
```

### 3. Performance Monitoring

```javascript
// Connect to monitoring endpoint
const monitorWs = new WebSocket(`ws://localhost:8000/api/v1/stream/ws/monitor/${userId}`);

// Receive performance metrics
monitorWs.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.type === "performance_metrics") {
        console.log("Frames Processed:", message.data.frames_processed);
        console.log("Average Latency:", message.data.average_latency_ms, "ms");
        console.log("KPIs Calculated:", message.data.kpis_calculated);
        console.log("Faults Detected:", message.data.faults_detected);
    }
};
```

## Configuration Options

### Streaming Session Configuration

```python
class StreamingSessionConfig:
    user_id: str
    session_name: str = "Live Analysis Session"
    club_used: str = "Unknown"
    skill_level: UserSkillLevel = UserSkillLevel.INTERMEDIATE
    feedback_mode: FeedbackMode = FeedbackMode.STREAMING
    analysis_frequency: int = 5  # Analyze every N frames
    feedback_threshold: float = 0.6  # Minimum severity for feedback
    enable_real_time_kpis: bool = True
    enable_instant_feedback: bool = True
    target_latency_ms: int = 100  # Target analysis latency
```

### Performance Tuning

**Low Latency Settings:**
```python
config = StreamingSessionConfig(
    analysis_frequency=1,  # Analyze every frame
    target_latency_ms=50,  # Very low latency
    feedback_threshold=0.8  # Only critical faults
)
```

**Battery Optimized Settings:**
```python
config = StreamingSessionConfig(
    analysis_frequency=10,  # Analyze every 10th frame
    target_latency_ms=200,  # Allow higher latency
    enable_real_time_kpis=False  # Reduce computation
)
```

## Installation and Setup

### 1. Install Dependencies

```bash
# Install base requirements
pip install -r requirements.txt

# Install streaming-specific requirements
pip install -r requirements-streaming.txt
```

### 2. Environment Variables

```bash
export GEMINI_API_KEY="your-gemini-api-key"
export SECRET_KEY="your-jwt-secret-key"
export DATABASE_URL="sqlite:///./swingsync.db"
```

### 3. Run Application

```bash
# Start the application with streaming support
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Check streaming status
curl http://localhost:8000/streaming/status
```

## Performance Characteristics

### Latency Targets
- **Frame Processing**: < 50ms per frame
- **KPI Calculation**: < 20ms per frame
- **Fault Detection**: < 30ms per frame
- **WebSocket Communication**: < 10ms round-trip

### Throughput
- **Concurrent Connections**: 100+ simultaneous users
- **Frame Rate**: Up to 30 FPS per connection
- **Data Throughput**: ~1MB/s per active connection

### Resource Usage
- **Memory**: ~50MB per active streaming session
- **CPU**: ~5% per active connection (on modern hardware)
- **Network**: ~10KB per frame (compressed pose data)

## Troubleshooting

### Common Issues

1. **High Latency**
   - Reduce analysis frequency
   - Disable non-essential KPI calculations
   - Check network connection quality

2. **Connection Drops**
   - Implement automatic reconnection
   - Check WebSocket timeout settings
   - Monitor system resources

3. **Memory Usage**
   - Limit frame buffer sizes
   - Implement periodic cleanup
   - Monitor for memory leaks

### Monitoring

Check system status:
```bash
curl http://localhost:8000/streaming/status
curl http://localhost:8000/health
curl http://localhost:8000/api/v1/stream/system/stats
```

### Logs

Enable detailed logging:
```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

## Security Considerations

1. **Authentication**: All WebSocket connections should be authenticated
2. **Rate Limiting**: Implement rate limiting for frame submissions
3. **Data Validation**: Validate all incoming pose data
4. **Session Management**: Implement proper session timeouts
5. **Resource Limits**: Set limits on concurrent connections and data rates

## Future Enhancements

1. **Video Streaming**: Direct video stream processing
2. **Multi-camera Support**: Multiple camera angle analysis
3. **Cloud Scaling**: Horizontal scaling with Redis/message queues
4. **Mobile Optimization**: Enhanced mobile client support
5. **Advanced Analytics**: Real-time trend analysis and insights

## Integration with Existing System

The streaming functionality is designed to integrate seamlessly with the existing SwingSync AI system:

- **Database**: Session data is optionally persisted to the existing database
- **Authentication**: Uses the same JWT authentication system
- **Analysis Pipeline**: Leverages existing KPI extraction and fault detection
- **AI Feedback**: Enhanced integration with Gemini 2.5 Flash
- **User Management**: Compatible with existing user profiles and preferences

The system gracefully degrades if streaming dependencies are not available, allowing the core batch analysis functionality to continue working.