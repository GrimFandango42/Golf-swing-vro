#!/usr/bin/env python3
"""
SwingSync AI - Personal Setup Script
====================================

This script sets up SwingSync AI for personal use with optimized settings
for individual golfers and small friend groups.

Key optimizations for personal use:
- Local database setup (SQLite)
- Development environment configuration
- Basic security with auto-generated keys
- Optimized performance for single user
- Simple voice processing setup
"""

import os
import sys
import subprocess
import sqlite3
from pathlib import Path
import secrets


def print_header():
    """Print setup header"""
    print("🏌️ SwingSync AI - Personal Setup")
    print("=" * 40)
    print("Setting up your personal golf swing analysis system...")
    print()


def check_dependencies():
    """Check if required dependencies are installed"""
    print("📋 Checking dependencies...")
    
    required_packages = [
        "fastapi",
        "uvicorn",
        "sqlalchemy", 
        "passlib",
        "python-jose",
        "numpy",
        "google-generativeai"
    ]
    
    missing_packages = []
    for package in required_packages:
        try:
            __import__(package.replace("-", "_"))
            print(f"✅ {package}")
        except ImportError:
            missing_packages.append(package)
            print(f"❌ {package} - MISSING")
    
    if missing_packages:
        print(f"\n⚠️  Installing missing packages: {', '.join(missing_packages)}")
        subprocess.run([sys.executable, "-m", "pip", "install"] + missing_packages)
    
    print("✅ All dependencies checked\n")


def setup_environment():
    """Setup environment variables for personal use"""
    print("🔧 Setting up environment...")
    
    env_file = Path(".env")
    
    # Generate secure secret key
    secret_key = secrets.token_urlsafe(32)
    
    env_content = f"""# SwingSync AI Personal Environment Configuration
# Generated automatically for personal use

# Security Keys
SECRET_KEY={secret_key}
ACCESS_TOKEN_EXPIRE_MINUTES=60
REFRESH_TOKEN_EXPIRE_DAYS=7

# Database (SQLite for personal use)
DATABASE_URL=sqlite:///./swingsync_personal.db

# AI Configuration (Add your API keys here)
GEMINI_API_KEY=your_gemini_api_key_here
OPENAI_API_KEY=your_openai_api_key_here

# Development Settings
DEBUG=true
LOG_LEVEL=INFO

# Personal Use Optimizations
MAX_CONCURRENT_USERS=5
ENABLE_ANALYTICS=true
ENABLE_VOICE_COACHING=true
"""
    
    with open(env_file, "w") as f:
        f.write(env_content)
    
    print(f"✅ Environment file created: {env_file}")
    print("⚠️  Remember to add your API keys to .env file")
    print()


def setup_database():
    """Initialize SQLite database for personal use"""
    print("🗄️  Setting up personal database...")
    
    db_path = "swingsync_personal.db"
    
    # Remove existing database if it exists
    if os.path.exists(db_path):
        os.remove(db_path)
        print(f"🗑️  Removed existing database")
    
    # Initialize database using our existing setup
    try:
        from database import init_database
        init_database()
        print("✅ Database initialized successfully")
    except Exception as e:
        print(f"❌ Database setup failed: {e}")
        print("📝 Manual setup required - see README for details")
    
    print()


def create_launch_script():
    """Create easy launch script for personal use"""
    print("🚀 Creating launch script...")
    
    launch_script = Path("start_swingsync.py")
    
    script_content = '''#!/usr/bin/env python3
"""
SwingSync AI Personal Launch Script
Starts the SwingSync AI server for personal use
"""

import os
import sys
import subprocess
from pathlib import Path

def main():
    print("🏌️ Starting SwingSync AI...")
    print("=" * 40)
    
    # Load environment variables
    env_file = Path(".env")
    if env_file.exists():
        print("✅ Loading environment configuration")
    else:
        print("⚠️  No .env file found - using defaults")
    
    # Check if API keys are configured
    gemini_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not gemini_key or gemini_key == "your_gemini_api_key_here":
        print("⚠️  GEMINI_API_KEY not configured - some features may not work")
        print("   Add your API key to the .env file")
    
    # Start the server
    print("🚀 Starting SwingSync AI server...")
    print("📱 Access the API at: http://localhost:8000")
    print("📖 API Documentation: http://localhost:8000/docs")
    print("🛑 Press Ctrl+C to stop")
    print()
    
    try:
        subprocess.run([
            sys.executable, "-m", "uvicorn", 
            "main:app", 
            "--reload",
            "--host", "0.0.0.0",
            "--port", "8000"
        ])
    except KeyboardInterrupt:
        print("\\n👋 SwingSync AI stopped")
    except Exception as e:
        print(f"❌ Error starting server: {e}")

if __name__ == "__main__":
    main()
'''
    
    with open(launch_script, "w") as f:
        f.write(script_content)
    
    # Make executable
    os.chmod(launch_script, 0o755)
    
    print(f"✅ Launch script created: {launch_script}")
    print()


def create_test_user():
    """Create a test user for immediate use"""
    print("👤 Creating test user...")
    
    try:
        from database import get_db
        from user_management import UserRegistration, create_user
        
        # Create test user
        test_user = UserRegistration(
            email="golfer@swingsync.ai",
            username="test_golfer", 
            password="swingsync123",
            first_name="Test",
            last_name="Golfer",
            skill_level="INTERMEDIATE"
        )
        
        db = next(get_db())
        user = create_user(db, test_user)
        db.close()
        
        print("✅ Test user created successfully!")
        print("   Email: golfer@swingsync.ai")
        print("   Username: test_golfer")
        print("   Password: swingsync123")
        
    except Exception as e:
        print(f"⚠️  Could not create test user: {e}")
        print("   You can create users via the API after starting the server")
    
    print()


def print_next_steps():
    """Print next steps for the user"""
    print("🎯 Setup Complete! Next Steps:")
    print("=" * 40)
    print()
    print("1. 🔑 Add your API keys to .env file:")
    print("   - Get Gemini API key: https://aistudio.google.com/app/apikey")
    print("   - Edit .env file and replace 'your_gemini_api_key_here'")
    print()
    print("2. 🚀 Start SwingSync AI:")
    print("   python start_swingsync.py")
    print()
    print("3. 📱 Test the system:")
    print("   - Open browser: http://localhost:8000/docs")
    print("   - Login with test account: test_golfer / swingsync123")
    print("   - Try the /health endpoint")
    print()
    print("4. 📲 For Android app:")
    print("   - Open Android Studio")
    print("   - Import the /android folder")
    print("   - Update API base URL to your local IP")
    print()
    print("🏌️ Happy golfing with SwingSync AI!")


def main():
    """Main setup function"""
    print_header()
    
    try:
        check_dependencies()
        setup_environment()
        setup_database()
        create_launch_script()
        create_test_user()
        print_next_steps()
        
    except KeyboardInterrupt:
        print("\n❌ Setup cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Setup failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()