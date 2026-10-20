package com.example.network

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import org.webrtc.PeerConnection.*

/**
 * Robust WebRTC Manager for Real-Time Peer-to-Peer Video & Audio.
 * Directly streams the live hardware camera between the 2 phones with zero latency.
 */
object WebRtcManager {

    private const val TAG = "WebRtcManager"

    val eglBase: EglBase by lazy { EglBase.create() }

    private var isInitialized = false
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var currentRoomId: String? = null
    private var offerListener: ListenerRegistration? = null
    private var answerListener: ListenerRegistration? = null
    private var candidatesListener: ListenerRegistration? = null

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _isRemoteCameraStreaming = MutableStateFlow(false)
    val isRemoteCameraStreaming: StateFlow<Boolean> = _isRemoteCameraStreaming.asStateFlow()

    private val _isLocalCameraStreaming = MutableStateFlow(false)
    val isLocalCameraStreaming: StateFlow<Boolean> = _isLocalCameraStreaming.asStateFlow()

    private var currentCameraIsFront = true
    private var isCameraOn = true

    private fun getDb(context: Context): FirebaseFirestore {
        return try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val iceServers = listOf(
        IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()

            isInitialized = true
            Log.d(TAG, "WebRTC PeerConnectionFactory initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebRTC factory", e)
        }
    }

    fun startLocalCamera(context: Context, isFront: Boolean = true) {
        initialize(context)
        currentCameraIsFront = isFront

        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            Log.w(TAG, "Device has no camera hardware.")
            return
        }

        val factory = peerConnectionFactory ?: return
        if (videoCapturer != null) return

