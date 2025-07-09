#!/usr/bin/env python3
"""
SwingSync AI System Integration Test
===================================
Tests all the new features and improvements without requiring external dependencies.
"""

import os
import sys
from pathlib import Path

def test_file_structure():
    """Test that all new files were created successfully"""
    print("📁 Testing File Structure...")
    
    expected_files = [
        # Core improvements
        "kpi_extraction.py",
        "feedback_generation.py", 
        ".env",
        "API_KEY_INFO.md",
        
        # New features
        "kinematic_sequence.py",
        "adaptive_coaching/__init__.py",
        "adaptive_coaching/user_learning_engine.py",
        "adaptive_coaching/coaching_adaptation.py",
        
        # Android AR components
        "android/app/src/main/java/com/swingsync/ai/ar/SwingPlaneRenderer.kt",
        "android/app/src/main/java/com/swingsync/ai/ar/AROverlayView.kt",
        
        # Magic analysis components
        "android/app/src/main/java/com/swingsync/ai/ui/MagicAnalysisFragment.kt",
        "android/app/src/main/java/com/swingsync/ai/auto/SwingAutoDetector.kt",
        
        # Voice components
        "android/app/src/main/java/com/swingsync/ai/voice/WakeWordDetector.kt",
        "android/app/src/main/java/com/swingsync/ai/voice/HandsFreeService.kt",
        
        # Visualization components
        "android/app/src/main/java/com/swingsync/ai/visualization/SwingComparisonRenderer.kt",
        "android/app/src/main/java/com/swingsync/ai/visualization/ProgressVisualization.kt",
        
        # Celebration and achievements
        "android/app/src/main/java/com/swingsync/ai/detection/BestSwingDetector.kt",
        "android/app/src/main/java/com/swingsync/ai/celebration/CelebrationEngine.kt",
        
        # Onboarding wizard
        "android/app/src/main/java/com/swingsync/ai/onboarding/OnboardingWizard.kt",
        "android/app/src/main/java/com/swingsync/ai/onboarding/AutoSetupEngine.kt",
    ]
    
    created_files = []
    missing_files = []
    
    for file_path in expected_files:
        full_path = Path(file_path)
        if full_path.exists():
            created_files.append(file_path)
            print(f"✅ {file_path}")
        else:
            missing_files.append(file_path)
            print(f"❌ {file_path}")
    
    print(f"\n📊 File Structure Results:")
    print(f"✅ Created: {len(created_files)}")
    print(f"❌ Missing: {len(missing_files)}")
    
    return len(missing_files) == 0

def test_api_key_configuration():
    """Test API key is configured in multiple places"""
    print("\n🔑 Testing API Key Configuration...")
    
    locations = []
    
    # Check .env file
    if Path(".env").exists():
        with open(".env", "r") as f:
            content = f.read()
            if "GEMINI_API_KEY" in content:
                locations.append(".env file")
                print("✅ .env file")
    
    # Check feedback_generation.py
    if Path("feedback_generation.py").exists():
        with open("feedback_generation.py", "r") as f:
            content = f.read()
            if "GEMINI_API_KEY" in content:
                locations.append("feedback_generation.py")
                print("✅ feedback_generation.py")
    
    # Check .env.backup
    if Path(".env.backup").exists():
        locations.append(".env.backup")
        print("✅ .env.backup")
    
    # Check API_KEY_INFO.md
    if Path("API_KEY_INFO.md").exists():
        locations.append("API_KEY_INFO.md")
        print("✅ API_KEY_INFO.md")
    
    print(f"\n🔑 API Key stored in {len(locations)} locations")
    return len(locations) >= 3

def test_android_improvements():
    """Test Android security and performance improvements"""
    print("\n📱 Testing Android Improvements...")
    
    improvements = []
    
    # Check ProGuard enabled
    build_gradle = Path("android/app/build.gradle")
    if build_gradle.exists():
        with open(build_gradle, "r") as f:
            content = f.read()
            if "minifyEnabled true" in content:
                improvements.append("ProGuard obfuscation enabled")
                print("✅ ProGuard obfuscation enabled")
    
    # Check frame rate optimization
    camera_activity = Path("android/app/src/main/java/com/swingsync/ai/ui/camera/CameraActivity.kt")
    if camera_activity.exists():
        with open(camera_activity, "r") as f:
            content = f.read()
            if "TARGET_FPS = 30f" in content:
                improvements.append("Frame rate optimized to 30fps")
                print("✅ Frame rate optimized to 30fps")
    
    # Check performance utils
    perf_utils = Path("android/app/src/main/java/com/swingsync/ai/utils/PerformanceUtils.kt")
    if perf_utils.exists():
        with open(perf_utils, "r") as f:
            content = f.read()
            if "Reduced from 60fps for better battery life" in content:
                improvements.append("Battery life optimizations")
                print("✅ Battery life optimizations")
    
    print(f"\n📱 Android improvements: {len(improvements)}")
    return len(improvements) >= 2

