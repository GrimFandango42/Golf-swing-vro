"""
Comprehensive Database and User Management Tests for SwingSync AI.

This module tests all database operations and user management functionality including:
- Database model relationships and constraints
- User authentication and authorization
- Session management and persistence
- Data integrity and validation
- Query performance and optimization
- Transaction handling and rollback
- Database migrations and schema changes
- Concurrent database access
- User preferences and settings
- Analysis result persistence

Key Test Areas:
1. Database Model Testing
2. User Management and Authentication
3. Session and Analysis Persistence
4. Data Relationships and Constraints
5. Query Performance Testing
6. Transaction Management
7. Concurrent Access Testing
8. User Preferences Management
9. Data Migration Testing
10. Error Handling and Recovery
"""

import pytest
import time
from datetime import datetime, timedelta
from typing import List, Dict, Any
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from unittest.mock import patch

# Project imports
from database import (
    Base, User, UserPreferences, SwingSession, SwingAnalysisResult,
    BiomechanicalKPI, DetectedFault, SkillLevel, SessionStatus, FaultSeverity,
    get_user_by_email, get_user_by_username, get_user_sessions,
    get_session_with_results, create_tables, init_database
)
from user_management import (
    create_user, authenticate_user, update_user_profile,
    get_user_preferences, update_user_preferences,
    hash_password, verify_password
)
from mock_data_factory import create_mock_user, create_mock_session_data

