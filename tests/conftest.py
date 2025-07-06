"""
Pytest Configuration and Shared Fixtures for SwingSync AI Test Suite.

This module provides:
- Test configuration and settings
- Shared fixtures for all test modules
- Database setup and cleanup for testing
- Mock API services for isolated testing
- Performance monitoring utilities
- Test data management and cleanup

Key Features:
- Isolated test database per test session
- Mock Gemini API responses
- Streaming test server setup
- Performance metrics collection
- Automatic test data cleanup
- Parameterized test scenarios
"""

import asyncio
import os
import pytest
import tempfile
import time
from typing import Dict, List, Any, Generator, AsyncGenerator
from unittest.mock import Mock, AsyncMock, patch
import sqlite3
from datetime import datetime

# FastAPI testing
from fastapi.testclient import TestClient
from httpx import AsyncClient

# Database testing
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.pool import StaticPool

# Project imports
from database import Base, get_db, User, SwingSession, init_database
from streaming_endpoints import router as streaming_router
from user_management import create_user, authenticate_user
from websocket_manager import connection_manager
from live_analysis import LiveAnalysisEngine
from mock_data_factory import (
    create_realistic_swing, create_mock_user, create_mock_session_data,
    create_mock_gemini_response, generate_streaming_session,
    SwingQuality, ClubType, create_performance_test_data
)

# Test configuration
TEST_DATABASE_URL = "sqlite:///:memory:"
ENABLE_PERFORMANCE_MONITORING = True
TEST_DATA_CLEANUP = True

# Performance tracking
performance_metrics = {
    "test_start_time": None,
    "test_durations": {},
    "memory_usage": {},
    "api_response_times": []
}

@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

@pytest.fixture(scope="session")
def performance_tracker():
    """Track performance metrics across test session"""
    performance_metrics["test_start_time"] = time.time()
    yield performance_metrics
    
    # Print performance summary at end of session
    if ENABLE_PERFORMANCE_MONITORING:
        print("\n" + "="*50)
        print("PERFORMANCE TEST SUMMARY")
        print("="*50)
        
        total_time = time.time() - performance_metrics["test_start_time"]
        print(f"Total test session time: {total_time:.2f}s")
        
        if performance_metrics["test_durations"]:
            slowest_tests = sorted(
                performance_metrics["test_durations"].items(), 
                key=lambda x: x[1], 
                reverse=True
            )[:5]
            
            print("\nSlowest tests:")
            for test_name, duration in slowest_tests:
                print(f"  {test_name}: {duration:.3f}s")
        
        if performance_metrics["api_response_times"]:
            avg_response_time = sum(performance_metrics["api_response_times"]) / len(performance_metrics["api_response_times"])
            max_response_time = max(performance_metrics["api_response_times"])
            print(f"\nAPI Response Times:")
            print(f"  Average: {avg_response_time:.3f}s")
            print(f"  Maximum: {max_response_time:.3f}s")
            print(f"  Total API calls: {len(performance_metrics['api_response_times'])}")

@pytest.fixture
def measure_performance(request, performance_tracker):
    """Measure individual test performance"""
    test_name = request.node.name
    start_time = time.time()
    
    yield
    
    duration = time.time() - start_time
    performance_metrics["test_durations"][test_name] = duration
    
    if duration > 1.0:  # Warn about slow tests
        print(f"\nWARNING: Slow test detected: {test_name} took {duration:.3f}s")

# Database fixtures
@pytest.fixture(scope="session")
def test_engine():
    """Create test database engine"""
    engine = create_engine(
        TEST_DATABASE_URL,
        connect_args={
            "check_same_thread": False,
            "isolation_level": None,
        },
        poolclass=StaticPool,
        echo=False  # Set to True for SQL debugging
    )
    
    # Create all tables
    Base.metadata.create_all(bind=engine)
    yield engine
    
    # Cleanup
    Base.metadata.drop_all(bind=engine)

