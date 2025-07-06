"""
Performance Benchmarks and Validation Tests for SwingSync AI.

This module provides comprehensive performance testing and validation including:
- Latency benchmarks for sub-100ms real-time requirements
- Throughput testing for batch processing
- Memory usage analysis and leak detection
- Scalability testing with varying loads
- Resource utilization monitoring
- Performance regression detection
- Load testing for concurrent users
- Algorithm efficiency validation

Key Performance Requirements:
- Real-time analysis: < 100ms per frame
- Batch processing: < 2s per complete swing
- Memory usage: < 500MB for typical workloads
- Concurrent users: Support 50+ simultaneous analyses
- Database operations: < 50ms for typical queries
- API response times: < 200ms for feedback generation
"""

import asyncio
import gc
import psutil
import pytest
import time
import threading
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Any, Tuple
import numpy as np
from unittest.mock import patch

# Project imports
from kpi_extraction import extract_all_kpis
from fault_detection import check_swing_faults
from feedback_generation import generate_feedback
from live_analysis import LiveAnalysisEngine, StreamingKPICalculator
from streaming_endpoints import StreamingSessionManager
from database import SessionLocal, SwingSession, User
from mock_data_factory import (
    create_realistic_swing, create_performance_test_data,
    generate_streaming_session, ClubType, SwingQuality
)

class PerformanceMonitor:
    """Utility class for monitoring performance metrics"""
    
    def __init__(self):
        self.process = psutil.Process()
        self.start_time = None
        self.start_memory = None
        self.peak_memory = 0
        self.measurements = []
    
    def start_monitoring(self):
        """Start performance monitoring"""
        self.start_time = time.time()
        self.start_memory = self.process.memory_info().rss / 1024 / 1024  # MB
        self.peak_memory = self.start_memory
        self.measurements = []
    
    def take_measurement(self, label: str = ""):
        """Take a performance measurement"""
        current_time = time.time()
        current_memory = self.process.memory_info().rss / 1024 / 1024  # MB
        cpu_percent = self.process.cpu_percent()
        
        if current_memory > self.peak_memory:
            self.peak_memory = current_memory
        
        measurement = {
            "label": label,
            "timestamp": current_time,
            "elapsed_time": current_time - self.start_time if self.start_time else 0,
            "memory_mb": current_memory,
            "memory_delta": current_memory - self.start_memory if self.start_memory else 0,
            "cpu_percent": cpu_percent
        }
        
        self.measurements.append(measurement)
        return measurement
    
    def get_summary(self) -> Dict[str, Any]:
        """Get performance summary"""
        if not self.measurements:
            return {}
        
        total_time = self.measurements[-1]["elapsed_time"]
        memory_usage = [m["memory_mb"] for m in self.measurements]
        cpu_usage = [m["cpu_percent"] for m in self.measurements]
        
        return {
            "total_time": total_time,
            "start_memory_mb": self.start_memory,
            "peak_memory_mb": self.peak_memory,
            "memory_delta_mb": self.peak_memory - self.start_memory,
            "avg_memory_mb": np.mean(memory_usage),
            "avg_cpu_percent": np.mean(cpu_usage),
            "max_cpu_percent": max(cpu_usage) if cpu_usage else 0,
            "measurement_count": len(self.measurements)
        }

@pytest.fixture
def performance_monitor():
    """Performance monitoring fixture"""
    monitor = PerformanceMonitor()
    monitor.start_monitoring()
    yield monitor
    
    # Print summary after test
    summary = monitor.get_summary()
    if summary:
        print(f"\nPerformance Summary:")
        print(f"  Duration: {summary['total_time']:.3f}s")
        print(f"  Memory: {summary['start_memory_mb']:.1f}MB → {summary['peak_memory_mb']:.1f}MB (Δ{summary['memory_delta_mb']:.1f}MB)")
        print(f"  CPU: avg {summary['avg_cpu_percent']:.1f}%, max {summary['max_cpu_percent']:.1f}%")

