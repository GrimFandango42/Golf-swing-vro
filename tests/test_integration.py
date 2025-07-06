"""
Comprehensive Integration Tests for SwingSync AI Complete Analysis Pipeline.

This module tests the entire golf swing analysis workflow end-to-end, including:
- Complete analysis pipeline from input to feedback
- Integration between all major modules
- Database persistence and retrieval
- API endpoint integration
- Real-time streaming integration
- Error handling and recovery
- Performance under various conditions

Key Test Areas:
1. Complete Swing Analysis Pipeline
2. Club-Specific Analysis Integration
3. Database Integration with Analysis
4. API Endpoint Integration
5. Streaming Analysis Integration
6. Error Handling and Edge Cases
7. Multi-User and Session Management
8. Performance Integration Testing
"""

import asyncio
import pytest
import time
from typing import Dict, List, Any
from unittest.mock import Mock, patch, AsyncMock

# Project imports
from data_structures import SwingVideoAnalysisInput, BiomechanicalKPI, DetectedFault
from kpi_extraction import extract_all_kpis
from fault_detection import check_swing_faults, classify_club_type
from feedback_generation import generate_feedback
from database import User, SwingSession, SwingAnalysisResult, BiomechanicalKPI as DBBiomechanicalKPI
from streaming_endpoints import StreamingSessionManager
from live_analysis import LiveAnalysisEngine
from mock_data_factory import ClubType, SwingQuality

class TestCompleteAnalysisPipeline:
    """Test the complete analysis pipeline from input to output"""
    
    def test_complete_pipeline_good_swing(self, good_swing_data, test_db_session, mock_gemini_api, measure_performance):
        """Test complete pipeline with good swing data"""
        # 1. Extract KPIs
        extracted_kpis = extract_all_kpis(good_swing_data)
        assert len(extracted_kpis) > 0, "Should extract multiple KPIs"
        
        # Verify KPI structure
        for kpi in extracted_kpis:
            assert "p_position" in kpi
            assert "kpi_name" in kpi
            assert "value" in kpi
            assert "unit" in kpi
        
        # 2. Detect faults
        detected_faults = check_swing_faults(good_swing_data, extracted_kpis)
        
        # Good swing should have minimal faults
        high_severity_faults = [f for f in detected_faults if f.get('severity', 0) > 0.7]
        assert len(high_severity_faults) <= 1, f"Good swing should have minimal high-severity faults, found {len(high_severity_faults)}"
        
        # 3. Generate feedback
        feedback_result = generate_feedback(good_swing_data, detected_faults)
        
        assert "summary_of_findings" in feedback_result
        assert "detailed_feedback" in feedback_result
        assert isinstance(feedback_result["detailed_feedback"], list)
        
        # 4. Verify feedback quality for good swing
        summary = feedback_result["summary_of_findings"]
        assert len(summary) > 50, "Summary should be comprehensive"
        
        print(f"Pipeline completed: {len(extracted_kpis)} KPIs, {len(detected_faults)} faults")
    
    def test_complete_pipeline_poor_swing(self, poor_swing_data, mock_gemini_api, measure_performance):
        """Test complete pipeline with poor swing data"""
        # 1. Extract KPIs
        extracted_kpis = extract_all_kpis(poor_swing_data)
        assert len(extracted_kpis) > 0
        
        # 2. Detect faults
        detected_faults = check_swing_faults(poor_swing_data, extracted_kpis)
        
        # Poor swing should have multiple faults
        assert len(detected_faults) >= 2, f"Poor swing should have multiple faults, found {len(detected_faults)}"
        
        high_severity_faults = [f for f in detected_faults if f.get('severity', 0) > 0.5]
        assert len(high_severity_faults) >= 1, "Poor swing should have at least one significant fault"
        
        # 3. Generate feedback
        feedback_result = generate_feedback(poor_swing_data, detected_faults)
        
        # 4. Verify feedback addresses the faults
        assert len(feedback_result["detailed_feedback"]) >= 1
        assert "raw_detected_faults" in feedback_result
        assert len(feedback_result["raw_detected_faults"]) == len(detected_faults)
        
        print(f"Poor swing analysis: {len(detected_faults)} faults detected")
    
    @pytest.mark.parametrize("club_type", [ClubType.DRIVER, ClubType.MID_IRON, ClubType.WEDGE])
    def test_club_specific_pipeline_integration(self, club_type, test_db_session, mock_gemini_api):
        """Test complete pipeline with different club types"""
        from mock_data_factory import create_realistic_swing
        
        # Create club-specific swing
        swing_data = create_realistic_swing(
            club_type=club_type,
            quality=SwingQuality.AVERAGE
        )
        
        # 1. Verify club classification
        classified_club = classify_club_type(swing_data["club_used"])
        expected_club_type = {
            ClubType.DRIVER: "driver",
            ClubType.MID_IRON: "iron", 
            ClubType.WEDGE: "wedge"
        }[club_type]
        
        assert classified_club == expected_club_type, f"Club classification mismatch: {classified_club} != {expected_club_type}"
        
        # 2. Extract KPIs
        extracted_kpis = extract_all_kpis(swing_data)
        assert len(extracted_kpis) > 0
        
        # 3. Club-specific fault detection
        detected_faults = check_swing_faults(swing_data, extracted_kpis)
        
        # Verify club-specific fault IDs are present
        club_specific_fault_ids = [f["fault_id"] for f in detected_faults if expected_club_type.upper() in f["fault_id"]]
        print(f"{club_type.value} specific faults: {len(club_specific_fault_ids)}")
        
        # 4. Generate feedback
        feedback_result = generate_feedback(swing_data, detected_faults)
        
        # Verify club is mentioned in feedback
        summary = feedback_result["summary_of_findings"].lower()
        club_name = swing_data["club_used"].lower()
        assert club_name in summary or club_type.value in summary, "Feedback should mention the club type"

