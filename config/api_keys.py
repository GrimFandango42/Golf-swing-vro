"""
API Keys Configuration
======================
This file provides secure API key management using environment variables.
IMPORTANT: Never commit actual API keys to version control!
"""

import os
import sys
from typing import Optional

def get_gemini_key() -> Optional[str]:
    """
    Get Gemini API key securely from environment variables.
    
    Returns:
        Optional[str]: The API key if found, None otherwise
        
    Raises:
        EnvironmentError: If API key is not configured
    """
    # Try environment variable first
    api_key = os.getenv("GEMINI_API_KEY")
    
    if not api_key:
        # Try loading from .env file using python-dotenv
        try:
            from dotenv import load_dotenv
            load_dotenv()
            api_key = os.getenv("GEMINI_API_KEY")
        except ImportError:
            pass
    
    if not api_key:
        raise EnvironmentError(
            "GEMINI_API_KEY environment variable not set. "
            "Please set it in your environment or .env file."
        )
    
    return api_key

def validate_api_key(api_key: str) -> bool:
    """
    Validate API key format (basic validation).
    
    Args:
        api_key (str): The API key to validate
        
    Returns:
        bool: True if format appears valid, False otherwise
    """
    if not api_key:
        return False
    
    # Basic format validation for Google API keys
    return (
        api_key.startswith("AIza") and 
        len(api_key) == 39 and 
        api_key.replace("AIza", "").replace("_", "").replace("-", "").isalnum()
    )

# For backward compatibility
def get_api_key() -> Optional[str]:
    """Deprecated: Use get_gemini_key() instead"""
    import warnings
    warnings.warn("get_api_key() is deprecated. Use get_gemini_key() instead.", DeprecationWarning)
    return get_gemini_key()

# Test the key is accessible
if __name__ == "__main__":
    try:
        api_key = get_gemini_key()
        if validate_api_key(api_key):
            print(f"✅ Gemini API Key: {api_key[:10]}...{api_key[-4:]}")
            print("✅ API key is configured and format appears valid")
        else:
            print("❌ API key format appears invalid")
            sys.exit(1)
    except EnvironmentError as e:
        print(f"❌ Configuration Error: {e}")
        print("📋 Setup Instructions:")
        print("   1. Set GEMINI_API_KEY environment variable")
        print("   2. Or create a .env file with: GEMINI_API_KEY=your_key_here")
        sys.exit(1)