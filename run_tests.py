#!/usr/bin/env python3
"""
Comprehensive Test Runner for SwingSync AI Test Suite.

This script provides an easy way to run all tests with different configurations
and generate comprehensive test reports.

Usage:
    python run_tests.py                    # Run all tests
    python run_tests.py --fast             # Run only fast tests
    python run_tests.py --integration      # Run integration tests only
    python run_tests.py --performance      # Run performance tests only
    python run_tests.py --coverage         # Run with coverage report
    python run_tests.py --benchmark        # Run performance benchmarks
"""

import argparse
import os
import subprocess
import sys
import time
from pathlib import Path

def run_command(cmd, description=""):
    """Run a command and handle output"""
    print(f"\n{'='*60}")
    print(f"Running: {description or cmd}")
    print(f"{'='*60}")
    
    start_time = time.time()
    
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        
        if result.stdout:
            print("STDOUT:")
            print(result.stdout)
        
        if result.stderr:
            print("STDERR:")
            print(result.stderr)
        
        elapsed = time.time() - start_time
        
        if result.returncode == 0:
            print(f"✅ SUCCESS ({elapsed:.2f}s)")
            return True
        else:
            print(f"❌ FAILED ({elapsed:.2f}s) - Return code: {result.returncode}")
            return False
            
    except Exception as e:
        print(f"❌ ERROR: {e}")
        return False

def check_dependencies():
    """Check if required dependencies are installed"""
    print("Checking dependencies...")
    
    required_packages = [
        "pytest",
        "pytest-asyncio", 
        "pytest-cov",
        "numpy",
        "psutil"
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package.replace("-", "_"))
        except ImportError:
            missing_packages.append(package)
    
    if missing_packages:
        print(f"❌ Missing packages: {', '.join(missing_packages)}")
        print("Install with: pip install " + " ".join(missing_packages))
        return False
    
    print("✅ All dependencies available")
    return True

def run_unit_tests(args):
    """Run unit tests"""
    cmd_parts = ["python", "-m", "pytest"]
    
    # Add test directories
    cmd_parts.extend([
        "tests/test_kpi_extraction.py",
        "tests/test_fault_detection.py", 
        "tests/test_feedback_generation.py"
    ])
    
    # Add options
    cmd_parts.extend(["-v", "--tb=short"])
    
    if args.fast:
        cmd_parts.extend(["-m", "not slow"])
    
    if args.coverage:
        cmd_parts.extend([
            "--cov=kpi_extraction",
            "--cov=fault_detection", 
            "--cov=feedback_generation",
            "--cov-report=html",
            "--cov-report=term"
        ])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "Unit Tests")

def run_integration_tests(args):
    """Run integration tests"""
    cmd_parts = ["python", "-m", "pytest", "tests/test_integration.py"]
    cmd_parts.extend(["-v", "--tb=short"])
    
    if args.fast:
        cmd_parts.extend(["-m", "not slow"])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "Integration Tests")

def run_performance_tests(args):
    """Run performance tests"""
    cmd_parts = ["python", "-m", "pytest", "tests/test_performance.py"]
    cmd_parts.extend(["-v", "--tb=short", "-s"])  # -s to show print output
    
    if not args.benchmark:
        cmd_parts.extend(["-m", "not slow"])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "Performance Tests")

def run_streaming_tests(args):
    """Run streaming tests"""
    cmd_parts = ["python", "-m", "pytest", "tests/test_streaming.py"]
    cmd_parts.extend(["-v", "--tb=short"])
    
    if args.fast:
        cmd_parts.extend(["-m", "not slow"])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "Streaming Tests")

def run_database_tests(args):
    """Run database tests"""
    cmd_parts = ["python", "-m", "pytest", "tests/test_database.py"]
    cmd_parts.extend(["-v", "--tb=short"])
    
    if args.fast:
        cmd_parts.extend(["-m", "not slow"])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "Database Tests")