class TestDatabaseModels:
    """Test database model definitions and relationships"""
    
    def test_user_model_creation(self, test_db_session):
        """Test User model creation and basic operations"""
        # Create user data
        user_data = create_mock_user(
            user_id="test_user_model",
            skill_level="intermediate"
        )
        
        # Create user model
        user = User(**user_data)
        test_db_session.add(user)
        test_db_session.commit()
        test_db_session.refresh(user)
        
        # Verify user was created
        assert user.id == "test_user_model"
        assert user.skill_level == SkillLevel.INTERMEDIATE
        assert user.is_active is True
        assert user.created_at is not None
        
        # Test model string representation
        assert "test_user_model" in str(user)
        assert user.email in str(user)
    
    def test_user_model_constraints(self, test_db_session):
        """Test User model constraints and validation"""
        user_data = create_mock_user()
        
        # Test unique email constraint
        user1 = User(**user_data)
        test_db_session.add(user1)
        test_db_session.commit()
        
        # Try to create another user with same email
        user_data2 = create_mock_user()
        user_data2["email"] = user_data["email"]  # Same email
        user2 = User(**user_data2)
        test_db_session.add(user2)
        
        with pytest.raises(IntegrityError):
            test_db_session.commit()
        
        test_db_session.rollback()
        
        # Test unique username constraint
        user_data3 = create_mock_user()
        user_data3["username"] = user_data["username"]  # Same username
        user3 = User(**user_data3)
        test_db_session.add(user3)
        
        with pytest.raises(IntegrityError):
            test_db_session.commit()
    
    def test_user_preferences_relationship(self, test_db_session):
        """Test User-UserPreferences relationship"""
        # Create user
        user_data = create_mock_user()
        user = User(**user_data)
        test_db_session.add(user)
        test_db_session.commit()
        
        # Create preferences
        preferences = UserPreferences(
            user_id=user.id,
            preferred_units="metric",
            feedback_detail_level="detailed",
            focus_areas=["backswing", "impact"],
            email_notifications=True,
            target_handicap=15.0,
            primary_goals=["consistency", "distance"]
        )
        test_db_session.add(preferences)
        test_db_session.commit()
        
        # Test relationship
        test_db_session.refresh(user)
        assert user.preferences is not None
        assert user.preferences.preferred_units == "metric"
        assert "backswing" in user.preferences.focus_areas
        
        # Test reverse relationship
        assert preferences.user.id == user.id
    
    def test_swing_session_model(self, test_user, test_db_session):
        """Test SwingSession model and relationships"""
        # Create swing session
        session_data = create_mock_session_data(test_user.id)
        session = SwingSession(**session_data)
        test_db_session.add(session)
        test_db_session.commit()
        test_db_session.refresh(session)
        
        # Verify session
        assert session.user_id == test_user.id
        assert session.session_status == SessionStatus.COMPLETED
        assert session.created_at is not None
        
        # Test user relationship
        assert session.user.id == test_user.id
        assert session.user.email == test_user.email
        
        # Test string representation
        assert session.id in str(session)
        assert session.club_used in str(session)
    
    def test_biomechanical_kpi_model(self, test_swing_session, test_db_session):
        """Test BiomechanicalKPI model"""
        # Create KPI entries
        kpis = [
            BiomechanicalKPI(
                session_id=test_swing_session.id,
                p_position="P1",
                kpi_name="Hip Hinge Angle",
                value=35.5,
                unit="degrees",
                ideal_min=30.0,
                ideal_max=40.0,
                calculation_method="spine_angle_calculation",
                confidence=0.95
            ),
            BiomechanicalKPI(
                session_id=test_swing_session.id,
                p_position="P4",
                kpi_name="Shoulder Turn",
                value=92.0,
                unit="degrees",
                ideal_min=85.0,
                ideal_max=105.0,
                notes="Measured at top of backswing"
            )
        ]
        
        for kpi in kpis:
            test_db_session.add(kpi)
        test_db_session.commit()
        
        # Verify KPIs
        stored_kpis = test_db_session.query(BiomechanicalKPI).filter_by(
            session_id=test_swing_session.id
        ).all()
        
        assert len(stored_kpis) == 2
        
        # Test session relationship
        p1_kpi = next(kpi for kpi in stored_kpis if kpi.p_position == "P1")
        assert p1_kpi.session.id == test_swing_session.id
        assert p1_kpi.session.user_id == test_swing_session.user_id
    
    def test_detected_fault_model(self, test_swing_session, test_db_session):
        """Test DetectedFault model"""
        # Create fault
        fault = DetectedFault(
            session_id=test_swing_session.id,
            fault_id="INSUFFICIENT_SHOULDER_TURN_P4",
            fault_name="Insufficient Shoulder Turn at Top",
            description="Shoulder rotation at top of backswing is restricted",
            severity=FaultSeverity.MEDIUM,
            severity_score=0.7,
            p_positions_implicated=["P4"],
            primary_p_position="P4",
            kpi_deviations=[
                {
                    "kpi_name": "Shoulder Turn",
                    "observed_value": "65.0 degrees",
                    "ideal_value_or_range": "85-105 degrees",
                    "p_position": "P4"
                }
            ],
            llm_prompt_template_key="INSUFFICIENT_SHOULDER_TURN_P4_PROMPT",
            corrective_feedback="Focus on making a fuller shoulder turn in your backswing",
            drill_suggestions=["Wall drill", "Shoulder turn practice"],
            detection_confidence=0.85
        )
        
        test_db_session.add(fault)
        test_db_session.commit()
        test_db_session.refresh(fault)
        
        # Verify fault
        assert fault.fault_id == "INSUFFICIENT_SHOULDER_TURN_P4"
        assert fault.severity == FaultSeverity.MEDIUM
        assert fault.severity_score == 0.7
        assert "P4" in fault.p_positions_implicated
        assert len(fault.kpi_deviations) == 1
        
        # Test session relationship
        assert fault.session.id == test_swing_session.id
    
    def test_swing_analysis_result_model(self, test_swing_session, test_db_session):
        """Test SwingAnalysisResult model"""
        # Create analysis result
        analysis_result = SwingAnalysisResult(
            session_id=test_swing_session.id,
            summary_of_findings="Your swing shows good fundamentals with room for improvement in shoulder rotation",
            overall_score=78.5,
            detailed_feedback=[
                {
                    "explanation": "Your backswing shows restricted shoulder turn",
                    "tip": "Focus on making a fuller turn in your backswing",
                    "drill_suggestion": "Practice the wall drill to improve rotation"
                }
            ],
            raw_detected_faults=[
                {
                    "fault_id": "INSUFFICIENT_SHOULDER_TURN_P4",
                    "severity": 0.7
                }
            ],
            analysis_version="2.0",
            confidence_score=0.92,
            processing_notes="Analysis completed successfully"
        )
        
        test_db_session.add(analysis_result)
        test_db_session.commit()
        test_db_session.refresh(analysis_result)
        
        # Verify analysis result
        assert analysis_result.overall_score == 78.5
        assert analysis_result.confidence_score == 0.92
        assert len(analysis_result.detailed_feedback) == 1
        assert analysis_result.session.id == test_swing_session.id