@pytest.fixture
def test_db_session(test_engine) -> Generator[Session, None, None]:
    """Create a test database session with automatic rollback"""
    connection = test_engine.connect()
    transaction = connection.begin()
    
    # Create session
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=connection)
    session = TestingSessionLocal()
    
    try:
        yield session
    finally:
        session.close()
        transaction.rollback()
        connection.close()

@pytest.fixture
def override_get_db(test_db_session):
    """Override the get_db dependency for testing"""
    def _override_get_db():
        try:
            yield test_db_session
        finally:
            pass
    return _override_get_db

# Mock users and data fixtures
@pytest.fixture
def mock_user_data():
    """Create mock user data for testing"""
    return create_mock_user(
        user_id="test_user_123",
        skill_level="intermediate"
    )

@pytest.fixture
def test_user(test_db_session, mock_user_data):
    """Create a test user in the database"""
    user = User(**mock_user_data)
    test_db_session.add(user)
    test_db_session.commit()
    test_db_session.refresh(user)
    return user

@pytest.fixture
def multiple_test_users(test_db_session):
    """Create multiple test users for relationship testing"""
    users = []
    for i in range(5):
        user_data = create_mock_user(skill_level=["beginner", "intermediate", "advanced"][i % 3])
        user = User(**user_data)
        test_db_session.add(user)
        users.append(user)
    
    test_db_session.commit()
    for user in users:
        test_db_session.refresh(user)
    
    return users

@pytest.fixture
def mock_swing_session_data():
    """Create mock swing session data"""
    return create_mock_session_data("test_user_123")

@pytest.fixture
def test_swing_session(test_db_session, test_user, mock_swing_session_data):
    """Create a test swing session in the database"""
    session_data = mock_swing_session_data.copy()
    session_data["user_id"] = test_user.id
    
    session = SwingSession(**session_data)
    test_db_session.add(session)
    test_db_session.commit()
    test_db_session.refresh(session)
    return session

# Swing data fixtures
@pytest.fixture
def good_swing_data():
    """Create high-quality swing data for testing"""
    return create_realistic_swing(
        session_id="test_good_swing",
        club_type=ClubType.MID_IRON,
        quality=SwingQuality.GOOD
    )

@pytest.fixture
def poor_swing_data():
    """Create poor-quality swing data for fault detection testing"""
    return create_realistic_swing(
        session_id="test_poor_swing",
        club_type=ClubType.MID_IRON,
        quality=SwingQuality.POOR,
        specific_faults=["insufficient_shoulder_turn", "cupped_wrist"]
    )

@pytest.fixture
def driver_swing_data():
    """Create driver swing data for club-specific testing"""
    return create_realistic_swing(
        session_id="test_driver_swing",
        club_type=ClubType.DRIVER,
        quality=SwingQuality.GOOD
    )

@pytest.fixture
def wedge_swing_data():
    """Create wedge swing data for club-specific testing"""
    return create_realistic_swing(
        session_id="test_wedge_swing",
        club_type=ClubType.WEDGE,
        quality=SwingQuality.GOOD
    )

@pytest.fixture
def streaming_test_data():
    """Create streaming frame data for real-time testing"""
    return generate_streaming_session(duration_seconds=10.0, fps=30.0)

@pytest.fixture
def performance_test_dataset():
    """Create large dataset for performance testing"""
    return create_performance_test_data(num_sessions=50)  # Smaller for faster tests

# Mock API fixtures
@pytest.fixture
def mock_gemini_api():
    """Mock Gemini API responses"""
    with patch('feedback_generation.generate_realtime_feedback') as mock_feedback:
        mock_feedback.return_value = asyncio.coroutine(lambda: create_mock_gemini_response(
            "test fault context", "7-Iron"
        ))()
        yield mock_feedback

@pytest.fixture
def mock_gemini_streaming():
    """Mock Gemini API for streaming responses"""
    async def mock_streaming_response(*args, **kwargs):
        await asyncio.sleep(0.1)  # Simulate API delay
        return create_mock_gemini_response("streaming context")
    
    with patch('streaming_endpoints.generate_instant_feedback') as mock_stream:
        mock_stream.side_effect = mock_streaming_response
        yield mock_stream

