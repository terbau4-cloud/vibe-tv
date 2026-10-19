package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Country
import com.example.model.Gender
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SearchingRadarView(
    selectedCountry: Country,
    selectedGender: Gender,
    onCancel: () -> Unit,
    onOpenCountryModal: () -> Unit = {},
    onConnectDemo: (() -> Unit)? = null,
    onConnectImmediate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) {
                        // Sliding left immediately connects to the user
                        (onConnectImmediate ?: onConnectDemo)?.invoke()
                    } else if (dragAmount > 20f) {
                        onCancel()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Radar waves canvas
        Canvas(modifier = Modifier.size(260.dp)) {
            val center = this.center

            listOf(pulse1, pulse2).forEach { p ->
                val radius = (size.minDimension / 2f) * p
                val alpha = (1f - p).coerceIn(0f, 0.8f)
                drawCircle(
                    color = Color(0xFFFF473A).copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF473A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TravelExplore,
                    contentDescription = "Searching",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Finding...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Looking for active users...",
                color = Color(0xFFB0B0C0),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Real status banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x55000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "2 Users Online • Connecting...",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Filter Pill in Radar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x66000000))
                    .clickable { onOpenCountryModal() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Filter: ${selectedCountry.flag} ${selectedCountry.name}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change country",
                    tint = Color(0xFFFF473A),
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2D38)),
                    modifier = Modifier.testTag("cancel_search_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop", color = Color.LightGray, fontSize = 13.sp)
                }

                Button(
                    onClick = { (onConnectImmediate ?: onConnectDemo)?.invoke() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF473A)),
                    modifier = Modifier.testTag("connect_now_button")
                ) {
                    Text("Connect Now • Slide Left", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
