package io.github.sanitised.st

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.speech.tts.TextToSpeech
import android.content.Intent
import java.util.Locale

// Custom Sleek Dark Modern Palette
private val ColorOledBackground = Color(0xFF090A0E)
private val ColorSurfaceCard = Color(0xFF13141C)
private val ColorCardBorder = Color(0xFF222433)
private val ColorAssistantBubble = Color(0xFF161722)
private val ColorAssistantBorder = Color(0xFF27293C)
private val ColorUserGradientStart = Color(0xFF4F46E5)
private val ColorUserGradientEnd = Color(0xFF6366F1)
private val ColorAccentCyan = Color(0xFF06B6D4)
private val ColorAccentViolet = Color(0xFF8B5CF6)
private val ColorTextPrimary = Color(0xFFF8FAFC)
private val ColorTextSecondary = Color(0xFF94A3B8)
private val ColorRoleplayAction = Color(0xFFC4B5FD)

data class ThemePalette(
    val id: String,
    val name: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val accent: Color
)

val themePalettes = listOf(
    ThemePalette("violet", "Cyber Violet", Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF8B5CF6)),
    ThemePalette("cyan", "Electric Cyan", Color(0xFF0284C7), Color(0xFF06B6D4), Color(0xFF06B6D4)),
    ThemePalette("emerald", "Emerald Neon", Color(0xFF059669), Color(0xFF10B981), Color(0xFF10B981)),
    ThemePalette("amber", "Amber Gold", Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFF59E0B)),
    ThemePalette("crimson", "Crimson Red", Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFEF4444))
)

fun cleanTextForTts(text: String): String {
    return text.replace(Regex("\\*+"), "").trim()
}