class TestLatencyBenchmarks:
    """Test latency requirements for real-time analysis"""
    
    def test_single_frame_analysis_latency(self, good_swing_data, performance_monitor):
        """Test latency of analyzing a single frame"""
        frame_data = good_swing_data["frames"][0]
        
        # Warm up
        for _ in range(3):
            extract_all_kpis({"frames": [frame_data], "video_fps": 60.0, "p_system_classification": [], "session_id": "warmup", "user_id": "test", "club_used": "7-Iron"})
        
        # Measure latency over multiple iterations
        latencies = []
        for i in range(50):
            start_time = time.perf_counter()
            
            swing_input = {
                "frames": [frame_data],
                "video_fps": 60.0,
                "p_system_classification": [{"phase_name": "P1", "start_frame_index": 0, "end_frame_index": 0}],
                "session_id": f"latency_test_{i}",
                "user_id": "test",
                "club_used": "7-Iron"
            }
            
            kpis = extract_all_kpis(swing_input)
            
            end_time = time.perf_counter()
            latency_ms = (end_time - start_time) * 1000
            latencies.append(latency_ms)
            
            performance_monitor.take_measurement(f"frame_{i}")
        
        # Analyze latency statistics
        avg_latency = np.mean(latencies)
        p95_latency = np.percentile(latencies, 95)
        p99_latency = np.percentile(latencies, 99)
        max_latency = max(latencies)
        
        print(f"\nFrame Analysis Latency:")
        print(f"  Average: {avg_latency:.2f}ms")
        print(f"  95th percentile: {p95_latency:.2f}ms")
        print(f"  99th percentile: {p99_latency:.2f}ms")
        print(f"  Maximum: {max_latency:.2f}ms")
        
        # Performance assertions
        assert avg_latency < 50, f"Average latency {avg_latency:.2f}ms exceeds 50ms target"
        assert p95_latency < 100, f"95th percentile latency {p95_latency:.2f}ms exceeds 100ms target"
        assert p99_latency < 150, f"99th percentile latency {p99_latency:.2f}ms exceeds 150ms target"
    
    @pytest.mark.asyncio
    async def test_streaming_analysis_latency(self, streaming_test_data, performance_monitor):
        """Test latency of streaming analysis pipeline"""
        live_engine = LiveAnalysisEngine()
        latencies = []
        
        # Mock session context
        session_context = {
            "config": type('Config', (), {
                'enable_real_time_kpis': True,
                'analysis_frequency': 1,  # Analyze every frame
                'feedback_threshold': 0.6
            })()
        }
        
        # Process frames and measure latency
        for i, frame_data in enumerate(streaming_test_data[:30]):  # Test 30 frames
            streaming_frame = type('StreamingFrame', (), {
                'frame_index': frame_data['frame_index'],
                'timestamp': frame_data['timestamp'],
                'keypoints': frame_data['keypoints']
            })()
            
            start_time = time.perf_counter()
            
            result = await live_engine.analyze_frame(
                streaming_frame,
                session_context,
                session_context["config"]
            )
            
            end_time = time.perf_counter()
            latency_ms = (end_time - start_time) * 1000
            latencies.append(latency_ms)
            
            if result:
                assert result.analysis_latency_ms > 0
                # Verify internal latency measurement is consistent
                assert abs(result.analysis_latency_ms - latency_ms) < 10, "Internal latency measurement inconsistent"
            
            performance_monitor.take_measurement(f"stream_frame_{i}")
        
        # Analyze streaming latency
        avg_latency = np.mean(latencies)
        p95_latency = np.percentile(latencies, 95)
        max_latency = max(latencies)
        
        print(f"\nStreaming Analysis Latency:")
        print(f"  Average: {avg_latency:.2f}ms")
        print(f"  95th percentile: {p95_latency:.2f}ms")
        print(f"  Maximum: {max_latency:.2f}ms")
        
        # Real-time performance requirements
        assert avg_latency < 100, f"Average streaming latency {avg_latency:.2f}ms exceeds 100ms target"
        assert p95_latency < 150, f"95th percentile streaming latency {p95_latency:.2f}ms exceeds 150ms"
        assert max_latency < 300, f"Maximum streaming latency {max_latency:.2f}ms too high"
    
    def test_fault_detection_latency(self, performance_test_dataset, performance_monitor):
        """Test latency of fault detection with various data sizes"""
        latency_by_kpi_count = defaultdict(list)
        
        for i, swing_data in enumerate(performance_test_dataset[:25]):
            # Extract KPIs first
            start_kpi = time.perf_counter()
            kpis = extract_all_kpis(swing_data)
            kpi_time = time.perf_counter() - start_kpi
            
            # Measure fault detection latency
            start_fault = time.perf_counter()
            faults = check_swing_faults(swing_data, kpis)
            fault_time = time.perf_counter() - start_fault
            
            fault_latency_ms = fault_time * 1000
            latency_by_kpi_count[len(kpis)].append(fault_latency_ms)
            
            performance_monitor.take_measurement(f"fault_detection_{i}")
            
            # Performance assertion per swing
            assert fault_latency_ms < 200, f"Fault detection latency {fault_latency_ms:.2f}ms too high for swing {i}"
        
        # Analyze latency by KPI count
        print(f"\nFault Detection Latency by KPI Count:")
        for kpi_count, latencies in sorted(latency_by_kpi_count.items()):
            avg_latency = np.mean(latencies)
            max_latency = max(latencies)
            print(f"  {kpi_count} KPIs: avg {avg_latency:.2f}ms, max {max_latency:.2f}ms")
            
            # Latency should scale reasonably with KPI count
            assert avg_latency < (kpi_count * 10 + 50), f"Fault detection doesn't scale well with {kpi_count} KPIs"

