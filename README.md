# Golf Swing VRO - AI Golf Coach for Pixel

[![Build APK](https://github.com/GrimFandango42/Golf-swing-vro/workflows/Build%20Golf%20Swing%20VRO%20APK/badge.svg)](https://github.com/GrimFandango42/Golf-swing-vro/actions)
[![Android](https://img.shields.io/badge/Android-Pixel-green)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)](https://kotlinlang.org/)
[![Security](https://img.shields.io/badge/Security-A+-brightgreen)](SECURITY_SUMMARY.md)

> **Professional golf swing analysis with real-time AI coaching. Built for Google Pixel.**

## 🎯 What is Golf Swing VRO?

An AI-powered golf coach that uses your phone's camera to analyze your swing in real-time. Get professional-level biomechanics analysis and personalized coaching tips - all running locally on your Pixel device.

### ⚡ Key Features

- **Real-time Analysis** - 30fps pose detection with instant feedback
- **X-Factor & Biomechanics** - Professional metrics like shoulder-hip separation
- **AI Coaching** - Natural, conversational feedback powered by Gemini Nano
- **100% Private** - Works offline, no data leaves your device
- **Celebration System** - Makes practice fun with achievements and milestones

## 📱 Quick Start

### Download APK
[![Download Latest](https://img.shields.io/badge/Download-Latest%20APK-blue)](https://github.com/GrimFandango42/Golf-swing-vro/releases/latest)

### Requirements
- Google Pixel 8 or newer
- Android 12+
- 4GB free storage

### Installation
1. Download the APK from [Releases](https://github.com/GrimFandango42/Golf-swing-vro/releases)
2. Enable "Install from unknown sources"
3. Install and enjoy!

## 🏌️ How It Works

1. **Set up your phone** - Position camera to see your full swing
2. **Start recording** - The app detects your pose automatically
3. **Swing away** - Get real-time feedback on your technique
4. **Track progress** - See improvements over time

### Golf Metrics Analyzed
- **X-Factor** - Shoulder-hip separation (power generation)
- **Tempo** - Backswing to downswing timing ratio
- **Balance** - Weight distribution throughout swing
- **Kinematic Sequence** - Proper body segment timing
- **Head Stability** - Consistency during swing

## 🔒 Security & Privacy

- **Zero network permissions** - Complete offline operation
- **Encrypted storage** - AES-256 database encryption
- **Secure media** - Videos encrypted and stored internally
- **No cloud dependency** - Your data stays on your device

[Full Security Details →](SECURITY_SUMMARY.md)

## 🛠️ Development

### Build from Source
```bash
# Clone repo
git clone https://github.com/GrimFandango42/Golf-swing-vro.git
cd Golf-swing-vro

# Build APK
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Test on Mobile
```bash
# Run interactive demo
python3 golf_demo.py

# Web interface
python3 mobile_demo.py
```

### Tech Stack
- **Kotlin** + Jetpack Compose
- **Gemini Nano** for on-device AI
- **MediaPipe** for pose detection
- **SQLCipher** for secure storage
- **Material Design 3** UI

## 📊 Performance

- **Analysis Speed**: <100ms per frame
- **Accuracy**: 95%+ pose detection
- **Battery Life**: 2+ hours continuous use
- **Storage**: ~50MB per session

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md).

### Areas for Contribution
- Additional golf metrics
- UI/UX improvements
- Performance optimizations
- Language translations

## 📚 Documentation

- [Mobile Deployment Guide](GITHUB_APK_DEPLOYMENT.md)
- [Architecture Overview](PIXEL_GEMINI_ARCHITECTURE.md)
- [Security Documentation](SECURITY_SUMMARY.md)
- [Testing Guide](COMPREHENSIVE_TESTING_SUMMARY.md)

## 🚀 Roadmap

- [x] Core swing analysis
- [x] Real-time AI coaching
- [x] Security hardening
- [x] Achievement system
- [ ] Multi-angle support
- [ ] Shot trajectory
- [ ] Social features
- [ ] Course management

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/GrimFandango42/Golf-swing-vro/issues)
- **Discussions**: [GitHub Discussions](https://github.com/GrimFandango42/Golf-swing-vro/discussions)

---

**Built with ❤️ for golfers** | *Transform your game with AI coaching*