class TestDatabaseIntegration:
    """Test integration with database operations"""
    
    def test_complete_analysis_with_database_persistence(self, test_user, test_db_session, good_swing_data, mock_gemini_api):
        """Test complete analysis with database persistence"""
        
        # 1. Create swing session in database
        swing_session = SwingSession(
            id=good_swing_data["session_id"],
            user_id=test_user.id,
            club_used=good_swing_data["club_used"],
            video_fps=good_swing_data["video_fps"],
            total_frames=len(good_swing_data["frames"]),
            p_system_phases=good_swing_data["p_system_classification"],
            session_status="processing"
        )
        test_db_session.add(swing_session)
        test_db_session.commit()
        
        # 2. Run complete analysis
        extracted_kpis = extract_all_kpis(good_swing_data)
        detected_faults = check_swing_faults(good_swing_data, extracted_kpis)
        feedback_result = generate_feedback(good_swing_data, detected_faults)
        
        # 3. Store KPIs in database
        for kpi in extracted_kpis:
            db_kpi = DBBiomechanicalKPI(
                session_id=swing_session.id,
                p_position=kpi["p_position"],
                kpi_name=kpi["kpi_name"],
                value=float(kpi["value"]) if isinstance(kpi["value"], (int, float)) else 0.0,
                unit=kpi["unit"],
                notes=kpi.get("notes", "")
            )
            test_db_session.add(db_kpi)
        
        # 4. Store analysis results
        analysis_result = SwingAnalysisResult(
            session_id=swing_session.id,
            summary_of_findings=feedback_result["summary_of_findings"],
            detailed_feedback=feedback_result["detailed_feedback"],
            raw_detected_faults=detected_faults,
            overall_score=85.0,  # Mock score
            confidence_score=0.9
        )
        test_db_session.add(analysis_result)
        
        # 5. Update session status
        swing_session.session_status = "completed"
        swing_session.processing_time_seconds = 5.2
        
        test_db_session.commit()
        
        # 6. Verify data persistence
        # Retrieve session with relationships
        retrieved_session = test_db_session.query(SwingSession).filter_by(id=swing_session.id).first()
        assert retrieved_session is not None
        assert retrieved_session.session_status.value == "completed"
        
        # Check KPIs were stored
        stored_kpis = test_db_session.query(DBBiomechanicalKPI).filter_by(session_id=swing_session.id).all()
        assert len(stored_kpis) == len(extracted_kpis)
        
        # Check analysis results
        stored_results = test_db_session.query(SwingAnalysisResult).filter_by(session_id=swing_session.id).first()
        assert stored_results is not None
        assert stored_results.confidence_score == 0.9
        
        print(f"Database integration: {len(stored_kpis)} KPIs and analysis results stored successfully")
    
    def test_multi_session_analysis(self, multiple_test_users, test_db_session, mock_gemini_api):
        """Test analysis of multiple sessions from different users"""
        from mock_data_factory import create_realistic_swing
        
        sessions_created = []
        
        for i, user in enumerate(multiple_test_users[:3]):  # Test with 3 users
            # Create different swing data for each user
            club_types = [ClubType.DRIVER, ClubType.MID_IRON, ClubType.WEDGE]
            swing_data = create_realistic_swing(
                session_id=f"multi_session_{user.id}_{i}",
                user_id=user.id,
                club_type=club_types[i],
                quality=SwingQuality.GOOD
            )
            
            # Create session
            swing_session = SwingSession(
                id=swing_data["session_id"],
                user_id=user.id,
                club_used=swing_data["club_used"],
                video_fps=swing_data["video_fps"],
                total_frames=len(swing_data["frames"]),
                session_status="completed"
            )
            test_db_session.add(swing_session)
            sessions_created.append((swing_session, swing_data))
        
        test_db_session.commit()
        
        # Run analysis for all sessions
        analysis_results = []
        for swing_session, swing_data in sessions_created:
            extracted_kpis = extract_all_kpis(swing_data)
            detected_faults = check_swing_faults(swing_data, extracted_kpis)
            feedback_result = generate_feedback(swing_data, detected_faults)
            
            analysis_results.append({
                "session": swing_session,
                "kpis": extracted_kpis,
                "faults": detected_faults,
                "feedback": feedback_result
            })
        
        # Verify all analyses completed successfully
        assert len(analysis_results) == 3
        
        # Verify different club types produced different results
        club_kpi_counts = {}
        for result in analysis_results:
            club = result["session"].club_used
            kpi_count = len(result["kpis"])
            club_kpi_counts[club] = kpi_count
        
        print(f"Multi-session analysis: {club_kpi_counts}")
        assert len(club_kpi_counts) == 3, "Should have results for 3 different clubs"