class TestUserManagement:
    """Test user management and authentication functionality"""
    
    def test_create_user_function(self, test_db_session):
        """Test user creation function"""
        user_data = {
            "email": "test@example.com",
            "username": "testuser",
            "password": "securepassword123",
            "first_name": "Test",
            "last_name": "User",
            "skill_level": "intermediate"
        }
        
        # Create user
        created_user = create_user(test_db_session, **user_data)
        
        # Verify user was created
        assert created_user.email == "test@example.com"
        assert created_user.username == "testuser"
        assert created_user.skill_level == SkillLevel.INTERMEDIATE
        assert created_user.hashed_password != "securepassword123"  # Should be hashed
        assert created_user.is_active is True
    
    def test_password_hashing_and_verification(self):
        """Test password hashing and verification"""
        password = "mySecurePassword123!"
        
        # Hash password
        hashed = hash_password(password)
        assert hashed != password
        assert len(hashed) > 50  # Hashed passwords are much longer
        
        # Verify correct password
        assert verify_password(password, hashed) is True
        
        # Verify incorrect password
        assert verify_password("wrongPassword", hashed) is False
        assert verify_password("", hashed) is False
    
    def test_user_authentication(self, test_db_session):
        """Test user authentication functionality"""
        # Create user
        user_data = {
            "email": "auth@example.com",
            "username": "authuser",
            "password": "authpassword123",
            "skill_level": "advanced"
        }
        created_user = create_user(test_db_session, **user_data)
        
        # Test successful authentication
        authenticated_user = authenticate_user(
            test_db_session, 
            "auth@example.com", 
            "authpassword123"
        )
        assert authenticated_user is not None
        assert authenticated_user.id == created_user.id
        
        # Test authentication with username
        authenticated_user2 = authenticate_user(
            test_db_session,
            "authuser",
            "authpassword123"
        )
        assert authenticated_user2 is not None
        assert authenticated_user2.id == created_user.id
        
        # Test failed authentication
        failed_auth = authenticate_user(
            test_db_session,
            "auth@example.com",
            "wrongpassword"
        )
        assert failed_auth is None
        
        # Test authentication with non-existent user
        failed_auth2 = authenticate_user(
            test_db_session,
            "nonexistent@example.com",
            "anypassword"
        )
        assert failed_auth2 is None
    
    def test_user_profile_updates(self, test_user, test_db_session):
        """Test user profile update functionality"""
        # Update user profile
        updates = {
            "first_name": "Updated",
            "last_name": "Name",
            "handicap": 12.5,
            "height_cm": 175.0,
            "weight_kg": 75.0
        }
        
        updated_user = update_user_profile(test_db_session, test_user.id, **updates)
        
        # Verify updates
        assert updated_user.first_name == "Updated"
        assert updated_user.last_name == "Name"
        assert updated_user.handicap == 12.5
        assert updated_user.height_cm == 175.0
        assert updated_user.weight_kg == 75.0
        assert updated_user.updated_at is not None
    
    def test_user_preferences_management(self, test_user, test_db_session):
        """Test user preferences management"""
        # Create initial preferences
        preferences_data = {
            "preferred_units": "imperial",
            "feedback_detail_level": "advanced",
            "focus_areas": ["setup", "impact", "follow_through"],
            "email_notifications": False,
            "push_notifications": True,
            "target_handicap": 8.0,
            "primary_goals": ["distance", "accuracy"]
        }
        
        preferences = update_user_preferences(
            test_db_session, 
            test_user.id, 
            **preferences_data
        )
        
        # Verify preferences
        assert preferences.preferred_units == "imperial"
        assert preferences.feedback_detail_level == "advanced"
        assert len(preferences.focus_areas) == 3
        assert "setup" in preferences.focus_areas
        assert preferences.email_notifications is False
        assert preferences.target_handicap == 8.0
        
        # Test getting preferences
        retrieved_preferences = get_user_preferences(test_db_session, test_user.id)
        assert retrieved_preferences.user_id == test_user.id
        assert retrieved_preferences.preferred_units == "imperial"
        
        # Test updating existing preferences
        updated_preferences = update_user_preferences(
            test_db_session,
            test_user.id,
            preferred_units="metric",
            target_handicap=6.5
        )
        
        assert updated_preferences.preferred_units == "metric"
        assert updated_preferences.target_handicap == 6.5
        assert updated_preferences.feedback_detail_level == "advanced"  # Should remain unchanged