class TestThroughputBenchmarks:
    """Test throughput for batch processing scenarios"""
    
    @pytest.mark.slow
    def test_batch_processing_throughput(self, performance_test_dataset, performance_monitor, mock_gemini_api):
        """Test throughput of batch processing multiple swings"""
        
        start_time = time.time()
        processed_swings = 0
        total_kpis = 0
        total_faults = 0
        
        # Process swings in batches
        batch_size = 10
        for batch_start in range(0, min(50, len(performance_test_dataset)), batch_size):
            batch = performance_test_dataset[batch_start:batch_start + batch_size]
            
            batch_start_time = time.time()
            
            for swing_data in batch:
                # Complete analysis pipeline
                kpis = extract_all_kpis(swing_data)
                faults = check_swing_faults(swing_data, kpis)
                feedback = generate_feedback(swing_data, faults)
                
                processed_swings += 1
                total_kpis += len(kpis)
                total_faults += len(faults)
                
                performance_monitor.take_measurement(f"batch_swing_{processed_swings}")
            
            batch_time = time.time() - batch_start_time
            print(f"Batch {batch_start//batch_size + 1}: {len(batch)} swings in {batch_time:.2f}s ({len(batch)/batch_time:.1f} swings/s)")
        
        total_time = time.time() - start_time
        throughput = processed_swings / total_time
        
        print(f"\nBatch Processing Throughput:")
        print(f"  Total swings: {processed_swings}")
        print(f"  Total time: {total_time:.2f}s")
        print(f"  Throughput: {throughput:.2f} swings/second")
        print(f"  Total KPIs: {total_kpis}")
        print(f"  Total faults: {total_faults}")
        
        # Throughput requirements
        assert throughput >= 5.0, f"Batch throughput {throughput:.2f} swings/s below 5.0 target"
        assert total_time / processed_swings < 2.0, "Average processing time per swing exceeds 2s"
    
    def test_concurrent_processing_throughput(self, performance_test_dataset, performance_monitor, mock_gemini_api):
        """Test throughput with concurrent processing"""
        
        def process_swing(swing_data):
            """Process a single swing"""
            thread_id = threading.get_ident()
            start_time = time.time()
            
            try:
                kpis = extract_all_kpis(swing_data)
                faults = check_swing_faults(swing_data, kpis)
                feedback = generate_feedback(swing_data, faults)
                
                processing_time = time.time() - start_time
                
                return {
                    "session_id": swing_data["session_id"],
                    "thread_id": thread_id,
                    "processing_time": processing_time,
                    "kpi_count": len(kpis),
                    "fault_count": len(faults),
                    "success": True
                }
            except Exception as e:
                return {
                    "session_id": swing_data["session_id"],
                    "thread_id": thread_id,
                    "error": str(e),
                    "success": False
                }
        
        # Test with different thread counts
        thread_counts = [1, 2, 4, 8]
        results_by_threads = {}
        
        test_data = performance_test_dataset[:20]  # Smaller set for concurrent testing
        
        for num_threads in thread_counts:
            start_time = time.time()
            
            with ThreadPoolExecutor(max_workers=num_threads) as executor:
                futures = [executor.submit(process_swing, swing_data) for swing_data in test_data]
                results = [future.result() for future in as_completed(futures)]
            
            total_time = time.time() - start_time
            successful_results = [r for r in results if r["success"]]
            
            throughput = len(successful_results) / total_time
            avg_processing_time = np.mean([r["processing_time"] for r in successful_results])
            
            results_by_threads[num_threads] = {
                "throughput": throughput,
                "total_time": total_time,
                "avg_processing_time": avg_processing_time,
                "success_rate": len(successful_results) / len(results)
            }
            
            print(f"{num_threads} threads: {throughput:.2f} swings/s, avg {avg_processing_time:.3f}s/swing")
            
            performance_monitor.take_measurement(f"concurrent_{num_threads}_threads")
        
        # Analyze concurrency benefits
        single_thread_throughput = results_by_threads[1]["throughput"]
        best_throughput = max(results_by_threads[tc]["throughput"] for tc in thread_counts)
        concurrency_benefit = best_throughput / single_thread_throughput
        
        print(f"\nConcurrency Analysis:")
        print(f"  Single thread: {single_thread_throughput:.2f} swings/s")
        print(f"  Best throughput: {best_throughput:.2f} swings/s")
        print(f"  Concurrency benefit: {concurrency_benefit:.1f}x")
        
        # Verify concurrency provides benefit
        assert concurrency_benefit >= 1.5, f"Insufficient concurrency benefit: {concurrency_benefit:.1f}x"
        
        # Verify all thread counts have good success rates
        for num_threads, result in results_by_threads.items():
            assert result["success_rate"] >= 0.95, f"{num_threads} threads has low success rate: {result['success_rate']:.2f}"

