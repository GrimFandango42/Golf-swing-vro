# 🔑 SwingSync AI - API Key Information

## Your Gemini API Key
```
AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04
```

## Where Your Key is Saved (Multiple Backups)

1. **Primary Location**: `.env` file
   - Path: `/data/data/com.termux/files/home/Golf-swing-vro/.env`
   - Used by: Main application

2. **Backup Location**: `.env.backup` file
   - Path: `/data/data/com.termux/files/home/Golf-swing-vro/.env.backup`
   - Purpose: Backup if .env is deleted

3. **Python Fallback**: `config/api_keys.py`
   - Path: `/data/data/com.termux/files/home/Golf-swing-vro/config/api_keys.py`
   - Has multiple fallback methods to find the key

4. **Hardcoded Fallback**: `feedback_generation.py`
   - Path: `/data/data/com.termux/files/home/Golf-swing-vro/feedback_generation.py`
   - Line: ~590
   - Will use key even if environment variable not set

5. **This Documentation**: `API_KEY_INFO.md`
   - You're reading it now!

## How to Use

### From Python Code:
```python
import os
# Method 1: Environment variable
api_key = os.getenv("GEMINI_API_KEY", "AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04")

# Method 2: From config
from config.api_keys import get_gemini_key
api_key = get_gemini_key()
```

### From Command Line:
```bash
# Set temporarily
export GEMINI_API_KEY=AIzaSyB_ifq6-bO_pkMki5j5ECkBd0hDAqato04

# Check it's set
echo $GEMINI_API_KEY
```

## Testing Your Key
```bash
python test_gemini_key.py
```

## If You Lose It Again
Don't worry! Check any of these files:
- `.env`
- `.env.backup`
- `config/api_keys.py`
- `feedback_generation.py` (line ~590)
- This file (`API_KEY_INFO.md`)

## Important Notes
- This is a free tier Gemini API key
- Rate limits apply (60 requests per minute)
- Keep it secure - don't share publicly
- Works with Gemini Pro and Gemini 2.5 Flash models

---
Last updated: 2025-07-07
Key saved in 5 different locations for redundancy!