def run_all_tests(args):
    """Run all test suites"""
    cmd_parts = ["python", "-m", "pytest", "tests/"]
    cmd_parts.extend(["-v", "--tb=short"])
    
    if args.fast:
        cmd_parts.extend(["-m", "not slow"])
    elif args.performance:
        cmd_parts.extend(["-m", "performance"])
    elif args.integration:
        cmd_parts.extend(["-m", "integration"])
    elif args.streaming:
        cmd_parts.extend(["-m", "streaming"])
    elif args.database:
        cmd_parts.extend(["-m", "database"])
    
    if args.coverage:
        cmd_parts.extend([
            "--cov=.",
            "--cov-report=html:htmlcov",
            "--cov-report=term-missing",
            "--cov-exclude=tests/*"
        ])
    
    cmd = " ".join(cmd_parts)
    return run_command(cmd, "All Tests")

def generate_test_report():
    """Generate a test report summary"""
    print(f"\n{'='*60}")
    print("TEST SUMMARY REPORT")
    print(f"{'='*60}")
    
    # Check if coverage report exists
    if os.path.exists("htmlcov/index.html"):
        print("📊 Coverage report generated: htmlcov/index.html")
    
    # Performance report
    print("\n📈 Performance Test Information:")
    print("- Run with --benchmark flag for full performance suite")
    print("- Performance tests validate sub-100ms latency requirements")
    print("- Memory usage tests ensure <500MB limit compliance")
    
    # Test categories
    print("\n📋 Test Categories Available:")
    print("- Unit Tests: Core functionality testing")
    print("- Integration Tests: End-to-end pipeline testing")
    print("- Performance Tests: Latency and throughput validation") 
    print("- Streaming Tests: Real-time analysis testing")
    print("- Database Tests: Data persistence and query testing")
    
    print("\n🔧 Test Configuration:")
    print("- Mock Gemini API: No external API calls required")
    print("- In-memory Database: Fast, isolated testing")
    print("- Performance Monitoring: Automated metrics collection")

def main():
    """Main test runner function"""
    parser = argparse.ArgumentParser(description="SwingSync AI Test Runner")
    
    # Test selection
    parser.add_argument("--unit", action="store_true", help="Run unit tests only")
    parser.add_argument("--integration", action="store_true", help="Run integration tests only")
    parser.add_argument("--performance", action="store_true", help="Run performance tests only")
    parser.add_argument("--streaming", action="store_true", help="Run streaming tests only")
    parser.add_argument("--database", action="store_true", help="Run database tests only")
    
    # Test options
    parser.add_argument("--fast", action="store_true", help="Skip slow tests")
    parser.add_argument("--benchmark", action="store_true", help="Run full performance benchmarks")
    parser.add_argument("--coverage", action="store_true", help="Generate coverage report")
    
    # Output options
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    parser.add_argument("--quiet", "-q", action="store_true", help="Quiet output")
    
    args = parser.parse_args()
    
    # Change to script directory
    script_dir = Path(__file__).parent
    os.chdir(script_dir)
    
    print("🧪 SwingSync AI Test Suite Runner")
    print(f"📁 Working directory: {os.getcwd()}")
    
    # Check dependencies
    if not check_dependencies():
        sys.exit(1)
    
    # Track results
    test_results = {}
    
    # Run specific test suites
    if args.unit:
        test_results["unit"] = run_unit_tests(args)
    elif args.integration:
        test_results["integration"] = run_integration_tests(args)
    elif args.performance:
        test_results["performance"] = run_performance_tests(args)
    elif args.streaming:
        test_results["streaming"] = run_streaming_tests(args)
    elif args.database:
        test_results["database"] = run_database_tests(args)
    else:
        # Run all tests
        test_results["all"] = run_all_tests(args)
    
    # Generate report
    generate_test_report()
    
    # Final results
    print(f"\n{'='*60}")
    print("FINAL RESULTS")
    print(f"{'='*60}")
    
    all_passed = True
    for test_type, passed in test_results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        print(f"{test_type.upper()} TESTS: {status}")
        if not passed:
            all_passed = False
    
    if all_passed:
        print("\n🎉 All tests passed!")
        sys.exit(0)
    else:
        print("\n⚠️  Some tests failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()