class TestMemoryUsage:
    """Test memory usage and leak detection"""
    
    @pytest.mark.slow
    def test_memory_usage_scaling(self, performance_monitor):
        """Test memory usage with increasing data sizes"""
        
        gc.collect()  # Start with clean memory
        baseline_memory = performance_monitor.process.memory_info().rss / 1024 / 1024
        
        memory_measurements = []
        data_sizes = [10, 25, 50, 100]  # Number of frames
        
        for size in data_sizes:
            # Create swing data of specific size
            swing_data = create_realistic_swing(
                session_id=f"memory_test_{size}",
                club_type=ClubType.MID_IRON,
                quality=SwingQuality.GOOD
            )
            
            # Extend frames to test size
            while len(swing_data["frames"]) < size:
                swing_data["frames"].extend(swing_data["frames"][:min(size - len(swing_data["frames"]), len(swing_data["frames"]))])
            swing_data["frames"] = swing_data["frames"][:size]
            
            # Process and measure memory
            gc.collect()
            before_memory = performance_monitor.process.memory_info().rss / 1024 / 1024
            
            kpis = extract_all_kpis(swing_data)
            faults = check_swing_faults(swing_data, kpis)
            
            after_memory = performance_monitor.process.memory_info().rss / 1024 / 1024
            memory_used = after_memory - before_memory
            
            memory_measurements.append({
                "data_size": size,
                "memory_used_mb": memory_used,
                "total_memory_mb": after_memory,
                "memory_per_frame_kb": (memory_used * 1024) / size if size > 0 else 0
            })
            
            print(f"Size {size}: {memory_used:.2f}MB used, {(memory_used * 1024) / size:.1f}KB per frame")
            
            performance_monitor.take_measurement(f"memory_test_{size}")
            
            # Clean up
            del swing_data, kpis, faults
            gc.collect()
        
        # Analyze memory scaling
        max_memory_used = max(m["memory_used_mb"] for m in memory_measurements)
        max_total_memory = max(m["total_memory_mb"] for m in memory_measurements)
        
        print(f"\nMemory Usage Analysis:")
        print(f"  Baseline memory: {baseline_memory:.1f}MB")
        print(f"  Maximum used: {max_memory_used:.2f}MB")
        print(f"  Peak total: {max_total_memory:.1f}MB")
        
        # Memory usage assertions
        assert max_memory_used < 100, f"Memory usage {max_memory_used:.2f}MB too high"
        assert max_total_memory < 500, f"Total memory {max_total_memory:.1f}MB exceeds 500MB limit"
        
        # Check for reasonable scaling
        memory_per_frame = [m["memory_per_frame_kb"] for m in memory_measurements]
        assert max(memory_per_frame) < 50, "Memory per frame too high"
    
    def test_memory_leak_detection(self, performance_monitor):
        """Test for memory leaks over repeated operations"""
        
        gc.collect()
        initial_memory = performance_monitor.process.memory_info().rss / 1024 / 1024
        
        memory_snapshots = [initial_memory]
        
        # Perform repeated operations
        for iteration in range(20):
            # Create and process swing data
            swing_data = create_realistic_swing(
                session_id=f"leak_test_{iteration}",
                club_type=ClubType.MID_IRON,
                quality=SwingQuality.GOOD
            )
            
            kpis = extract_all_kpis(swing_data)
            faults = check_swing_faults(swing_data, kpis)
            
            # Clean up references
            del swing_data, kpis, faults
            
            # Force garbage collection every 5 iterations
            if iteration % 5 == 0:
                gc.collect()
                current_memory = performance_monitor.process.memory_info().rss / 1024 / 1024
                memory_snapshots.append(current_memory)
                
                performance_monitor.take_measurement(f"leak_test_{iteration}")
        
        final_memory = memory_snapshots[-1]
        memory_growth = final_memory - initial_memory
        
        print(f"\nMemory Leak Analysis:")
        print(f"  Initial memory: {initial_memory:.1f}MB")
        print(f"  Final memory: {final_memory:.1f}MB")
        print(f"  Memory growth: {memory_growth:.2f}MB")
        
        # Check for memory leaks
        assert memory_growth < 50, f"Potential memory leak detected: {memory_growth:.2f}MB growth"
        
        # Check for consistent memory usage (no monotonic growth)
        memory_deltas = [memory_snapshots[i+1] - memory_snapshots[i] for i in range(len(memory_snapshots)-1)]
        avg_growth_per_cycle = np.mean(memory_deltas)
        
        assert avg_growth_per_cycle < 5, f"Consistent memory growth detected: {avg_growth_per_cycle:.2f}MB per cycle"

