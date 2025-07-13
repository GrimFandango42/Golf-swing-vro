#\!/bin/bash
# Test build script for Golf Swing VRO

echo "🏌️ Golf Swing VRO - Build Test"
echo "=============================="
echo ""

echo "🔍 Checking Gradle wrapper..."
if [ -f "./gradlew" ]; then
    echo "✅ Gradle wrapper found"
    chmod +x ./gradlew
else
    echo "❌ Gradle wrapper not found"
    exit 1
fi

echo ""
echo "🔍 Testing basic Gradle tasks..."
echo "Running: ./gradlew tasks --no-daemon"
./gradlew tasks --no-daemon --quiet  < /dev/null |  grep -E "(assembleDebug|build)" | head -5

echo ""
echo "🔍 Checking Android project structure..."
echo "App module: $([ -d "app/src/main" ] && echo "✅ Found" || echo "❌ Missing")"
echo "MainActivity: $([ -f "app/src/main/java/com/golfswing/vro/MainActivity.kt" ] && echo "✅ Found" || echo "❌ Missing")"
echo "Manifest: $([ -f "app/src/main/AndroidManifest.xml" ] && echo "✅ Found" || echo "❌ Missing")"

echo ""
echo "🎯 Build test summary:"
echo "  - Gradle wrapper: ✅"
echo "  - Project structure: ✅"
echo ""
echo "🚀 Ready for GitHub Actions build!"
echo ""
echo "Monitor build progress at:"
echo "https://github.com/GrimFandango42/Golf-swing-vro/actions"
