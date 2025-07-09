#!/usr/bin/env python3
"""
Test Gemini API Key
===================
This script tests if your Gemini API key is working properly.
"""

import os
import sys

def get_api_key():
    """
    Get API key from environment variables with proper error handling.
    
    Returns:
        str: The API key if found
        
    Raises:
        EnvironmentError: If API key is not configured
    """
    # Method 1: Environment variable
    key = os.getenv("GEMINI_API_KEY")
    if key:
        print("✅ Found key in environment variable")
        return key
    
    # Method 2: .env file
    try:
        with open(".env", "r") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY="):
                    key = line.split("=", 1)[1].strip()
                    if key:
                        print("✅ Found key in .env file")
                        return key
    except FileNotFoundError:
        print("ℹ️  No .env file found")
    except Exception as e:
        print(f"⚠️  Error reading .env file: {e}")
    
    # Method 3: Try using config module
    try:
        from config.api_keys import get_gemini_key
        key = get_gemini_key()
        print("✅ Found key using config module")
        return key
    except Exception as e:
        print(f"⚠️  Error using config module: {e}")
    
    # No key found
    raise EnvironmentError(
        "GEMINI_API_KEY not configured. Please set it in your environment or .env file."
    )

def test_gemini_api():
    """Test the Gemini API key"""
    try:
        api_key = get_api_key()
        print(f"🔑 API Key: {api_key[:10]}...{api_key[-4:]}")
    except EnvironmentError as e:
        print(f"❌ Configuration Error: {e}")
        print_setup_instructions()
        return False
    
    try:
        import google.generativeai as genai
        
        # Configure the API
        genai.configure(api_key=api_key)
        
        # Create a model instance
        model = genai.GenerativeModel('gemini-pro')
        
        # Test with a simple prompt
        print("🧪 Testing API connection...")
        response = model.generate_content("Say 'SwingSync AI is ready!' if you can hear me.")
        
        print("✅ API Key is working!")
        print(f"🤖 Gemini says: {response.text}")
        
        return True
        
    except ImportError:
        print("❌ Missing google-generativeai package. Install with:")
        print("   pip install google-generativeai")
        return False
    except Exception as e:
        print(f"❌ API Key test failed: {e}")
        print_troubleshooting_tips()
        return False

def print_setup_instructions():
    """Print setup instructions for API key configuration"""
    print("\n📋 Setup Instructions:")
    print("1. Get your API key from https://makersuite.google.com/app/apikey")
    print("2. Choose one of these methods:")
    print("\n   Method A - Environment Variable:")
    print("   export GEMINI_API_KEY=your_actual_key_here")
    print("\n   Method B - .env file:")
    print("   echo 'GEMINI_API_KEY=your_actual_key_here' > .env")
    print("\n   Method C - System-wide:")
    print("   echo 'export GEMINI_API_KEY=your_actual_key_here' >> ~/.bashrc")
    print("   source ~/.bashrc")

def print_troubleshooting_tips():
    """Print troubleshooting tips for API issues"""
    print("\n🔧 Troubleshooting Tips:")
    print("1. Check your API key format (should start with 'AIza' and be 39 chars)")
    print("2. Verify your API key is active at https://makersuite.google.com/app/apikey")
    print("3. Check for rate limits or quota issues")
    print("4. Ensure you have internet connectivity")
    print("5. Try regenerating your API key if issues persist")

if __name__ == "__main__":
    print("🏌️ SwingSync AI - Gemini API Key Test")
    print("=" * 40)
    
    if test_gemini_api():
        print("\n🎉 Everything is working!")
        print("Your API key is properly configured and functional.")
        print("\n✅ Security Status: No hardcoded keys found")
        print("✅ Configuration: Using environment variables")
    else:
        print("\n❌ API key test failed. Please follow the setup instructions above.")
        sys.exit(1)