class TestScalabilityBenchmarks:
    """Test system scalability with varying loads"""
    
    @pytest.mark.slow
    @pytest.mark.asyncio
    async def test_streaming_session_scalability(self, performance_monitor):
        """Test scalability with multiple concurrent streaming sessions"""
        
        session_manager = StreamingSessionManager()
        active_sessions = []
        
        # Create multiple streaming sessions
        session_counts = [1, 5, 10, 20]
        results_by_count = {}
        
        for session_count in session_counts:
            print(f"\nTesting {session_count} concurrent sessions...")
            
            # Create sessions
            sessions = []
            for i in range(session_count):
                from streaming_endpoints import StreamingSessionConfig
                config = StreamingSessionConfig(
                    user_id=f"scale_test_user_{i}",
                    session_name=f"Scalability Test {i}",
                    analysis_frequency=3,
                    enable_real_time_kpis=True
                )
                
                session_id = session_manager.create_session(config)
                sessions.append(session_id)
            
            # Generate test data for each session
            session_data = {}
            for session_id in sessions:
                session_data[session_id] = generate_streaming_session(duration_seconds=5.0, fps=30.0)
            
            # Process frames concurrently
            start_time = time.time()
            processed_frames = 0
            
            async def process_session_frames(session_id):
                frames = session_data[session_id]
                session_processed = 0
                
                for frame_data in frames[:30]:  # Limit frames for testing
                    streaming_frame = type('StreamingFrame', (), {
                        'frame_index': frame_data['frame_index'],
                        'timestamp': frame_data['timestamp'],
                        'keypoints': frame_data['keypoints']
                    })()
                    
                    result = await session_manager.process_frame(session_id, streaming_frame)
                    if result:
                        session_processed += 1
                
                return session_processed
            
            # Run all sessions concurrently
            tasks = [process_session_frames(session_id) for session_id in sessions]
            session_results = await asyncio.gather(*tasks)
            
            total_time = time.time() - start_time
            total_processed = sum(session_results)
            throughput = total_processed / total_time
            
            # Clean up sessions
            for session_id in sessions:
                session_manager.end_session(session_id)
            
            results_by_count[session_count] = {
                "total_time": total_time,
                "total_processed": total_processed,
                "throughput": throughput,
                "avg_per_session": total_processed / session_count
            }
            
            print(f"  {session_count} sessions: {throughput:.1f} frames/s total")
            performance_monitor.take_measurement(f"scale_test_{session_count}_sessions")
        
        # Analyze scalability
        print(f"\nStreaming Scalability Analysis:")
        for count, result in results_by_count.items():
            print(f"  {count} sessions: {result['throughput']:.1f} frames/s, {result['avg_per_session']:.1f} frames/session")
        
        # Verify reasonable scaling
        single_session_throughput = results_by_count[1]["throughput"]
        max_session_throughput = max(results_by_count[count]["throughput"] for count in session_counts)
        
        # Should maintain reasonable performance with more sessions
        performance_retention = results_by_count[20]["throughput"] / single_session_throughput
        assert performance_retention > 0.5, f"Poor scalability: {performance_retention:.2f} performance retention"
    
    def test_database_query_performance(self, test_db_session, multiple_test_users, performance_monitor):
        """Test database query performance with varying data sizes"""
        
        # Create sessions for testing
        session_data = []
        for i in range(100):
            user = multiple_test_users[i % len(multiple_test_users)]
            session = SwingSession(
                id=f"perf_session_{i}",
                user_id=user.id,
                club_used=f"Club_{i % 5}",
                video_fps=60.0,
                total_frames=150 + (i % 50),
                session_status="completed"
            )
            test_db_session.add(session)
            session_data.append(session)
        
        test_db_session.commit()
        
        # Test various query patterns
        query_tests = [
            ("single_user_sessions", lambda: test_db_session.query(SwingSession).filter_by(user_id=multiple_test_users[0].id).all()),
            ("all_sessions", lambda: test_db_session.query(SwingSession).all()),
            ("session_by_club", lambda: test_db_session.query(SwingSession).filter_by(club_used="Club_0").all()),
            ("user_with_sessions", lambda: test_db_session.query(User).join(SwingSession).filter(User.id == multiple_test_users[0].id).first()),
        ]
        
        print(f"\nDatabase Query Performance:")
        for test_name, query_func in query_tests:
            # Warm up
            query_func()
            
            # Measure query performance
            latencies = []
            for _ in range(10):
                start_time = time.perf_counter()
                result = query_func()
                end_time = time.perf_counter()
                
                latency_ms = (end_time - start_time) * 1000
                latencies.append(latency_ms)
            
            avg_latency = np.mean(latencies)
            max_latency = max(latencies)
            
            print(f"  {test_name}: avg {avg_latency:.2f}ms, max {max_latency:.2f}ms")
            
            # Performance assertions
            assert avg_latency < 100, f"Query {test_name} average latency {avg_latency:.2f}ms too high"
            assert max_latency < 200, f"Query {test_name} max latency {max_latency:.2f}ms too high"
            
            performance_monitor.take_measurement(f"db_query_{test_name}")

