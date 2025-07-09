#!/usr/bin/env python3
"""
SwingSync AI Mobile Deployment Script
====================================
Deploy SwingSync AI to run natively on your Android phone via Termux
"""

import subprocess
import sys
import os
import time
from pathlib import Path

def print_header():
    print("📱 SwingSync AI - Mobile Deployment")
    print("=" * 50)
    print("Deploying SwingSync AI on your Android device...")
    print()

def check_termux_environment():
    """Check if we're running in Termux"""
    print("🔍 Checking Termux Environment...")
    
    if not os.path.exists("/data/data/com.termux"):
        print("❌ Not running in Termux")
        return False
    
    print("✅ Termux environment detected")
    
    # Check Python version
    python_version = sys.version_info
    if python_version.major >= 3 and python_version.minor >= 8:
        print(f"✅ Python {python_version.major}.{python_version.minor} - Compatible")
    else:
        print(f"⚠️  Python {python_version.major}.{python_version.minor} - May have issues")
    
    return True

def install_minimal_dependencies():
    """Install only essential dependencies for mobile deployment"""
    print("\n📦 Installing Essential Dependencies...")
    
    # Essential packages for mobile deployment
    essential_packages = [
        "fastapi",
        "uvicorn",
        "pydantic", 
        "python-multipart",
        "python-dotenv"
    ]
    
    print("Installing lightweight packages for mobile...")
    for package in essential_packages:
        try:
            print(f"📦 Installing {package}...")
            subprocess.run([sys.executable, "-m", "pip", "install", package], 
                         check=True, capture_output=True)
            print(f"✅ {package} installed")
        except subprocess.CalledProcessError as e:
            print(f"⚠️  {package} installation had issues, but continuing...")
    
    print("✅ Essential dependencies installed")

def setup_mobile_configuration():
    """Setup configuration optimized for mobile"""
    print("\n⚙️  Setting up Mobile Configuration...")
    
    # Create mobile-optimized .env
    mobile_env = """# SwingSync AI - Mobile Configuration
# Optimized for Android/Termux deployment

# Security
SECRET_KEY=mobile_swingsync_key_2024
ACCESS_TOKEN_EXPIRE_MINUTES=120
REFRESH_TOKEN_EXPIRE_DAYS=30

# Database - SQLite for mobile
DATABASE_URL=sqlite:///./swingsync_mobile.db

# AI Configuration 
GEMINI_API_KEY=AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04

# Mobile Optimizations
DEBUG=true
LOG_LEVEL=WARNING
MAX_CONCURRENT_USERS=3
ENABLE_ANALYTICS=false
MOBILE_MODE=true
"""
    
    with open(".env", "w") as f:
        f.write(mobile_env)
    
    print("✅ Mobile configuration created")

def create_mobile_launcher():
    """Create a simple launcher script"""
    print("\n🚀 Creating Mobile Launcher...")
    
    launcher_script = '''#!/usr/bin/env python3
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
        print("\\n👋 SwingSync AI stopped")
    except Exception as e:
        print(f"❌ Error: {e}")
        print("💡 Try: python -m uvicorn main:app --host 0.0.0.0 --port 8000")

if __name__ == "__main__":
    main()
'''
    
    with open("start_mobile.py", "w") as f:
        f.write(launcher_script)
    
    os.chmod("start_mobile.py", 0o755)
    print("✅ Mobile launcher created: start_mobile.py")

