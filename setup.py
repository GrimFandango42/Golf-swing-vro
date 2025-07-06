#!/usr/bin/env python3
"""
SwingSync AI Setup Script

This script helps set up the SwingSync AI application by:
1. Checking dependencies
2. Setting up environment variables
3. Initializing the database
4. Creating sample data (optional)
5. Verifying the installation

Usage:
    python setup.py
"""

import os
import sys
import subprocess
import secrets
from pathlib import Path

def print_header(title):
    """Print a formatted header."""
    print(f"\n{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")

def print_step(step_num, total_steps, description):
    """Print a formatted step."""
    print(f"\n[{step_num}/{total_steps}] {description}")
    print("-" * 40)

def check_python_version():
    """Check if Python version is compatible."""
    version = sys.version_info
    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print("❌ Python 3.8 or higher is required")
        return False
    print(f"✅ Python {version.major}.{version.minor}.{version.micro} detected")
    return True

def install_dependencies():
    """Install required dependencies."""
    print("Installing dependencies from requirements.txt...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
        print("✅ Dependencies installed successfully")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to install dependencies: {e}")
        return False

def setup_environment():
    """Set up environment variables."""
    env_path = Path(".env")
    env_example_path = Path(".env.example")
    
    if env_path.exists():
        print("⚠️  .env file already exists")
        overwrite = input("Do you want to overwrite it? (y/n): ").lower()
        if overwrite != 'y':
            print("Keeping existing .env file")
            return True
    
    if not env_example_path.exists():
        print("❌ .env.example file not found")
        return False
    
    # Read example file
    with open(env_example_path, 'r') as f:
        content = f.read()
    
    # Generate secret key
    secret_key = secrets.token_urlsafe(32)
    content = content.replace("your_super_secret_jwt_signing_key_change_in_production", secret_key)
    
    # Get Gemini API key from user
    print("\nGemini API Key Setup:")
    print("You need a Google Gemini API key for AI feedback generation.")
    print("Get one at: https://makersuite.google.com/app/apikey")
    
    gemini_key = input("Enter your Gemini API key (or press Enter to skip): ").strip()
    if gemini_key:
        content = content.replace("your_google_gemini_api_key_here", gemini_key)
    else:
        print("⚠️  Skipping Gemini API key - AI feedback will not work")
    
    # Write .env file
    with open(env_path, 'w') as f:
        f.write(content)
    
    print("✅ Environment configuration created")
    return True

def initialize_database():
    """Initialize the database."""
    print("Initializing database...")
    try:
        # Import after potential dependency installation
        sys.path.append(os.path.dirname(os.path.abspath(__file__)))
        from database import init_database
        
        init_database()
        print("✅ Database initialized successfully")
        return True
    except Exception as e:
        print(f"❌ Failed to initialize database: {e}")
        return False

def create_sample_data():
    """Create sample data for testing."""
    create_data = input("\nWould you like to create sample data for testing? (y/n): ").lower()
    if create_data != 'y':
        print("Skipping sample data creation")
        return True
    
    print("Creating sample data...")
    try:
        subprocess.check_call([sys.executable, "migrate.py", "seed"])
        print("✅ Sample data created successfully")
        print("   Sample login: john.doe@example.com / password123")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to create sample data: {e}")
        return False

def verify_installation():
    """Verify the installation by running basic tests."""
    print("Verifying installation...")
    try:
        # Try importing main modules
        sys.path.append(os.path.dirname(os.path.abspath(__file__)))
        import database
        import user_management
        import main
        
        print("✅ All modules imported successfully")
        
        # Check database connection
        from database import SessionLocal
        db = SessionLocal()
        db.close()
        print("✅ Database connection successful")
        
        return True
    except Exception as e:
        print(f"❌ Verification failed: {e}")
        return False

def print_next_steps():
    """Print next steps for the user."""
    print_header("Setup Complete! 🎉")
    
    print("\nNext Steps:")
    print("1. Start the API server:")
    print("   uvicorn main:app --reload")
    print()
    print("2. Access the API documentation:")
    print("   http://127.0.0.1:8000/docs")
    print()
    print("3. Test the API:")
    print("   - Register a new user: POST /auth/register")
    print("   - Login: POST /auth/login") 
    print("   - Analyze swing: POST /analyze_swing/")
    print()
    print("4. Manage the database:")
    print("   python migrate.py status    # Check database status")
    print("   python migrate.py backup    # Create backup")
    print("   python migrate.py reset     # Reset database (deletes all data)")
    print()
    print("Need help? Check the documentation in README.md")

def main():
    """Main setup process."""
    print_header("SwingSync AI Setup")
    print("This script will help you set up the SwingSync AI application.")
    
    total_steps = 6
    current_step = 0
    
    # Step 1: Check Python version
    current_step += 1
    print_step(current_step, total_steps, "Checking Python version")
    if not check_python_version():
        print("\n❌ Setup failed: Incompatible Python version")
        sys.exit(1)
    
    # Step 2: Install dependencies
    current_step += 1
    print_step(current_step, total_steps, "Installing dependencies")
    if not install_dependencies():
        print("\n❌ Setup failed: Could not install dependencies")
        sys.exit(1)
    
    # Step 3: Setup environment
    current_step += 1
    print_step(current_step, total_steps, "Setting up environment configuration")
    if not setup_environment():
        print("\n❌ Setup failed: Could not setup environment")
        sys.exit(1)
    
    # Step 4: Initialize database
    current_step += 1
    print_step(current_step, total_steps, "Initializing database")
    if not initialize_database():
        print("\n❌ Setup failed: Could not initialize database")
        sys.exit(1)
    
    # Step 5: Create sample data (optional)
    current_step += 1
    print_step(current_step, total_steps, "Creating sample data (optional)")
    create_sample_data()  # Non-critical, continue even if it fails
    
    # Step 6: Verify installation
    current_step += 1
    print_step(current_step, total_steps, "Verifying installation")
    if not verify_installation():
        print("\n⚠️  Installation may have issues, but basic setup is complete")
    
    # Print next steps
    print_next_steps()

if __name__ == "__main__":
    main()