class TestPerformanceRegression:
    """Test for performance regression detection"""
    
    def test_kpi_extraction_performance_baseline(self, good_swing_data, performance_monitor):
        """Establish performance baseline for KPI extraction"""
        
        # Measure baseline performance
        iterations = 100
        latencies = []
        
        for i in range(iterations):
            start_time = time.perf_counter()
            kpis = extract_all_kpis(good_swing_data)
            end_time = time.perf_counter()
            
            latency_ms = (end_time - start_time) * 1000
            latencies.append(latency_ms)
        
        # Calculate baseline metrics
        baseline_metrics = {
            "avg_latency_ms": np.mean(latencies),
            "p95_latency_ms": np.percentile(latencies, 95),
            "p99_latency_ms": np.percentile(latencies, 99),
            "max_latency_ms": max(latencies),
            "min_latency_ms": min(latencies),
            "std_latency_ms": np.std(latencies)
        }
        
        print(f"\nKPI Extraction Performance Baseline:")
        for metric, value in baseline_metrics.items():
            print(f"  {metric}: {value:.2f}")
        
        # Store baseline for regression testing (in real implementation, this would be persisted)
        expected_baselines = {
            "avg_latency_ms": 100,  # Expected baseline values
            "p95_latency_ms": 150,
            "p99_latency_ms": 200
        }
        
        # Check against expected baselines
        for metric, expected in expected_baselines.items():
            actual = baseline_metrics[metric]
            regression_threshold = expected * 1.5  # 50% regression threshold
            
            assert actual < regression_threshold, f"Performance regression detected in {metric}: {actual:.2f}ms > {regression_threshold:.2f}ms threshold"
        
        performance_monitor.take_measurement("baseline_complete")
    
    def test_end_to_end_performance_baseline(self, good_swing_data, mock_gemini_api, performance_monitor):
        """Establish end-to-end performance baseline"""
        
        iterations = 20
        end_to_end_times = []
        
        for i in range(iterations):
            start_time = time.perf_counter()
            
            # Complete pipeline
            kpis = extract_all_kpis(good_swing_data)
            faults = check_swing_faults(good_swing_data, kpis)
            feedback = generate_feedback(good_swing_data, faults)
            
            end_time = time.perf_counter()
            total_time_ms = (end_time - start_time) * 1000
            end_to_end_times.append(total_time_ms)
        
        # Calculate end-to-end metrics
        e2e_metrics = {
            "avg_time_ms": np.mean(end_to_end_times),
            "p95_time_ms": np.percentile(end_to_end_times, 95),
            "max_time_ms": max(end_to_end_times)
        }
        
        print(f"\nEnd-to-End Performance Baseline:")
        for metric, value in e2e_metrics.items():
            print(f"  {metric}: {value:.2f}")
        
        # Performance requirements
        assert e2e_metrics["avg_time_ms"] < 2000, f"Average end-to-end time {e2e_metrics['avg_time_ms']:.2f}ms exceeds 2s"
        assert e2e_metrics["p95_time_ms"] < 3000, f"95th percentile time {e2e_metrics['p95_time_ms']:.2f}ms exceeds 3s"
        assert e2e_metrics["max_time_ms"] < 5000, f"Maximum time {e2e_metrics['max_time_ms']:.2f}ms exceeds 5s"
        
        performance_monitor.take_measurement("e2e_baseline_complete")

if __name__ == "__main__":
    print("SwingSync AI Performance Test Suite")
    print("==================================")
    print("Performance requirements:")
    print("- Real-time analysis: < 100ms per frame")
    print("- Batch processing: < 2s per swing")
    print("- Memory usage: < 500MB for typical workloads")
    print("- Concurrent users: 50+ simultaneous analyses")
    print("- Database queries: < 50ms typical")
    print("\nRun with: pytest test_performance.py -v -m slow")