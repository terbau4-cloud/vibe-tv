package com.example.network

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.Country
import com.example.model.Gender
import com.example.model.PartnerProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
 * Driven directly by Firebase Firestore for cross-device synchronization with zero fake bots.
 */
object RealtimeMatchmaker {

    private const val TAG = "RealtimeMatchmaker"
    private const val LOBBY_TOPIC = "vibetv_global_match_lobby"
    private const val NTFY_BASE_HTTP = "https://ntfy.sh"
    private const val NTFY_BASE_WS = "wss://ntfy.sh"

    var myClientId: String = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Exception) {
        null
    } ?: ("dev_" + UUID.randomUUID().toString().replace("-", "").take(8))

    private val db: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance("ai-studio-android-vibetv-82088118-dd78-4b2b-b4e0-b8bc7a991965")
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lobbyWebSocket: WebSocket? = null
    private var roomWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var firestoreHeartbeatJob: Job? = null

    private var queueListener: ListenerRegistration? = null
    private var myDocListener: ListenerRegistration? = null
    private var roomDocListener: ListenerRegistration? = null
    private var roomMessagesListener: ListenerRegistration? = null
    private var roomSignalsListener: ListenerRegistration? = null

    private var isSearching = false
    private var currentRoomId: String? = null

    var onMatchedCallback: ((PartnerProfile, String) -> Unit)? = null
    var onChatMessageCallback: ((ChatMessage) -> Unit)? = null
    var onPartnerTypingCallback: ((Boolean) -> Unit)? = null
    var onReactionCallback: ((String) -> Unit)? = null
    var onPartnerLeftCallback: (() -> Unit)? = null
    var onRealOnlineCountChanged: ((Int) -> Unit)? = null

    fun updateClientId(newId: String) {
        if (newId.isNotBlank()) {
            myClientId = newId
        }
    }

    /**
     * Start broadcasting presence to the Firestore matchmaking queue and global lobby.
     * When another real physical device broadcasts, both will match and connect automatically.
     */
    fun startSearching(
        userName: String,
        gender: Gender,
        country: Country
    ) {
        stopSearching()
        isSearching = true

        val currentAuthUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
        if (currentAuthUid != null) {
            myClientId = currentAuthUid
        }

        // 1. Register presence in Firestore matchmaking_queue
        val myData = hashMapOf(
            "userId" to myClientId,
            "userName" to userName,
            "gender" to gender.name,
            "countryCode" to country.code,
            "countryName" to country.name,
            "countryFlag" to country.flag,
            "status" to "WAITING",
            "matchedRoomId" to null,
            "matchedWith" to null,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            db.collection("matchmaking_queue").document(myClientId).set(myData)
                .addOnSuccessListener {
                    Log.d(TAG, "Joined matchmaking_queue in Firestore: $myClientId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing to matchmaking_queue", e)
                }

            // Periodic Firestore heartbeat so other devices know we are genuinely live
            firestoreHeartbeatJob = scope.launch {
                while (isActive && isSearching) {
                    delay(4000)
                    if (isSearching) {
                        try {
                            db.collection("matchmaking_queue").document(myClientId)
                                .update("updatedAt", System.currentTimeMillis())
                        } catch (e: Exception) {
                            Log.w(TAG, "Heartbeat notice: ${e.message}")
                        }
                    }
                }
            }

            // Listen to real waiting count and find real partners in queue
            queueListener = db.collection("matchmaking_queue")
                .whereEqualTo("status", "WAITING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Queue listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (!isSearching || snapshot == null) return@addSnapshotListener

                    val docs = snapshot.documents
                    val now = System.currentTimeMillis()

                    // Prune stale queue entries (older than 35s) from previously disconnected devices
                    for (doc in docs) {
                        val updated = doc.getLong("updatedAt") ?: 0L
                        if (now - updated > 35_000L && doc.id != myClientId) {
                            try {
                                db.collection("matchmaking_queue").document(doc.id).delete()
                            } catch (e: Exception) {}
                        }
                    }

                    // Only consider devices active within the last 25 seconds
                    val activeDocs = docs.filter { doc ->
                        val updated = doc.getLong("updatedAt") ?: 0L
                        (now - updated) < 25_000L
                    }

                    val realCount = activeDocs.size
                    onRealOnlineCountChanged?.invoke(realCount.coerceAtLeast(1))

                    // Look for another real waiting user
                    val otherDoc = activeDocs.firstOrNull { it.id != myClientId }
                    if (otherDoc != null && myClientId < otherDoc.id) {
                        // Deterministic initiator: pair both users!
                        val otherId = otherDoc.id
                        val otherName = otherDoc.getString("userName") ?: "Real Partner"
                        val otherGender = otherDoc.getString("gender") ?: "MALE"
                        val otherCountryCode = otherDoc.getString("countryCode") ?: "NP"
                        val otherCountryName = otherDoc.getString("countryName") ?: "Nepal"
                        val otherCountryFlag = otherDoc.getString("countryFlag") ?: "🇳🇵"

                        val roomId = "room_${myClientId}_${otherId}"
                        createMatchedRoomInFirestore(
                            roomId = roomId,
                            initiatorId = myClientId,
                            initiatorName = userName,
                            initiatorGender = gender.name,
                            initiatorCountry = country,
                            targetId = otherId,
                            targetName = otherName,
                            targetGender = otherGender,
                            targetCountryCode = otherCountryCode,
                            targetCountryName = otherCountryName,
                            targetCountryFlag = otherCountryFlag
                        )
                    }
                }

            // Listen to own document to know when matched as target
            myDocListener = db.collection("matchmaking_queue").document(myClientId)
                .addSnapshotListener { doc, error ->
                    if (error != null || !isSearching || doc == null || !doc.exists()) return@addSnapshotListener

                    val status = doc.getString("status")
                    if (status == "MATCHED") {
                        val roomId = doc.getString("matchedRoomId")
                        val partnerId = doc.getString("matchedWith")
                        val partnerName = doc.getString("partnerName") ?: "Real Partner"
                        val partnerCountryCode = doc.getString("partnerCountryCode") ?: "NP"
                        val partnerCountryName = doc.getString("partnerCountryName") ?: "Nepal"
                        val partnerCountryFlag = doc.getString("partnerCountryFlag") ?: "🇳🇵"

                        if (roomId != null && partnerId != null) {
                            Log.d(TAG, "Matched via Firestore! Room: $roomId with $partnerId")
                            isSearching = false
                            stopSearchingKeepRoom()

                            val partner = PartnerProfile(
                                id = partnerId,
                                name = partnerName,
                                age = 23,
                                country = Country(
                                    code = partnerCountryCode,
                                    name = partnerCountryName,
                                    flag = partnerCountryFlag
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
                }

        } catch (e: Exception) {
            Log.e(TAG, "Firestore search error", e)
        }

        // 2. Auxiliary real-time WebSocket accelerator
        startWebSocketLobby(userName, gender, country)
    }

    private fun createMatchedRoomInFirestore(
        roomId: String,
        initiatorId: String,
        initiatorName: String,
        initiatorGender: String,
        initiatorCountry: Country,
        targetId: String,
        targetName: String,
        targetGender: String,
        targetCountryCode: String,
        targetCountryName: String,
        targetCountryFlag: String
    ) {
        val roomDoc = hashMapOf(
            "roomId" to roomId,
            "initiatorId" to initiatorId,
            "initiatorName" to initiatorName,
            "targetId" to targetId,
            "targetName" to targetName,
            "status" to "ACTIVE",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("rooms").document(roomId).set(roomDoc)
            .addOnSuccessListener {
                // Update target user's queue entry
                db.collection("matchmaking_queue").document(targetId).update(
                    mapOf(
                        "status" to "MATCHED",
                        "matchedRoomId" to roomId,
                        "matchedWith" to initiatorId,
                        "partnerName" to initiatorName,
                        "partnerCountryCode" to initiatorCountry.code,
                        "partnerCountryName" to initiatorCountry.name,
                        "partnerCountryFlag" to initiatorCountry.flag
                    )
                )

                // Immediately connect initiator directly without waiting
                if (isSearching) {
                    isSearching = false
                    stopSearchingKeepRoom()

                    val partner = PartnerProfile(
                        id = targetId,
                        name = targetName,
                        age = 23,
                        country = Country(
                            code = targetCountryCode,
                            name = targetCountryName,
                            flag = targetCountryFlag
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
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create matched room", e)
            }
    }

    private fun startWebSocketLobby(
        userName: String,
        gender: Gender,
        country: Country
    ) {
        val request = Request.Builder()
            .url("$NTFY_BASE_WS/$LOBBY_TOPIC/ws")
            .build()

        lobbyWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                broadcastSearch(userName, gender, country)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLobbyMessage(text, userName, gender, country)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Lobby WebSocket notice: ${t.message}")
            }
        })

        heartbeatJob = scope.launch {
            while (isActive && isSearching) {
                delay(3000)
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
                // Ignore network blips
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
                    val otherName = payload.optString("userName", "Real User")
                    val otherGender = payload.optString("gender", "MALE")
                    val otherCountryCode = payload.optString("countryCode", "NP")
                    val otherCountryName = payload.optString("countryName", "Nepal")
                    val otherCountryFlag = payload.optString("countryFlag", "🇳🇵")

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
                    isSearching = false
                    stopSearchingKeepRoom()

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
                    isSearching = false
                    stopSearchingKeepRoom()

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

    private fun stopSearchingKeepRoom() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        firestoreHeartbeatJob?.cancel()
        firestoreHeartbeatJob = null
        queueListener?.remove()
        queueListener = null
        myDocListener?.remove()
        myDocListener = null
        try {
            lobbyWebSocket?.close(1000, "Matched")
        } catch (e: Exception) {}
        lobbyWebSocket = null

        // Remove from queue
        try {
            db.collection("matchmaking_queue").document(myClientId).delete()
        } catch (e: Exception) {}
    }

    fun stopSearching() {
        isSearching = false
        stopSearchingKeepRoom()
    }

    /**
     * Join private paired room for real-time chat, typing, and emoji reactions.
     * Uses Firestore snapshot listeners as authoritative cross-device state channel.
     */
    fun joinRoom(roomId: String) {
        leaveCurrentRoom()
        currentRoomId = roomId

        // 1. Listen to Room document (for partner disconnect / state changes)
        try {
            roomDocListener = db.collection("rooms").document(roomId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val status = snapshot.getString("status")
                    if (status == "ENDED") {
                        Log.d(TAG, "Room status changed to ENDED by partner.")
                        onPartnerLeftCallback?.invoke()
                    }
                }

            // 2. Listen to Messages subcollection in real time
            roomMessagesListener = db.collection("rooms").document(roomId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    for (docChange in snapshot.documentChanges) {
                        if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val doc = docChange.document
                            val senderId = doc.getString("senderId")
                            if (senderId != null && senderId != myClientId) {
                                val text = doc.getString("text") ?: ""
                                val senderName = doc.getString("senderName") ?: "Partner"
                                val chatMsg = ChatMessage(
                                    id = doc.id,
                                    senderIsMe = false,
                                    senderName = senderName,
                                    text = text,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                                onChatMessageCallback?.invoke(chatMsg)
                            }
                        }
                    }
                }

            // 3. Listen to Signals (reactions, typing indicators)
            roomSignalsListener = db.collection("rooms").document(roomId)
                .collection("signals").document("events")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val senderId = snapshot.getString("senderId")
                    if (senderId != null && senderId != myClientId) {
                        val reaction = snapshot.getString("reaction")
                        if (!reaction.isNullOrBlank()) {
                            onReactionCallback?.invoke(reaction)
                        }
                        val typing = snapshot.getBoolean("typing")
                        if (typing != null) {
                            onPartnerTypingCallback?.invoke(typing)
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "joinRoom Firestore error", e)
        }

        // Auxiliary WebSocket connection
        val roomTopic = "vibetv_$roomId"
        val request = Request.Builder()
            .url("$NTFY_BASE_WS/$roomTopic/ws")
            .build()

        roomWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleRoomMessage(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Room WS notice: ${t.message}")
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
            if (senderId == myClientId) return

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
                    val emoji = payload.optString("emoji")
                    if (emoji.isNotBlank()) {
                        onReactionCallback?.invoke(emoji)
                    }
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

        // 1. Write to Firestore
        try {
            val msgDoc = hashMapOf(
                "senderId" to myClientId,
                "senderName" to myName,
                "text" to text,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("rooms").document(roomId).collection("messages").add(msgDoc)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sendChatMessage error", e)
        }

        // 2. Auxiliary WebSocket broadcast
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT")
                    put("senderId", myClientId)
                    put("senderName", myName)
                    put("text", text)
                }
                postHttp("vibetv_$roomId", json.toString())
            } catch (e: Exception) {}
        }
    }

    fun sendTyping(isTyping: Boolean) {
        val roomId = currentRoomId ?: return

        try {
            db.collection("rooms").document(roomId).collection("signals").document("events")
                .set(mapOf("senderId" to myClientId, "typing" to isTyping, "timestamp" to System.currentTimeMillis()))
        } catch (e: Exception) {}

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "TYPING")
                    put("senderId", myClientId)
                    put("isTyping", isTyping)
                }
                postHttp("vibetv_$roomId", json.toString())
            } catch (e: Exception) {}
        }
    }

    fun sendReaction(emoji: String) {
        val roomId = currentRoomId ?: return

        try {
            db.collection("rooms").document(roomId).collection("signals").document("events")
                .set(mapOf("senderId" to myClientId, "reaction" to emoji, "timestamp" to System.currentTimeMillis()))
        } catch (e: Exception) {}

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "REACTION")
                    put("senderId", myClientId)
                    put("emoji", emoji)
                }
                postHttp("vibetv_$roomId", json.toString())
            } catch (e: Exception) {}
        }
    }

    fun leaveCurrentRoom() {
        val roomId = currentRoomId ?: return
        currentRoomId = null

        // 1. Mark room as ENDED in Firestore
        try {
            db.collection("rooms").document(roomId).update("status", "ENDED")
        } catch (e: Exception) {}

        roomDocListener?.remove()
        roomDocListener = null
        roomMessagesListener?.remove()
        roomMessagesListener = null
        roomSignalsListener?.remove()
        roomSignalsListener = null

        // 2. Notify over WebSocket
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "LEAVE")
                    put("senderId", myClientId)
                }
                postHttp("vibetv_$roomId", json.toString())
            } catch (e: Exception) {}
        }

        try {
            roomWebSocket?.close(1000, "Left room")
        } catch (e: Exception) {}
        roomWebSocket = null
    }

    private fun postHttp(topic: String, body: String) {
        try {
            val request = Request.Builder()
                .url("$NTFY_BASE_HTTP/$topic")
                .post(body.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            // Silently fail if offline
        }
    }
}
