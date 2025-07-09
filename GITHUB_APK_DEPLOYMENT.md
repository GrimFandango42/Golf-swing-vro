# 📱 Deploy Golf Swing VRO APK via GitHub Actions

This guide shows you how to build and download your APK using GitHub Actions - **no PC required!**

## 🚀 Step-by-Step Guide (All from your phone)

### Step 1: Commit and Push the Workflow
The GitHub Actions workflow file has already been created. Now push it:

```bash
# In Termux
cd /data/data/com.termux/files/home/Golf-swing-vro
git add .github/workflows/build-apk.yml gradlew gradle/
git commit -m "Add GitHub Actions workflow for APK building"
git push origin feat/golf-swing-analysis-backend
```

### Step 2: Set Up GitHub Secret (Important!)
You need to add your Gemini API key as a GitHub secret:

1. **Open GitHub in your phone browser**
2. Go to: https://github.com/GrimFandango42/Golf-swing-vro
3. Tap **Settings** → **Secrets and variables** → **Actions**
4. Tap **New repository secret**
5. Add:
   - Name: `GEMINI_API_KEY`
   - Value: Your API key (starts with AIza...)
6. Tap **Add secret**

### Step 3: Trigger the Build
You have 3 ways to trigger the build:

#### Option A: Manual Trigger (Easiest)
1. Go to: https://github.com/GrimFandango42/Golf-swing-vro/actions
2. Find "Build Golf Swing VRO APK"
3. Tap **Run workflow** → **Run workflow**

#### Option B: Push to Branch
```bash
# Any push to your branch triggers build
git push origin feat/golf-swing-analysis-backend
```

#### Option C: Merge PR
When you merge your PR, it automatically builds.

### Step 4: Download Your APK
After ~5-10 minutes:

1. Go to: https://github.com/GrimFandango42/Golf-swing-vro/actions
2. Click on the latest workflow run
3. Scroll down to **Artifacts**
4. Download **golf-swing-vro-debug**
5. The APK is inside the zip file

### Step 5: Install on Your Phone
1. Extract the downloaded zip
2. You'll find `app-debug.apk`
3. Enable **Install from unknown sources** in Android settings
4. Tap the APK to install
5. Enjoy Golf Swing VRO! 🎉

## 🔧 Troubleshooting

### Build Failed?
Check the error in Actions tab. Common issues:
- Missing GEMINI_API_KEY secret
- Gradle wrapper not found (already fixed)
- Code syntax errors

### Can't Download APK?
- Make sure the build succeeded (green checkmark)
- Artifacts expire after 7 days
- Check you're signed in to GitHub

### Installation Blocked?
1. Go to **Settings** → **Security**
2. Enable **Install unknown apps** for your browser
3. Try installing again

## 🎯 Alternative: Direct Release Downloads

For easier access, the workflow also creates releases:

1. Go to: https://github.com/GrimFandango42/Golf-swing-vro/releases
2. Download the latest APK directly
3. No need to extract from zip!

## 📊 Build Status

You can add this badge to your README to show build status:
```markdown
![Build Status](https://github.com/GrimFandango42/Golf-swing-vro/workflows/Build%20Golf%20Swing%20VRO%20APK/badge.svg)
```

## 🚀 Next Steps

1. **Test the APK** on your Pixel phone
2. **Share feedback** via GitHub issues
3. **Iterate** based on real-world usage

That's it! You've successfully deployed your Android app without needing a PC. The power of modern CI/CD! 🎉