package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Country
import com.example.model.PartnerProfile
import kotlinx.coroutines.delay

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
    modifier: Modifier = Modifier
) {
    var callSeconds by remember(partner.id) { mutableIntStateOf(0) }
    LaunchedEffect(partner.id) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "webcam_motion")
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val speakingPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingPulse"
    )

    val backgroundGradient = remember(partner.videoTone) {
        when (partner.videoTone) {
            "neon" -> Brush.verticalGradient(listOf(Color(0xFF2A164D), Color(0xFF140D26)))
            "sunset" -> Brush.verticalGradient(listOf(Color(0xFF4A2810), Color(0xFF1C0F08)))
            "warm" -> Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFF1A120B)))
            "cozy" -> Brush.verticalGradient(listOf(Color(0xFF1A2A3A), Color(0xFF0D161F)))
            "soft" -> Brush.verticalGradient(listOf(Color(0xFF421D37), Color(0xFF1C0A17)))
            else -> Brush.verticalGradient(listOf(Color(0xFF1E242B), Color(0xFF0F1216)))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .testTag("partner_video_feed")
            .pointerInput(partner.id) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -25f) {
                        onSkip()
                    }
                }
            }
    ) {
        // Room lighting backdrop & webcam simulation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(partner.avatarBgColor).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height * 0.25f),
                    radius = width * 0.65f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33FFFFFF), Color.Transparent),
                    center = Offset(width * 0.25f, height * 0.8f),
                    radius = width * 0.5f
                )
            )

            if (isVoiceSpeaking) {
                drawCircle(
                    color = Color(0xFFFF473A).copy(alpha = 0.3f),
                    radius = 90.dp.toPx() * speakingPulse,
                    center = Offset(width / 2f, height / 2f - 20.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Live partner avatar presentation
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(partner.avatarBgColor))
                    .border(
                        width = if (isVoiceSpeaking) 3.dp else 1.dp,
                        color = if (isVoiceSpeaking) Color(0xFFFF473A) else Color(0x44FFFFFF),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partner.avatarEmoji,
                        fontSize = 50.sp,
                        modifier = Modifier
                            .offset(y = breathingOffset.dp)
                            .scale(if (isVoiceSpeaking) speakingPulse else 1f)
                    )
                }

                if (isVoiceSpeaking) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF473A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Speaking",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Partner name, age & country badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = partner.country.flag,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${partner.name}, ${partner.age}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${partner.country.name}",
                    color = Color(0xFFD0D0D8),
                    fontSize = 12.sp
                )
            }
        }

        // Top bar overlay (Connection status, duration, ACTIVE FILTER BADGE, audio toggle, report button)
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
                        .background(Color(0xFF00E676))
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
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
            }

            // ACTIVE COUNTRY FILTER BADGE IN THE VIDEO STREAM!
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

        // Live speech bubble or typing indicator
        AnimatedVisibility(
            visible = speechBubble != null || isTyping,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp, end = 120.dp)
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


        // Next partner button
        Button(
            onClick = onSkip,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF473A)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .testTag("skip_partner_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
