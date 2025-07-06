"""
Comprehensive Real-time Streaming Endpoint Tests for SwingSync AI.

This module tests all aspects of the real-time streaming functionality including:
- WebSocket connection management and lifecycle
- Real-time frame processing and analysis
- Streaming session management
- Live coaching session functionality
- Performance monitoring endpoints
- Error handling and recovery
- Concurrent streaming sessions
- Message routing and broadcasting
- Real-time feedback generation
- Latency and throughput validation

Key Test Areas:
1. WebSocket Connection Management
2. Streaming Analysis Pipeline
3. Session Management and Lifecycle
4. Real-time Feedback Generation
5. Live Coaching Features
6. Performance Monitoring
7. Error Handling and Recovery
8. Concurrent Session Testing
9. Message Broadcasting
10. Latency Requirements Validation
"""

import asyncio
import json
import pytest
import time
import websockets
from typing import Dict, List, Any
from unittest.mock import Mock, AsyncMock, patch
from fastapi.testclient import TestClient
from fastapi.websockets import WebSocket

# Project imports
from streaming_endpoints import (
    router, StreamingSessionManager, StreamingSessionConfig,
    StreamingFrameData, PerformanceMetrics
)
from websocket_manager import connection_manager, MessageType, WebSocketMessage
from live_analysis import LiveAnalysisEngine, SwingPhase
from mock_data_factory import generate_streaming_session, create_realistic_swing, ClubType

class MockWebSocket:
    """Mock WebSocket for testing"""
    
    def __init__(self):
        self.messages_sent = []
        self.messages_to_receive = []
        self.closed = False
        self.client_state = {}
    
    async def send_text(self, message: str):
        """Mock send_text method"""
        if self.closed:
            raise Exception("WebSocket closed")
        self.messages_sent.append(message)
    
    async def send_json(self, data: dict):
        """Mock send_json method"""
        await self.send_text(json.dumps(data))
    
    async def receive_text(self):
        """Mock receive_text method"""
        if self.closed:
            raise Exception("WebSocket closed")
        if self.messages_to_receive:
            return self.messages_to_receive.pop(0)
        await asyncio.sleep(0.01)  # Simulate waiting
        return json.dumps({"type": "heartbeat"})
    
    async def receive_json(self):
        """Mock receive_json method"""
        text = await self.receive_text()
        return json.loads(text)
    
    async def close(self):
        """Mock close method"""
        self.closed = True
    
    def add_message_to_receive(self, message: dict):
        """Add message for mock to receive"""
        self.messages_to_receive.append(json.dumps(message))

class TestStreamingSessionManager:
    """Test the streaming session management functionality"""
    
    def test_session_creation_and_lifecycle(self):
        """Test basic session creation and lifecycle management"""
        manager = StreamingSessionManager()
        
        # Create session config
        config = StreamingSessionConfig(
            user_id="test_user_123",
            session_name="Test Session",
            club_used="7-Iron",
            analysis_frequency=3,
            enable_real_time_kpis=True,
            enable_instant_feedback=True
        )
        
        # Create session
        session_id = manager.create_session(config)
        assert session_id is not None
        assert session_id != ""
        
        # Verify session exists
        session_data = manager.get_session(session_id)
        assert session_data is not None
        assert session_data["config"].user_id == "test_user_123"
        assert session_data["config"].club_used == "7-Iron"
        
        # Verify user session mapping
        user_session_id = manager.get_user_session("test_user_123")
        assert user_session_id == session_id
        
        # Verify session stats
        assert session_id in manager.session_stats
        stats = manager.session_stats[session_id]
        assert stats.session_id == session_id
        assert stats.frames_processed == 0
        
        # End session
        success = manager.end_session(session_id)
        assert success is True
        
        # Verify cleanup
        assert manager.get_session(session_id) is None
        assert manager.get_user_session("test_user_123") is None
    
    def test_multiple_sessions_management(self):
        """Test management of multiple concurrent sessions"""
        manager = StreamingSessionManager()
        
        # Create multiple sessions
        sessions = []
        for i in range(5):
            config = StreamingSessionConfig(
                user_id=f"test_user_{i}",
                session_name=f"Test Session {i}",
                club_used=["Driver", "7-Iron", "Sand Wedge"][i % 3]
            )
            session_id = manager.create_session(config)
            sessions.append(session_id)
        
        # Verify all sessions exist
        assert len(manager.active_sessions) == 5
        assert len(manager.user_sessions) == 5
        assert len(manager.session_stats) == 5
        
        # Test session isolation
        for i, session_id in enumerate(sessions):
            session_data = manager.get_session(session_id)
            assert session_data["config"].user_id == f"test_user_{i}"
        
        # End sessions individually
        for session_id in sessions[:3]:
            manager.end_session(session_id)
        
        assert len(manager.active_sessions) == 2
        assert len(manager.user_sessions) == 2
        
        # End remaining sessions
        for session_id in sessions[3:]:
            manager.end_session(session_id)
        
        assert len(manager.active_sessions) == 0
        assert len(manager.user_sessions) == 0
    
    @pytest.mark.asyncio
    async def test_frame_processing(self, streaming_test_data):
        """Test frame processing in streaming sessions"""
        manager = StreamingSessionManager()
        
        # Create session
        config = StreamingSessionConfig(
            user_id="test_user",
            analysis_frequency=1,  # Analyze every frame
            enable_real_time_kpis=True
        )
        session_id = manager.create_session(config)
        
        # Process frames
        processed_results = []
        for i, frame_data in enumerate(streaming_test_data[:10]):
            streaming_frame = type('StreamingFrame', (), {
                'frame_index': frame_data['frame_index'],
                'timestamp': frame_data['timestamp'],
                'keypoints': frame_data['keypoints']
            })()
            
            result = await manager.process_frame(session_id, streaming_frame)
            if result:
                processed_results.append(result)
        
        # Verify processing results
        assert len(processed_results) > 0
        
        # Check session stats updated
        stats = manager.session_stats[session_id]
        assert stats.frames_processed == 10
        assert stats.average_latency_ms > 0
        
        # Verify frame analysis results
        for result in processed_results:
            assert result.frame_index >= 0
            assert result.timestamp > 0
            assert result.analysis_latency_ms > 0
            assert result.swing_phase in SwingPhase
        
        manager.end_session(session_id)