fun exportChat(context: Context, characterName: String, messages: List<StChatMessage>) {
    val sb = StringBuilder()
    sb.append("# Roleplay with $characterName\n\n")
    for (m in messages) {
        if (m.isGenerating || m.text.isBlank()) continue
        sb.append("**${m.name}**:\n${m.text}\n\n")
    }
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export / Share Chat"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Simple in-memory bitmap cache for avatars
private val avatarBitmapCache = mutableMapOf<String, Bitmap>()

@Composable
fun StAvatar(
    avatarFileName: String,
    characterName: String,
    baseUrl: String,
    localCharacterDirs: List<File> = emptyList(),
    sizeDp: Int = 40,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(avatarFileName) { mutableStateOf(avatarBitmapCache[avatarFileName]) }

    LaunchedEffect(avatarFileName, baseUrl) {
        if (avatarFileName.isNotBlank() && bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                // 1. Try local files first (instant 0ms load)
                for (dir in localCharacterDirs) {
                    val localFile = File(dir, avatarFileName)
                    if (localFile.exists()) {
                        val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                        if (bmp != null) return@withContext bmp
                    }
                }
                // 2. Try HTTP URL with cookies
                try {
                    val urlStr = "$baseUrl/thumbnail?type=avatar&avatar=$avatarFileName"
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    NativeChatApi.sessionCookie?.let { conn.setRequestProperty("Cookie", it) }
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    val stream = conn.inputStream
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream.close()
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
            if (loaded != null) {
                avatarBitmapCache[avatarFileName] = loaded
                bitmap = loaded
            }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .border(1.dp, ColorCardBorder, CircleShape)
            .background(ColorSurfaceCard),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = characterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder with character initial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(ColorAccentViolet.copy(alpha = 0.8f), ColorAccentCyan.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = characterName.take(1).uppercase().ifBlank { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (sizeDp / 2.3).sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StNativeChatScreen(
    baseUrl: String,
    viewModel: NativeChatViewModel,
    localCharacterDirs: List<File> = emptyList(),
    onBackToDashboard: () -> Unit,
    onOpenFullSillyTavern: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(baseUrl) {
        viewModel.setConfig(baseUrl, localCharacterDirs)
        viewModel.loadCharacters()
    }

    val characters by viewModel.characters.collectAsState()
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsState()
    val isLoadingCharacters by viewModel.isLoadingCharacters.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val aiSettings by viewModel.aiSettings.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userPersona by viewModel.userPersona.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val themeAccent by viewModel.themeAccent.collectAsState()
    val showQuickActions by viewModel.showQuickActions.collectAsState()
    val activeChatFileName by viewModel.activeChatFileName.collectAsState()

    val activeTheme = remember(themeAccent) {
        themePalettes.firstOrNull { it.id == themeAccent } ?: themePalettes.first()
    }

    var inputPrompt by remember { mutableStateOf("") }
    var showCharacterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSessionsSheet by remember { mutableStateOf(false) }
    var showPersonaDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var tempPersonaName by remember(userName) { mutableStateOf(userName) }
    var tempPersonaText by remember(userPersona) { mutableStateOf(userPersona) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    var selectedMessageForMenu by remember { mutableStateOf<Pair<Int, StChatMessage>?>(null) }
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var editingMessageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // File picker launcher to import character cards (.png / .json) directly from device
    val characterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val targetDir = localCharacterDirs.firstOrNull { it.exists() }
                        ?: File(context.filesDir, "data/default-user/characters").apply { mkdirs() }
                    val fileName = "card_${System.currentTimeMillis()}.png"
                    val targetFile = File(targetDir, fileName)
                    FileOutputStream(targetFile).use { out ->
                        inputStream.copyTo(out)
                    }
                    inputStream.close()
                    Toast.makeText(context, "Character card imported!", Toast.LENGTH_SHORT).show()
                    viewModel.loadCharacters(initialAvatar = fileName)
                    showCharacterSheet = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import card: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(ColorOledBackground)
    ) {
        // --- Top Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ColorSurfaceCard,
            tonalElevation = 4.dp
        ) {
            if (isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close Search",
                            tint = ColorTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF191B26))
                            .border(1.dp, ColorCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search conversation...",
                                color = ColorTextSecondary.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            textStyle = TextStyle(color = ColorTextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(activeTheme.accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ColorTextSecondary
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Dashboard",
                            tint = ColorTextPrimary
                        )
                    }

                    val char = selectedCharacter
                    if (char != null) {
                        StAvatar(
                            avatarFileName = char.avatar,
                            characterName = char.name,
                            baseUrl = baseUrl,
                            localCharacterDirs = localCharacterDirs,
                            sizeDp = 38,
                            modifier = Modifier.clickable { showCharacterSheet = true }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showCharacterSheet = true }
                        ) {
                            Text(
                                text = char.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isGenerating) activeTheme.accent else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isGenerating) "Generating..." else "${getProviderDisplayName(aiSettings.provider)} • ${aiSettings.model.substringAfterLast('/')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showCharacterSheet = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Character",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextSecondary
                            )
                        }
                    }

                    // Search Button
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Messages",
                            tint = ColorTextSecondary
                        )
                    }

                    // Chat History / Sessions Switcher & New Chat Button
                    IconButton(onClick = {
                        viewModel.loadChatSessions()
                        showSessionsSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat Sessions & History",
                            tint = activeTheme.accent
                        )
                    }

                    // User Persona Dialog Button
                    IconButton(onClick = {
                        tempPersonaName = userName
                        tempPersonaText = userPersona
                        showPersonaDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Persona",
                            tint = ColorAccentCyan
                        )
                    }

                    // AI Model & Key Settings Button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "AI Model & Key Settings",
                            tint = ColorAccentViolet
                        )
                    }

                    // Export / Share Chat
                    IconButton(onClick = {
                        exportChat(context, selectedCharacter?.name ?: "Chat", messages)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export / Share Chat",
                            tint = ColorTextSecondary
                        )
                    }

                    // 1-Tap Switch to Classic SillyTavern Web
                    IconButton(onClick = onOpenFullSillyTavern) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open Full SillyTavern",
                            tint = ColorTextSecondary
                        )
                    }
                }
            }
        }

        // --- Error Banner ---
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val errText = errorMessage ?: ""
            val suggestedModel = remember(errText) {
                Regex("use models/([a-zA-Z0-9._-]+)", RegexOption.IGNORE_CASE).find(errText)?.groupValues?.get(1)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF450A0A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errText,
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFCA5A5)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.retryLastMessage() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (suggestedModel != null) {
                            Button(
                                onClick = {
                                    viewModel.saveAiSettings(aiSettings.copy(model = suggestedModel)) {
                                        viewModel.retryLastMessage()
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColorAccentViolet),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Switch to $suggestedModel & Retry",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Message List (120fps LazyColumn) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoadingMessages || isLoadingCharacters) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorAccentViolet)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connecting to SillyTavern engine...",
                            color = ColorTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else if (selectedCharacter == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = ColorAccentViolet,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Character Selected",
                            color = ColorTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (characters.isEmpty()) "Tap below to import a character card (.png) or reload."
                            else "Select a character from your library to start chatting.",
                            color = ColorTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row {
                            Button(onClick = { showCharacterSheet = true }) {
                                Text("Choose Character")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(onClick = { characterPickerLauncher.launch("image/*") }) {
                                Text("Import Card")
                            }
                        }
                    }
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StAvatar(
                            avatarFileName = selectedCharacter?.avatar.orEmpty(),
                            characterName = selectedCharacter?.name.orEmpty(),
                            baseUrl = baseUrl,
                            localCharacterDirs = localCharacterDirs,
                            sizeDp = 72
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Say hello to ${selectedCharacter?.name}!",
                            color = ColorTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val filteredMessages = remember(messages, searchQuery) {
                    if (searchQuery.isBlank()) {
                        messages.mapIndexed { i, m -> Pair(i, m) }
                    } else {
                        messages.mapIndexed { i, m -> Pair(i, m) }.filter { (_, m) ->
                            m.text.contains(searchQuery, ignoreCase = true) || m.name.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredMessages, key = { _, (origIdx, msg) -> "${msg.id}-$origIdx" }) { _, (origIdx, msg) ->
                        val isLast = (origIdx == messages.lastIndex)
                        MessageBubble(
                            message = msg,
                            character = selectedCharacter,
                            baseUrl = baseUrl,
                            localCharacterDirs = localCharacterDirs,
                            isLastMessage = isLast,
                            statusText = statusMessage,
                            fontSizeSp = fontSizeSp,
                            activeTheme = activeTheme,
                            isSpeaking = (speakingMessageId == msg.id),
                            onToggleTts = {
                                if (speakingMessageId == msg.id) {
                                    tts?.stop()
                                    speakingMessageId = null
                                } else {
                                    tts?.stop()
                                    val toSpeak = cleanTextForTts(msg.text)
                                    tts?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                                    speakingMessageId = msg.id
                                }
                            },
                            onSwipeLeft = {
                                if (msg.swipes.size > 1 && msg.swipeId > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.switchSwipe(origIdx, msg.swipeId - 1)
                                }
                            },
                            onSwipeRight = {
                                if (msg.swipes.size > 1 && msg.swipeId < msg.swipes.lastIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.switchSwipe(origIdx, msg.swipeId + 1)
                                }
                            },
                            onContinue = { viewModel.continueLastMessage() },
                            onRegenerate = { viewModel.regenerateLastMessage() },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SillyTavern", msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onClick = { selectedMessageForMenu = Pair(origIdx, msg) }
                        )
                    }
                }
            }
        }

        // --- Bottom Input Bar ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            color = ColorSurfaceCard,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick RP Action Chips
                val quickActionsList = listOf(
                    "*smiles*", "*sighs*", "*nods*", "*laughs*", "*chuckles*",
                    "*whispers*", "*looks away*", "*pauses*", "*smirks*", "*frowns*",
                    "*steps closer*", "*leans in*", "*blushes*", "*gasps*"
                )
                AnimatedVisibility(
                    visible = showQuickActions && !isGenerating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (action in quickActionsList) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF191B26),
                                border = BorderStroke(1.dp, ColorCardBorder),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    inputPrompt = if (inputPrompt.isBlank()) "$action " else "$inputPrompt $action "
                                }
                            ) {
                                Text(
                                    text = action,
                                    color = ColorRoleplayAction,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Action Bar: Regenerate & Continue (when last message is from assistant and not generating)
                AnimatedVisibility(
                    visible = messages.isNotEmpty() && !messages.last().isUser && !isGenerating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Regenerate Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF191B26),
                            border = BorderStroke(1.dp, ColorCardBorder),
                            modifier = Modifier.clickable { viewModel.regenerateLastMessage() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = ColorAccentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Regenerate",
                                    color = ColorAccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Continue Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF191B26),
                            border = BorderStroke(1.dp, ColorCardBorder),
                            modifier = Modifier.clickable { viewModel.continueLastMessage() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Continue",
                                    tint = ColorAccentViolet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Continue",
                                    color = ColorAccentViolet,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Status Message (Auto-retry feedback / Model migration status)
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = ColorAccentCyan,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage.orEmpty(),
                            color = ColorAccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Action Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleQuickActions() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Quick RP Actions",
                            tint = if (showQuickActions) activeTheme.accent else ColorTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF191B26))
                            .border(1.dp, ColorCardBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (inputPrompt.isEmpty()) {
                            Text(
                                text = "Message ${selectedCharacter?.name ?: "character"}...",
                                color = ColorTextSecondary.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        BasicTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = ColorTextPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(activeTheme.accent),
                            maxLines = 5
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputPrompt.isNotBlank() && !isGenerating) {
                                    Brush.horizontalGradient(
                                        listOf(activeTheme.gradientStart, activeTheme.gradientEnd)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF262838), Color(0xFF262838))
                                    )
                                }
                            )
                            .clickable(enabled = inputPrompt.isNotBlank() && !isGenerating) {
                                val text = inputPrompt
                                inputPrompt = ""
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendMessage(text)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = ColorAccentCyan,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputPrompt.isNotBlank()) Color.White else ColorTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Character Selector Bottom Sheet ---
    if (showCharacterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCharacterSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = ColorSurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Characters (${characters.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )

                    Row {
                        IconButton(onClick = { viewModel.loadCharacters() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = ColorTextSecondary
                            )
                        }
                        IconButton(onClick = { characterPickerLauncher.launch("image/*") }) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import Character",
                                tint = ColorAccentViolet
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Import button
                OutlinedButton(
                    onClick = { characterPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ColorAccentCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Import Character Card (.png)", color = ColorAccentCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (characters.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF191B26),
                        border = BorderStroke(1.dp, ColorCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No characters loaded yet",
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Make sure SillyTavern server is started, or tap '+ Import Character Card' above to pick a PNG card from your phone.",
                                color = ColorTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadCharacters() }) {
                                Text("Retry Loading")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(characters) { item ->
                            val isSelected = item.avatar == selectedCharacter?.avatar

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ColorAccentViolet.copy(alpha = 0.2f) else Color(0xFF191B26))
                                    .border(
                                        1.dp,
                                        if (isSelected) ColorAccentViolet else ColorCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectCharacter(item)
                                        showCharacterSheet = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StAvatar(
                                    avatarFileName = item.avatar,
                                    characterName = item.name,
                                    baseUrl = baseUrl,
                                    localCharacterDirs = localCharacterDirs,
                                    sizeDp = 48
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorTextPrimary,
                                        fontSize = 16.sp
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            color = ColorTextSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- AI Model & Key Settings Bottom Sheet ---
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = ColorSurfaceCard
        ) {
            AiSettingsSheetContent(
                aiSettings = aiSettings,
                fontSizeSp = fontSizeSp,
                themeAccent = themeAccent,
                onSetFontSize = { viewModel.setFontSize(it) },
                onSetThemeAccent = { viewModel.setThemeAccent(it) },
                onOpenPersona = {
                    showSettingsSheet = false
                    tempPersonaName = userName
                    tempPersonaText = userPersona
                    showPersonaDialog = true
                },
                onSave = { updatedSettings ->
                    viewModel.saveAiSettings(updatedSettings) { success ->
                        if (success) {
                            Toast.makeText(context, "AI settings applied: ${updatedSettings.model.substringAfterLast('/')}", Toast.LENGTH_SHORT).show()
                            showSettingsSheet = false
                        } else {
                            Toast.makeText(context, "Failed to save settings to SillyTavern", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFetchModels = { source, callback ->
                    viewModel.fetchAvailableModels(source, callback)
                },
                onSaveApiKeyDirect = { provider, key, callback ->
                    viewModel.saveApiKeyDirect(provider, key, callback)
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    // --- Chat Sessions & History Bottom Sheet ---
    if (showSessionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSessionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = ColorSurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = activeTheme.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Chat History (${chatSessions.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                    }

                    IconButton(onClick = { viewModel.loadChatSessions() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Sessions",
                            tint = ColorTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Start New Chat Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createNewChat(onComplete = {
                            showSessionsSheet = false
                            Toast.makeText(context, "New chat started!", Toast.LENGTH_SHORT).show()
                        })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start New Chat",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (chatSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved chat history found for ${selectedCharacter?.name ?: "this character"}.",
                            color = ColorTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatSessions) { session ->
                            val isCurrent = session.fileName == activeChatFileName ||
                                    (activeChatFileName.isNullOrBlank() && session == chatSessions.firstOrNull())
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectChatSession(session.fileName)
                                        showSessionsSheet = false
                                        Toast.makeText(context, "Loaded chat: ${session.fileName}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) activeTheme.accent.copy(alpha = 0.2f) else Color(0xFF191B26),
                                border = BorderStroke(1.dp, if (isCurrent) activeTheme.accent else ColorCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = if (isCurrent) activeTheme.accent else ColorTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.fileName.removeSuffix(".jsonl"),
                                            color = ColorTextPrimary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                        if (session.previewText.isNotBlank()) {
                                            Text(
                                                text = session.previewText,
                                                color = ColorTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = activeTheme.accent
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- User Persona / Profile Dialog ---
    if (showPersonaDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPersonaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ColorAccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "User Persona & Profile", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Customize your name and persona background. The AI will address you and adapt roleplay based on this.",
                        color = ColorTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempPersonaName,
                        onValueChange = { tempPersonaName = it },
                        label = { Text("Your Display Name", fontSize = 12.sp, color = ColorTextSecondary) },
                        placeholder = { Text("User", fontSize = 12.sp, color = ColorTextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        textStyle = TextStyle(color = ColorTextPrimary, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempPersonaText,
                        onValueChange = { tempPersonaText = it },
                        label = { Text("Persona Description / Background", fontSize = 12.sp, color = ColorTextSecondary) },
                        placeholder = {
                            Text(
                                "e.g. A skilled adventurer, thoughtful traveler, student...",
                                fontSize = 12.sp,
                                color = ColorTextSecondary.copy(alpha = 0.5f)
                            )
                        },
                        maxLines = 6,
                        textStyle = TextStyle(color = ColorTextPrimary, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveUserPersona(tempPersonaName, tempPersonaText)
                        showPersonaDialog = false
                        Toast.makeText(context, "Persona saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save", color = activeTheme.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonaDialog = false }) {
                    Text("Cancel", color = ColorTextSecondary)
                }
            },
            containerColor = ColorSurfaceCard
        )
    }

    // --- Message Context Actions Sheet ---
    selectedMessageForMenu?.let { (idx, msg) ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForMenu = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = ColorSurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Message Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SillyTavern", msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = ColorTextPrimary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Copy Text", color = ColorTextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val toSpeak = cleanTextForTts(msg.text)
                            tts?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                            speakingMessageId = msg.id
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = ColorAccentCyan)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Read Aloud (Text to Speech)", color = ColorAccentCyan)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMessageForMenu = null
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, msg.text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Message"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ColorTextPrimary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Share Message", color = ColorTextPrimary)
                }

                if (!msg.isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                viewModel.regenerateLastMessage()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = ColorAccentCyan)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "Regenerate Response", color = ColorAccentCyan)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                viewModel.continueLastMessage()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Continue", tint = ColorAccentViolet)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = "Continue Response", color = ColorAccentViolet)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingMessageIndex = idx
                            editingMessageText = msg.text
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = ColorTextPrimary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Edit Message", color = ColorTextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.deleteMessage(idx)
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "Delete Message", color = Color(0xFFEF4444))
                }
            }
        }
    }

    // --- Edit Message Dialog ---
    editingMessageIndex?.let { idx ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingMessageIndex = null },
            title = { Text(text = "Edit Message", color = ColorTextPrimary) },
            text = {
                OutlinedTextField(
                    value = editingMessageText,
                    onValueChange = { editingMessageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.editMessage(idx, editingMessageText)
                        editingMessageIndex = null
                    }
                ) {
                    Text("Save", color = ColorAccentViolet)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessageIndex = null }) {
                    Text("Cancel", color = ColorTextSecondary)
                }
            },
            containerColor = ColorSurfaceCard
        )
    }
}

data class GeminiModelPreset(
    val id: String,
    val displayName: String,
    val category: String, // "2.0", "2.5", "1.5", "Gemma"
    val badge: String? = null
)

private val allGeminiPresets = listOf(
    // Gemini 3.8 Series (CURRENT FLAGSHIP 2026)
    GeminiModelPreset("gemini-3.8-flash", "Gemini 3.8 Flash", "3.8", "⚡ Recommended Flagship"),
    GeminiModelPreset("gemini-3.8-flash-lite", "Gemini 3.8 Flash-Lite", "3.8", "⚡ Lightweight"),
    GeminiModelPreset("gemini-3.8-pro", "Gemini 3.8 Pro", "3.8", "💎 Frontier Reasoning"),
    GeminiModelPreset("gemini-3.8-live", "Gemini 3.8 Live", "3.8", "🎙️ Live Voice"),
    GeminiModelPreset("gemini-3.8-live-extended-thinking", "Gemini 3.8 Live Thinking", "3.8", "🧠 Deep Thinking"),
    GeminiModelPreset("gemini-3.8-flash-tts", "Gemini 3.8 Flash TTS", "3.8", "TTS Audio"),

    // Gemini 3.1 & 3.x Flash-Lite Series (High Speed / Low Latency)
    GeminiModelPreset("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "3.x", "⚡ Super Fast Lite"),
    GeminiModelPreset("gemini-3.1-flash", "Gemini 3.1 Flash", "3.x", "Fast"),
    GeminiModelPreset("gemini-3.1-pro", "Gemini 3.1 Pro", "3.x", "Logic Pro"),
    GeminiModelPreset("gemini-3.7-flash", "Gemini 3.7 Flash", "3.x", "⚡ Multimodal"),
    GeminiModelPreset("gemini-3.6-flash", "Gemini 3.6 Flash", "3.x", "High Speed"),
    GeminiModelPreset("gemini-3.5-pro", "Gemini 3.5 Pro", "3.x", "📚 1M Context"),
    GeminiModelPreset("gemini-3.5-flash", "Gemini 3.5 Flash", "3.x", "Fast"),

    // Gemini 3.0 Series
    GeminiModelPreset("gemini-3.0-pro", "Gemini 3.0 Pro", "3.0", "Frontier 3.0"),
    GeminiModelPreset("gemini-3.0-flash", "Gemini 3.0 Flash", "3.0", "Flash 3.0"),

    // Gemini 4 Series
    GeminiModelPreset("gemini-4-argon", "Gemini 4 Argon", "4.0", "🌟 Next-Era Flagship"),

    // Gemini 2.5 Series
    GeminiModelPreset("gemini-2.5-pro", "Gemini 2.5 Pro", "2.5", "High Context Pro"),
    GeminiModelPreset("gemini-2.5-flash", "Gemini 2.5 Flash", "2.5", "Fast 2.5"),

    // Gemini 1.5 Series
    GeminiModelPreset("gemini-1.5-pro-latest", "Gemini 1.5 Pro Latest", "1.5", "2M Context"),
    GeminiModelPreset("gemini-1.5-pro-002", "Gemini 1.5 Pro 002", "1.5", null),
    GeminiModelPreset("gemini-1.5-pro", "Gemini 1.5 Pro", "1.5", null),
    GeminiModelPreset("gemini-1.5-flash-latest", "Gemini 1.5 Flash Latest", "1.5", null),
    GeminiModelPreset("gemini-1.5-flash-002", "Gemini 1.5 Flash 002", "1.5", null),
    GeminiModelPreset("gemini-1.5-flash", "Gemini 1.5 Flash", "1.5", null),
    GeminiModelPreset("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", "1.5", "🏎️ 8B Flash"),

    // Gemma Open Models on AI Studio
    GeminiModelPreset("gemma-2-27b-it", "Gemma 2 27B IT", "Gemma", "Open Model"),
    GeminiModelPreset("gemma-2-9b-it", "Gemma 2 9B IT", "Gemma", "Open Model"),

    // Legacy
    GeminiModelPreset("gemini-1.0-pro", "Gemini 1.0 Pro", "1.5", "Legacy")
)

fun getProviderDisplayName(provider: String): String = when (provider.lowercase()) {
    "makersuite" -> "Google Gemini"
    "openrouter" -> "OpenRouter"
    "openai" -> "OpenAI"
    "claude" -> "Claude"
    "deepseek" -> "DeepSeek"
    "groq" -> "Groq"
    "custom" -> "Custom"
    else -> provider.replaceFirstChar { it.uppercase() }
}

fun getDefaultModelForProvider(provider: String): String = when (provider) {
    "makersuite" -> "gemini-3.8-flash"
    "openrouter" -> "anthropic/claude-3.7-sonnet"
    "claude" -> "claude-3-7-sonnet-20250219"
    "deepseek" -> "deepseek-chat"
    "groq" -> "llama-3.3-70b-versatile"
    else -> "gpt-4o"
}

@Composable
fun AiSettingsSheetContent(
    aiSettings: AiSettings,
    fontSizeSp: Int = 15,
    themeAccent: String = "violet",
    onSetFontSize: ((Int) -> Unit)? = null,
    onSetThemeAccent: ((String) -> Unit)? = null,
    onOpenPersona: (() -> Unit)? = null,
    onSave: (AiSettings) -> Unit,
    onFetchModels: ((String, (List<String>) -> Unit) -> Unit)? = null,
    onSaveApiKeyDirect: ((String, String, (Boolean) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember(aiSettings) { mutableStateOf(aiSettings.provider) }
    var modelInput by remember(aiSettings, selectedProvider) {
        mutableStateOf(
            if (selectedProvider == aiSettings.provider) aiSettings.model
            else getDefaultModelForProvider(selectedProvider)
        )
    }
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var customUrlInput by remember(aiSettings) { mutableStateOf(aiSettings.customUrl) }
    var tempValue by remember(aiSettings) { mutableStateOf(aiSettings.temperature.toFloat()) }
    var maxTokensValue by remember(aiSettings) { mutableStateOf(aiSettings.maxTokens.toFloat()) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Gemini category filter state
    var selectedGeminiCategory by remember { mutableStateOf("All") } // "All", "2.0", "2.5", "1.5", "Gemma"
    var isFetchingLiveModels by remember { mutableStateOf(false) }
    var liveModelsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedProvider) {
        if (selectedProvider == "makersuite" && aiSettings.hasExistingKey && liveModelsList.isEmpty()) {
            isFetchingLiveModels = true
            onFetchModels?.invoke("makersuite") { models ->
                isFetchingLiveModels = false
                if (models.isNotEmpty()) {
                    liveModelsList = models
                }
            }
        }
    }

    val providers = listOf(
        "makersuite" to "✨ Google AI Studio (Gemini)",
        "openrouter" to "🌐 OpenRouter",
        "openai" to "🤖 OpenAI",
        "claude" to "🎭 Claude",
        "deepseek" to "⚡ DeepSeek",
        "groq" to "🚀 Groq",
        "custom" to "⚙️ Custom URL"
    )

    val standardPresets = when (selectedProvider) {
        "openrouter" -> listOf(
            "anthropic/claude-3.7-sonnet",
            "anthropic/claude-3.5-sonnet",
            "deepseek/deepseek-chat",
            "deepseek/deepseek-r1",
            "google/gemini-2.5-pro",
            "google/gemini-2.0-flash-001",
            "meta-llama/llama-3.3-70b-instruct",
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "qwen/qwen-2.5-72b-instruct",
            "mistralai/mistral-large-2411",
            "gryphe/mythomax-l2-13b"
        )
        "openai" -> listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "o1", "o1-mini", "gpt-4-turbo")
        "claude" -> listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        "deepseek" -> listOf("deepseek-chat", "deepseek-reasoner")
        "groq" -> listOf("llama-3.3-70b-versatile", "deepseek-r1-distill-llama-70b", "mixtral-8x7b-32768", "gemma2-9b-it")
        else -> listOf("gpt-4o", "anthropic/claude-3.7-sonnet")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ColorAccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI Model & Key Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ColorTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Current Status Badge ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF191B26),
            border = BorderStroke(1.dp, ColorCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary
                    )
                    Text(
                        text = "${getProviderDisplayName(selectedProvider).uppercase()} • ${modelInput.substringAfterLast('/')}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ColorAccentCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (aiSettings.hasExistingKey) Color(0xFF064E3B) else Color(0xFF78350F)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (aiSettings.hasExistingKey) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (aiSettings.hasExistingKey) "Key Active" else "No Key",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aiSettings.hasExistingKey) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Provider Selection ---
        Text(
            text = "AI PROVIDER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((id, label) in providers) {
                val isSelected = selectedProvider == id
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedProvider = id
                            modelInput = getDefaultModelForProvider(id)
                            liveModelsList = emptyList()
                            fetchStatusMessage = null
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ColorAccentViolet.copy(alpha = 0.25f) else Color(0xFF191B26),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ColorAccentViolet else ColorCardBorder
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) ColorTextPrimary else ColorTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Model Selection Section ---
        if (selectedProvider == "makersuite") {
            // GOOGLE GEMINI SPECIALIZED SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOOGLE AI STUDIO MODELS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary
                    )
                    Text(
                        text = "Tap a model to select it",
                        fontSize = 11.sp,
                        color = ColorTextSecondary.copy(alpha = 0.7f)
                    )
                }

                // Live Fetch Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isFetchingLiveModels) {
                            if (onFetchModels != null) {
                                isFetchingLiveModels = true
                                fetchStatusMessage = "Fetching models from Google AI Studio..."

                                // If user typed a new key in the box, save it first so backend has it
                                val doFetch = {
                                    onFetchModels("makersuite") { models ->
                                        isFetchingLiveModels = false
                                        if (models.isNotEmpty()) {
                                            liveModelsList = models
                                            fetchStatusMessage = "✓ Synchronized ${models.size} models from Google!"
                                        } else {
                                            fetchStatusMessage = "Could not fetch models. Verify your API key."
                                        }
                                    }
                                }

                                if (apiKeyInput.isNotBlank() && onSaveApiKeyDirect != null) {
                                    onSaveApiKeyDirect("makersuite", apiKeyInput) { _ ->
                                        doFetch()
                                    }
                                } else {
                                    doFetch()
                                }
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = ColorAccentCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ColorAccentCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFetchingLiveModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = ColorAccentCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ColorAccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isFetchingLiveModels) "Syncing..." else "Fetch Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorAccentCyan
                        )
                    }
                }
            }

            if (!fetchStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = fetchStatusMessage.orEmpty(),
                    fontSize = 11.sp,
                    color = if (fetchStatusMessage?.startsWith("✓") == true) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gemini Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf("All", "3.8", "3.x", "3.0", "4.0", "2.5", "1.5", "Gemma")
                for (cat in categories) {
                    val isCatSelected = selectedGeminiCategory == cat
                    val catLabel = when (cat) {
                        "All" -> "All Models (${allGeminiPresets.size + liveModelsList.size})"
                        "3.8" -> "⚡ Gemini 3.8 (Flagship)"
                        "3.x" -> "⚡ Gemini 3.1 - 3.7"
                        "3.0" -> "Gemini 3.0"
                        "4.0" -> "Gemini 4"
                        "2.5" -> "Gemini 2.5"
                        "1.5" -> "Gemini 1.5"
                        "Gemma" -> "Gemma 2"
                        else -> cat
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedGeminiCategory = cat },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) ColorAccentViolet else Color(0xFF191B26),
                        border = BorderStroke(1.dp, if (isCatSelected) ColorAccentViolet else ColorCardBorder)
                    ) {
                        Text(
                            text = catLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCatSelected) Color.White else ColorTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gemini Model Presets Chips
            val filteredGemini = remember(selectedGeminiCategory, liveModelsList) {
                val presets = if (selectedGeminiCategory == "All") {
                    allGeminiPresets
                } else {
                    allGeminiPresets.filter { it.category == selectedGeminiCategory }
                }

                // Append any extra models discovered via Live Fetch
                val extras = liveModelsList
                    .filter { id -> allGeminiPresets.none { it.id == id } }
                    .map { id -> GeminiModelPreset(id, id, "Live", "Live") }

                if (selectedGeminiCategory == "All") presets + extras else presets
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (preset in filteredGemini) {
                    val isSelected = modelInput == preset.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { modelInput = preset.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) ColorAccentCyan.copy(alpha = 0.22f) else Color(0xFF191B26),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) ColorAccentCyan else ColorCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = preset.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ColorAccentCyan else ColorTextPrimary
                                )
                                if (preset.badge != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (preset.badge.contains("Recommended")) Color(0xFF065F46) else Color(0xFF312E81)
                                    ) {
                                        Text(
                                            text = preset.badge,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (preset.badge.contains("Recommended")) Color(0xFFA7F3D0) else Color(0xFFDDD6FE)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = preset.id,
                                fontSize = 10.sp,
                                color = ColorTextSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            // STANDARD PROVIDER MODEL PRESETS
            Text(
                text = "QUICK MODEL PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (preset in standardPresets) {
                    val isSelected = modelInput == preset
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { modelInput = preset },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) ColorAccentCyan.copy(alpha = 0.2f) else Color(0xFF191B26),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ColorAccentCyan else ColorCardBorder
                        )
                    ) {
                        Text(
                            text = preset.substringAfterLast('/'),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ColorAccentCyan else ColorTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Custom Model ID Input ---
        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            label = { Text("Model ID / Name", color = ColorTextSecondary) },
            singleLine = true,
            textStyle = TextStyle(color = ColorTextPrimary, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- API Key Input ---
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = {
                Text(
                    text = if (selectedProvider == "makersuite") {
                        if (aiSettings.hasExistingKey) "Change Google AI Studio Key (Leave blank to keep active)"
                        else "Google AI Studio API Key"
                    } else {
                        if (aiSettings.hasExistingKey) "Change API Key (Leave blank to keep active)"
                        else "Enter API Key"
                    },
                    color = ColorTextSecondary
                )
            },
            placeholder = {
                Text(
                    text = if (selectedProvider == "makersuite") {
                        if (aiSettings.hasExistingKey) "•••••••••••••••• (Google Key Active)" else "Paste API Key (AIzaSy...)"
                    } else {
                        if (aiSettings.hasExistingKey) "•••••••••••••••• (Active)" else "Paste API Key (sk-...)"
                    },
                    color = ColorTextSecondary.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            textStyle = TextStyle(color = ColorTextPrimary, fontSize = 14.sp),
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle key visibility",
                        tint = ColorTextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Helper Banner for Google AI Studio
        if (selectedProvider == "makersuite") {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = ColorAccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google AI Studio keys start with 'AIzaSy...'. Get your free key at aistudio.google.com",
                        fontSize = 11.sp,
                        color = ColorTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Advanced Parameters Accordion ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showAdvanced = !showAdvanced },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF191B26),
            border = BorderStroke(1.dp, ColorCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Parameters (Temperature, Tokens, Proxy)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ColorTextSecondary
                )
            }
        }

        if (showAdvanced) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141520),
                border = BorderStroke(1.dp, ColorCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature", fontSize = 12.sp, color = ColorTextSecondary)
                        Text(String.format(java.util.Locale.US, "%.2f", tempValue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorAccentCyan)
                    }
                    Slider(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorAccentCyan,
                            activeTrackColor = ColorAccentCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Max Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Output Tokens", fontSize = 12.sp, color = ColorTextSecondary)
                        Text("${maxTokensValue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorAccentViolet)
                    }
                    Slider(
                        value = maxTokensValue,
                        onValueChange = { maxTokensValue = it },
                        valueRange = 100f..4000f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = ColorAccentViolet,
                            activeTrackColor = ColorAccentViolet
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom URL / Reverse Proxy
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Custom Reverse Proxy / URL (Optional)", fontSize = 12.sp, color = ColorTextSecondary) },
                        placeholder = { Text("https://...", fontSize = 12.sp, color = ColorTextSecondary.copy(alpha = 0.4f)) },
                        singleLine = true,
                        textStyle = TextStyle(color = ColorTextPrimary, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // --- Appearance & Theme Accent Accordion ---
        Spacer(modifier = Modifier.height(16.dp))
        var showAppearance by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF191B26),
            border = BorderStroke(1.dp, ColorCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppearance = !showAppearance },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = ColorAccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme Accent",
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        imageVector = if (showAppearance) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ColorTextSecondary
                    )
                }

                if (showAppearance) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Theme Accent Color", color = ColorTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (palette in themePalettes) {
                            val isSelected = palette.id == themeAccent
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) palette.accent.copy(alpha = 0.25f) else Color(0xFF222433),
                                border = BorderStroke(1.dp, if (isSelected) palette.accent else ColorCardBorder),
                                modifier = Modifier.clickable { onSetThemeAccent?.invoke(palette.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(palette.accent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = palette.name,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else ColorTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chat Font Size", color = ColorTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            13 to "Compact",
                            15 to "Standard",
                            17 to "Large",
                            19 to "X-Large"
                        ).forEach { (size, label) ->
                            val isSelected = (fontSizeSp == size)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetFontSize?.invoke(size) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ColorAccentViolet.copy(alpha = 0.25f) else Color(0xFF222433),
                                border = BorderStroke(1.dp, if (isSelected) ColorAccentViolet else ColorCardBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else ColorTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    if (onOpenPersona != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onOpenPersona,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ColorAccentCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure User Persona & Profile", color = ColorAccentCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Save Button ---
        Button(
            onClick = {
                onSave(
                    aiSettings.copy(
                        provider = selectedProvider,
                        model = modelInput.trim().ifBlank { getDefaultModelForProvider(selectedProvider) },
                        apiKey = apiKeyInput.trim(),
                        customUrl = customUrlInput.trim(),
                        temperature = tempValue.toDouble(),
                        maxTokens = maxTokensValue.toInt()
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save & Apply Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: StChatMessage,
    character: StCharacter?,
    baseUrl: String,
    localCharacterDirs: List<File> = emptyList(),
    isLastMessage: Boolean = false,
    statusText: String? = null,
    fontSizeSp: Int = 15,
    activeTheme: ThemePalette = themePalettes.first(),
    isSpeaking: Boolean = false,
    onToggleTts: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    if (message.isGenerating) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StAvatar(
                avatarFileName = character?.avatar.orEmpty(),
                characterName = character?.name.orEmpty(),
                baseUrl = baseUrl,
                localCharacterDirs = localCharacterDirs,
                sizeDp = 32
            )
            Spacer(modifier = Modifier.width(8.dp))
            TypingShimmer(statusText = statusText)
        }
        return
    }

    val isUser = message.isUser
    val wordCount = remember(message.text) {
        message.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            StAvatar(
                avatarFileName = character?.avatar.orEmpty(),
                characterName = character?.name.orEmpty(),
                baseUrl = baseUrl,
                localCharacterDirs = localCharacterDirs,
                sizeDp = 34
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) Color.Transparent else ColorAssistantBubble,
            border = if (!isUser) BorderStroke(1.dp, ColorAssistantBorder) else null
        ) {
            Box(
                modifier = if (isUser) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(activeTheme.gradientStart, activeTheme.gradientEnd)
                        )
                    )
                } else {
                    Modifier
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ColorAccentCyan
                            )

                            // Swipe switcher pager (< 1/3 >)
                            if (message.swipes.size > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF191B26))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    IconButton(
                                        onClick = { onSwipeLeft?.invoke() },
                                        enabled = message.swipeId > 0,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Swipe",
                                            tint = if (message.swipeId > 0) activeTheme.accent else ColorTextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = "${message.swipeId + 1}/${message.swipes.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTheme.accent
                                    )

                                    IconButton(
                                        onClick = { onSwipeRight?.invoke() },
                                        enabled = message.swipeId < message.swipes.lastIndex,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Swipe",
                                            tint = if (message.swipeId < message.swipes.lastIndex) activeTheme.accent else ColorTextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    FormattedMessageText(text = message.text, isUser = isUser, fontSize = fontSizeSp)

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$wordCount words",
                                fontSize = 10.sp,
                                color = ColorTextSecondary.copy(alpha = 0.45f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onToggleTts != null) {
                                    IconButton(
                                        onClick = onToggleTts,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = if (isSpeaking) "Stop Speaking" else "Read Aloud",
                                            tint = if (isSpeaking) Color(0xFFEF4444) else ColorTextSecondary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (onCopy != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = onCopy,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = ColorTextSecondary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                if (isLastMessage && onContinue != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = onContinue,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Continue",
                                            tint = ColorAccentViolet,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                if (isLastMessage && onRegenerate != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = onRegenerate,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Regenerate",
                                            tint = ColorAccentCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMessageText(text: String, isUser: Boolean, fontSize: Int = 15) {
    val annotated = remember(text, isUser) {
        buildAnnotatedString {
            var inAction = false
            var buffer = StringBuilder()

            for (ch in text) {
                if (ch == '*') {
                    if (inAction) {
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = if (isUser) Color(0xFFDDD6FE) else ColorRoleplayAction
                            )
                        ) {
                            append(buffer.toString())
                        }
                        buffer = StringBuilder()
                        inAction = false
                    } else {
                        if (buffer.isNotEmpty()) {
                            withStyle(
                                SpanStyle(
                                    color = if (isUser) Color.White else ColorTextPrimary
                                )
                            ) {
                                append(buffer.toString())
                            }
                            buffer = StringBuilder()
                        }
                        inAction = true
                    }
                } else {
                    buffer.append(ch)
                }
            }

            if (buffer.isNotEmpty()) {
                if (inAction) {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isUser) Color(0xFFDDD6FE) else ColorRoleplayAction
                        )
                    ) {
                        append(buffer.toString())
                    }
                } else {
                    withStyle(
                        SpanStyle(
                            color = if (isUser) Color.White else ColorTextPrimary
                        )
                    ) {
                        append(buffer.toString())
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = TextStyle(fontSize = fontSize.sp, lineHeight = (fontSize * 1.4f).sp)
    )
}

@Composable
fun TypingShimmer(statusText: String? = null) {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ColorAssistantBubble,
        border = BorderStroke(1.dp, ColorAssistantBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ColorAccentCyan.copy(alpha = alpha1))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ColorAccentViolet.copy(alpha = alpha2))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ColorAccentCyan.copy(alpha = alpha3))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText ?: "Thinking...",
                color = if (statusText != null) ColorAccentCyan else ColorTextSecondary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