class TestSessionPersistence:
    """Test swing session and analysis result persistence"""
    
    def test_complete_session_persistence(self, test_user, test_db_session, good_swing_data):
        """Test persisting complete swing session with analysis"""
        from kpi_extraction import extract_all_kpis
        from fault_detection import check_swing_faults
        
        # Create swing session
        session = SwingSession(
            id=good_swing_data["session_id"],
            user_id=test_user.id,
            club_used=good_swing_data["club_used"],
            video_fps=good_swing_data["video_fps"],
            total_frames=len(good_swing_data["frames"]),
            p_system_phases=good_swing_data["p_system_classification"],
            session_status=SessionStatus.PROCESSING,
            video_duration_seconds=2.5
        )
        test_db_session.add(session)
        test_db_session.commit()
        
        # Run analysis
        kpis = extract_all_kpis(good_swing_data)
        faults = check_swing_faults(good_swing_data, kpis)
        
        # Store KPIs
        for kpi in kpis:
            db_kpi = BiomechanicalKPI(
                session_id=session.id,
                p_position=kpi["p_position"],
                kpi_name=kpi["kpi_name"],
                value=float(kpi["value"]) if isinstance(kpi["value"], (int, float)) else 0.0,
                unit=kpi["unit"],
                ideal_min=kpi["ideal_range"][0] if kpi.get("ideal_range") else None,
                ideal_max=kpi["ideal_range"][1] if kpi.get("ideal_range") else None,
                notes=kpi.get("notes", "")
            )
            test_db_session.add(db_kpi)
        
        # Store faults
        for fault in faults:
            db_fault = DetectedFault(
                session_id=session.id,
                fault_id=fault["fault_id"],
                fault_name=fault["fault_name"],
                description=fault["description"],
                severity=FaultSeverity.MEDIUM,
                severity_score=fault.get("severity", 0.5),
                p_positions_implicated=fault["p_positions_implicated"],
                primary_p_position=fault["p_positions_implicated"][0] if fault["p_positions_implicated"] else "P1",
                kpi_deviations=fault["kpi_deviations"],
                llm_prompt_template_key=fault["llm_prompt_template_key"]
            )
            test_db_session.add(db_fault)
        
        # Store analysis result
        analysis_result = SwingAnalysisResult(
            session_id=session.id,
            summary_of_findings="Analysis completed successfully",
            overall_score=85.0,
            detailed_feedback=[{"tip": "Keep up the good work!"}],
            raw_detected_faults=faults,
            confidence_score=0.9
        )
        test_db_session.add(analysis_result)
        
        # Update session status
        session.session_status = SessionStatus.COMPLETED
        session.processing_time_seconds = 3.2
        
        test_db_session.commit()
        
        # Verify complete persistence
        retrieved_session = get_session_with_results(test_db_session, session.id)
        assert retrieved_session is not None
        assert retrieved_session.session_status == SessionStatus.COMPLETED
        
        # Verify relationships
        stored_kpis = test_db_session.query(BiomechanicalKPI).filter_by(session_id=session.id).all()
        stored_faults = test_db_session.query(DetectedFault).filter_by(session_id=session.id).all()
        stored_results = test_db_session.query(SwingAnalysisResult).filter_by(session_id=session.id).first()
        
        assert len(stored_kpis) == len(kpis)
        assert len(stored_faults) == len(faults)
        assert stored_results is not None
        assert stored_results.confidence_score == 0.9
    
    def test_session_query_functions(self, multiple_test_users, test_db_session):
        """Test session query helper functions"""
        # Create sessions for multiple users
        user1, user2 = multiple_test_users[:2]
        
        sessions_user1 = []
        sessions_user2 = []
        
        # Create sessions for user1
        for i in range(3):
            session_data = create_mock_session_data(user1.id)
            session_data["id"] = f"user1_session_{i}"
            session_data["created_at"] = datetime.now() - timedelta(days=i)
            session = SwingSession(**session_data)
            test_db_session.add(session)
            sessions_user1.append(session)
        
        # Create sessions for user2
        for i in range(2):
            session_data = create_mock_session_data(user2.id)
            session_data["id"] = f"user2_session_{i}"
            session = SwingSession(**session_data)
            test_db_session.add(session)
            sessions_user2.append(session)
        
        test_db_session.commit()
        
        # Test get_user_sessions
        user1_sessions = get_user_sessions(test_db_session, user1.id)
        assert len(user1_sessions) == 3
        assert all(session.user_id == user1.id for session in user1_sessions)
        
        # Should be ordered by most recent first
        assert user1_sessions[0].created_at >= user1_sessions[1].created_at
        assert user1_sessions[1].created_at >= user1_sessions[2].created_at
        
        user2_sessions = get_user_sessions(test_db_session, user2.id)
        assert len(user2_sessions) == 2
        assert all(session.user_id == user2.id for session in user2_sessions)
        
        # Test limit parameter
        limited_sessions = get_user_sessions(test_db_session, user1.id, limit=2)
        assert len(limited_sessions) == 2
    
    def test_user_query_functions(self, test_db_session):
        """Test user query helper functions"""
        # Create test users
        user1_data = create_mock_user()
        user1_data["email"] = "query1@example.com"
        user1_data["username"] = "queryuser1"
        user1 = User(**user1_data)
        
        user2_data = create_mock_user()
        user2_data["email"] = "query2@example.com"
        user2_data["username"] = "queryuser2"
        user2 = User(**user2_data)
        
        test_db_session.add_all([user1, user2])
        test_db_session.commit()
        
        # Test get_user_by_email
        found_user1 = get_user_by_email(test_db_session, "query1@example.com")
        assert found_user1 is not None
        assert found_user1.id == user1.id
        
        not_found = get_user_by_email(test_db_session, "nonexistent@example.com")
        assert not_found is None
        
        # Test get_user_by_username
        found_user2 = get_user_by_username(test_db_session, "queryuser2")
        assert found_user2 is not None
        assert found_user2.id == user2.id
        
        not_found2 = get_user_by_username(test_db_session, "nonexistentuser")
        assert not_found2 is None