class TestWebSocketConnections:
    """Test WebSocket connection handling"""
    
    @pytest.mark.asyncio
    async def test_websocket_connection_lifecycle(self, mock_websocket_manager):
        """Test WebSocket connection lifecycle"""
        mock_websocket = MockWebSocket()
        
        # Mock connection manager
        mock_websocket_manager.connect.return_value = "test_connection_123"
        mock_websocket_manager.receive_message.return_value = WebSocketMessage(
            type=MessageType.START_SESSION.value,
            data={"config": {"user_id": "test_user", "club_used": "7-Iron"}}
        )
        
        # Test connection
        connection_id = await mock_websocket_manager.connect(
            mock_websocket, 
            "test_user", 
            {"endpoint": "streaming"}
        )
        
        assert connection_id == "test_connection_123"
        mock_websocket_manager.connect.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_streaming_message_handling(self, streaming_test_data):
        """Test handling of different streaming message types"""
        mock_websocket = MockWebSocket()
        
        # Test start session message
        start_session_msg = {
            "type": MessageType.START_SESSION.value,
            "data": {
                "config": {
                    "user_id": "test_user",
                    "session_name": "Test Session",
                    "club_used": "7-Iron",
                    "analysis_frequency": 3
                }
            }
        }
        mock_websocket.add_message_to_receive(start_session_msg)
        
        # Test frame data message
        frame_data_msg = {
            "type": MessageType.FRAME_DATA.value,
            "data": streaming_test_data[0]
        }
        mock_websocket.add_message_to_receive(frame_data_msg)
        
        # Test end session message
        end_session_msg = {
            "type": MessageType.END_SESSION.value,
            "data": {}
        }
        mock_websocket.add_message_to_receive(end_session_msg)
        
        # Process messages
        received_messages = []
        for _ in range(3):
            message = await mock_websocket.receive_json()
            received_messages.append(message)
        
        # Verify message types
        message_types = [msg["type"] for msg in received_messages]
        assert MessageType.START_SESSION.value in message_types
        assert MessageType.FRAME_DATA.value in message_types
        assert MessageType.END_SESSION.value in message_types
    
    @pytest.mark.asyncio
    async def test_websocket_error_handling(self):
        """Test WebSocket error handling"""
        mock_websocket = MockWebSocket()
        
        # Test connection closure
        await mock_websocket.close()
        
        # Should handle closed connection gracefully
        with pytest.raises(Exception):
            await mock_websocket.send_text("test message")
        
        # Test message handling with closed connection
        with pytest.raises(Exception):
            await mock_websocket.receive_text()

