# SwingSync AI - Android Application

A modern Android application for golf swing analysis using AI-powered pose estimation, real-time coaching, and comprehensive swing analytics.

## Features

### Core Functionality
- **Video Recording**: High-quality swing recording with CameraX
- **Real-time Pose Estimation**: MediaPipe integration for live swing analysis
- **AI-Powered Coaching**: Intelligent feedback and recommendations
- **Voice Interface**: Speech recognition and text-to-speech coaching
- **Offline Capability**: Local storage and processing when network unavailable
- **Cloud Sync**: Seamless data synchronization with backend services

### Technical Features
- **Modern Architecture**: MVVM pattern with Clean Architecture principles
- **Jetpack Compose UI**: Modern declarative UI framework
- **Dependency Injection**: Hilt for clean dependency management
- **Local Database**: Room for offline data persistence
- **Network Layer**: Retrofit + OkHttp for API communication
- **WebSocket Support**: Real-time streaming analysis
- **Material Design 3**: Latest Material Design guidelines

## Architecture

### Layer Structure
```
├── presentation/           # UI Layer (Activities, Fragments, ViewModels)
├── domain/                # Business Logic Layer (Use Cases, Repository Interfaces)
├── data/                  # Data Layer (Repository Implementations, Data Sources)
├── utils/                 # Utility Classes (Camera, MediaPipe, Voice, Network)
└── di/                    # Dependency Injection Modules
```

### Key Components

#### Camera Module (`utils/camera/`)
- **CameraManager**: Manages camera lifecycle and video recording
- **Features**: Auto-focus, exposure control, high-quality video capture
- **Integration**: CameraX with pose estimation pipeline

#### Pose Estimation (`utils/mediapipe/`)
- **PoseEstimationManager**: MediaPipe integration for real-time pose detection
- **SwingPhaseAnalysis**: Intelligent swing phase detection (Address, Backswing, etc.)
- **Features**: 33-point pose landmarks, confidence scoring, phase transitions

#### Voice Interface (`utils/voice/`)
- **VoiceManager**: Speech recognition and text-to-speech
- **Features**: Real-time coaching, voice commands, multilingual support
- **Commands**: Start/stop recording, analysis requests, settings control

#### Network Management (`utils/network/`)
- **NetworkManager**: Connection monitoring and adaptive behavior
- **Features**: WiFi/cellular detection, bandwidth optimization, offline fallback

## Data Models

### SwingAnalysis
```kotlin
data class SwingAnalysis(
    val id: String,
    val userId: String,
    val videoPath: String,
    val timestamp: Date,
    val poseData: String,
    val analysisResults: AnalysisResults,
    val feedback: String,
    val score: Float,
    val swingType: SwingType
)
```

### UserProfile
```kotlin
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val handicap: Float?,
    val dominantHand: HandType,
    val experienceLevel: ExperienceLevel,
    val settings: UserSettings
)
```

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.10+
- Gradle 8.1+

### Dependencies
```gradle
// Core Android
implementation "androidx.core:core-ktx:1.12.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
implementation "androidx.activity:activity-compose:1.8.0"

// Compose
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.material3:material3:1.1.2"

// Architecture
implementation "com.google.dagger:hilt-android:2.48"
implementation "androidx.room:room-runtime:2.5.0"
implementation "androidx.navigation:navigation-compose:2.7.4"

// Camera & MediaPipe
implementation "androidx.camera:camera-core:1.3.0"
implementation "com.google.mediapipe:tasks-vision:0.10.7"

// Network
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.okhttp3:okhttp:4.11.0"
```

### Build Configuration
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Add MediaPipe model files to assets folder
5. Configure API endpoints in build configuration
6. Build and run

### Required Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## API Integration

### Base URL Configuration
```kotlin
// Update in AppModule.kt
.baseUrl("https://api.swingsync.ai/v1/")
```

### WebSocket Configuration
```kotlin
// Real-time analysis endpoint
wss://api.swingsync.ai/v1/realtime
```

## Usage

### Recording a Swing
1. Open camera screen
2. Position phone to capture full swing
3. Tap record button
4. Perform golf swing
5. Stop recording
6. Review AI analysis and feedback

### Voice Commands
- "Start recording" - Begin video capture
- "Stop recording" - End video capture
- "Analyze swing" - Process current recording
- "Help" - Show instructions

### Settings Configuration
- Video quality: Low/Medium/High
- Analysis sensitivity: Low/Medium/High
- Voice coaching: Enable/Disable
- Offline mode: Enable/Disable

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- ViewModels: Business logic testing
- Repository: Data layer testing
- Use Cases: Domain logic testing

## Deployment

### Release Build
```bash
./gradlew assembleRelease
```

### Play Store Preparation
1. Generate signed APK/AAB
2. Configure ProGuard rules
3. Test on multiple devices
4. Submit for review

## Contributing

1. Fork the repository
2. Create feature branch
3. Implement changes with tests
4. Submit pull request
5. Code review and merge

## License

Proprietary - SwingSync AI Technologies

## Support

For technical support or questions:
- Email: support@swingsync.ai
- Documentation: https://docs.swingsync.ai
- Issues: GitHub Issues tracker