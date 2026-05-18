package com.imu.myapplication.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.lifecycle.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.googlefonts.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.imu.myapplication.R
import com.imu.myapplication.viewmodel.*

// Modern Heritage Theming Colors
val Parchment = Color(0xFFFDF5E6)
val DeepCharcoal = Color(0xFF2C2C2C)
val EarthyBrown = Color(0xFF8B5A2B)
val GoldBorder = Color(0xFFD4AF37)

// Google Fonts Implementation
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val CrimsonProFont = GoogleFont("Crimson Pro")
val CrimsonProFamily = FontFamily(
    Font(googleFont = CrimsonProFont, fontProvider = provider)
)

// Unicode-compliant Kannada Font
val NotoSansKannadaFont = GoogleFont("Noto Sans Kannada")
val NotoSansKannadaFamily = FontFamily(
    Font(googleFont = NotoSansKannadaFont, fontProvider = provider)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemScreen(viewModel: PoemViewModel) {
    val currentPoem by viewModel.currentPoem.collectAsState()
    val selectedMeaning by viewModel.selectedMeaning.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val isContextExplained by viewModel.isContextExplained.collectAsState()
    val isApiKeyMissing by viewModel.isApiKeyMissing.collectAsState()
    
    var isVisible by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pausePlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentPoem) {
        if (currentPoem != null) {
            isVisible = true
        }
    }

    Scaffold(
        containerColor = Parchment,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kavya-Kanaja", 
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp,
                            letterSpacing = 2.sp,
                            color = DeepCharcoal,
                            fontFamily = CrimsonProFamily
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!isOfflineMode) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Parchment,
                    titleContentColor = DeepCharcoal
                )
            )
        },
        floatingActionButton = {
            if (currentPoem != null) { 
                FloatingActionButton(
                    onClick = { viewModel.togglePlayback() },
                    containerColor = EarthyBrown,
                    contentColor = Parchment
                ) {
                    val icon = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    Icon(icon, contentDescription = "Listen")
                }
            }
        }
    ) { padding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1200))
        ) {
            currentPoem?.let { poem ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isOfflineMode && !isApiKeyMissing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EarthyBrown.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Offline", tint = EarthyBrown)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reading by Candlelight (Offline)", color = EarthyBrown, fontFamily = CrimsonProFamily)
                        }
                    }
                    
                    if (isApiKeyMissing) {
                        var tempKey by remember { mutableStateOf("") }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFE0B2), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = "API Key Missing", tint = EarthyBrown)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("No API Key Available", color = EarthyBrown, fontWeight = FontWeight.Bold, fontFamily = CrimsonProFamily)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("To unlock AI bounds, please provide a Gemini API Key.", color = DeepCharcoal, fontFamily = CrimsonProFamily, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                placeholder = { Text("Paste your API Key here...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.saveUserApiKey(tempKey) },
                                colors = ButtonDefaults.buttonColors(containerColor = EarthyBrown),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Key", color = Parchment)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = poem.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = DeepCharcoal,
                        textAlign = TextAlign.Center,
                        fontFamily = NotoSansKannadaFamily, // Use Unicode Kannada font
                        lineHeight = 40.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "— ${poem.author} —",
                        fontSize = 18.sp,
                        color = EarthyBrown,
                        fontFamily = NotoSansKannadaFamily, // Use Unicode Kannada font
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = playbackState == PlaybackState.PLAYING) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = EarthyBrown,
                            trackColor = EarthyBrown.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    InteractiveVerse(
                        verse = poem.verse,
                        onWordClick = { word ->
                            viewModel.onComplexWordClicked(word)
                        }
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "ಭಾವಾರ್ಥ (Bhavartha)",
                                fontSize = 16.sp,
                                color = EarthyBrown,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = NotoSansKannadaFamily
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = poem.bhavartha,
                                fontSize = 18.sp,
                                color = DeepCharcoal.copy(alpha = 0.8f),
                                lineHeight = 28.sp,
                                fontFamily = CrimsonProFamily
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (poem.aiInsight != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "✨ AI Insight",
                                        fontSize = 16.sp,
                                        color = EarthyBrown,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CrimsonProFamily
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.speakMixedLanguageText(poem.aiInsight) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play AI Insight", tint = EarthyBrown)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = poem.aiInsight,
                                    fontSize = 16.sp,
                                    color = DeepCharcoal.copy(alpha = 0.8f),
                                    lineHeight = 24.sp,
                                    fontFamily = CrimsonProFamily,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.fetchAIInsight() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EarthyBrown),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Get AI Insight", fontFamily = CrimsonProFamily)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Poet's Corner",
                        fontSize = 20.sp,
                        color = EarthyBrown,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CrimsonProFamily,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.jnanpithPoets) { poet ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable {
                                        viewModel.onPoetClicked(poet)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawCircle(color = Parchment)
                                        drawCircle(
                                            color = GoldBorder,
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                    }
                                    Text(
                                        text = poet.name.first().toString(),
                                        color = EarthyBrown,
                                        fontSize = 32.sp,
                                        fontFamily = CrimsonProFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = poet.name,
                                    fontFamily = CrimsonProFamily,
                                    fontSize = 14.sp,
                                    color = DeepCharcoal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
                }

                if (selectedMeaning != null) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.dismissMeaning() },
                        containerColor = Parchment
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        ) {
                            Text(
                                text = selectedMeaning!!.first,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = DeepCharcoal,
                                fontFamily = CrimsonProFamily
                            )
                            HorizontalDivider(color = EarthyBrown.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 16.dp))
                            Text(
                                text = selectedMeaning!!.second,
                                fontSize = 20.sp,
                                color = DeepCharcoal,
                                lineHeight = 30.sp,
                                fontFamily = CrimsonProFamily
                            )
                            
                            if (isContextExplained) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { viewModel.speakMixedLanguageText(selectedMeaning!!.second) },
                                        modifier = Modifier
                                            .background(EarthyBrown.copy(alpha = 0.1f), CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Listen to explanation", tint = EarthyBrown)
                                    }
                                }
                            }
                            
                            val isPoet = viewModel.jnanpithPoets.any { it.name == selectedMeaning!!.first }
                            if (!isPoet) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.explainWordInContext(selectedMeaning!!.first) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EarthyBrown),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Explain in context of the poem", fontFamily = CrimsonProFamily, color = Parchment)
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            } ?: run {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EarthyBrown)
                }
            }
        }
    }
}

@Composable
fun InteractiveVerse(
    verse: String,
    onWordClick: (String) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        val tokens = verse.split(Regex("(?<=[\\s\\n])|(?=[\\s\\n])"))
        
        for (token in tokens) {
            val cleanToken = token.trimEnd(',', '.', ';', '!', '?').trim()
            
            if (cleanToken.isNotBlank() && cleanToken.length > 2) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = cleanToken,
                        linkInteractionListener = { 
                            onWordClick(cleanToken)
                        }
                    )
                ) {
                    withStyle(
                        style = SpanStyle(
                            color = EarthyBrown,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NotoSansKannadaFamily,
                            fontSize = 24.sp
                        )
                    ) {
                        append(token)
                    }
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        color = DeepCharcoal,
                        fontFamily = NotoSansKannadaFamily,
                        fontSize = 24.sp
                    )
                ) {
                    append(token)
                }
            }
        }
    }

    Text(
        text = annotatedString,
        lineHeight = 42.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
