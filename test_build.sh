#!/bin/bash
# Test build script to validate dependencies locally before pushing

echo "🔧 Testing Gradle Build Locally"
echo "================================"

# Test clean
echo "1. Testing clean..."
if ./gradlew clean; then
    echo "✅ Clean successful"
else
    echo "❌ Clean failed"
    exit 1
fi

# Test dependency resolution
echo "2. Testing dependency resolution..."
if ./gradlew app:dependencies --configuration debugCompileClasspath > /dev/null 2>&1; then
    echo "✅ Dependencies resolved"
else
    echo "❌ Dependencies failed to resolve"
    echo "Running with verbose output:"
    ./gradlew app:dependencies --configuration debugCompileClasspath
    exit 1
fi

# Test compilation
echo "3. Testing compilation..."
if ./gradlew compileDebugKotlin; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi

echo "🎉 All tests passed! Safe to push to GitHub."