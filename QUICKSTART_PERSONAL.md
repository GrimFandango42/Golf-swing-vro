# 🏌️ SwingSync AI - Personal Quick Start Guide

## 🚀 Quick Setup (5 minutes)

### 1. Install Dependencies
```bash
# If pip install is slow, try:
pip install --no-deps -r requirements-personal.txt
# Then install dependencies one by one
```

### 2. Add Your Gemini API Key
Edit `.env` file and replace `your_api_key_here` with your actual key.

### 3. Start the Server
```bash
python main.py
# Or use: uvicorn main:app --reload
```

### 4. Access the System
- API: http://localhost:8000
- Docs: http://localhost:8000/docs
- Health Check: http://localhost:8000/health

## 🎯 Key Features for Personal Use

### ✅ What's Ready Now
1. **32 Biomechanical KPIs** including X-Factor
2. **6 AI Coaching Personalities**
3. **Voice-Enabled Coaching** with noise reduction
4. **Real-time Swing Analysis**
5. **Progress Tracking**

### 📱 Testing Your First Swing

1. **Create Your Account** (via API):
```bash
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "username": "your_username",
    "password": "your_password",
    "skill_level": "INTERMEDIATE"
  }'
```

2. **Login and Get Token**:
```bash
curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username_or_email": "your_username",
    "password": "your_password"
  }'
```

3. **Analyze a Swing**:
Use the `/analyze_swing/` endpoint with your video data.

## 🔧 Personal Optimizations Applied

### Performance
- ✅ Frame rates optimized (30fps max)
- ✅ Memory management improved
- ✅ Single-user SQLite database

### Security
- ✅ Auto-generated secret keys
- ✅ Local-only CORS policy
- ✅ ProGuard enabled for Android

### Features
- ✅ X-Factor calculation added
- ✅ Golf course noise reduction
- ✅ Simplified dependencies

## 🎤 Voice Commands

Say these while practicing:
- "Start practice session"
- "Analyze my swing"
- "What should I work on?"
- "Give me tips for my driver"
- "How's my tempo?"

## 🚨 Troubleshooting

### If Dependencies Won't Install
```bash
# Try installing one at a time:
pip install sqlalchemy
pip install fastapi
pip install uvicorn
# etc...
```

### If Server Won't Start
1. Check `.env` file exists
2. Ensure port 8000 is free
3. Try: `python -m uvicorn main:app --port 8080`

### If No AI Feedback
- Add your Gemini API key to `.env`
- Get free key at: https://aistudio.google.com/app/apikey

## 📱 Android App Setup

1. Open Android Studio
2. Import `/android` folder
3. Update `ApiClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://YOUR_IP:8000/"
   ```
4. Build and run on device

## 🏌️ Ready to Improve Your Swing!

The system is optimized for personal use. Start with the API, then move to the Android app when ready. Happy golfing! 🎯