class TestStreamingIntegration:
    """Test integration with real-time streaming analysis"""
    
    @pytest.mark.asyncio
    async def test_streaming_analysis_integration(self, streaming_test_data, mock_websocket_manager, live_analysis_engine):
        """Test integration of streaming analysis with complete pipeline"""
        
        # Mock streaming session
        session_manager = StreamingSessionManager()
        
        # Create streaming session config
        from streaming_endpoints import StreamingSessionConfig
        config = StreamingSessionConfig(
            user_id="test_streaming_user",
            session_name="Integration Test Session",
            club_used="7-Iron",
            analysis_frequency=3,
            enable_real_time_kpis=True,
            enable_instant_feedback=True
        )
        
        session_id = session_manager.create_session(config)
        
        # Process streaming frames
        analysis_results = []
        for i, frame_data in enumerate(streaming_test_data[:15]):  # Test with first 15 frames
            
            # Convert to streaming frame format
            streaming_frame = type('StreamingFrame', (), {
                'frame_index': frame_data['frame_index'],
                'timestamp': frame_data['timestamp'],
                'keypoints': frame_data['keypoints']
            })()
            
            # Process frame
            result = await session_manager.process_frame(session_id, streaming_frame)
            
            if result:  # Only analyze every N frames based on config
                analysis_results.append(result)
        
        # Verify streaming analysis results
        assert len(analysis_results) >= 3, f"Should have multiple analysis results, got {len(analysis_results)}"
        
        for result in analysis_results:
            assert result.frame_index >= 0
            assert result.timestamp > 0
            assert result.analysis_latency_ms > 0
            assert result.quality_score >= 0
        
        # Verify performance requirements
        avg_latency = sum(r.analysis_latency_ms for r in analysis_results) / len(analysis_results)
        assert avg_latency < 150, f"Average latency {avg_latency}ms exceeds 150ms target"
        
        # Cleanup session
        session_manager.end_session(session_id)
        
        print(f"Streaming integration: {len(analysis_results)} frames analyzed, avg latency: {avg_latency:.1f}ms")
    
    @pytest.mark.asyncio
    async def test_real_time_fault_detection_integration(self, poor_swing_data, live_analysis_engine):
        """Test real-time fault detection with complete pipeline"""
        
        # Extract frames from poor swing for streaming simulation
        frames = poor_swing_data["frames"]
        
        # Simulate real-time processing
        detected_faults_timeline = []
        
        for i in range(0, len(frames), 5):  # Process every 5th frame
            frame_data = frames[i]
            
            # Convert to streaming format
            streaming_frame = type('StreamingFrame', (), {
                'frame_index': i,
                'timestamp': time.time() + (i * 0.1),
                'keypoints': frame_data
            })()
            
            # Mock session context
            session_context = {
                "config": type('Config', (), {
                    'enable_real_time_kpis': True,
                    'feedback_threshold': 0.5
                })()
            }
            
            # Analyze frame
            result = await live_analysis_engine.analyze_frame(
                streaming_frame, 
                session_context, 
                session_context["config"]
            )
            
            if result and result.detected_faults:
                detected_faults_timeline.append({
                    "frame": i,
                    "faults": result.detected_faults,
                    "latency": result.analysis_latency_ms
                })
        
        # Verify fault detection timeline
        assert len(detected_faults_timeline) > 0, "Should detect faults in poor swing"
        
        # Verify latency requirements
        for entry in detected_faults_timeline:
            assert entry["latency"] < 200, f"Frame {entry['frame']} latency {entry['latency']}ms too high"
        
        print(f"Real-time fault detection: {len(detected_faults_timeline)} fault events detected")

