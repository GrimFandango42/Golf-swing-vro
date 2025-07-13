#!/bin/bash
# Quick deployment script for Golf Swing VRO

echo "🏌️ Golf Swing VRO - Quick Deploy"
echo "================================="
echo ""
echo "Choose deployment option:"
echo "1. Start Web Interface (Recommended)"
echo "2. View Mobile Demo"
echo "3. Push to GitHub for APK build"
echo "4. Download pre-built APK"
echo ""

read -p "Enter choice (1-4): " choice

case $choice in
    1)
        echo "Starting web interface..."
        python start_mobile.py
        ;;
    2)
        echo "Opening mobile demo..."
        termux-open mobile_demo.html
        ;;
    3)
        echo "Pushing to GitHub..."
        git add -A
        git commit -m "Deploy to mobile"
        git push origin main
        echo ""
        echo "✅ Pushed! Now go to:"
        echo "https://github.com/GrimFandango42/Golf-swing-vro/actions"
        echo "to download your APK"
        ;;
    4)
        echo "Opening releases page..."
        termux-open-url "https://github.com/GrimFandango42/Golf-swing-vro/releases"
        ;;
    *)
        echo "Invalid choice. Starting web interface..."
        python start_mobile.py
        ;;
esac