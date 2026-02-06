# Music Player Application

## Overview

This project presents a gesture-controlled music player application for the Android platform. The application leverages modern Android development practices, including Jetpack Compose for declarative UI construction and AndroidX Media3 for robust media playback functionality. A distinguishing feature of this implementation is the integration of Google MediaPipe for real-time hand gesture recognition, enabling touchless control of playback operations.

## System Requirements

| Requirement | Specification |
|-------------|---------------|
| Minimum Android SDK | API Level 29 (Android 10.0) |
| Target Android SDK | API Level 36 |
| Build System | Gradle with Kotlin DSL |
| Development Environment | Android Studio Ladybug or later |

## Core Features

### Media Playback
- Local audio file playback utilizing ExoPlayer via AndroidX Media3
- Background playback support through MediaSessionService implementation
- Standard playback controls: play, pause, seek, next, and previous track
- Shuffle and repeat mode functionality (Off, Repeat One, Repeat All)

### User Interface
- Material Design 3 theming with dynamic color adaptation
- Palette extraction from album artwork for contextual UI coloring
- Responsive layout supporting various screen configurations
- Navigation between library view and player view

### Gesture Recognition
The application implements hand gesture recognition using Google MediaPipe Vision. Supported gestures include:

| Gesture | Action |
|---------|--------|
| Open Palm | Toggle Play/Pause |
| Thumb Up | Skip to Next Track |
| Thumb Down | Return to Previous Track |
| Pinch Spread | Increase Volume |
| Pinch Close | Decrease Volume |

## Technical Architecture

### Component Structure

```
com.example.musicplayer/
├── MainActivity.kt              # Application entry point
├── AppNavigation.kt             # Navigation graph configuration
├── data/
│   ├── AudioFile.kt             # Data class for audio metadata
│   └── AudioRepository.kt       # MediaStore query implementation
├── service/
│   └── PlaybackService.kt       # MediaSessionService for background playback
└── ui/
    ├── MusicViewModel.kt        # ViewModel for state management
    ├── MusicPlayerScreen.kt     # Player interface composable
    ├── LibraryScreen.kt         # Library browser composable
    ├── GestureAnalyzer.kt       # MediaPipe gesture processing
    ├── PaletteExtractor.kt      # Album art color extraction utility
    └── theme/                   # Material theming configuration
```

### Dependencies

| Category | Library |
|----------|---------|
| UI Framework | Jetpack Compose with Material 3 |
| Media Playback | AndroidX Media3 ExoPlayer, Media3 Session |
| State Management | Kotlin StateFlow, ViewModel |
| Gesture Recognition | Google MediaPipe Tasks Vision |
| Image Loading | Coil Compose |
| Permissions Handling | Accompanist Permissions |
| Asynchronous Operations | Kotlin Coroutines |

### Design Pattern

The application follows the Model-View-ViewModel (MVVM) architectural pattern:

1. **Model Layer**: `AudioRepository` queries the device MediaStore for audio files. `AudioFile` represents the data model.

2. **ViewModel Layer**: `MusicViewModel` manages application state using `StateFlow`, handles user interactions, and communicates with the playback service via `MediaController`.

3. **View Layer**: Composable functions (`MusicPlayerScreen`, `LibraryScreen`) observe state changes and render the user interface accordingly.

## Installation and Configuration

### Prerequisites

1. Clone the repository to your local development environment.
2. Obtain the MediaPipe gesture recognizer model file (`gesture_recognizer.task`) from the official MediaPipe documentation.
3. Place the model file in the following directory:
   ```
   app/src/main/assets/gesture_recognizer.task
   ```

### Build Instructions

Execute the following command to build the application:

```bash
./gradlew assembleDebug
```

Alternatively, utilize Android Studio's build and run functionality.

### Required Permissions

The application requires the following permissions at runtime:

- **Media/Audio Access**: Required to read audio files from device storage
- **Camera Access**: Required for gesture recognition functionality

## License

This project is distributed under the MIT License. Refer to the LICENSE file for complete terms.

## Acknowledgments

- AndroidX Media3 Team for the comprehensive media playback framework
- Google MediaPipe Team for the gesture recognition solution
- Jetpack Compose Team for the modern UI toolkit