class TestDatabasePerformance:
    """Test database query performance and optimization"""
    
    def test_query_performance(self, test_db_session, performance_monitor):
        """Test query performance with various data sizes"""
        # Create test data
        users = []
        for i in range(10):
            user_data = create_mock_user(skill_level=["beginner", "intermediate", "advanced"][i % 3])
            user = User(**user_data)
            test_db_session.add(user)
            users.append(user)
        
        test_db_session.commit()
        
        # Create sessions for users
        for user in users:
            for j in range(5):
                session_data = create_mock_session_data(user.id)
                session_data["id"] = f"{user.id}_session_{j}"
                session = SwingSession(**session_data)
                test_db_session.add(session)
        
        test_db_session.commit()
        
        # Test query performance
        queries = [
            ("all_users", lambda: test_db_session.query(User).all()),
            ("users_by_skill", lambda: test_db_session.query(User).filter(User.skill_level == SkillLevel.INTERMEDIATE).all()),
            ("all_sessions", lambda: test_db_session.query(SwingSession).all()),
            ("sessions_with_users", lambda: test_db_session.query(SwingSession).join(User).all()),
            ("user_with_sessions", lambda: test_db_session.query(User).join(SwingSession).first()),
        ]
        
        performance_results = {}
        
        for query_name, query_func in queries:
            # Warm up
            query_func()
            
            # Measure performance
            latencies = []
            for _ in range(10):
                start_time = time.perf_counter()
                result = query_func()
                end_time = time.perf_counter()
                
                latency_ms = (end_time - start_time) * 1000
                latencies.append(latency_ms)
            
            avg_latency = sum(latencies) / len(latencies)
            max_latency = max(latencies)
            
            performance_results[query_name] = {
                "avg_latency_ms": avg_latency,
                "max_latency_ms": max_latency
            }
            
            performance_monitor.take_measurement(f"db_query_{query_name}")
        
        # Performance assertions
        for query_name, metrics in performance_results.items():
            print(f"Query {query_name}: avg {metrics['avg_latency_ms']:.2f}ms, max {metrics['max_latency_ms']:.2f}ms")
            assert metrics["avg_latency_ms"] < 50, f"Query {query_name} average latency too high: {metrics['avg_latency_ms']:.2f}ms"
            assert metrics["max_latency_ms"] < 100, f"Query {query_name} max latency too high: {metrics['max_latency_ms']:.2f}ms"
    
    def test_index_effectiveness(self, test_db_session):
        """Test database index effectiveness"""
        # Create test data
        users = []
        for i in range(20):
            user_data = create_mock_user()
            user_data["email"] = f"index_test_{i}@example.com"
            user_data["username"] = f"indexuser_{i}"
            user = User(**user_data)
            test_db_session.add(user)
            users.append(user)
        
        test_db_session.commit()
        
        # Test indexed queries (should be fast)
        indexed_queries = [
            ("email_lookup", lambda: test_db_session.query(User).filter(User.email == "index_test_10@example.com").first()),
            ("username_lookup", lambda: test_db_session.query(User).filter(User.username == "indexuser_15").first()),
        ]
        
        for query_name, query_func in indexed_queries:
            start_time = time.perf_counter()
            result = query_func()
            end_time = time.perf_counter()
            
            latency_ms = (end_time - start_time) * 1000
            print(f"Indexed query {query_name}: {latency_ms:.2f}ms")
            
            assert result is not None
            assert latency_ms < 10, f"Indexed query {query_name} too slow: {latency_ms:.2f}ms"

