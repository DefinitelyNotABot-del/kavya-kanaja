# Kavya-Kanaja (ಕಾವ್ಯ-ಕಣಜ)

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2)

**Kavya-Kanaja** is a visually rich, AI-powered Android application built using Modern Kotlin and Jetpack Compose. It serves as a digital repository and pedagogical tool for classic Kannada poetry, presenting a "Daily Poem" augmented with Generative AI insights, native Text-to-Speech, and contextual linguistic aids.

## 🌟 Features

* **Daily Poem Rotation:** Automatically cycles through a curated list of classic Kannada poems based on a daily mathematical rotation scheme.
* **AI-Powered "Bhavartha" (Meaning):** Integrates with Google's Generative AI (Gemini 2.5 Pro, Flash, and Flash-Lite) to dynamically generate deep insights and contextual meanings for each poem. Features a highly resilient **multi-model fallback** mechanism to handle API downtime.
* **Native Kannada Text-to-Speech (TTS):** Beautifully reads out poems automatically. The engine is strictly locked to the `kn-IN` locale for authentic pronunciation.
* **Unified Interactive Glossary & Bios:** Tap on poet names (e.g., Jnanpith awardees like Kuvempu and D.R. Bendre) or difficult words to instantly view biographies and definitions. Displayed via a highly optimized, unified `ModalBottomSheet`.
* **Offline Resilience:** Falls back to hardcoded local JSON data (`poems.json`) if network or AI generation fails, ensuring a seamless user experience.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel)
* **AI Integration:** Google Generative AI SDK (Gemini)
* **Audio:** Android Native `TextToSpeech` API
* **Build System:** Gradle (Kotlin DSL)

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Stable or Ladybug recommended)
* JDK 17+
* A valid Google Gemini API Key

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/kavya-kanaja.git
   ```
2. **Open the project in Android Studio:**
   Navigate to the target folder and let Gradle sync.
3. **Configure API Key:**
   Add your `GEMINI_API_KEY` to your `local.properties` file:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
4. **Build and Run:**
   Select an emulator or a physical device and click Run.

## 🏗️ High-Level Architecture

* **Data Layer:** Loads data from `res/raw/poems.json`.
* **ViewModel (`PoemViewModel`):** Manages AI state, handles the fallback logic string template injection, calculates the daily poem, and controls TTS.
* **UI Layer (`PoemScreen`):** Reacts to state changes using Compose. Implements Elvis operators (`aiInsight ?: localBhavartha`) for seamless data degradation.

For a deep dive into the architecture, state management, and the multi-model AI loop, see [DOCUMENTATION.md](DOCUMENTATION.md).

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
