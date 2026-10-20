package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraPreview
import com.example.model.ChatConnectionState
import com.example.model.ChatMessage
import com.example.ui.components.ActiveFilterBadge
import com.example.ui.components.CountrySelectionSheet
import com.example.ui.components.FloatingEmojiBurstContainer
import com.example.ui.components.FullScreenEmojiOverlay
import com.example.ui.components.GenderSelectionSheet
import com.example.ui.components.InteractiveReactionRow
import com.example.ui.components.InCallChatDrawer
import com.example.ui.components.PartnerVideoFeed
import com.example.ui.components.ReportUserDialog
import com.example.ui.components.SafetyDialog
import com.example.ui.components.SearchingRadarView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TvStaticBackground
import com.example.viewmodel.VideoChatViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MainChatScreen(
    viewModel: VideoChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val formattedUserCount = NumberFormat.getNumberInstance(Locale.US).format(uiState.onlineUsersCount)

    var chatInputText by remember { mutableStateOf("") }
    var isCameraActuallyStreaming by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    // Auto-focus and open keyboard when keyboard chat mode is triggered
    LaunchedEffect(uiState.isKeyboardChatOpen) {
        if (uiState.isKeyboardChatOpen) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1015))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== TOP HALF ====================
            // Sliding the top half to the left switches to the NEXT user! ("mathi ko lai slide garyo left side tira vane AARKO USER AAUXA")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(uiState.connectionState) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -20f) {
                                // Left slide: start matching or find next real partner
                                if (uiState.connectionState == ChatConnectionState.IDLE) {
                                    viewModel.startMatching()
                                } else if (uiState.connectionState == ChatConnectionState.CONNECTED) {
                                    viewModel.nextPartner()
                                } else if (uiState.connectionState == ChatConnectionState.SEARCHING) {
                                    viewModel.startMatching()
                                }
                            } else if (dragAmount > 20f) {
                                // Right slide: return back to start screen
                                viewModel.stopChat()
                            }
                        }
                    }
                    .testTag("top_half_container")
            ) {
                when (uiState.connectionState) {
                    ChatConnectionState.IDLE -> {
                        // CRT TV static background
                        TvStaticBackground()

                        // Settings Icon (top right, matching screenshot)
                        IconButton(
                            onClick = { viewModel.setShowSettingsDialog(true) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Center content: Logo, Online counter, Filter buttons
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Logo Box: Orange TV with cute antennas and branding (like OmeTV logo)
                            Box(
                                modifier = Modifier
                                    .size(118.dp, 92.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFFF5722), Color(0xFFFF3D00))
                                        )
                                    )
                                    .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(22.dp))
                                    .shadow(8.dp, RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp, 68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Vibe",
                                            color = Color(0xFFFF473A),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            lineHeight = 24.sp
                                        )
                                        Text(
                                            text = "TV",
                                            color = Color(0xFF1B5E20),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Online Counter: 🟢 267,938 Users online (exact match from screenshot)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x88000000))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.onlineUsersCount == 1) "1 User online (You)" else "${uiState.onlineUsersCount} Users online",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Two Pill Selection Buttons side-by-side (matching screenshot)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Left pill: "I AM 👦"
                                Surface(
                                    onClick = { viewModel.setShowGenderSheet(true) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x99242630),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(145.dp)
                                        .testTag("gender_filter_button")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.selectedGender.label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.selectedGender.emoji,
                                            fontSize = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right pill: Country selection filter with active highlight
                                val isCountryFiltered = uiState.selectedCountry.code != "ALL"
                                Surface(
                                    onClick = { viewModel.setShowCountrySheet(true) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCountryFiltered) Color(0x44FF473A) else Color(0x99242630),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isCountryFiltered) 1.5.dp else 1.dp,
                                        color = if (isCountryFiltered) Color(0xFFFF473A) else Color(0x66FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(165.dp)
                                        .testTag("country_filter_button")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (!isCountryFiltered) "ALL COUNTRIES" else uiState.selectedCountry.name.take(11),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.selectedCountry.flag,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            // Active Filter display badge when specific country is selected
                            if (uiState.selectedCountry.code != "ALL") {
                                Spacer(modifier = Modifier.height(10.dp))
                                ActiveFilterBadge(
                                    selectedCountry = uiState.selectedCountry,
                                    onOpenFilterModal = { viewModel.setShowCountrySheet(true) },
                                    onResetToAll = { viewModel.resetCountryFilter() }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Start matching button or slide left hint
                            Button(
                                onClick = { viewModel.startMatching() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF473A)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("start_matching_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("START • SLIDE LEFT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    ChatConnectionState.SEARCHING -> {
                        SearchingRadarView(
                            selectedCountry = uiState.selectedCountry,
                            selectedGender = uiState.selectedGender,
                            onlineUsersCount = uiState.onlineUsersCount,
                            onCancel = { viewModel.stopChat() },
                            onOpenCountryModal = { viewModel.setShowCountrySheet(true) }
                        )
                    }

                    ChatConnectionState.CONNECTED -> {
                        uiState.currentPartner?.let { partner ->
                            PartnerVideoFeed(
                                partner = partner,
                                selectedCountry = uiState.selectedCountry,
                                speechBubble = uiState.partnerSpeechBubble,
                                isTyping = uiState.isPartnerTyping,
                                isVoiceSpeaking = uiState.isVoiceSpeaking,
                                isVoiceMuted = uiState.isVoiceMuted,
                                onToggleVoiceMute = { viewModel.toggleVoiceAudio() },
                                onOpenCountryModal = { viewModel.setShowCountrySheet(true) },
                                onResetCountryFilter = { viewModel.resetCountryFilter() },
                                onSendReaction = { viewModel.sendEmojiReaction(it) },
                                onSkip = { viewModel.nextPartner() },
                                onReport = { viewModel.setShowReportDialog(true) },
                                onBackToStart = { viewModel.stopChat() }
                            )
                        }
                    }

                    ChatConnectionState.ENDED -> {
                        TvStaticBackground()
                        // Automatically search for another real user without requiring button or closing app
                        LaunchedEffect(Unit) {
                            viewModel.startMatching()
                        }
                    }
                }
            }

            // Divider between top and bottom halves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF333644))
            )

            // ==================== BOTTOM HALF (OWN CAMERA) ====================
            // "if camera off xa vane tala kei na dekhaunu":
            // When camera is turned off by user, bottom shows completely clean pitch black with NO text or placeholder banners!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(uiState.connectionState) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -20f) {
                                if (uiState.connectionState == ChatConnectionState.IDLE) {
                                    viewModel.startMatching()
                                } else if (uiState.connectionState == ChatConnectionState.CONNECTED) {
                                    viewModel.nextPartner()
                                } else if (uiState.connectionState == ChatConnectionState.SEARCHING) {
                                    viewModel.startMatching()
                                }
                            } else if (dragAmount > 20f) {
                                viewModel.stopChat()
                            }
                        }
                    }
                    .testTag("own_camera_container")
            ) {
                // Live camera preview (shows pure solid black when camera is not open, off, or on PC without camera)
                CameraPreview(
                    isCameraOn = uiState.isCameraOn,
                    isFrontCamera = uiState.isFrontCamera,
                    onCameraActiveChanged = { isCameraActuallyStreaming = it }
                )

                // Only show badge & disclaimer if camera is ACTUALLY streaming and open!
                // If camera is not open or if on PC without camera: "then show black" (pure clean black, nothing else)
                if (uiState.isCameraOn && isCameraActuallyStreaming) {
                    // Top-left app badge in camera preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x99000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = "VibeTV",
                            tint = Color(0xFFFF473A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VibeTV",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Live chat message & typing overlay on bottom-left of own camera:
                // "ani messege send garda messege tala dekhau aafno camera ma left side ani opponent le msg typin garda 3 . dekhau ani messege pani tehi dekhau aaye paxi"
                if (uiState.chatMessages.isNotEmpty() || uiState.isPartnerTyping) {
                    InCameraChatStream(
                        messages = uiState.chatMessages,
                        isPartnerTyping = uiState.isPartnerTyping,
                        partnerName = uiState.currentPartner?.name ?: "Partner",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = if (uiState.isKeyboardChatOpen) 115.dp else 64.dp)
                    )
                } else if (uiState.isCameraOn && isCameraActuallyStreaming) {
                    // Disclaimer text (matching exact text and position from user's screenshot)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, end = 14.dp, bottom = if (uiState.isKeyboardChatOpen) 115.dp else 64.dp)
                            .background(Color(0x77000000), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "By using this video chat, you confirm that you are an adult and agree to our rules. Rules violators will be banned. Try to keep your face visible in camera frame.",
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.setShowSafetyDialog(true) }
                                .testTag("safety_reminder_link"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Safety Reminder",
                                color = Color(0xFFFFCA28),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Talk / Voice Prompts overlay when "Talk / Kura garne" is opened
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showTalkPrompts && uiState.connectionState == ChatConnectionState.CONNECTED,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (uiState.isKeyboardChatOpen) 115.dp else 64.dp)
                ) {
                    val talkPhrases = listOf(
                        "Hey! How's it going? 👋",
                        "Where are you from? 🌍",
                        "Nice meeting you! ✨",
                        "Tell me something fun! 😄",
                        "I love your vibe! 🔥"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(talkPhrases) { phrase ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xDDFF473A))
                                    .clickable {
                                        viewModel.talkToPartner(phrase)
                                        viewModel.toggleTalkPrompts()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = phrase,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // INLINE KEYBOARD CHAT INPUT BAR:
                // "tala emoji ko side ma xa tesma cick garda keyboad aauxa ani chatting pani garna pauxa"
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isKeyboardChatOpen,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 56.dp, start = 8.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xF01E1F28))
                            .border(1.dp, Color(0xFFFF473A), RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Type and chat...", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("inline_chat_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (chatInputText.isNotBlank()) {
                                        viewModel.sendChatMessage(chatInputText.trim())
                                        chatInputText = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFFFF473A)
                            )
                        )

                        // Send button
                        IconButton(
                            onClick = {
                                if (chatInputText.isNotBlank()) {
                                    viewModel.sendChatMessage(chatInputText.trim())
                                    chatInputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (chatInputText.isNotBlank()) Color(0xFFFF473A) else Color(0xFF333544))
                                .testTag("inline_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Close keyboard chat button
                        IconButton(
                            onClick = {
                                viewModel.closeKeyboardChat()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close chat",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom Action Bar: Camera Power toggle, Flip, Emojis, Chat button, Talk/Voice button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC000000), Color(0xEE000000))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Power Toggle (ON / OFF) -> when OFF, "tala kei na dekhaunu"!
                    IconButton(
                        onClick = { viewModel.toggleCameraPower() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isCameraOn) Color(0x77000000) else Color(0xAAFF5252))
                            .testTag("toggle_camera_power_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Flip camera button (front / back) - only shown when camera hardware is open and active
                    if (uiState.isCameraOn && isCameraActuallyStreaming) {
                        IconButton(
                            onClick = { viewModel.toggleCameraLens() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Interactive animated reaction buttons with bouncy tap animations
                    InteractiveReactionRow(
                        onReactionTap = { emoji ->
                            viewModel.sendEmojiReaction(emoji)
                        }
                    )

                    // Talk / Kura Garne Button: speaks to partner!
                    IconButton(
                        onClick = { viewModel.toggleTalkPrompts() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (uiState.showTalkPrompts) Color(0xFFFF473A) else Color(0x77000000))
                            .testTag("talk_with_partner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Talk / Bolna",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // In-call text chat button beside emojis:
                    // "tala emoji ko side ma xa tesma cick garda keyboad aauxa ani chatting pani garna pauxa"
                    BadgedBox(
                        badge = {
                            if (uiState.unreadMessageCount > 0) {
                                Badge(containerColor = Color(0xFFFF473A)) {
                                    Text("${uiState.unreadMessageCount}")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.toggleKeyboardChat()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isKeyboardChatOpen) Color(0xFFFF473A) else Color(0x77000000))
                                .testTag("open_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat with Keyboard",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating emojis container across entire screen
        FloatingEmojiBurstContainer(reactions = uiState.floatingReactions)

        // Full-screen burst when emoji button is tapped: "ani emoji ma click garda purai screen ma aaos ekxin ko lagi"
        FullScreenEmojiOverlay(emoji = uiState.fullScreenEmoji)

        // Banner Notice for report confirmation or alerts
        AnimatedVisibility(
            visible = uiState.bannerNotice != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE212121))
                    .border(1.dp, Color(0xFFFF473A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = uiState.bannerNotice ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Gender Selection Sheet
        if (uiState.showGenderSheet) {
            GenderSelectionSheet(
                selectedGender = uiState.selectedGender,
                onSelectGender = { viewModel.setGender(it) },
                onDismiss = { viewModel.setShowGenderSheet(false) }
            )
        }

        // Country Selection Sheet
        if (uiState.showCountrySheet) {
            CountrySelectionSheet(
                selectedCountry = uiState.selectedCountry,
                onSelectCountry = { viewModel.setCountry(it) },
                onDismiss = { viewModel.setShowCountrySheet(false) }
            )
        }

        // Safety Dialog
        if (uiState.showSafetyDialog) {
            SafetyDialog(onDismiss = { viewModel.setShowSafetyDialog(false) })
        }

        // Report Partner Dialog
        if (uiState.showReportDialog && uiState.currentPartner != null) {
            ReportUserDialog(
                partnerName = uiState.currentPartner!!.name,
                onReportConfirmed = { reason ->
                    viewModel.reportAndBlockCurrentPartner(reason)
                },
                onDismiss = { viewModel.setShowReportDialog(false) }
            )
        }

        // Full In-call Chat Drawer (when opened via settings/menu)
        if (uiState.showChatDrawer) {
            InCallChatDrawer(
                partnerName = uiState.currentPartner?.name ?: "Partner",
                messages = uiState.chatMessages,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onDismiss = { viewModel.setShowChatDrawer(false) }
            )
        }

        // Settings Dialog
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                isFrontCamera = uiState.isFrontCamera,
                onToggleCameraLens = { viewModel.toggleCameraLens() },
                isMicMuted = uiState.isMicMuted,
                onToggleMicMute = { viewModel.toggleVoiceAudio() },
                blockedCount = uiState.blockedUserIds.size,
                onClearBlocked = { viewModel.clearBlockedUsers() },
                onOpenSafetyGuidelines = { viewModel.setShowSafetyDialog(true) },
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                onSignOut = { viewModel.signOut() },
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }
    }
}

/**
 * Overlay on the left side of the user's camera feed displaying recent messages and partner typing indicator:
 * "ani messege send garda messege tala dekhau aafno camera ma left side ani opponent le msg typin garda 3 . dekhau ani messege pani tehi dekhau aaye paxi"
 */
@Composable
private fun InCameraChatStream(
    messages: List<ChatMessage>,
    isPartnerTyping: Boolean,
    partnerName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        messages.takeLast(3).forEach { msg ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (msg.senderIsMe) Color(0xD0FF473A)
                        else Color(0xD01E1F28)
                    )
                    .border(
                        width = 1.dp,
                        color = if (msg.senderIsMe) Color(0xFFFF8A80) else Color(0x55FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (msg.senderIsMe) "You: ${msg.text}" else "$partnerName: ${msg.text}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3-dots typing indicator: "ani opponent le msg typin garda 3 . dekhau"
        if (isPartnerTyping) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE01E1F28))
                    .border(1.dp, Color(0xFFFF8A80), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$partnerName is typing ",
                    color = Color(0xFFFF8A80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                PulsingThreeDots()
            }
        }
    }
}

@Composable
private fun PulsingThreeDots() {
    val transition = rememberInfiniteTransition(label = "pulsingDots")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 180, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 360, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF8A80).copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF8A80).copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF8A80).copy(alpha = dot3Alpha))
        )
    }
}