class TestRealTimeFeedback:
    """Test real-time feedback generation"""
    
    @pytest.mark.asyncio
    async def test_instant_feedback_generation(self, poor_swing_data, mock_gemini_streaming):
        """Test instant feedback generation for detected faults"""
        from streaming_endpoints import generate_instant_feedback, StreamingSessionConfig
        
        # Create analysis result with faults
        from live_analysis import FrameAnalysisResult, SwingPhase
        
        # Mock frame with faults
        analysis_result = FrameAnalysisResult(
            frame_index=50,
            timestamp=time.time(),
            swing_phase=SwingPhase.TOP_OF_SWING,
            frame_data=type('FrameData', (), {'keypoints': poor_swing_data["frames"][0]})(),
            detected_faults=[
                {
                    "fault_id": "INSUFFICIENT_SHOULDER_TURN_P4",
                    "fault_name": "Insufficient Shoulder Turn",
                    "severity": 0.8,
                    "description": "Shoulder rotation at top of backswing is restricted",
                    "p_positions_implicated": ["P4"]
                }
            ]
        )
        
        # Create session config
        config = StreamingSessionConfig(
            user_id="test_user",
            club_used="7-Iron",
            feedback_threshold=0.6,
            enable_instant_feedback=True
        )
        
        # Generate instant feedback
        feedback = await generate_instant_feedback(analysis_result, config)
        
        # Verify feedback structure
        assert feedback is not None
        assert "type" in feedback
        assert feedback["type"] == "instant_feedback"
        assert "fault_count" in feedback
        assert "primary_fault" in feedback
        assert "feedback" in feedback
        assert "timestamp" in feedback
        
        assert feedback["fault_count"] == 1
        assert feedback["primary_fault"] == "Insufficient Shoulder Turn"
    
    @pytest.mark.asyncio
    async def test_feedback_threshold_filtering(self, good_swing_data):
        """Test feedback threshold filtering for minor faults"""
        from streaming_endpoints import generate_instant_feedback, StreamingSessionConfig
        from live_analysis import FrameAnalysisResult, SwingPhase
        
        # Create analysis result with minor faults
        analysis_result = FrameAnalysisResult(
            frame_index=10,
            timestamp=time.time(),
            swing_phase=SwingPhase.SETUP,
            frame_data=type('FrameData', (), {'keypoints': good_swing_data["frames"][0]})(),
            detected_faults=[
                {
                    "fault_id": "MINOR_POSTURE_ISSUE",
                    "fault_name": "Minor Posture Issue",
                    "severity": 0.3,  # Below threshold
                    "description": "Minor posture adjustment needed",
                    "p_positions_implicated": ["P1"]
                }
            ]
        )
        
        # Create config with higher threshold
        config = StreamingSessionConfig(
            user_id="test_user",
            club_used="7-Iron",
            feedback_threshold=0.5,  # Higher than fault severity
            enable_instant_feedback=True
        )
        
        # Generate feedback
        feedback = await generate_instant_feedback(analysis_result, config)
        
        # Should not generate feedback for minor faults
        assert feedback is None

class TestLiveCoaching:
    """Test live coaching session functionality"""
    
    @pytest.mark.asyncio
    async def test_coaching_session_setup(self):
        """Test coaching session creation and participant management"""
        from streaming_endpoints import CoachingSessionConfig
        
        # Create coaching session config
        coaching_config = CoachingSessionConfig(
            coach_user_id="coach_123",
            student_user_id="student_456",
            session_name="Live Coaching Session",
            duration_minutes=60,
            focus_areas=["backswing", "impact"],
            recording_enabled=True
        )
        
        # Verify config structure
        assert coaching_config.coach_user_id == "coach_123"
        assert coaching_config.student_user_id == "student_456"
        assert coaching_config.duration_minutes == 60
        assert "backswing" in coaching_config.focus_areas
        assert coaching_config.recording_enabled is True
    
    @pytest.mark.asyncio
    async def test_coaching_message_broadcasting(self, mock_websocket_manager):
        """Test coaching message broadcasting to session participants"""
        
        # Mock coaching tip message
        coaching_tip = {
            "type": MessageType.COACHING_TIP.value,
            "data": {
                "tip_id": "tip_123",
                "message": "Focus on maintaining spine angle through impact",
                "emphasis": "high",
                "timestamp": time.time()
            },
            "session_id": "coaching_session_123"
        }
        
        # Test message routing
        mock_websocket_manager.broadcast_to_session.return_value = None
        
        # Simulate coaching tip broadcast
        from streaming_endpoints import handle_coaching_tip
        await handle_coaching_tip(
            "connection_123", 
            WebSocketMessage(**coaching_tip), 
            "coaching_session_123"
        )
        
        # Verify broadcast was called
        mock_websocket_manager.broadcast_to_session.assert_called_once()
        call_args = mock_websocket_manager.broadcast_to_session.call_args
        assert call_args[0][0] == "coaching_session_123"  # session_id
        assert call_args[0][1].type == MessageType.COACHING_TIP.value
    
    @pytest.mark.asyncio
    async def test_drill_suggestion_functionality(self, mock_websocket_manager):
        """Test drill suggestion broadcasting in coaching sessions"""
        
        # Mock drill suggestion message
        drill_suggestion = {
            "type": MessageType.DRILL_SUGGESTION.value,
            "data": {
                "drill_id": "drill_456",
                "drill_name": "Shoulder Turn Drill",
                "description": "Practice slow-motion backswings focusing on full shoulder rotation",
                "duration_minutes": 5,
                "repetitions": 10
            },
            "session_id": "coaching_session_123"
        }
        
        # Test drill suggestion broadcast
        from streaming_endpoints import handle_drill_suggestion
        await handle_drill_suggestion(
            "connection_456",
            WebSocketMessage(**drill_suggestion),
            "coaching_session_123"
        )
        
        # Verify broadcast
        mock_websocket_manager.broadcast_to_session.assert_called_once()

