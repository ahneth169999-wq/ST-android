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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    val aiSettings by viewModel.aiSettings.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showCharacterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
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
                                    .background(if (isGenerating) ColorAccentCyan else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGenerating) "Generating..." else "${aiSettings.provider.uppercase()} • ${aiSettings.model.substringAfterLast('/')}",
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

                // AI Model & Key Settings Button
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "AI Model & Key Settings",
                        tint = ColorAccentCyan
                    )
                }

                // Characters Drawer Switcher
                IconButton(onClick = { showCharacterSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Switch Character",
                        tint = ColorAccentViolet
                    )
                }

                // + Button: Creates New Chat if character selected; otherwise selects character or opens picker
                IconButton(onClick = {
                    if (selectedCharacter != null) {
                        viewModel.createNewChat()
                        Toast.makeText(context, "Started new chat session with ${selectedCharacter?.name}", Toast.LENGTH_SHORT).show()
                    } else if (characters.isNotEmpty()) {
                        val firstChar = characters.first()
                        viewModel.selectCharacter(firstChar)
                        Toast.makeText(context, "Selected ${firstChar.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        showCharacterSheet = true
                        Toast.makeText(context, "Select or import a character first", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = if (selectedCharacter != null || characters.isNotEmpty()) ColorAccentCyan else ColorTextSecondary
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

        // --- Error Banner ---
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF450A0A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage ?: "",
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(messages, key = { index, msg -> "${msg.id}-$index" }) { index, msg ->
                        MessageBubble(
                            message = msg,
                            character = selectedCharacter,
                            baseUrl = baseUrl,
                            localCharacterDirs = localCharacterDirs,
                            onClick = { selectedMessageForMenu = Pair(index, msg) }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        cursorBrush = SolidColor(ColorAccentViolet),
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
                                    listOf(ColorUserGradientStart, ColorAccentViolet)
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
                onDismiss = { showSettingsSheet = false }
            )
        }
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

@Composable
fun AiSettingsSheetContent(
    aiSettings: AiSettings,
    onSave: (AiSettings) -> Unit,
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

    val providers = listOf(
        "openrouter" to "🌐 OpenRouter",
        "openai" to "🤖 OpenAI",
        "claude" to "🎭 Claude",
        "makersuite" to "✨ Gemini",
        "deepseek" to "⚡ DeepSeek",
        "groq" to "🚀 Groq",
        "custom" to "⚙️ Custom URL"
    )

    val presetModels = when (selectedProvider) {
        "openrouter" -> listOf(
            "anthropic/claude-3.5-sonnet",
            "deepseek/deepseek-chat",
            "meta-llama/llama-3.3-70b-instruct",
            "google/gemini-2.0-flash-exp:free",
            "openai/gpt-4o-mini",
            "gryphe/mythomax-l2-13b"
        )
        "openai" -> listOf("gpt-4o", "gpt-4o-mini", "o1-mini", "gpt-4-turbo")
        "claude" -> listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        "makersuite" -> listOf("gemini-1.5-pro-latest", "gemini-1.5-flash-latest", "gemini-2.0-flash-exp")
        "deepseek" -> listOf("deepseek-chat", "deepseek-reasoner")
        "groq" -> listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768", "gemma2-9b-it")
        else -> listOf("gpt-4o", "anthropic/claude-3.5-sonnet")
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
                        text = "${selectedProvider.uppercase()} • ${modelInput.substringAfterLast('/')}",
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

        // --- Model Presets ---
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
            for (preset in presetModels) {
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
                    text = if (aiSettings.hasExistingKey) "Change API Key (Leave blank to keep active)" else "Enter API Key",
                    color = ColorTextSecondary
                )
            },
            placeholder = {
                Text(
                    text = if (aiSettings.hasExistingKey) "•••••••••••••••• (Active)" else "Paste API Key (sk-...)",
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

private fun getDefaultModelForProvider(provider: String): String = when (provider) {
    "openrouter" -> "anthropic/claude-3.5-sonnet"
    "claude" -> "claude-3-5-sonnet-20241022"
    "makersuite" -> "gemini-1.5-pro-latest"
    "deepseek" -> "deepseek-chat"
    "groq" -> "llama-3.3-70b-versatile"
    else -> "gpt-4o"
}

@Composable
fun MessageBubble(
    message: StChatMessage,
    character: StCharacter?,
    baseUrl: String,
    localCharacterDirs: List<File> = emptyList(),
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
            TypingShimmer()
        }
        return
    }

    val isUser = message.isUser

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
                            listOf(ColorUserGradientStart, ColorUserGradientEnd)
                        )
                    )
                } else {
                    Modifier
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isUser) {
                        Text(
                            text = message.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ColorAccentCyan
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    FormattedMessageText(text = message.text, isUser = isUser)
                }
            }
        }
    }
}

@Composable
fun FormattedMessageText(text: String, isUser: Boolean) {
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
        style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
    )
}

@Composable
fun TypingShimmer() {
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
                text = "Thinking...",
                color = ColorTextSecondary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
