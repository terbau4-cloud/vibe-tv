package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthManager
import com.example.model.ChatMessage
import com.example.model.ChatConnectionState
import com.example.model.Country
import com.example.model.FloatingReaction
import com.example.model.Gender
import com.example.model.PartnerProfile
import com.example.network.RealtimeMatchmaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class VideoChatViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application.applicationContext)

    private val _uiState = MutableStateFlow(VideoChatUiState())
    val uiState: StateFlow<VideoChatUiState> = _uiState.asStateFlow()

    private var partnerInteractionJob: Job? = null
    private var matchingJob: Job? = null
    private var fullScreenEmojiJob: Job? = null
    var onSpeakRequested: ((text: String, countryCode: String) -> Unit)? = null

    init {
        // Restore existing login session
        val session = authManager.getPersistedSession()
        if (session != null) {
            _uiState.update {
                it.copy(
                    isUserSignedIn = true,
                    userName = session.name,
                    userEmail = session.email,
                    selectedGender = session.gender
                )
            }
            RealtimeMatchmaker.updateClientId(session.uid)
        }

        // Live real online count from Firestore queue
        RealtimeMatchmaker.onRealOnlineCountChanged = { count ->
            viewModelScope.launch {
                _uiState.update { it.copy(onlineUsersCount = count) }
            }
        }

        // Real-time matchmaking callbacks connecting physical phones across the internet
        RealtimeMatchmaker.onMatchedCallback = { partner, _ ->
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        connectionState = ChatConnectionState.CONNECTED,
                        currentPartner = partner,
                        partnerSpeechBubble = "Connected with ${partner.name}! 👋",
                        isVoiceSpeaking = false,
                        onlineUsersCount = 2,
                        chatMessages = listOf(
                            ChatMessage(
                                id = "conn_${System.currentTimeMillis()}",
                                senderIsMe = false,
                                senderName = partner.name,
                                text = "Real Mobile Device Connected! 🟢"
                            )
                        ),
                        unreadMessageCount = 1
                    )
                }
            }
        }

        RealtimeMatchmaker.onChatMessageCallback = { chatMsg ->
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + chatMsg,
                        partnerSpeechBubble = chatMsg.text,
                        unreadMessageCount = if (it.isKeyboardChatOpen || it.showChatDrawer) 0 else (it.unreadMessageCount + 1)
                    )
                }
            }
        }

        RealtimeMatchmaker.onPartnerTypingCallback = { isTyping ->
            viewModelScope.launch {
                _uiState.update { it.copy(isPartnerTyping = isTyping) }
            }
        }

        RealtimeMatchmaker.onReactionCallback = { emoji ->
            viewModelScope.launch {
                sendEmojiReactionLocally(emoji)
            }
        }

        // User requirement: automatically search for another real user when partner ends or leaves
        RealtimeMatchmaker.onPartnerLeftCallback = {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        bannerNotice = "Partner left. Searching for real users…",
                        connectionState = ChatConnectionState.SEARCHING,
                        currentPartner = null,
                        partnerSpeechBubble = null,
                        isVoiceSpeaking = false,
                        isPartnerTyping = false,
                        chatMessages = emptyList(),
                        onlineUsersCount = 1
                    )
                }
                val state = _uiState.value
                RealtimeMatchmaker.startSearching(
                    userName = state.userName ?: "Real User",
                    gender = state.selectedGender,
                    country = state.selectedCountry
                )
            }
        }
    }

    fun loginUser(
        emailOrUsername: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.login(emailOrUsername, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                authenticateUser(user.name, user.email, user.gender, user.uid)
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun registerUser(
        name: String,
        emailOrUsername: String,
        password: String,
        gender: Gender,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.register(name, emailOrUsername, password, gender)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                authenticateUser(user.name, user.email, user.gender, user.uid)
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun authenticateUser(name: String, email: String, gender: Gender = Gender.MALE, uid: String? = null) {
        if (uid != null) {
            RealtimeMatchmaker.updateClientId(uid)
        }
        _uiState.update {
            it.copy(
                isUserSignedIn = true,
                userName = name,
                userEmail = email,
                selectedGender = gender,
                bannerNotice = "Welcome to VibeTV, $name!"
            )
        }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { if (it.bannerNotice?.startsWith("Welcome") == true) it.copy(bannerNotice = null) else it }
        }
    }

    fun signInWithGoogle(name: String = "Google User", email: String = "user@gmail.com") {
        authenticateUser(name, email)
    }

    fun signOut() {
        stopChat()
        authManager.logout()
        _uiState.update {
            it.copy(
                isUserSignedIn = false,
                userName = null,
                userEmail = null,
                showSettingsDialog = false
            )
        }
    }

    fun startMatching() {
        partnerInteractionJob?.cancel()
        matchingJob?.cancel()
        RealtimeMatchmaker.leaveCurrentRoom()

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

        val state = _uiState.value
        RealtimeMatchmaker.startSearching(
            userName = state.userName ?: "Real User",
            gender = state.selectedGender,
            country = state.selectedCountry
        )
    }

    fun nextPartner() {
        startMatching()
    }

    fun stopChat() {
        partnerInteractionJob?.cancel()
        matchingJob?.cancel()
        RealtimeMatchmaker.leaveCurrentRoom()
        RealtimeMatchmaker.stopSearching()
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

    fun sendEmojiReaction(emoji: String) {
        sendEmojiReactionLocally(emoji)
        // Broadcast to real partner phone
        RealtimeMatchmaker.sendReaction(emoji)
    }

    private fun sendEmojiReactionLocally(emoji: String) {
        fullScreenEmojiJob?.cancel()
        _uiState.update { it.copy(fullScreenEmoji = emoji) }
        fullScreenEmojiJob = viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(fullScreenEmoji = null) }
        }

        val baseStartX = 0.25f + (Random.nextFloat() * 0.5f)
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
        val myName = _uiState.value.userName ?: "You"
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            senderIsMe = true,
            senderName = "You",
            text = text
        )

        _uiState.update {
            it.copy(chatMessages = it.chatMessages + userMsg)
        }

        // Send to real partner device
        RealtimeMatchmaker.sendChatMessage(text, myName)
    }

    fun notifyTyping(isTyping: Boolean) {
        RealtimeMatchmaker.sendTyping(isTyping)
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
                bannerNotice = if (country.code == "ALL") "Matching with real users worldwide 🌎" else "Active filter: ${country.flag} ${country.name}"
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

    override fun onCleared() {
        super.onCleared()
        stopChat()
    }
}
