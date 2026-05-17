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

### B. View Degradation Logic & Content Separation
Inside `PoemScreen.kt`, structural separation ensures that AI content complements rather than replaces foundational data:
* **Original UI Fallback:** The top section "ಭಾವಾರ್ಥ (Bhavartha)" always strictly renders `poem.bhavartha` from the local JSON.
* **AI Enrichment:** The "✨ AI Insight" block is dynamically injected below the core meaning *only* if `poem.aiInsight != null`. If the AI fetch is explicitly blocked (e.g., lack of internet or valid API Key), the system gracefully displays only the offline data and gracefully hides AI-specific UI options.

### C. Daily Poem Algorithm
The app statically rotates the featured poem every 24 hours. This is done inside the `PoemViewModel.kt` using a time-based modulus mathematical calculation:
* `(System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % listsSize`
* This ensures that every user across different devices sees the exact same poem on any given day without requiring a backend server.

### D. Dynamic Multi-Lingual Text-To-Speech (TTS)
The Android `TextToSpeech` engine features a highly customized multi-lingual chunking system (`speakMixedLanguageText`).
* **Adaptive Mapping:** Generative AI responses often mix English descriptions with Kannada terminology. The `PoemViewModel` utilizes a char-by-char language scanner (`0x0C80..0x0CFF`) to intelligently slice the response.
* **Locale Execution:** As TextToSpeech pushes to the audio queue (`QUEUE_ADD`), it instantaneously swaps locales—using `Locale("kn", "IN")` for native Kannada blocks, and `Locale("en", "US")` for English blocks. This achieves completely authentic, dynamic bilingual pronunciation.

### E. Unified BottomSheet Pattern & Contextual Fetching
Instead of defining multiple complex overlays, the app shares a single instance of `ModalBottomSheet`.
* The `PoemViewModel` exposes a state: `Pair<String, String>` (Title + Content).
* When a user taps a **Poet Bio**, the ViewModel simply overwrites this `Pair`.
* When a user taps a **Word Meaning**, they receive a local fallback dictionary definition. They are then presented with an **"Explain in context of the poem"** button.
* **Contextual Fetching:** Tapping this triggers a new AI query that specifically binds the single word to the current `poem.verse` scope, and live-updates the `ModalBottomSheet` content with highly granular English literary context once received.

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

## 5. Security and Persistent API Key Management
The `GEMINI_API_KEY` is completely obfuscated from version control.

1. **Local Properties Integration:** It is first checked via `BuildConfig` variables mapped natively from the developer's local, non-checked-in `local.properties` file.
2. **User Persistent SharedPreferences Fallback:** If the `BuildConfig` yields null (e.g., when a user freshly clones the OSS repository), the application seamlessly displays a specialized lock-screen UI. 
3. **Runtime Registration:** When a user inputs their own API key, it is encrypted and saved persistently locally to Android's `SharedPreferences` (`"kavya_kanaja_prefs"`). The `PoemViewModel.getApiKey()` immediately binds it to the Generative Model, enabling endless localized app usage without a single line of backend database code.

*End of Documentation.*