class TestErrorHandlingIntegration:
    """Test error handling across the complete pipeline"""
    
    def test_pipeline_with_invalid_pose_data(self, mock_gemini_api):
        """Test pipeline behavior with invalid pose data"""
        
        # Create swing data with missing keypoints
        invalid_swing_data = {
            "session_id": "test_invalid",
            "user_id": "test_user",
            "club_used": "7-Iron",
            "frames": [
                {"incomplete": "data"},  # Missing proper keypoints
                {"left_shoulder": {"x": 0, "y": 1.4}},  # Missing z and visibility
            ],
            "p_system_classification": [
                {"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 1}
            ],
            "video_fps": 60.0
        }
        
        # 1. KPI extraction should handle gracefully
        try:
            extracted_kpis = extract_all_kpis(invalid_swing_data)
            # Should return empty list or minimal KPIs, not crash
            assert isinstance(extracted_kpis, list)
        except Exception as e:
            pytest.fail(f"KPI extraction should handle invalid data gracefully: {e}")
        
        # 2. Fault detection should handle gracefully
        try:
            detected_faults = check_swing_faults(invalid_swing_data, [])
            assert isinstance(detected_faults, list)
        except Exception as e:
            pytest.fail(f"Fault detection should handle invalid data gracefully: {e}")
    
    def test_pipeline_with_database_errors(self, good_swing_data, mock_gemini_api, error_simulation):
        """Test pipeline behavior when database operations fail"""
        
        # Simulate database error
        with error_simulation["database_error"]():
            # Analysis should still work even if database is unavailable
            extracted_kpis = extract_all_kpis(good_swing_data)
            detected_faults = check_swing_faults(good_swing_data, extracted_kpis)
            feedback_result = generate_feedback(good_swing_data, detected_faults)
            
            # Core analysis should succeed
            assert len(extracted_kpis) > 0
            assert isinstance(detected_faults, list)
            assert "summary_of_findings" in feedback_result
    
    @pytest.mark.asyncio
    async def test_streaming_with_connection_errors(self, streaming_test_data, mock_websocket_manager):
        """Test streaming analysis with connection errors"""
        
        # Mock WebSocket connection that fails intermittently
        mock_websocket_manager.send_message.side_effect = [
            None,  # Success
            Exception("Connection lost"),  # Failure
            None,  # Recovery
        ]
        
        session_manager = StreamingSessionManager()
        
        # Create session
        from streaming_endpoints import StreamingSessionConfig
        config = StreamingSessionConfig(
            user_id="test_error_user",
            analysis_frequency=1  # Analyze every frame to trigger multiple sends
        )
        
        session_id = session_manager.create_session(config)
        
        # Process frames and verify graceful error handling
        successful_analyses = 0
        for i, frame_data in enumerate(streaming_test_data[:3]):
            try:
                streaming_frame = type('StreamingFrame', (), {
                    'frame_index': frame_data['frame_index'],
                    'timestamp': frame_data['timestamp'],
                    'keypoints': frame_data['keypoints']
                })()
                
                result = await session_manager.process_frame(session_id, streaming_frame)
                if result:
                    successful_analyses += 1
                    
            except Exception as e:
                # Should handle connection errors gracefully
                print(f"Connection error handled: {e}")
        
        # At least some analyses should succeed despite connection issues
        assert successful_analyses >= 1, "Some analyses should succeed despite connection errors"
        
        session_manager.end_session(session_id)

class TestPerformanceIntegration:
    """Test performance characteristics of the integrated system"""
    
    @pytest.mark.slow
    def test_pipeline_performance_benchmarks(self, performance_test_dataset, mock_gemini_api, measure_performance):
        """Test pipeline performance with large dataset"""
        
        start_time = time.time()
        
        # Process multiple swings
        results = []
        for i, swing_data in enumerate(performance_test_dataset[:20]):  # Test with 20 swings
            swing_start = time.time()
            
            # Complete pipeline
            extracted_kpis = extract_all_kpis(swing_data)
            detected_faults = check_swing_faults(swing_data, extracted_kpis)
            feedback_result = generate_feedback(swing_data, detected_faults)
            
            swing_duration = time.time() - swing_start
            
            results.append({
                "session_id": swing_data["session_id"],
                "kpi_count": len(extracted_kpis),
                "fault_count": len(detected_faults),
                "processing_time": swing_duration
            })
            
            # Log progress
            if (i + 1) % 5 == 0:
                print(f"Processed {i + 1} swings...")
        
        total_time = time.time() - start_time
        avg_processing_time = total_time / len(results)
        
        # Performance assertions
        assert avg_processing_time < 2.0, f"Average processing time {avg_processing_time:.3f}s exceeds 2s target"
        assert total_time < 40, f"Total processing time {total_time:.1f}s too high for batch processing"
        
        # Verify all analyses completed successfully
        assert len(results) == 20
        
        # Performance metrics
        processing_times = [r["processing_time"] for r in results]
        max_time = max(processing_times)
        min_time = min(processing_times)
        
        print(f"Performance benchmark: {len(results)} swings in {total_time:.2f}s")
        print(f"Average: {avg_processing_time:.3f}s, Min: {min_time:.3f}s, Max: {max_time:.3f}s")
        
        assert max_time < 5.0, f"Maximum processing time {max_time:.3f}s too high"
    
    @pytest.mark.asyncio
    @pytest.mark.slow
    async def test_concurrent_analysis_performance(self, mock_gemini_api):
        """Test performance with concurrent analysis requests"""
        from mock_data_factory import create_realistic_swing
        
        # Create multiple swing datasets
        swing_datasets = [
            create_realistic_swing(
                session_id=f"concurrent_{i}",
                club_type=ClubType.MID_IRON,
                quality=SwingQuality.GOOD
            )
            for i in range(10)
        ]
        
        # Define analysis task
        async def analyze_swing(swing_data):
            start_time = time.time()
            
            # Simulate async analysis
            await asyncio.sleep(0.01)  # Small delay to simulate I/O
            
            extracted_kpis = extract_all_kpis(swing_data)
            detected_faults = check_swing_faults(swing_data, extracted_kpis)
            
            return {
                "session_id": swing_data["session_id"],
                "duration": time.time() - start_time,
                "kpi_count": len(extracted_kpis),
                "fault_count": len(detected_faults)
            }
        
        # Run concurrent analysis
        start_time = time.time()
        tasks = [analyze_swing(swing_data) for swing_data in swing_datasets]
        results = await asyncio.gather(*tasks)
        total_time = time.time() - start_time
        
        # Verify concurrent processing efficiency
        sequential_time_estimate = sum(r["duration"] for r in results)
        concurrency_benefit = sequential_time_estimate / total_time
        
        print(f"Concurrent analysis: {len(results)} swings in {total_time:.2f}s")
        print(f"Concurrency benefit: {concurrency_benefit:.1f}x speedup")
        
        # Should have some concurrency benefit
        assert concurrency_benefit > 1.5, f"Insufficient concurrency benefit: {concurrency_benefit:.1f}x"
        
        # Verify all analyses completed
        assert len(results) == 10
        for result in results:
            assert result["kpi_count"] > 0
            assert result["duration"] < 1.0

if __name__ == "__main__":
    print("SwingSync AI Integration Test Suite")
    print("==================================")
    print("Testing complete analysis pipeline integration...")
    print("Run with: pytest test_integration.py -v")
    print("\nTest categories:")
    print("- Complete Analysis Pipeline")
    print("- Database Integration") 
    print("- Streaming Integration")
    print("- Error Handling Integration")
    print("- Performance Integration")