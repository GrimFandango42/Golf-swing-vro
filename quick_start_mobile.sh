#!/bin/bash
# Quick Start SwingSync AI on Android/Termux

echo "🏌️ SwingSync AI - Quick Mobile Start"
echo "====================================="

# Get device IP
echo "📱 Getting your device IP address..."
IP=$(ip route get 8.8.8.8 | awk '{print $7; exit}' 2>/dev/null || echo "localhost")
echo "📍 Your IP: $IP"

echo ""
echo "🚀 Starting SwingSync AI..."
echo "📱 Access URLs:"
echo "   • Main API: http://$IP:8000"
echo "   • Documentation: http://$IP:8000/docs"
echo "   • Health Check: http://$IP:8000/health"
echo ""
echo "💡 Open these URLs in any browser on your WiFi network!"
echo "🛑 Press Ctrl+C to stop"
echo ""

# Start the server
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload