#!/usr/bin/env python3
"""
SwingSync AI Mobile Launcher
Launch SwingSync AI optimized for mobile/Termux
"""

import subprocess
import sys
import os
import socket

def get_local_ip():
    """Get local IP address"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "localhost"

def main():
    print("🏌️ Starting SwingSync AI Mobile...")
    print("=" * 40)
    
    # Get IP address
    local_ip = get_local_ip()
    
    print(f"📱 Device IP: {local_ip}")
    print(f"🌐 API URL: http://{local_ip}:8000")
    print(f"📖 Docs: http://{local_ip}:8000/docs")
    print(f"🩺 Health: http://{local_ip}:8000/health")
    print()
    print("🎯 SwingSync AI Features Available:")
    print("   • Swing Analysis API")
    print("   • AI Coaching Feedback") 
    print("   • Progress Tracking")
    print("   • Voice Interface")
    print()
    print("📱 Access from any device on your network!")
    print("🛑 Press Ctrl+C to stop")
    print()
    
    try:
        # Start with mobile-optimized settings
        subprocess.run([
            sys.executable, "-m", "uvicorn", 
            "main:app",
            "--host", "0.0.0.0",
            "--port", "8000",
            "--reload",
            "--access-log",
            "--log-level", "warning"
        ])
    except KeyboardInterrupt:
        print("\n👋 SwingSync AI stopped")
    except Exception as e:
        print(f"❌ Error: {e}")
        print("💡 Try: python -m uvicorn main:app --host 0.0.0.0 --port 8000")

if __name__ == "__main__":
    main()
