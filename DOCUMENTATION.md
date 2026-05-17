# Kavya-Kanaja: Comprehensive Technical Documentation

This document serves as the ultimate source of truth for the architectural decisions, structural patterns, and implementation details of the **Kavya-Kanaja** Android application.

---

## 1. System Architecture (MVVM)

The project leverages a modern Model-View-ViewModel (MVVM) architecture optimized for Jetpack Compose and offline-first/AI-first hybrid resiliency.

### Core Components:
1. **Model (Data Source):** `res/raw/poems.json` provides a localized foundational dataset containing the poem titles, raw text, fallback "Bhavartha" (meanings), and offline definitions.
2. **ViewModel (`PoemViewModel.kt`):** The brain of the application. Responsible for parsing local data, scheduling the daily poetry logic, triggering AI network calls, managing TTS engines, and maintaining UI state.
3. **View (`PoemScreen.kt`):** A purely declarative Jetpack Compose UI that strictly observes the `PoemViewModel` state.

---

## 2. Key Technical Implementations

### A. Contextual GenAI Insight & Multi-Model Fallback
The app does not rely on a single point of failure for GenAI generation. 

**Logic Flow:**
* The `fetchAIInsight` function formulates a secure string template injection using `$'{poem.title}'` to prevent JSON-breaking characters while querying the LLM.
* A robust `try/catch` fallback loop named `generateContentWithFallback()` iterates over a priority list of Gemini models:
  1. `gemini-2.5-pro` (Primary)
  2. `gemini-2.5-flash` (Secondary)
  3. `gemini-2.5-flash-lite` (Ultimate fallback)
* If a 404 (wrong model version) or 503 (server load) is returned, it instantly falls back to the next tier, ensuring zero UI downtime.

### B. View Degradation Logic
Inside `PoemScreen.kt`, the content composables utilize the Elvis operator to seamlessly blend AI and offline data:
```kotlin
Text(text = poem.aiInsight ?: poem.bhavartha)
```
If the AI fetch is completely blocked (e.g., lack of internet), the system falls back to the local `poem.bhavartha` defined in the JSON.

### C. Daily Poem Algorithm
The app statically rotates the featured poem every 24 hours. This is done inside the `PoemViewModel.kt` using a time-based modulus mathematical calculation:
* `(System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % listsSize`
* This ensures that every user across different devices sees the exact same poem on any given day without requiring a backend server.

### D. Native Text-To-Speech (TTS) Localization
The Android `TextToSpeech` engine is heavily utilized to present audible versions of the poetry. 
* Initialization is handled in `initTTS()`.
* **Crucial Rule:** The engine is securely locked to `Locale("kn", "IN")` (Kannada, India). The system will strictly refuse to read the content under default system locales to avoid mispronunciation of Kannada poetry.

### E. Unified BottomSheet Pattern
Instead of defining multiple complex overlays, the app shares a single instance of `ModalBottomSheet`.
* The `PoemViewModel` exposes a state: `Pair<String, String>` (Title + Content).
* When a user clicks a **Word Meaning** or a **Poet Bio** (such as Kuvempu or D.R. Bendre), the ViewModel simply overwrites this `Pair`.
* The `ModalBottomSheet` observes this change and instantly recomposes the relevant data without mounting/unmounting new components, highly minimizing UI recomposition overhead.

---

## 3. Data Dictionary

### Jnanpith Biographies Validation
The hardcoded fallback logic for prominent poets contains structurally validated profiles for accuracy:
1. **Kuvempu:** Birth year `1904`. Major works include *Sri Ramayana Darshanam*. First Kannada writer to receive the Jnanpith Award (1967).
2. **Da Ra Bendre:** Birth year `1896`. Major works include *Naaku Tanthi*. Jnanpith Award winner (1973).

## 4. Diagrammatic Flow

```mermaid
graph TD;
    %% Data Sources
    LocalJSON[res/raw/poems.json] --> |Parsed on init| ViewModel(PoemViewModel);
    GeminiAPI{Google Generative AI} --> |Fetches 'Bhavartha' (API Fallback Loop)| ViewModel;

    %% ViewModel Logic
    subgraph State Management
        ViewModel -- Calculations --> DailyPoem(Daily Poem State);
        ViewModel -- TTS Engine --> AudioOut[Android TextToSpeech Locale: kn_IN];
        ViewModel -- Shared State --> BottomSheetState(Pair State Title/Content);
    end

    %% UI Observation
    DailyPoem --> UI[PoemScreen.kt];
    BottomSheetState --> |Observed by| ModalSheet(Unified ModalBottomSheet);

    %% Interactions
    UI --> |Click 'Poet Name'| ViewModel_Bio(Update Sheet State);
    UI --> |Click 'Word Meaning'| ViewModel_Dict(Update Sheet State);
    UI --> |Click 'Play Audio'| AudioOut;

    ViewModel_Bio --> ModalSheet;
    ViewModel_Dict --> ModalSheet;
```

---

## 5. Security and API Key Management
The `GEMINI_API_KEY` is completely obfuscated from the version control system. It is injected into the application at compile time via `BuildConfig` variables mapped from the local, non-checked-in `local.properties` file.

*End of Documentation.*