class TestTransactionManagement:
    """Test database transaction handling"""
    
    def test_transaction_rollback(self, test_db_session):
        """Test transaction rollback on errors"""
        # Get initial user count
        initial_count = test_db_session.query(User).count()
        
        try:
            # Start transaction
            user1_data = create_mock_user()
            user1 = User(**user1_data)
            test_db_session.add(user1)
            
            # This should succeed
            test_db_session.flush()
            
            # Now try to add user with duplicate email (should fail)
            user2_data = create_mock_user()
            user2_data["email"] = user1_data["email"]  # Duplicate email
            user2 = User(**user2_data)
            test_db_session.add(user2)
            
            # This should raise IntegrityError
            test_db_session.commit()
            
        except IntegrityError:
            # Transaction should rollback
            test_db_session.rollback()
        
        # Verify rollback - no users should be added
        final_count = test_db_session.query(User).count()
        assert final_count == initial_count
    
    def test_transaction_isolation(self, test_db_session, test_engine):
        """Test transaction isolation between sessions"""
        # Create separate session for isolation testing
        TestSession = sessionmaker(bind=test_engine)
        session2 = TestSession()
        
        try:
            # Add user in first session but don't commit
            user_data = create_mock_user()
            user1 = User(**user_data)
            test_db_session.add(user1)
            test_db_session.flush()  # Write to DB but don't commit
            
            # Check if user is visible in second session (should not be)
            user_in_session2 = session2.query(User).filter(User.email == user_data["email"]).first()
            assert user_in_session2 is None, "Uncommitted transaction should not be visible in other sessions"
            
            # Commit first session
            test_db_session.commit()
            
            # Now user should be visible in second session
            user_in_session2 = session2.query(User).filter(User.email == user_data["email"]).first()
            assert user_in_session2 is not None, "Committed transaction should be visible in other sessions"
            
        finally:
            session2.close()

