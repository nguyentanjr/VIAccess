# VisionAid: Visually Impaired Assistance Application

VisionAid (Internal Code Name: **Shravan**) is an advanced Android application designed to empower individuals with visual impairments. Built with a "Human-Computer Interaction (HCI) First" philosophy, it provides independent access to text and object recognition through a seamless, audio-centric interface.

## 🌟 Overview

VisionAid addresses the critical need for independent information access. Whether it's reading a medication label, finding a misplaced chair, or exploring a new environment, VisionAid transforms visual data into actionable audio and haptic feedback.

Unlike traditional solutions, VisionAid is designed for **Screen-Independent Usage**, catering to two primary user groups:
- **Totally Blind Users:** Audio-first interaction, voice commands, and haptic (vibration) feedback.
- **Partially Sighted Users:** High-contrast interfaces, large typography, and assistive visuals.

## ✨ Key Features

### 🔍 Vision & AI
- **Explore Mode:** Real-time object detection using YOLOv5, announcing surroundings via Text-to-Speech (TTS).
- **Find Mode:** Targeted object searching. Features **Haptic Pulse Guidance**—the phone's vibration frequency increases as the target object moves closer to the center of the frame.
- **Advanced OCR (Vietnamese & English):** Offline text recognition with:
  - **Auto-Capture:** Automatically triggers when the camera detects stable, clear text.
  - **Quality Guard:** Voice warnings for blur, low light, or camera shake.
  - **Reading Flow Control:** Navigate through long texts using gestures (Swipe right for next sentence, left for previous, two-finger tap to pause/resume).

### 🎙️ Accessibility & Control
- **Global Voice Commands:** Control the app entirely by voice ("Explore", "Find", "OCR", "Settings", "Back").
- **Voice Onboarding:** A guided audio tutorial for first-time users to learn gestures and commands.
- **Haptic Feedback:** Spatial orientation through vibration patterns.
- **Offline First:** All AI processing (YOLO, OCR, TTS) happens on-device to ensure privacy and low latency.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Modern, accessible, and high-contrast)
- **Camera:** Android CameraX
- **Machine Learning:** 
  - **TensorFlow Lite:** YOLOv5s for real-time object detection.
  - **ML Kit:** For high-accuracy offline OCR.
- **Speech:** Android Text-to-Speech (TTS) & Speech Recognizer.
- **Architecture:** Hybrid MVVM/MVI for robust state management.

## 🏗️ Architecture

The system is organized into three main layers:
1.  **UI Layer:** Jetpack Compose screens and ViewModels.
2.  **Domain Layer:** Specialized Managers (SettingsManager, HistoryManager, VoiceCommandManager).
3.  **Data Layer:** Image Analyzers (YoloAnalyzer, OCRManager) and CameraX integration.

## 🚀 Getting Started

### Prerequisites
- Android device running Android 7.0 (API 24) or higher.
- Android Studio Ladybug or newer.

### Installation
1. Clone the repository:
   ```shell
   git clone https://github.com/ssharmapavitra/Shravan.git
   ```
2. Open in Android Studio.
3. Sync Gradle and build the project.

## 📖 Usage

1.  **Setup & Onboarding:** On first launch, VisionAid will guide you through an audio-onboarding process.
2.  **Navigation:** Use the bottom navigation bar or simply say a command like **"OCR"** or **"Find"**.
3.  **Text Reading:** Point the camera at any text. The app will notify you if the lighting is too low or if the image is blurry. Once stable, it will read the text automatically.
4.  **Object Search:** Say **"Find [Object Name]"**. Move your phone around; the vibration will guide you toward the target.

## 📊 Research & Performance

VisionAid was evaluated with 8 participants (4 totally blind, 4 partially sighted):
- **Task Completion Rate:** 93.75%
- **System Usability Scale (SUS):** 89.4 (Excellent)
- **NASA-TLX (Cognitive Load):** 25.4 (Low)

## 🤝 Contributing

We welcome contributions to enhance accessibility. Please feel free to open issues or submit pull requests.

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

---
*Developed as part of the Human-Computer Interaction (INT2041) course at VNU University of Engineering and Technology (UET).*