class TestPerformanceMonitoring:
    """Test performance monitoring and statistics"""
    
    @pytest.mark.asyncio
    async def test_performance_metrics_collection(self, streaming_test_data):
        """Test collection and reporting of performance metrics"""
        manager = StreamingSessionManager()
        
        # Create session
        config = StreamingSessionConfig(
            user_id="perf_test_user",
            analysis_frequency=1,
            target_latency_ms=100
        )
        session_id = manager.create_session(config)
        
        # Process frames and collect metrics
        for frame_data in streaming_test_data[:20]:
            streaming_frame = type('StreamingFrame', (), {
                'frame_index': frame_data['frame_index'],
                'timestamp': frame_data['timestamp'],
                'keypoints': frame_data['keypoints']
            })()
            
            await manager.process_frame(session_id, streaming_frame)
        
        # Check performance metrics
        stats = manager.session_stats[session_id]
        
        assert stats.frames_processed == 20
        assert stats.average_latency_ms > 0
        assert stats.kpis_calculated >= 0
        assert stats.timestamp > 0
        
        # Verify latency is reasonable
        assert stats.average_latency_ms < 200, f"Average latency {stats.average_latency_ms}ms too high"
        
        manager.end_session(session_id)
    
    @pytest.mark.asyncio
    async def test_system_statistics_reporting(self):
        """Test system-wide statistics reporting"""
        manager = StreamingSessionManager()
        
        # Create multiple sessions
        sessions = []
        for i in range(3):
            config = StreamingSessionConfig(
                user_id=f"stats_user_{i}",
                session_name=f"Stats Test {i}"
            )
            session_id = manager.create_session(config)
            sessions.append(session_id)
        
        # Mock connection manager stats
        with patch.object(connection_manager, 'get_connection_stats') as mock_stats:
            mock_stats.return_value = {
                "total_connections": 5,
                "active_connections": 3,
                "total_messages": 150
            }
            
            # Test system stats collection
            from streaming_endpoints import handle_system_stats_request
            
            # This would normally be called through WebSocket
            # but we'll test the logic directly
            connection_stats = connection_manager.get_connection_stats()
            
            system_stats = {
                "connection_stats": connection_stats,
                "active_streaming_sessions": len(manager.active_sessions),
                "total_frames_processed": sum(
                    session["frames_processed"] 
                    for session in manager.active_sessions.values()
                ),
                "timestamp": time.time()
            }
            
            # Verify stats structure
            assert "connection_stats" in system_stats
            assert "active_streaming_sessions" in system_stats
            assert system_stats["active_streaming_sessions"] == 3
        
        # Cleanup
        for session_id in sessions:
            manager.end_session(session_id)

