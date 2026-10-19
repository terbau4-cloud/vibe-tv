package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PartnerRepository
import com.example.model.ChatMessage
import com.example.model.ChatConnectionState
import com.example.model.Country
import com.example.model.FloatingReaction
import com.example.model.Gender
import com.example.model.PartnerProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class VideoChatUiState(
    val connectionState: ChatConnectionState = ChatConnectionState.IDLE,
    val onlineUsersCount: Int = 1,
    val selectedGender: Gender = Gender.MALE,
    val selectedCountry: Country = Country.ALL,
    val currentPartner: PartnerProfile? = null,
    val partnerSpeechBubble: String? = null,
    val isPartnerTyping: Boolean = false,
    val isVoiceSpeaking: Boolean = false,
    val isVoiceMuted: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val floatingReactions: List<FloatingReaction> = emptyList(),
    val fullScreenEmoji: String? = null,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isVideoMuted: Boolean = false,
    val isMicMuted: Boolean = false,
    val isKeyboardChatOpen: Boolean = false,
    val blockedUserIds: Set<String> = emptySet(),
    val unreadMessageCount: Int = 0,
    val bannerNotice: String? = null,
    val showGenderSheet: Boolean = false,
    val showCountrySheet: Boolean = false,
    val showSafetyDialog: Boolean = false,
    val showReportDialog: Boolean = false,
    val showChatDrawer: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showTalkPrompts: Boolean = false,
    val isUserSignedIn: Boolean = false,
    val isSigningInWithGoogle: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null
)

class VideoChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VideoChatUiState())
    val uiState: StateFlow<VideoChatUiState> = _uiState.asStateFlow()

    private var partnerInteractionJob: Job? = null
    private var fullScreenEmojiJob: Job? = null
    var onSpeakRequested: ((text: String, countryCode: String) -> Unit)? = null

    fun signInWithGoogle(name: String = "Google User", email: String = "user@gmail.com") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningInWithGoogle = true) }
            delay(500)
            _uiState.update {
                it.copy(
                    isUserSignedIn = true,
                    isSigningInWithGoogle = false,
                    userName = name,
                    userEmail = email,
                    bannerNotice = "Welcome to VibeTV, $name!"
                )
            }
            delay(2500)
            _uiState.update { if (it.bannerNotice?.startsWith("Welcome") == true) it.copy(bannerNotice = null) else it }
        }
    }

    fun signOut() {
        stopChat()
        _uiState.update {
            it.copy(
                isUserSignedIn = false,
                userName = null,
                userEmail = null,
                showSettingsDialog = false
            )
        }
    }

    init {
        updateOnlineCount()
    }

    private fun updateOnlineCount() {
        // Show real online count: 1 when alone, 2 when connected
        _uiState.update { state ->
            val count = if (state.connectionState == ChatConnectionState.CONNECTED) 2 else 1
            state.copy(onlineUsersCount = count)
        }
    }

    fun startMatching() {
        partnerInteractionJob?.cancel()
        _uiState.update {
            it.copy(
                connectionState = ChatConnectionState.SEARCHING,
                currentPartner = null,
                partnerSpeechBubble = null,
                isVoiceSpeaking = false,
                isPartnerTyping = false,
                chatMessages = emptyList(),
                unreadMessageCount = 0,
                onlineUsersCount = 1
            )
        }
        // "and when slide then dont show fake show another user and dont find then show finding .."
        // We STAY in SEARCHING state (finding real users) instead of immediately showing a fake user!
    }

    fun nextPartner() {
        startMatching()
    }

    fun connectToDemoPartner() {
        partnerInteractionJob?.cancel()
        _uiState.update {
            it.copy(
                connectionState = ChatConnectionState.SEARCHING,
                currentPartner = null,
                partnerSpeechBubble = null,
                isVoiceSpeaking = false,
                isPartnerTyping = false,
                chatMessages = emptyList(),
                unreadMessageCount = 0
            )
        }
        viewModelScope.launch {
            delay(1200)
            connectToNewPartner()
        }
    }

    fun stopChat() {
        partnerInteractionJob?.cancel()
        _uiState.update {
            it.copy(
                connectionState = ChatConnectionState.IDLE,
                currentPartner = null,
                partnerSpeechBubble = null,
                isVoiceSpeaking = false,
                isPartnerTyping = false,
                chatMessages = emptyList(),
                unreadMessageCount = 0,
                isKeyboardChatOpen = false,
                onlineUsersCount = 1
            )
        }
    }

    private fun connectToNewPartner() {
        val currentState = _uiState.value
        val partner = PartnerRepository.samplePartners
            .filter { it.id !in currentState.blockedUserIds }
            .filter {
                currentState.selectedCountry.code == "ALL" || it.country.code == currentState.selectedCountry.code
            }
            .ifEmpty {
                PartnerRepository.samplePartners.filter { it.id !in currentState.blockedUserIds }
            }
            .filter { it.id != currentState.currentPartner?.id }
            .let { list ->
                if (list.isNotEmpty()) list[Random.nextInt(list.size)]
                else PartnerRepository.samplePartners[0]
            }

        val greeting = partner.greetings[Random.nextInt(partner.greetings.size)]

        _uiState.update {
            it.copy(
                connectionState = ChatConnectionState.CONNECTED,
                currentPartner = partner,
                partnerSpeechBubble = greeting,
                isVoiceSpeaking = !it.isVoiceMuted,
                onlineUsersCount = 2
            )
        }

        // Voice audio: speak greeting out loud!
        if (!currentState.isVoiceMuted) {
            onSpeakRequested?.invoke(greeting, partner.country.code)
        }

        // Add greeting to chat as well
        val greetingMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderIsMe = false,
            senderName = partner.name,
            text = greeting
        )

        _uiState.update {
            it.copy(
                chatMessages = listOf(greetingMessage),
                unreadMessageCount = if (it.isKeyboardChatOpen || it.showChatDrawer) 0 else 1
            )
        }

        startPartnerBehavior(partner)
    }

    private fun startPartnerBehavior(partner: PartnerProfile) {
        partnerInteractionJob?.cancel()
        partnerInteractionJob = viewModelScope.launch {
            // Stop speaking pulse after speech duration
            delay(3200)
            _uiState.update { it.copy(isVoiceSpeaking = false) }

            // Auto hide speech bubble after 5.5 seconds to keep view clean
            delay(2500)
            _uiState.update { it.copy(partnerSpeechBubble = null) }

            // Occasional partner reaction after some time
            delay(4000)
            if (_uiState.value.connectionState == ChatConnectionState.CONNECTED) {
                val emojis = listOf("👋", "✨", "😊", "✌️", "🔥")
                triggerReaction(emojis.random(), startXRatio = 0.5f + (Random.nextFloat() * 0.3f))
            }
        }
    }

    fun sendEmojiReaction(emoji: String) {
        // "ani emoji ma click garda purai screen ma aaos ekxin ko lagi"
        // Trigger full screen reaction animation
        fullScreenEmojiJob?.cancel()
        _uiState.update { it.copy(fullScreenEmoji = emoji) }
        fullScreenEmojiJob = viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(fullScreenEmoji = null) }
        }

        val baseStartX = 0.25f + (Random.nextFloat() * 0.5f)

        // Spawn a burst of 5-8 floating particles for a rich lively effect on the video stream!
        val burstCount = Random.nextInt(5, 9)
        for (i in 0 until burstCount) {
            val offsetX = (Random.nextFloat() * 0.28f) - 0.14f
            val startX = (baseStartX + offsetX).coerceIn(0.05f, 0.95f)
            val size = Random.nextInt(32, 54)
            val rotation = Random.nextFloat() * 40f - 20f
            triggerReaction(
                emoji = emoji,
                startXRatio = startX,
                sizeSp = size,
                rotationAngle = rotation
            )
        }

        if (_uiState.value.connectionState == ChatConnectionState.CONNECTED) {
            viewModelScope.launch {
                delay(650)
                val partnerEmoji = when (emoji) {
                    "😂" -> "🤣"
                    "❤️" -> "🥰"
                    "👍" -> "🙌"
                    "🔥" -> "💥"
                    "🎉" -> "🥳"
                    "⭐" -> "✨"
                    else -> emoji
                }
                val partnerBaseX = 0.35f + (Random.nextFloat() * 0.3f)
                for (j in 0 until 3) {
                    val pX = (partnerBaseX + (Random.nextFloat() * 0.14f - 0.07f)).coerceIn(0.15f, 0.85f)
                    triggerReaction(
                        emoji = partnerEmoji,
                        startXRatio = pX,
                        sizeSp = Random.nextInt(24, 38),
                        rotationAngle = Random.nextFloat() * 30f - 15f
                    )
                }
            }
        }
    }

    private fun triggerReaction(
        emoji: String,
        startXRatio: Float,
        sizeSp: Int = 34,
        rotationAngle: Float = 0f
    ) {
        val reaction = FloatingReaction(
            id = System.nanoTime() + Random.nextLong(1, 10000),
            emoji = emoji,
            startXRatio = startXRatio,
            startYRatio = 0.82f,
            sizeSp = sizeSp,
            rotationAngle = rotationAngle
        )
        _uiState.update {
            it.copy(floatingReactions = (it.floatingReactions + reaction).takeLast(30))
        }

        viewModelScope.launch {
            delay(2500)
            _uiState.update { state ->
                state.copy(floatingReactions = state.floatingReactions.filter { it.id != reaction.id })
            }
        }
    }

    fun sendChatMessage(text: String) {
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            senderIsMe = true,
            senderName = "You",
            text = text
        )

        _uiState.update {
            it.copy(chatMessages = it.chatMessages + userMsg)
        }

        val partner = _uiState.value.currentPartner ?: return

        // Partner speaks back in response!
        viewModelScope.launch {
            _uiState.update { it.copy(isPartnerTyping = true) }
            delay(1000 + (Random.nextLong(300, 800)))

            val replyText = partner.chatReplies.entries
                .firstOrNull { (k, _) -> text.contains(k, ignoreCase = true) }?.value
                ?: partner.chatReplies["default"]
                ?: "Nice! Having fun chatting with you!"

            val partnerMsg = ChatMessage(
                id = "partner_${System.currentTimeMillis()}",
                senderIsMe = false,
                senderName = partner.name,
                text = replyText
            )

            _uiState.update {
                it.copy(
                    isPartnerTyping = false,
                    partnerSpeechBubble = replyText,
                    isVoiceSpeaking = !it.isVoiceMuted,
                    chatMessages = it.chatMessages + partnerMsg,
                    unreadMessageCount = if (it.isKeyboardChatOpen || it.showChatDrawer) 0 else (it.unreadMessageCount + 1)
                )
            }

            if (!_uiState.value.isVoiceMuted) {
                onSpeakRequested?.invoke(replyText, partner.country.code)
            }

            delay(3000)
            _uiState.update { it.copy(isVoiceSpeaking = false) }

            delay(3000)
            _uiState.update { it.copy(partnerSpeechBubble = null) }
        }
    }

    fun talkToPartner(phrase: String) {
        sendChatMessage(phrase)
    }

    fun toggleCameraPower() {
        _uiState.update { it.copy(isCameraOn = !it.isCameraOn) }
    }

    fun toggleCameraLens() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun toggleVoiceAudio() {
        _uiState.update { it.copy(isVoiceMuted = !it.isVoiceMuted) }
    }

    fun toggleKeyboardChat() {
        _uiState.update {
            val newState = !it.isKeyboardChatOpen
            it.copy(
                isKeyboardChatOpen = newState,
                unreadMessageCount = if (newState) 0 else it.unreadMessageCount
            )
        }
    }

    fun openKeyboardChat() {
        _uiState.update { it.copy(isKeyboardChatOpen = true, unreadMessageCount = 0) }
    }

    fun closeKeyboardChat() {
        _uiState.update { it.copy(isKeyboardChatOpen = false) }
    }

    fun toggleTalkPrompts() {
        _uiState.update { it.copy(showTalkPrompts = !it.showTalkPrompts) }
    }

    fun reportAndBlockCurrentPartner(reason: String) {
        val partner = _uiState.value.currentPartner
        if (partner != null) {
            _uiState.update {
                it.copy(
                    blockedUserIds = it.blockedUserIds + partner.id,
                    bannerNotice = "User reported for: $reason. Partner blocked.",
                    showReportDialog = false
                )
            }
            startMatching()

            viewModelScope.launch {
                delay(3500)
                _uiState.update { it.copy(bannerNotice = null) }
            }
        }
    }

    fun setGender(gender: Gender) {
        _uiState.update { it.copy(selectedGender = gender) }
    }

    fun setCountry(country: Country) {
        _uiState.update {
            it.copy(
                selectedCountry = country,
                bannerNotice = if (country.code == "ALL") "Matching with users worldwide 🌎" else "Active filter: ${country.flag} ${country.name}"
            )
        }
        viewModelScope.launch {
            delay(3000)
            _uiState.update {
                if (it.bannerNotice?.startsWith("Active filter") == true || it.bannerNotice?.contains("worldwide") == true) {
                    it.copy(bannerNotice = null)
                } else it
            }
        }
    }

    fun resetCountryFilter() {
        setCountry(Country.ALL)
    }

    fun clearBlockedUsers() {
        _uiState.update { it.copy(blockedUserIds = emptySet()) }
    }

    fun setShowGenderSheet(show: Boolean) = _uiState.update { it.copy(showGenderSheet = show) }
    fun setShowCountrySheet(show: Boolean) = _uiState.update { it.copy(showCountrySheet = show) }
    fun setShowSafetyDialog(show: Boolean) = _uiState.update { it.copy(showSafetyDialog = show) }
    fun setShowReportDialog(show: Boolean) = _uiState.update { it.copy(showReportDialog = show) }
    fun setShowChatDrawer(show: Boolean) = _uiState.update {
        it.copy(showChatDrawer = show, unreadMessageCount = if (show) 0 else it.unreadMessageCount)
    }
    fun setShowSettingsDialog(show: Boolean) = _uiState.update { it.copy(showSettingsDialog = show) }
}