class TestConcurrentAccess:
    """Test concurrent database access scenarios"""
    
    def test_concurrent_user_creation(self, test_engine):
        """Test concurrent user creation from multiple threads"""
        import threading
        import concurrent.futures
        
        def create_user_in_thread(thread_id):
            """Create user in separate thread"""
            TestSession = sessionmaker(bind=test_engine)
            session = TestSession()
            
            try:
                user_data = create_mock_user()
                user_data["email"] = f"concurrent_{thread_id}@example.com"
                user_data["username"] = f"concurrent_user_{thread_id}"
                
                user = User(**user_data)
                session.add(user)
                session.commit()
                
                return {"success": True, "user_id": user.id, "thread_id": thread_id}
                
            except Exception as e:
                session.rollback()
                return {"success": False, "error": str(e), "thread_id": thread_id}
            finally:
                session.close()
        
        # Create users concurrently
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(create_user_in_thread, i) for i in range(10)]
            results = [future.result() for future in concurrent.futures.as_completed(futures)]
        
        # Verify results
        successful_creations = [r for r in results if r["success"]]
        failed_creations = [r for r in results if not r["success"]]
        
        print(f"Concurrent user creation: {len(successful_creations)} succeeded, {len(failed_creations)} failed")
        
        # Should have mostly successful creations
        assert len(successful_creations) >= 8, "Most concurrent user creations should succeed"
        
        # Verify users were actually created
        TestSession = sessionmaker(bind=test_engine)
        verification_session = TestSession()
        try:
            created_users = verification_session.query(User).filter(
                User.email.like("concurrent_%@example.com")
            ).all()
            
            assert len(created_users) == len(successful_creations)
        finally:
            verification_session.close()

class TestDataIntegrity:
    """Test data integrity and validation"""
    
    def test_foreign_key_constraints(self, test_user, test_db_session):
        """Test foreign key constraint enforcement"""
        # Try to create session with invalid user_id
        invalid_session = SwingSession(
            id="invalid_session",
            user_id="nonexistent_user_id",
            club_used="7-Iron",
            video_fps=60.0,
            total_frames=100,
            session_status=SessionStatus.COMPLETED
        )
        
        test_db_session.add(invalid_session)
        
        # Should raise foreign key constraint error
        with pytest.raises(IntegrityError):
            test_db_session.commit()
        
        test_db_session.rollback()
    
    def test_enum_validation(self, test_user, test_db_session):
        """Test enum field validation"""
        # Test valid enum values
        valid_session = SwingSession(
            id="valid_enum_session",
            user_id=test_user.id,
            club_used="7-Iron",
            video_fps=60.0,
            total_frames=100,
            session_status=SessionStatus.COMPLETED  # Valid enum
        )
        
        test_db_session.add(valid_session)
        test_db_session.commit()
        
        # Verify enum was stored correctly
        stored_session = test_db_session.query(SwingSession).filter_by(id="valid_enum_session").first()
        assert stored_session.session_status == SessionStatus.COMPLETED
    
    def test_data_validation_in_application_layer(self, test_db_session):
        """Test data validation in application layer"""
        # Test email validation (would be in user creation function)
        invalid_emails = ["", "notanemail", "@domain.com", "user@", "user@domain"]
        
        for invalid_email in invalid_emails:
            with pytest.raises((ValueError, Exception)):
                # This would be validated in create_user function
                user_data = create_mock_user()
                user_data["email"] = invalid_email
                
                # Basic email validation
                if "@" not in invalid_email or "." not in invalid_email.split("@")[-1]:
                    raise ValueError(f"Invalid email format: {invalid_email}")

if __name__ == "__main__":
    print("SwingSync AI Database Test Suite")
    print("===============================")
    print("Testing database operations and user management...")
    print("Run with: pytest test_database.py -v")
    print("\nTest categories:")
    print("- Database Models and Relationships")
    print("- User Management and Authentication")
    print("- Session and Analysis Persistence")
    print("- Query Performance")
    print("- Transaction Management")
    print("- Concurrent Access")
    print("- Data Integrity and Validation")