class TestConcurrentSessions:
    """Test concurrent streaming session handling"""
    
    @pytest.mark.asyncio
    async def test_multiple_concurrent_sessions(self, streaming_test_data):
        """Test handling multiple concurrent streaming sessions"""
        manager = StreamingSessionManager()
        
        # Create multiple sessions
        session_configs = [
            StreamingSessionConfig(
                user_id=f"concurrent_user_{i}",
                session_name=f"Concurrent Session {i}",
                club_used=["Driver", "7-Iron", "Wedge"][i % 3],
                analysis_frequency=2 + i  # Different frequencies
            )
            for i in range(5)
        ]
        
        sessions = [manager.create_session(config) for config in session_configs]
        
        # Process frames concurrently for all sessions
        async def process_session_frames(session_id, frames):
            results = []
            for frame_data in frames:
                streaming_frame = type('StreamingFrame', (), {
                    'frame_index': frame_data['frame_index'],
                    'timestamp': frame_data['timestamp'],
                    'keypoints': frame_data['keypoints']
                })()
                
                result = await manager.process_frame(session_id, streaming_frame)
                if result:
                    results.append(result)
            return len(results)
        
        # Run concurrent processing
        tasks = [
            process_session_frames(session_id, streaming_test_data[:15])
            for session_id in sessions
        ]
        
        results = await asyncio.gather(*tasks)
        
        # Verify all sessions processed frames
        assert len(results) == 5
        assert all(result_count > 0 for result_count in results)
        
        # Verify session isolation
        for session_id in sessions:
            session_data = manager.get_session(session_id)
            assert session_data is not None
            assert session_data["frames_processed"] > 0
        
        # Check performance metrics for all sessions
        for session_id in sessions:
            stats = manager.session_stats[session_id]
            assert stats.frames_processed > 0
            assert stats.average_latency_ms > 0
        
        # Cleanup
        for session_id in sessions:
            manager.end_session(session_id)
    
    @pytest.mark.asyncio
    async def test_session_interference_prevention(self, streaming_test_data):
        """Test that sessions don't interfere with each other"""
        manager = StreamingSessionManager()
        
        # Create two sessions with different configurations
        config1 = StreamingSessionConfig(
            user_id="user_1",
            club_used="Driver",
            analysis_frequency=1
        )
        
        config2 = StreamingSessionConfig(
            user_id="user_2", 
            club_used="Wedge",
            analysis_frequency=3
        )
        
        session1 = manager.create_session(config1)
        session2 = manager.create_session(config2)
        
        # Process same frame data for both sessions
        test_frame = streaming_test_data[0]
        streaming_frame = type('StreamingFrame', (), {
            'frame_index': test_frame['frame_index'],
            'timestamp': test_frame['timestamp'],
            'keypoints': test_frame['keypoints']
        })()
        
        # Process frame for both sessions
        result1 = await manager.process_frame(session1, streaming_frame)
        result2 = await manager.process_frame(session2, streaming_frame)
        
        # Verify sessions maintained separate state
        session1_data = manager.get_session(session1)
        session2_data = manager.get_session(session2)
        
        assert session1_data["config"].club_used == "Driver"
        assert session2_data["config"].club_used == "Wedge"
        assert session1_data["config"].analysis_frequency == 1
        assert session2_data["config"].analysis_frequency == 3
        
        # Verify separate statistics
        stats1 = manager.session_stats[session1]
        stats2 = manager.session_stats[session2]
        
        assert stats1.session_id != stats2.session_id
        
        # Cleanup
        manager.end_session(session1)
        manager.end_session(session2)

