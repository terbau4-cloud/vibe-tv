package com.example.network

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.Country
import com.example.model.Gender
import com.example.model.PartnerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Real-time Matchmaking and Messaging engine for connecting physical devices (Phone 1 and Phone 2).
 * Operates zero-config across any cellular/WiFi networks with zero fake bots.
 */
object RealtimeMatchmaker {

    private const val TAG = "RealtimeMatchmaker"
    private const val LOBBY_TOPIC = "vibetv_global_match_lobby"
    private const val NTFY_BASE_HTTP = "https://ntfy.sh"
    private const val NTFY_BASE_WS = "wss://ntfy.sh"

    val myClientId: String = "dev_" + UUID.randomUUID().toString().replace("-", "").take(8)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Keep WS open
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lobbyWebSocket: WebSocket? = null
    private var roomWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    private var isSearching = false
    private var currentRoomId: String? = null

    var onMatchedCallback: ((PartnerProfile, String) -> Unit)? = null
    var onChatMessageCallback: ((ChatMessage) -> Unit)? = null
    var onPartnerTypingCallback: ((Boolean) -> Unit)? = null
    var onReactionCallback: ((String) -> Unit)? = null
    var onPartnerLeftCallback: (() -> Unit)? = null

    /**
     * Start broadcasting presence to the global matchmaking lobby.
     * When another real physical device broadcasts, both will match and connect automatically.
     */
    fun startSearching(
        userName: String,
        gender: Gender,
        country: Country
    ) {
        stopSearching()
        isSearching = true

        val request = Request.Builder()
            .url("$NTFY_BASE_WS/$LOBBY_TOPIC/ws")
            .build()

        lobbyWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Lobby WebSocket connected.")
                // Send immediate search broadcast
                broadcastSearch(userName, gender, country)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLobbyMessage(text, userName, gender, country)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Lobby WebSocket failure: ${t.message}")
            }
        })

        // Heartbeat broadcast every 2.5 seconds to find other real devices
        heartbeatJob = scope.launch {
            while (isActive && isSearching) {
                delay(2500)
                if (isSearching) {
                    broadcastSearch(userName, gender, country)
                }
            }
        }
    }

    private fun broadcastSearch(userName: String, gender: Gender, country: Country) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("event", "SEARCH")
                    put("userId", myClientId)
                    put("userName", userName)
                    put("gender", gender.name)
                    put("countryCode", country.code)
                    put("countryName", country.name)
                    put("countryFlag", country.flag)
                    put("timestamp", System.currentTimeMillis())
                }
                postHttp(LOBBY_TOPIC, json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "broadcastSearch error", e)
            }
        }
    }

    private fun handleLobbyMessage(
        rawMessage: String,
        myName: String,
        myGender: Gender,
        myCountry: Country
    ) {
        if (!isSearching) return
        try {
            val root = JSONObject(rawMessage)
            if (root.optString("event") != "message") return
            val payloadStr = root.optString("message")
            if (payloadStr.isBlank()) return

            val payload = JSONObject(payloadStr)
            val event = payload.optString("event")

            if (event == "SEARCH") {
                val otherId = payload.optString("userId")
                if (otherId.isNotBlank() && otherId != myClientId) {
                    // We found another real mobile device in the lobby!
                    val otherName = payload.optString("userName", "Real User")
                    val otherGender = payload.optString("gender", "MALE")
                    val otherCountryCode = payload.optString("countryCode", "NP")
                    val otherCountryName = payload.optString("countryName", "Nepal")
                    val otherCountryFlag = payload.optString("countryFlag", "🇳🇵")

                    // Deterministic match initiator to prevent duplicate offers:
                    if (myClientId < otherId) {
                        val roomId = "room_${myClientId}_${otherId}"
                        val matchPayload = JSONObject().apply {
                            put("event", "MATCH")
                            put("initiatorId", myClientId)
                            put("initiatorName", myName)
                            put("initiatorGender", myGender.name)
                            put("initiatorCountryCode", myCountry.code)
                            put("initiatorCountryName", myCountry.name)
                            put("initiatorCountryFlag", myCountry.flag)

                            put("targetId", otherId)
                            put("targetName", otherName)
                            put("targetGender", otherGender)
                            put("targetCountryCode", otherCountryCode)
                            put("targetCountryName", otherCountryName)
                            put("targetCountryFlag", otherCountryFlag)

                            put("roomId", roomId)
                        }
                        scope.launch {
                            postHttp(LOBBY_TOPIC, matchPayload.toString())
                        }
                    }
                }
            } else if (event == "MATCH") {
                val initiatorId = payload.optString("initiatorId")
                val targetId = payload.optString("targetId")
                val roomId = payload.optString("roomId")

                if (myClientId == initiatorId && targetId != myClientId) {
                    // Match succeeded as initiator!
                    isSearching = false
                    stopLobbyWebSocket()

                    val partner = PartnerProfile(
                        id = targetId,
                        name = payload.optString("targetName", "Real Partner"),
                        age = 23,
                        country = Country(
                            code = payload.optString("targetCountryCode", "NP"),
                            name = payload.optString("targetCountryName", "Nepal"),
                            flag = payload.optString("targetCountryFlag", "🇳🇵")
                        ),
                        bio = "🟢 Real Online Mobile Device",
                        avatarBgColor = 0xFF1B5E20,
                        avatarEmoji = "📱",
                        videoTone = "neon",
                        greetings = listOf("Hi! Connected with you from my phone!"),
                        isRealDevice = true
                    )

                    onMatchedCallback?.invoke(partner, roomId)
                    joinRoom(roomId)

                } else if (myClientId == targetId && initiatorId != myClientId) {
                    // Match succeeded as target!
                    isSearching = false
                    stopLobbyWebSocket()

                    val partner = PartnerProfile(
                        id = initiatorId,
                        name = payload.optString("initiatorName", "Real Partner"),
                        age = 23,
                        country = Country(
                            code = payload.optString("initiatorCountryCode", "NP"),
                            name = payload.optString("initiatorCountryName", "Nepal"),
                            flag = payload.optString("initiatorCountryFlag", "🇳🇵")
                        ),
                        bio = "🟢 Real Online Mobile Device",
                        avatarBgColor = 0xFF1B5E20,
                        avatarEmoji = "📱",
                        videoTone = "neon",
                        greetings = listOf("Hello! Connected with you from my phone!"),
                        isRealDevice = true
                    )

                    onMatchedCallback?.invoke(partner, roomId)
                    joinRoom(roomId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleLobbyMessage error", e)
        }
    }

    fun stopSearching() {
        isSearching = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        stopLobbyWebSocket()
    }

    private fun stopLobbyWebSocket() {
        try {
            lobbyWebSocket?.close(1000, "Stop searching")
        } catch (e: Exception) {
            // ignore
        }
        lobbyWebSocket = null
    }

    /**
     * Join private paired room for real-time chat, typing, and emoji reactions.
     */
    fun joinRoom(roomId: String) {
        leaveCurrentRoom()
        currentRoomId = roomId

        val roomTopic = "vibetv_$roomId"
        val request = Request.Builder()
            .url("$NTFY_BASE_WS/$roomTopic/ws")
            .build()

        roomWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Joined room WS: $roomTopic")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleRoomMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Room WS failure: ${t.message}")
            }
        })
    }

    private fun handleRoomMessage(rawMessage: String) {
        try {
            val root = JSONObject(rawMessage)
            if (root.optString("event") != "message") return
            val payloadStr = root.optString("message")
            if (payloadStr.isBlank()) return

            val payload = JSONObject(payloadStr)
            val senderId = payload.optString("senderId")
            if (senderId == myClientId) return // Ignore own broadcast echoes

            when (payload.optString("type")) {
                "CHAT" -> {
                    val msgText = payload.optString("text")
                    val senderName = payload.optString("senderName", "Partner")
                    val chatMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}_${senderId.take(4)}",
                        senderIsMe = false,
                        senderName = senderName,
                        text = msgText,
                        timestamp = System.currentTimeMillis()
                    )
                    onChatMessageCallback?.invoke(chatMsg)
                }
                "TYPING" -> {
                    val isTyping = payload.optBoolean("isTyping", false)
                    onPartnerTypingCallback?.invoke(isTyping)
                }
                "REACTION" -> {
                    val emoji = payload.optString("emoji", "❤️")
                    onReactionCallback?.invoke(emoji)
                }
                "LEAVE" -> {
                    onPartnerLeftCallback?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleRoomMessage error", e)
        }
    }

    fun sendChatMessage(text: String, myName: String) {
        val roomId = currentRoomId ?: return
        val roomTopic = "vibetv_$roomId"
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT")
                    put("senderId", myClientId)
                    put("senderName", myName)
                    put("text", text)
                    put("timestamp", System.currentTimeMillis())
                }
                postHttp(roomTopic, json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "sendChatMessage error", e)
            }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        val roomId = currentRoomId ?: return
        val roomTopic = "vibetv_$roomId"
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "TYPING")
                    put("senderId", myClientId)
                    put("isTyping", isTyping)
                }
                postHttp(roomTopic, json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "sendTyping error", e)
            }
        }
    }

    fun sendReaction(emoji: String) {
        val roomId = currentRoomId ?: return
        val roomTopic = "vibetv_$roomId"
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "REACTION")
                    put("senderId", myClientId)
                    put("emoji", emoji)
                }
                postHttp(roomTopic, json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "sendReaction error", e)
            }
        }
    }

    fun leaveCurrentRoom() {
        val roomId = currentRoomId ?: return
        val roomTopic = "vibetv_$roomId"
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "LEAVE")
                    put("senderId", myClientId)
                }
                postHttp(roomTopic, json.toString())
            } catch (e: Exception) {
                // ignore
            }
        }
        try {
            roomWebSocket?.close(1000, "Leaving room")
        } catch (e: Exception) {
            // ignore
        }
        roomWebSocket = null
        currentRoomId = null
    }

    private fun postHttp(topic: String, jsonBody: String) {
        val request = Request.Builder()
            .url("$NTFY_BASE_HTTP/$topic")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(request).execute().close()
    }
}