        try {
            val enumerator = Camera2Enumerator(context)
            val deviceNames = enumerator.deviceNames
            var selectedCamera: String? = null

            for (name in deviceNames) {
                if (if (isFront) enumerator.isFrontFacing(name) else enumerator.isBackFacing(name)) {
                    selectedCamera = name
                    break
                }
            }
            if (selectedCamera == null) {
                selectedCamera = deviceNames.firstOrNull()
            }

            if (selectedCamera != null) {
                val capturer = enumerator.createCapturer(selectedCamera, object : CameraVideoCapturer.CameraEventsHandler {
                    override fun onCameraError(p0: String?) { Log.e(TAG, "Camera error: $p0") }
                    override fun onCameraDisconnected() { _isLocalCameraStreaming.value = false }
                    override fun onCameraFreezed(p0: String?) {}
                    override fun onCameraOpening(p0: String?) {}
                    override fun onFirstFrameAvailable() { _isLocalCameraStreaming.value = true }
                    override fun onCameraClosed() { _isLocalCameraStreaming.value = false }
                })

                val surfaceTextureHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", eglBase.eglBaseContext)
                val videoSource = factory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
                capturer.startCapture(480, 640, 30)

                videoCapturer = capturer
                localVideoSource = videoSource
                localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
                localVideoTrack?.setEnabled(isCameraOn)

                Log.d(TAG, "Local camera started: $selectedCamera")
            }

            // Create Audio Track
            if (localAudioTrack == null) {
                val audioSource = factory.createAudioSource(MediaConstraints())
                localAudioSource = audioSource
                localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
            }

        } catch (e: Exception) {
            Log.e(TAG, "startLocalCamera error", e)
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        isCameraOn = enabled
        localVideoTrack?.setEnabled(enabled)
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                currentCameraIsFront = isFront
                Log.d(TAG, "Camera switched. IsFront: $isFront")
            }
            override fun onCameraSwitchError(p0: String?) {
                Log.e(TAG, "Camera switch error: $p0")
            }
        })
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(currentCameraIsFront)
            localVideoTrack?.addSink(renderer)
        } catch (e: Exception) {
            Log.w(TAG, "attachLocalRenderer error", e)
        }
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(false)
            _remoteVideoTrack.value?.addSink(renderer)
        } catch (e: Exception) {
            Log.w(TAG, "attachRemoteRenderer error", e)
        }
    }

    fun startCall(
        context: Context,
        roomId: String,
        isInitiator: Boolean,
        myClientId: String
    ) {
        endCall(context)
        currentRoomId = roomId

        initialize(context)
        startLocalCamera(context, currentCameraIsFront)

        val factory = peerConnectionFactory ?: return
        val db = getDb(context)

        val rtcConfig = RTCConfiguration(iceServers).apply {
            sdpSemantics = SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: SignalingState?) {}
            override fun onIceConnectionChange(state: IceConnectionState?) {
                Log.d(TAG, "IceConnectionState changed: $state")
                if (state == IceConnectionState.CONNECTED || state == IceConnectionState.COMPLETED) {
                    _isRemoteCameraStreaming.value = true
                } else if (state == IceConnectionState.DISCONNECTED || state == IceConnectionState.FAILED || state == IceConnectionState.CLOSED) {
                    _isRemoteCameraStreaming.value = false
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) return
                val candidateMap = hashMapOf(
                    "candidate" to candidate.sdp,
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "senderId" to myClientId,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("rooms").document(roomId).collection("candidates")
                    .add(candidateMap)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                val videoTrack = stream?.videoTracks?.firstOrNull()
                if (videoTrack != null) {
                    _remoteVideoTrack.value = videoTrack
                    _isRemoteCameraStreaming.value = true
                    Log.d(TAG, "Remote video track received via onAddStream")
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                _remoteVideoTrack.value = null
                _isRemoteCameraStreaming.value = false
            }

            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() as? VideoTrack
                if (track != null) {
                    _remoteVideoTrack.value = track
                    _isRemoteCameraStreaming.value = true
                    Log.d(TAG, "Remote video track received via onAddTrack")
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track() as? VideoTrack
                if (track != null) {
                    _remoteVideoTrack.value = track
                    _isRemoteCameraStreaming.value = true
                    Log.d(TAG, "Remote video track received via onTrack")
                }
            }
        }

        val pc = factory.createPeerConnection(rtcConfig, observer)
        peerConnection = pc

        localVideoTrack?.let { pc?.addTrack(it, listOf("ARDAMS")) }
        localAudioTrack?.let { pc?.addTrack(it, listOf("ARDAMS")) }

        // Signaling Offer & Answer via Firestore
        if (isInitiator) {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }

            pc?.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    if (desc == null) return
                    pc.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            val offerMap = hashMapOf(
                                "type" to "offer",
                                "sdp" to desc.description,
                                "senderId" to myClientId,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("rooms").document(roomId).collection("signals").document("offer")
                                .set(offerMap)
                                .addOnSuccessListener { Log.d(TAG, "Offer saved to Firestore.") }
                        }
                    }, desc)
                }
            }, constraints)

            // Listen for Answer
            answerListener = db.collection("rooms").document(roomId).collection("signals").document("answer")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val senderId = snapshot.getString("senderId")
                    if (senderId != myClientId) {
                        val sdp = snapshot.getString("sdp")
                        if (!sdp.isNullOrBlank()) {
                            val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                            pc?.setRemoteDescription(SimpleSdpObserver(), answerDesc)
                            Log.d(TAG, "Set remote Answer description.")
                        }
                    }
                }

        } else {
            // Target: Listen for Offer
            offerListener = db.collection("rooms").document(roomId).collection("signals").document("offer")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val senderId = snapshot.getString("senderId")
                    if (senderId != myClientId) {
                        val sdp = snapshot.getString("sdp")
                        if (!sdp.isNullOrBlank()) {
                            val offerDesc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                            pc?.setRemoteDescription(object : SimpleSdpObserver() {
                                override fun onSetSuccess() {
                                    val constraints = MediaConstraints().apply {
                                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                                    }
                                    pc.createAnswer(object : SimpleSdpObserver() {
                                        override fun onCreateSuccess(answerDesc: SessionDescription?) {
                                            if (answerDesc == null) return
                                            pc.setLocalDescription(object : SimpleSdpObserver() {
                                                override fun onSetSuccess() {
                                                    val answerMap = hashMapOf(
                                                        "type" to "answer",
                                                        "sdp" to answerDesc.description,
                                                        "senderId" to myClientId,
                                                        "timestamp" to System.currentTimeMillis()
                                                    )
                                                    db.collection("rooms").document(roomId).collection("signals").document("answer")
                                                        .set(answerMap)
                                                        .addOnSuccessListener { Log.d(TAG, "Answer saved to Firestore.") }
                                                }
                                            }, answerDesc)
                                        }
                                    }, constraints)
                                }
                            }, offerDesc)
                        }
                    }
                }
        }

        // Listen for Remote ICE Candidates
        candidatesListener = db.collection("rooms").document(roomId).collection("candidates")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val senderId = doc.getString("senderId")
                        if (senderId != myClientId) {
                            val sdp = doc.getString("candidate")
                            val sdpMid = doc.getString("sdpMid")
                            val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: 0
                            if (!sdp.isNullOrBlank()) {
                                val ice = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                                pc?.addIceCandidate(ice)
                            }
                        }
                    }
                }
            }
    }

    fun endCall(context: Context? = null) {
        offerListener?.remove()
        offerListener = null
        answerListener?.remove()
        answerListener = null
        candidatesListener?.remove()
        candidatesListener = null

        try {
            peerConnection?.close()
        } catch (e: Exception) {}
        peerConnection = null

        _remoteVideoTrack.value = null
        _isRemoteCameraStreaming.value = false
        currentRoomId = null
    }

    fun release() {
        endCall()
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {}
        videoCapturer = null

        try {
            localVideoTrack?.dispose()
            localVideoSource?.dispose()
            localAudioTrack?.dispose()
            localAudioSource?.dispose()
            peerConnectionFactory?.dispose()
        } catch (e: Exception) {}

        peerConnectionFactory = null
        isInitialized = false
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(reason: String?) { Log.e(TAG, "SDP create failure: $reason") }
        override fun onSetFailure(reason: String?) { Log.e(TAG, "SDP set failure: $reason") }
    }
}