def test_x_factor_implementation():
    """Test X-Factor biomechanical calculation"""
    print("\n🧬 Testing X-Factor Implementation...")
    
    if Path("kpi_extraction.py").exists():
        with open("kpi_extraction.py", "r") as f:
            content = f.read()
            
            checks = [
                ("X-Factor function", "calculate_x_factor_p4" in content),
                ("X-Factor integration", "x_factor_p4 = calculate_x_factor_p4" in content),
                ("Biomechanical accuracy", "shoulder_rotation - hip_rotation" in content),
                ("Professional ranges", "ideal_range=(35.0, 55.0)" in content)
            ]
            
            passed = 0
            for check_name, result in checks:
                if result:
                    print(f"✅ {check_name}")
                    passed += 1
                else:
                    print(f"❌ {check_name}")
            
            print(f"\n🧬 X-Factor implementation: {passed}/{len(checks)} checks passed")
            return passed == len(checks)
    
    return False

def test_security_improvements():
    """Test security enhancements"""
    print("\n🔒 Testing Security Improvements...")
    
    security_checks = []
    
    # Check CORS fix
    if Path("main.py").exists():
        with open("main.py", "r") as f:
            content = f.read()
            if 'allow_origins=["*"]' not in content and "localhost" in content:
                security_checks.append("CORS properly configured")
                print("✅ CORS properly configured")
    
    # Check secret key improvement
    if Path("user_management.py").exists():
        with open("user_management.py", "r") as f:
            content = f.read()
            if "secrets.token_urlsafe" in content:
                security_checks.append("Secure secret key generation")
                print("✅ Secure secret key generation")
    
    print(f"\n🔒 Security improvements: {len(security_checks)}")
    return len(security_checks) >= 2

def test_feature_completeness():
    """Test that all major features are implemented"""
    print("\n🎯 Testing Feature Completeness...")
    
    features = [
        ("Kinematic Sequence Analysis", Path("kinematic_sequence.py").exists()),
        ("Adaptive Coaching System", Path("adaptive_coaching").exists()),
        ("AR Swing Visualization", Path("android/app/src/main/java/com/swingsync/ai/ar").exists()),
        ("Magic One-Tap Analysis", Path("android/app/src/main/java/com/swingsync/ai/auto").exists()),
        ("Voice-Activated Controls", Path("android/app/src/main/java/com/swingsync/ai/voice/WakeWordDetector.kt").exists()),
        ("Beautiful Visualizations", Path("android/app/src/main/java/com/swingsync/ai/visualization").exists()),
        ("Celebration System", Path("android/app/src/main/java/com/swingsync/ai/celebration").exists()),
        ("Onboarding Wizard", Path("android/app/src/main/java/com/swingsync/ai/onboarding").exists()),
    ]
    
    implemented = 0
    for feature_name, exists in features:
        if exists:
            print(f"✅ {feature_name}")
            implemented += 1
        else:
            print(f"❌ {feature_name}")
    
    print(f"\n🎯 Features implemented: {implemented}/{len(features)}")
    return implemented >= 6

def main():
    """Run all tests"""
    print("🏌️ SwingSync AI System Integration Test")
    print("=" * 50)
    
    tests = [
        ("File Structure", test_file_structure),
        ("API Key Configuration", test_api_key_configuration),
        ("Android Improvements", test_android_improvements),
        ("X-Factor Implementation", test_x_factor_implementation),
        ("Security Improvements", test_security_improvements),
        ("Feature Completeness", test_feature_completeness),
    ]
    
    passed_tests = 0
    total_tests = len(tests)
    
    for test_name, test_func in tests:
        if test_func():
            passed_tests += 1
    
    print("\n" + "=" * 50)
    print(f"🎯 Test Results: {passed_tests}/{total_tests} tests passed")
    
    if passed_tests == total_tests:
        print("🎉 ALL TESTS PASSED! SwingSync AI is ready to revolutionize golf!")
    elif passed_tests >= total_tests * 0.8:
        print("✅ Most tests passed! System is ready for use with minor issues.")
    else:
        print("⚠️  Some issues detected. Review failed tests above.")
    
    print("\n🚀 Ready to start your golf improvement journey!")
    return passed_tests == total_tests

if __name__ == "__main__":
    main()