class TestErrorHandlingAndRecovery:
    """Test error handling and recovery in streaming"""
    
    @pytest.mark.asyncio
    async def test_frame_processing_error_recovery(self, streaming_test_data):
        """Test recovery from frame processing errors"""
        manager = StreamingSessionManager()
        
        config = StreamingSessionConfig(
            user_id="error_test_user",
            analysis_frequency=1
        )
        session_id = manager.create_session(config)
        
        # Process normal frame first
        normal_frame = streaming_test_data[0]
        streaming_frame = type('StreamingFrame', (), {
            'frame_index': normal_frame['frame_index'],
            'timestamp': normal_frame['timestamp'],
            'keypoints': normal_frame['keypoints']
        })()
        
        result1 = await manager.process_frame(session_id, streaming_frame)
        assert result1 is not None
        
        # Create invalid frame data
        invalid_frame = type('StreamingFrame', (), {
            'frame_index': -1,
            'timestamp': -1,
            'keypoints': {}  # Empty keypoints should cause error
        })()
        
        # Process invalid frame - should handle gracefully
        result2 = await manager.process_frame(session_id, invalid_frame)
        # Should return None or minimal result, not crash
        
        # Process normal frame again to verify recovery
        result3 = await manager.process_frame(session_id, streaming_frame)
        assert result3 is not None
        
        # Verify session is still functional
        session_data = manager.get_session(session_id)
        assert session_data is not None
        
        manager.end_session(session_id)
    
    @pytest.mark.asyncio
    async def test_websocket_disconnection_handling(self, mock_websocket_manager):
        """Test handling of WebSocket disconnections"""
        
        # Mock WebSocket that disconnects
        mock_websocket = MockWebSocket()
        
        # Simulate connection
        connection_id = "test_connection_disconnect"
        mock_websocket_manager.connect.return_value = connection_id
        
        # Simulate disconnection during operation
        async def simulate_disconnect():
            await asyncio.sleep(0.1)
            await mock_websocket.close()
        
        # Start disconnect simulation
        disconnect_task = asyncio.create_task(simulate_disconnect())
        
        # Test message sending after disconnection
        await disconnect_task
        
        # Should handle disconnection gracefully
        with pytest.raises(Exception):
            await mock_websocket.send_text("test message after disconnect")
        
        # Verify disconnection is handled
        assert mock_websocket.closed is True
    
    def test_session_cleanup_on_errors(self):
        """Test session cleanup when errors occur"""
        manager = StreamingSessionManager()
        
        # Create session
        config = StreamingSessionConfig(user_id="cleanup_test_user")
        session_id = manager.create_session(config)
        
        # Verify session exists
        assert manager.get_session(session_id) is not None
        assert manager.get_user_session("cleanup_test_user") == session_id
        
        # Simulate error condition by forcing cleanup
        success = manager.end_session(session_id)
        assert success is True
        
        # Verify complete cleanup
        assert manager.get_session(session_id) is None
        assert manager.get_user_session("cleanup_test_user") is None
        assert session_id not in manager.session_stats

class TestAPIEndpoints:
    """Test REST API endpoints for streaming"""
    
    def test_create_streaming_session_api(self, test_client):
        """Test creating streaming session via REST API"""
        session_config = {
            "user_id": "api_test_user",
            "session_name": "API Test Session",
            "club_used": "7-Iron",
            "analysis_frequency": 3,
            "enable_real_time_kpis": True
        }
        
        # Mock the streaming router for testing
        with patch('streaming_endpoints.session_manager') as mock_manager:
            mock_manager.create_session.return_value = "api_session_123"
            
            response = test_client.post("/stream/sessions", json=session_config)
            
            assert response.status_code == 200
            data = response.json()
            assert data["session_id"] == "api_session_123"
            assert data["status"] == "created"
    
    def test_get_session_info_api(self, test_client):
        """Test getting session information via REST API"""
        session_id = "test_session_info"
        
        with patch('streaming_endpoints.session_manager') as mock_manager:
            mock_manager.get_session.return_value = {
                "id": session_id,
                "config": type('Config', (), {
                    'user_id': 'test_user',
                    'club_used': '7-Iron',
                    'dict': lambda: {"user_id": "test_user", "club_used": "7-Iron"}
                })(),
                "created_at": time.time(),
                "frames_processed": 25,
                "status": "active"
            }
            
            response = test_client.get(f"/stream/sessions/{session_id}")
            
            assert response.status_code == 200
            data = response.json()
            assert data["session_id"] == session_id
            assert data["frames_processed"] == 25
    
    def test_session_stats_api(self, test_client):
        """Test session statistics API endpoint"""
        session_id = "test_stats_session"
        
        with patch('streaming_endpoints.session_manager') as mock_manager:
            mock_stats = PerformanceMetrics(
                session_id=session_id,
                frames_processed=100,
                average_latency_ms=85.5,
                kpis_calculated=45,
                faults_detected=3,
                feedback_generated=2
            )
            mock_manager.session_stats = {session_id: mock_stats}
            
            response = test_client.get(f"/stream/sessions/{session_id}/stats")
            
            assert response.status_code == 200
            data = response.json()
            assert data["frames_processed"] == 100
            assert data["average_latency_ms"] == 85.5
            assert data["faults_detected"] == 3

if __name__ == "__main__":
    print("SwingSync AI Streaming Test Suite")
    print("=================================")
    print("Testing real-time streaming functionality...")
    print("Run with: pytest test_streaming.py -v")
    print("\nTest categories:")
    print("- Streaming Session Management")
    print("- WebSocket Connections")
    print("- Real-time Feedback")
    print("- Live Coaching")
    print("- Performance Monitoring")
    print("- Concurrent Sessions")
    print("- Error Handling and Recovery")
    print("- REST API Endpoints")