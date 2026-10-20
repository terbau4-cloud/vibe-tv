package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.Country
import com.example.model.PartnerProfile
import com.example.network.WebRtcManager
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

/**
 * Top half feed: displays the opponent's genuine live WebRTC camera stream.
 * Center is completely clean (avatar, big name, and "Connected with" removed).
 * Country and flag are shown at the bottom ("ali tala dekhau").
 */
@Composable
fun PartnerVideoFeed(
    partner: PartnerProfile,
    selectedCountry: Country,
    speechBubble: String?,
    isTyping: Boolean,
    isVoiceSpeaking: Boolean,
    isVoiceMuted: Boolean,
    onToggleVoiceMute: () -> Unit,
    onOpenCountryModal: () -> Unit,
    onResetCountryFilter: () -> Unit,
    onSendReaction: (String) -> Unit,
    onSkip: () -> Unit,
    onReport: () -> Unit,
    onBackToStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var callSeconds by remember(partner.id) { mutableIntStateOf(0) }
    LaunchedEffect(partner.id) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val remoteVideoTrack by WebRtcManager.remoteVideoTrack.collectAsState()
    val isRemoteStreaming by WebRtcManager.isRemoteCameraStreaming.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("partner_video_feed")
            .pointerInput(partner.id) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) {
                        onSkip()
                    } else if (dragAmount > 20f) {
                        onBackToStart()
                    }
                }
            }
    ) {
        // Opponent's Real Live Camera View
        if (remoteVideoTrack != null && isRemoteStreaming) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("remote_partner_video_renderer"),
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        WebRtcManager.attachRemoteRenderer(this)
                    }
                }
            )
        } else {
            // When camera is off or connecting, show pure black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("remote_partner_black_screen")
            )
        }

        // Top bar overlay (call duration, HD badge, active filter pill, voice audio, report)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live indicator & call duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRemoteStreaming) Color(0xFF00E676) else Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(6.dp))
                val minutes = callSeconds / 60
                val seconds = callSeconds % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.SignalCellularAlt,
                    contentDescription = "HD Quality",
                    tint = if (isRemoteStreaming) Color(0xFF00E676) else Color(0xFFFFB300),
                    modifier = Modifier.size(14.dp)
                )
            }

            // ACTIVE COUNTRY FILTER BADGE IN THE VIDEO STREAM
            ActiveFilterBadge(
                selectedCountry = selectedCountry,
                onOpenFilterModal = onOpenCountryModal,
                onResetToAll = onResetCountryFilter
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speaker Audio Voice toggle
                IconButton(
                    onClick = onToggleVoiceMute,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x77000000))
                        .testTag("toggle_voice_audio_button")
                ) {
                    Icon(
                        imageVector = if (!isVoiceMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Voice Audio",
                        tint = if (!isVoiceMuted) Color(0xFF00E676) else Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Report button
                IconButton(
                    onClick = onReport,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x77000000))
                        .testTag("report_partner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Report Partner",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // ==================== BOTTOM: COUNTRY SHOWN DOWN HERE ("ali tala dekhau") ====================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${partner.country.flag} ${partner.country.name}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        // Live speech bubble or typing indicator (shown above the country badge if active)
        AnimatedVisibility(
            visible = !speechBubble.isNullOrBlank() || isTyping,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 48.dp, end = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 2.dp
                        )
                    )
                    .background(Color(0xEE1E1E26))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isTyping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8A80))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "typing...",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isVoiceSpeaking) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFFFF473A),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = speechBubble ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