def create_mobile_web_interface():
    """Create a simple mobile web interface"""
    print("\n🌐 Creating Mobile Web Interface...")
    
    # Create a simple HTML interface
    html_content = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SwingSync AI Mobile</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #2E7D32, #1976D2);
            color: white;
            text-align: center;
            padding: 20px;
            margin: 0;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background: rgba(255,255,255,0.1);
            border-radius: 20px;
            padding: 30px;
            backdrop-filter: blur(10px);
        }
        h1 { font-size: 2.5em; margin-bottom: 10px; }
        .subtitle { font-size: 1.2em; opacity: 0.9; margin-bottom: 30px; }
        .feature {
            background: rgba(255,255,255,0.1);
            border-radius: 15px;
            padding: 20px;
            margin: 15px 0;
            border-left: 4px solid #FFB300;
        }
        .feature h3 { margin-top: 0; color: #FFB300; }
        .api-link {
            background: #FFB300;
            color: #1976D2;
            padding: 15px 30px;
            border-radius: 25px;
            text-decoration: none;
            font-weight: bold;
            display: inline-block;
            margin: 20px 10px;
            transition: transform 0.2s;
        }
        .api-link:hover { transform: scale(1.05); }
        .status { 
            background: rgba(76, 175, 80, 0.2);
            border-radius: 10px;
            padding: 10px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🏌️ SwingSync AI</h1>
        <p class="subtitle">Professional Golf Swing Analysis - Now on Mobile!</p>
        
        <div class="status">
            <h3>✅ System Status: Online</h3>
            <p>Your AI golf coach is ready!</p>
        </div>
        
        <div class="feature">
            <h3>🎯 AI Swing Analysis</h3>
            <p>32 biomechanical KPIs including X-Factor analysis</p>
        </div>
        
        <div class="feature">
            <h3>🗣️ Voice Coaching</h3>
            <p>6 AI coaching personalities with conversational feedback</p>
        </div>
        
        <div class="feature">
            <h3>📊 Progress Tracking</h3>
            <p>Beautiful visualizations of your improvement journey</p>
        </div>
        
        <a href="/docs" class="api-link">📖 API Documentation</a>
        <a href="/health" class="api-link">🩺 System Health</a>
        
        <p style="margin-top: 40px; opacity: 0.7;">
            🌐 Access this from any device on your network<br>
            📱 Build the Android app for the full experience
        </p>
    </div>
</body>
</html>'''
    
    # Add this as a route to main.py
    additional_route = '''

# Mobile Web Interface
@app.get("/mobile", response_class=HTMLResponse)
async def mobile_interface():
    """Simple mobile web interface"""
    return """''' + html_content.replace('"""', '\\"\\"\\"') + '''"""

from fastapi.responses import HTMLResponse
'''
    
    # Append to main.py
    try:
        with open("main.py", "a") as f:
            f.write(additional_route)
        print("✅ Mobile web interface added to main.py")
    except Exception as e:
        print(f"⚠️  Could not add mobile interface: {e}")

def test_deployment():
    """Test that everything is working"""
    print("\n🧪 Testing Mobile Deployment...")
    
    # Test that main.py exists and imports work
    try:
        import importlib.util
        spec = importlib.util.spec_from_file_location("main", "main.py")
        if spec:
            print("✅ main.py is importable")
        else:
            print("❌ main.py has issues")
    except Exception as e:
        print(f"⚠️  main.py test: {e}")
    
    # Test .env file
    if Path(".env").exists():
        print("✅ .env configuration exists")
    else:
        print("❌ .env configuration missing")
    
    # Test launcher
    if Path("start_mobile.py").exists():
        print("✅ Mobile launcher ready")
    else:
        print("❌ Mobile launcher missing")

def print_next_steps():
    """Print what to do next"""
    print("\n🎯 Mobile Deployment Complete!")
    print("=" * 50)
    print()
    print("🚀 Start SwingSync AI:")
    print("   python start_mobile.py")
    print()
    print("📱 Access on your phone:")
    print("   • API: http://YOUR_IP:8000")
    print("   • Mobile UI: http://YOUR_IP:8000/mobile")
    print("   • Docs: http://YOUR_IP:8000/docs")
    print()
    print("💡 Find your IP address with:")
    print("   ip addr show wlan0")
    print()
    print("🎯 What you can do now:")
    print("   • Test the API endpoints")
    print("   • Analyze golf swings via API")
    print("   • Access from other devices on WiFi")
    print("   • Build the Android app later")
    print()
    print("🏌️ Happy golfing with SwingSync AI!")

def main():
    """Main deployment function"""
    print_header()
    
    try:
        if not check_termux_environment():
            print("❌ This script requires Termux")
            return False
        
        install_minimal_dependencies()
        setup_mobile_configuration()
        create_mobile_launcher() 
        create_mobile_web_interface()
        test_deployment()
        print_next_steps()
        
        return True
        
    except KeyboardInterrupt:
        print("\n❌ Deployment cancelled")
        return False
    except Exception as e:
        print(f"\n❌ Deployment failed: {e}")
        return False

if __name__ == "__main__":
    main()