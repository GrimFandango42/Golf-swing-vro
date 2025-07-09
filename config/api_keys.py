"""
API Keys Configuration
======================
This file stores API keys with multiple fallback options.
IMPORTANT: Keep this file secure and don't commit to public repos!
"""

import os

# Primary method: Environment variable
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

# Fallback 1: Direct configuration
if not GEMINI_API_KEY:
    GEMINI_API_KEY = "AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04"

# Fallback 2: Try loading from .env file
if not GEMINI_API_KEY:
    try:
        from dotenv import load_dotenv
        load_dotenv()
        GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
    except ImportError:
        pass

# Fallback 3: Try loading from backup file
if not GEMINI_API_KEY:
    try:
        with open(".env.backup", "r") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY="):
                    GEMINI_API_KEY = line.split("=", 1)[1].strip()
                    break
    except:
        pass

# Final fallback: Hardcoded (your key)
if not GEMINI_API_KEY:
    GEMINI_API_KEY = "AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04"

def get_gemini_key():
    """Get Gemini API key with multiple fallbacks"""
    return GEMINI_API_KEY

# Test the key is accessible
if __name__ == "__main__":
    print(f"Gemini API Key: {GEMINI_API_KEY[:10]}...{GEMINI_API_KEY[-4:]}")
    print("✅ API key is configured and accessible")