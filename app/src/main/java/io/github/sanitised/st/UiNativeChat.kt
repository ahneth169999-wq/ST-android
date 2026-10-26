package io.github.sanitised.st

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    sizeDp: Int = 40,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(avatarFileName) { mutableStateOf(avatarBitmapCache[avatarFileName]) }

    LaunchedEffect(avatarFileName, baseUrl) {
        if (avatarFileName.isNotBlank() && bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val urlStr = "$baseUrl/thumbnail?type=avatar&avatar=$avatarFileName"
                    val stream = URL(urlStr).openStream()
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
            // Initial placeholder with gradient background
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
                    text = characterName.take(1).uppercase(),
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
    onBackToDashboard: () -> Unit,
    onOpenFullSillyTavern: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(baseUrl) {
        viewModel.setBaseUrl(baseUrl)
        viewModel.loadCharacters()
    }

    val characters by viewModel.characters.collectAsState()
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showCharacterSheet by remember { mutableStateOf(false) }
    var selectedMessageForMenu by remember { mutableStateOf<Pair<Int, StChatMessage>?>(null) }
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var editingMessageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Automatically scroll to bottom when messages update
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
                                text = if (isGenerating) "Generating..." else "Local Engine Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Characters Drawer Switcher
                IconButton(onClick = { showCharacterSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Switch Character",
                        tint = ColorAccentViolet
                    )
                }

                // New Chat Button
                IconButton(onClick = { viewModel.createNewChat() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
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
            if (isLoadingMessages) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorAccentViolet)
                }
            } else if (messages.isEmpty() && selectedCharacter != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StAvatar(
                            avatarFileName = selectedCharacter?.avatar.orEmpty(),
                            characterName = selectedCharacter?.name.orEmpty(),
                            baseUrl = baseUrl,
                            sizeDp = 72
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Start chatting with ${selectedCharacter?.name}!",
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
                // Input box
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

                // Send Button with glowing gradient
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
                Text(
                    text = "Select Character",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(characters.size) { i ->
                        val item = characters[i]
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

                // Copy Text
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

                // Regenerate (only for assistant messages)
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

                // Edit Message
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

                // Delete Message
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
fun MessageBubble(
    message: StChatMessage,
    character: StCharacter?,
    baseUrl: String,
    onClick: () -> Unit
) {
    if (message.isGenerating) {
        // Typing indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StAvatar(
                avatarFileName = character?.avatar.orEmpty(),
                characterName = character?.name.orEmpty(),
                baseUrl = baseUrl,
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

/**
 * Parses markdown-style asterisks for roleplay actions (*action text*)
 * and renders them with soft italicized violet tint.
 */
@Composable
fun FormattedMessageText(text: String, isUser: Boolean) {
    val annotated = remember(text, isUser) {
        buildAnnotatedString {
            var inAction = false
            var buffer = StringBuilder()

            for (ch in text) {
                if (ch == '*') {
                    if (inAction) {
                        // End of action
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
                        // Start of action
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

/**
 * Animated glowing dots shimmer when the AI is generating.
 */
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