# FastAPI testing fixtures
@pytest.fixture
def test_client():
    """Create FastAPI test client"""
    from main import app  # Assuming main.py exists
    return TestClient(app)

@pytest.fixture
async def async_client():
    """Create async FastAPI test client"""
    from main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        yield client

# Streaming and WebSocket fixtures
@pytest.fixture
def mock_websocket_manager():
    """Mock WebSocket manager for testing"""
    mock_manager = Mock()
    mock_manager.connect = AsyncMock(return_value="test_connection_id")
    mock_manager.disconnect = AsyncMock()
    mock_manager.send_message = AsyncMock()
    mock_manager.receive_message = AsyncMock()
    mock_manager.broadcast_to_session = AsyncMock()
    
    return mock_manager

@pytest.fixture
def live_analysis_engine():
    """Create live analysis engine for testing"""
    return LiveAnalysisEngine()

@pytest.fixture
def mock_streaming_session():
    """Mock streaming session configuration"""
    return {
        "session_id": "test_streaming_session",
        "user_id": "test_user_123",
        "session_name": "Test Live Analysis",
        "club_used": "7-Iron",
        "skill_level": "intermediate",
        "feedback_mode": "streaming",
        "analysis_frequency": 3,
        "feedback_threshold": 0.6,
        "enable_real_time_kpis": True,
        "enable_instant_feedback": True,
        "target_latency_ms": 100
    }

# Parameterized test data
@pytest.fixture(params=[
    ClubType.DRIVER,
    ClubType.MID_IRON,
    ClubType.WEDGE
])
def club_type_param(request):
    """Parameterized club types for testing"""
    return request.param

@pytest.fixture(params=[
    SwingQuality.EXCELLENT,
    SwingQuality.GOOD,
    SwingQuality.AVERAGE,
    SwingQuality.POOR
])
def swing_quality_param(request):
    """Parameterized swing qualities for testing"""
    return request.param

@pytest.fixture(params=[
    ["insufficient_shoulder_turn"],
    ["excessive_hip_hinge"],
    ["cupped_wrist"],
    ["lateral_sway"],
    ["insufficient_shoulder_turn", "cupped_wrist"],
    []
])
def fault_combinations_param(request):
    """Parameterized fault combinations for testing"""
    return request.param

# Test environment setup
@pytest.fixture(autouse=True)
def test_environment_setup():
    """Setup test environment"""
    # Set test environment variables
    os.environ["TESTING"] = "true"
    os.environ["DATABASE_URL"] = TEST_DATABASE_URL
    
    yield
    
    # Cleanup environment
    if "TESTING" in os.environ:
        del os.environ["TESTING"]

@pytest.fixture
def temp_file_cleanup():
    """Temporary file cleanup for tests that create files"""
    temp_files = []
    
    def create_temp_file(suffix=".tmp"):
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
        temp_files.append(temp_file.name)
        return temp_file.name
    
    yield create_temp_file
    
    # Cleanup all temporary files
    if TEST_DATA_CLEANUP:
        for temp_file in temp_files:
            try:
                os.unlink(temp_file)
            except FileNotFoundError:
                pass

# Performance testing utilities
@pytest.fixture
def latency_monitor():
    """Monitor API latency during tests"""
    latencies = []
    
    def measure_latency(func):
        def wrapper(*args, **kwargs):
            start_time = time.time()
            result = func(*args, **kwargs)
            latency = time.time() - start_time
            latencies.append(latency)
            performance_metrics["api_response_times"].append(latency)
            return result
        return wrapper
    
    yield measure_latency, latencies

@pytest.fixture
async def async_latency_monitor():
    """Monitor async API latency during tests"""
    latencies = []
    
    def measure_async_latency(func):
        async def wrapper(*args, **kwargs):
            start_time = time.time()
            result = await func(*args, **kwargs)
            latency = time.time() - start_time
            latencies.append(latency)
            performance_metrics["api_response_times"].append(latency)
            return result
        return wrapper
    
    yield measure_async_latency, latencies

