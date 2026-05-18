package com.imu.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.imu.myapplication.BuildConfig
import com.imu.myapplication.model.Poem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale

enum class PlaybackState {
    STOPPED, PLAYING, PAUSED
}

class PoemViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentPoem = MutableStateFlow<Poem?>(null)
    val currentPoem: StateFlow<Poem?> = _currentPoem

    private val _selectedMeaning = MutableStateFlow<Pair<String, String>?>(null)
    val selectedMeaning: StateFlow<Pair<String, String>?> = _selectedMeaning

    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading

    private val _isContextExplained = MutableStateFlow(false)
    val isContextExplained: StateFlow<Boolean> = _isContextExplained

    private val _isApiKeyMissing = MutableStateFlow(false)
    val isApiKeyMissing: StateFlow<Boolean> = _isApiKeyMissing

    private var tts: TextToSpeech? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val sharedPreferences = application.getSharedPreferences("kavya_kanaja_prefs", Context.MODE_PRIVATE)

    private fun getApiKey(): String {
        val storedKey = sharedPreferences.getString("USER_GEMINI_API_KEY", "")
        if (!storedKey.isNullOrBlank()) {
            return storedKey
        }
        return BuildConfig.GEMINI_API_KEY
    }

    fun saveUserApiKey(key: String) {
        sharedPreferences.edit().putString("USER_GEMINI_API_KEY", key.trim()).apply()
        checkOnlineStatus()
    }

    data class PoetData(val name: String, val biography: String)

    // Task 1: Local List<PoetData> for all 8 Jnanpith Awardees with factual 3-sentence biographies.
    val jnanpithPoets = listOf(
        PoetData("Kuvempu", "Kuppali Venkatappa Puttappa, widely known as Kuvempu, was the first Kannada writer to receive the Jnanpith Award for his epic 'Sri Ramayana Darshanam'. He is celebrated as the 'Rashtrakavi' for his immense contribution to literature and his humanistic 'Vishwa Manava' (Universal Man) philosophy. His works continue to shape the cultural and literary identity of Karnataka through their lyrical beauty and spiritual depth."),
        PoetData("Da. Ra. Bendre", "Dattatreya Ramachandra Bendre, affectionately called 'Varakavi', is one of the most celebrated lyric poets of the Kannada language. He received the Jnanpith Award for his poetry collection 'Naaku Tanthi', which masterfully explores the mystical link between the human spirit and the cosmic. His verses are legendary for their perfect fusion of folk rhythms and profound philosophical insights."),
        PoetData("K. Shivaram Karanth", "Kota Shivaram Karanth was a prolific polymath, novelist, and environmentalist who profoundly influenced the modern cultural landscape of Karnataka. He was awarded the Jnanpith for his novel 'Mookajjiya Kanasugalu', which provides a deep philosophical inquiry into human history and evolving beliefs. His tireless work in documenting folk arts like Yakshagana has left an enduring impact on the preservation of traditional heritage."),
        PoetData("Masti Venkatesha Iyengar", "Masti Venkatesha Iyengar is honored as the father of the Kannada short story and is affectionately known as 'Maasti Kannadada Aasti'. He earned the Jnanpith Award for his historical novel 'Chikkavira Rajendra', which depicts the tragic end of the royalty in Coorg. His narratives are celebrated for their deep humanism, moral clarity, and vivid portrayal of rural life in Karnataka."),
        PoetData("V. K. Gokak", "Vinayaka Krishna Gokak was a distinguished scholar and writer who successfully bridged the gap between Kannada and English literary traditions. He received the Jnanpith Award for his monumental epic poem 'Bharatha Sindhu Rashmi', which reflects his lifelong engagement with Indian spirituality. He played a pivotal role as the leader of the Gokak movement, which secured the primary status of the Kannada language in education."),
        PoetData("U. R. Ananthamurthy", "Udupi Rajagopalacharya Ananthamurthy was a pioneering figure in the Navya movement, known for his critical and introspective approach to storytelling. He was awarded the Jnanpith for his total contribution to literature, with his novel 'Samskara' remaining a landmark work that questions traditional orthodoxies. His literature consistently explored the tension between traditional heritage and the values of a changing modern world."),
        PoetData("Girish Karnad", "Girish Raghunath Karnad was a world-renowned playwright and actor who brilliantly used history and mythology to address contemporary social issues. He received the Jnanpith Award for his immense contribution to Indian theatre, with plays like 'Tughlaq' and 'Hayavadana' becoming modern classics. His work brought Kannada drama to the international stage and fundamentally reshaped the landscape of modern Indian storytelling."),
        PoetData("Chandrashekhara Kambara", "Chandrashekhara Kambara is an acclaimed poet and playwright who masterfully blended folk mythology with modern literary forms. He won the Jnanpith Award for his diverse body of work, much of which is set in the fictional mythical village of 'Shivapura'. His narratives explore the clash between traditional folk culture and the complexities of the globalized world, preserving the spirit of North Karnataka.")
    )

    private suspend fun generateContentWithFallback(prompt: String): String? {
        val apiKeyToUse = getApiKey()
        if (apiKeyToUse.isEmpty() || apiKeyToUse == "null") {
            throw Exception("API_KEY_MISSING")
        }

        val modelNames = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite")
        var lastException: Exception? = null
        
        for (modelName in modelNames) {
            try {
                val model = GenerativeModel(modelName = modelName, apiKey = apiKeyToUse)
                val response = model.generateContent(prompt)
                if (response.text != null) {
                    return response.text
                }
            } catch (e: Exception) {
                Log.w("PoemViewModel", "Model $modelName failed: ${e.message}")
                lastException = e
            }
        }
        throw lastException ?: Exception("All AI models failed to generate content")
    }

    init {
        initTTS()
        fetchPoemOfTheDay()
        checkOnlineStatus()
    }

    private fun checkOnlineStatus() {
        val key = getApiKey()
        if (key.isEmpty() || key == "null") {
            _isApiKeyMissing.value = true
        } else {
            _isApiKeyMissing.value = false
        }
        _isOfflineMode.value = false
    }

    private fun initTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Task 3: Explicitly set the language to Kannada
                tts?.language = Locale("kn", "IN")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playbackState.value = PlaybackState.PLAYING
                        startProgressTracker() 
                    }
                    override fun onDone(utteranceId: String?) {
                        _playbackState.value = PlaybackState.STOPPED
                        _playbackProgress.value = 0f
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = PlaybackState.STOPPED
                    }
                })
            }
        }
    }

    private fun fetchPoemOfTheDay() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = getApplication<Application>().assets.open("poems.json").bufferedReader().use { it.readText() }
                val poems = json.decodeFromString<List<Poem>>(jsonString)
                
                if (poems.isNotEmpty()) {
                    val dayOfYear = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
                    val index = dayOfYear % poems.size
                    _currentPoem.value = poems[index]
                }
            } catch (e: Exception) {
                Log.e("PoemViewModel", "Failed to load local JSON fallback", e)
                _currentPoem.value = Poem(
                    id = 0,
                    title = "Error Loading Poems",
                    author = "System",
                    verse = "ದೋಷ (Error loading dataset)",
                    bhavartha = "Make sure poems.json is inside the assets folder."
                )
            }
        }
    }

    fun fetchAIInsight() {
        val poem = _currentPoem.value ?: return
        if (poem.aiInsight != null) return

        viewModelScope.launch(Dispatchers.IO) {
            _isInsightLoading.value = true
            try {
                // Task 2: Rewritten insight prompt with dynamic injection
                val prompt = "You are a Kannada Literature Expert. I am a student. Please provide a deep literary and philosophical insight for the poem titled '${poem.title}' by '${poem.author}'. The full verse is: '${poem.verse}'. Provide your insight in English, geared towards a college student, and keep it under 4 sentences."
                val responseText = generateContentWithFallback(prompt)
                
                val updatedPoem = poem.copy(aiInsight = responseText?.trim() ?: "No insight returned.")
                _currentPoem.value = updatedPoem
                _isOfflineMode.value = false
            } catch (e: Exception) {
                if (e.message == "API_KEY_MISSING") {
                    _isApiKeyMissing.value = true
                } else {
                    Log.e("PoemViewModel", "Failed to fetch insight from Gemini", e)
                    _isOfflineMode.value = true
                }
            } finally {
                _isInsightLoading.value = false
            }
        }
    }

    fun onComplexWordClicked(word: String) {
        val poem = _currentPoem.value ?: return
        _isContextExplained.value = false
        
        val localMeaning = poem.meanings[word]
        if (localMeaning != null) {
            _selectedMeaning.value = Pair(word, "\uD83D\uDCD6 ${localMeaning.definition}\n\uD83D\uDDE3\uFE0F ${localMeaning.pronunciation}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _selectedMeaning.value = Pair(word, "Thinking (AI)...")
                val prompt = "Provide a 1-sentence simple english meaning for the Kannada or Old Kannada literary word: '$word'."
                val responseText = generateContentWithFallback(prompt)
                _selectedMeaning.value = Pair(word, responseText ?: "Meaning not found.")
                _isOfflineMode.value = false
            } catch (e: Exception) {
                if (e.message == "API_KEY_MISSING") {
                    _isApiKeyMissing.value = true
                    _selectedMeaning.value = Pair(word, "API Key Missing. Add your Gemini API key.")
                } else {
                    _selectedMeaning.value = Pair(word, "Could not fetch meaning. Offline.")
                    _isOfflineMode.value = true
                }
            }
        }
    }

    fun onPoetClicked(poet: PoetData) {
        // Task 1: Opening the same ModalBottomSheet used for definitions
        _selectedMeaning.value = Pair(poet.name, poet.biography)
    }

    fun explainWordInContext(word: String) {
        val poem = _currentPoem.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _selectedMeaning.value = Pair(word, "Thinking (AI in context)...")
                val prompt = "Explain the meaning and significance of the Kannada word: '$word' specifically within the context of the poem titled '${poem.title}', where the full verse is: '${poem.verse}'. Provide a concise explanation in simple English. Keep it brief and limit your response to a maximum of 3 short paragraphs to fit neatly on a mobile screen."
                val responseText = generateContentWithFallback(prompt)
                _selectedMeaning.value = Pair(word, responseText ?: "Contextual meaning not found.")
                _isOfflineMode.value = false
                _isContextExplained.value = true
            } catch (e: Exception) {
                if (e.message == "API_KEY_MISSING") {
                    _isApiKeyMissing.value = true
                    _selectedMeaning.value = Pair(word, "API Key Missing. Add your Gemini API key.")
                } else {
                    _selectedMeaning.value = Pair(word, "Could not fetch contextual meaning. Offline.")
                    _isOfflineMode.value = true
                }
                _isContextExplained.value = false
            }
        }
    }

    fun togglePlayback() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            tts?.stop()
            _playbackState.value = PlaybackState.STOPPED
            _playbackProgress.value = 0f
        } else {
            // Task 3: Send raw Kannada verse text to the TTS engine
            val verse = _currentPoem.value?.verse ?: return
            _playbackProgress.value = 0f
            tts?.speak(verse, TextToSpeech.QUEUE_FLUSH, null, "POEM_ID")
        }
    }

    fun pausePlayback() {
        tts?.stop()
        _playbackState.value = PlaybackState.STOPPED
        _playbackProgress.value = 0f
    }

    private fun startProgressTracker() {
        viewModelScope.launch(Dispatchers.Main) {
            var progress = 0f
            while (isActive && _playbackState.value == PlaybackState.PLAYING) {
                progress += 0.05f
                if (progress >= 1f) progress = 1f
                _playbackProgress.value = progress
                delay(300) 
            }
        }
    }

    fun dismissMeaning() {
        _selectedMeaning.value = null
        _isContextExplained.value = false
    }

    private fun isKannada(c: Char): Boolean {
        return c.code in 0x0C80..0x0CFF
    }

    private fun splitByLanguage(text: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        if (text.isEmpty()) return result
        
        var currentLang = if (isKannada(text[0])) "kn" else "en"
        var currentChunk = StringBuilder()
        
        for (char in text) {
            val isKn = isKannada(char)
            val charLang = if (isKn) "kn" else if (char.isLetter()) "en" else currentLang
            
            if (charLang != currentLang && char.isLetter()) {
                result.add(Pair(currentLang, currentChunk.toString()))
                currentChunk = StringBuilder()
                currentLang = charLang
            }
            currentChunk.append(char)
        }
        if (currentChunk.isNotEmpty()) {
            result.add(Pair(currentLang, currentChunk.toString()))
        }
        return result
    }

    fun speakMixedLanguageText(text: String) {
        tts?.stop()
        val chunks = splitByLanguage(text)
        
        if (chunks.isNotEmpty()) {
            val firstChunk = chunks[0]
            val firstLocale = if (firstChunk.first == "kn") Locale("kn", "IN") else Locale("en", "US")
            tts?.language = firstLocale
            tts?.speak(firstChunk.second, TextToSpeech.QUEUE_FLUSH, null, "MIXED_ID_0")
            
            for (i in 1 until chunks.size) {
                val chunk = chunks[i]
                val locale = if (chunk.first == "kn") Locale("kn", "IN") else Locale("en", "US")
                val params = Bundle()
                tts?.language = locale
                tts?.speak(chunk.second, TextToSpeech.QUEUE_ADD, params, "MIXED_ID_$i")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
