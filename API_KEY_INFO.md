# 🔑 SwingSync AI - API Key Security Guide

⚠️ **SECURITY WARNING**: This file previously contained exposed API keys. All sensitive information has been removed for security.

## Secure API Key Management

### Required Environment Variables

The application requires the following environment variable to be set:

```bash
GEMINI_API_KEY=your_actual_api_key_here
```

### Secure Setup Instructions

1. **Get Your API Key**
   - Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Create a new API key or use an existing one
   - Copy the key (starts with "AIza...")

2. **Set Environment Variable**
   ```bash
   # For current session
   export GEMINI_API_KEY=your_actual_key_here
   
   # For permanent setup (add to ~/.bashrc or ~/.zshrc)
   echo 'export GEMINI_API_KEY=your_actual_key_here' >> ~/.bashrc
   source ~/.bashrc
   ```

3. **Using .env File (Recommended)**
   - Create a `.env` file in the project root
   - Add: `GEMINI_API_KEY=your_actual_key_here`
   - The `.env` file is automatically ignored by git

### Using API Keys in Code

```python
import os
from config.api_keys import get_gemini_key

# Secure method - raises error if not configured
try:
    api_key = get_gemini_key()
    # Use api_key safely
except EnvironmentError as e:
    print(f"Configuration error: {e}")
    # Handle missing key appropriately
```

### Testing Your Configuration

```bash
# Test API key configuration
python config/api_keys.py

# Run comprehensive API test
python test_gemini_key.py
```

### Security Best Practices

1. **Never commit API keys to version control**
2. **Use environment variables or secure .env files**
3. **Rotate keys regularly**
4. **Monitor API usage for suspicious activity**
5. **Use separate keys for development and production**

### Troubleshooting

If you encounter API key issues:

1. **Check Environment Variable**
   ```bash
   echo $GEMINI_API_KEY
   ```

2. **Verify .env File**
   - Ensure `.env` file exists in project root
   - Check file contains: `GEMINI_API_KEY=your_key`
   - Verify no extra spaces or quotes

3. **Test API Key Format**
   - Should start with "AIza"
   - Should be 39 characters long
   - Should contain only alphanumeric characters and hyphens

### Rate Limits and Usage

- **Free Tier**: 60 requests per minute
- **Paid Tier**: Higher limits available
- **Model Support**: Gemini Pro and Gemini 2.5 Flash

### Getting Help

If you need assistance:
1. Check the [API documentation](https://ai.google.dev/docs)
2. Review the security logs
3. Contact support if issues persist

---
**Last updated**: 2025-07-09
**Security Status**: ✅ Hardcoded keys removed, secure configuration implemented