# Data validation fixtures
@pytest.fixture
def validation_schemas():
    """Pydantic schemas for data validation in tests"""
    from pydantic import BaseModel, Field
    from typing import List, Optional
    
    class TestSwingValidation(BaseModel):
        session_id: str
        user_id: str
        club_used: str
        frames: List[Dict[str, Any]]
        p_system_classification: List[Dict[str, Any]]
        video_fps: float
    
    class TestKPIValidation(BaseModel):
        p_position: str
        kpi_name: str
        value: float
        unit: str
        ideal_range: Optional[Tuple[float, float]]
    
    return {
        "swing": TestSwingValidation,
        "kpi": TestKPIValidation
    }

# Error simulation fixtures
@pytest.fixture
def error_simulation():
    """Simulate various error conditions for testing"""
    def simulate_database_error():
        with patch('database.SessionLocal') as mock_session:
            mock_session.side_effect = Exception("Database connection failed")
            yield mock_session
    
    def simulate_api_timeout():
        with patch('httpx.AsyncClient.post') as mock_post:
            mock_post.side_effect = asyncio.TimeoutError("API request timed out")
            yield mock_post
    
    def simulate_invalid_pose_data():
        return {
            "frames": [{"invalid": "data"}],
            "session_id": "error_test",
            "user_id": "error_user"
        }
    
    return {
        "database_error": simulate_database_error,
        "api_timeout": simulate_api_timeout,
        "invalid_pose_data": simulate_invalid_pose_data
    }

# Test markers and custom configuration
def pytest_configure(config):
    """Configure pytest with custom markers"""
    config.addinivalue_line(
        "markers", "slow: marks tests as slow (deselect with '-m \"not slow\"')"
    )
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests"
    )
    config.addinivalue_line(
        "markers", "performance: marks tests as performance tests"
    )
    config.addinivalue_line(
        "markers", "streaming: marks tests as streaming/WebSocket tests"
    )
    config.addinivalue_line(
        "markers", "database: marks tests as database tests"
    )

# Test collection hooks
def pytest_collection_modifyitems(config, items):
    """Modify test collection to add markers automatically"""
    for item in items:
        # Add slow marker to tests that take > 1 second (estimated)
        if "performance" in item.nodeid or "integration" in item.nodeid:
            item.add_marker(pytest.mark.slow)
        
        # Add markers based on test file names
        if "test_streaming" in item.nodeid:
            item.add_marker(pytest.mark.streaming)
        elif "test_database" in item.nodeid:
            item.add_marker(pytest.mark.database)
        elif "test_integration" in item.nodeid:
            item.add_marker(pytest.mark.integration)
        elif "test_performance" in item.nodeid:
            item.add_marker(pytest.mark.performance)

# Cleanup hooks
@pytest.fixture(autouse=True)
def test_cleanup():
    """Automatic cleanup after each test"""
    yield
    
    # Clear any global state
    if hasattr(connection_manager, '_connections'):
        connection_manager._connections.clear()
    
    # Reset performance counters for isolated testing
    if ENABLE_PERFORMANCE_MONITORING:
        # Don't reset global metrics, just test-specific ones
        pass

if __name__ == "__main__":
    print("SwingSync AI Test Configuration")
    print("==============================")
    print(f"Test Database URL: {TEST_DATABASE_URL}")
    print(f"Performance Monitoring: {ENABLE_PERFORMANCE_MONITORING}")
    print(f"Auto Cleanup: {TEST_DATA_CLEANUP}")
    print("\nAvailable fixtures:")
    print("- Database: test_db_session, test_user, test_swing_session")
    print("- Mock Data: good_swing_data, poor_swing_data, streaming_test_data")
    print("- API Mocks: mock_gemini_api, mock_websocket_manager")
    print("- Performance: performance_tracker, latency_monitor")
    print("- Utilities: temp_file_cleanup, validation_schemas")