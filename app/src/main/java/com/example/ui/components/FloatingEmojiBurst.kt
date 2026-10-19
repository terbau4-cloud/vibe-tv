package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloatingReaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FloatingEmojiBurstContainer(
    reactions: List<FloatingReaction>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        reactions.forEach { reaction ->
            FloatingSingleEmoji(
                reaction = reaction,
                containerWidth = widthPx,
                containerHeight = heightPx
            )
        }
    }
}

@Composable
private fun FloatingSingleEmoji(
    reaction: FloatingReaction,
    containerWidth: Float,
    containerHeight: Float
) {
    val progress = remember { Animatable(0f) }
    val horizontalWiggle = remember { Random.nextFloat() * 60f - 30f }
    val wobbleFrequency = remember { 2.5f + Random.nextFloat() * 2f }
    val rotationOffset = remember { reaction.rotationAngle + (Random.nextFloat() * 30f - 15f) }

    LaunchedEffect(reaction.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2400,
                easing = LinearEasing
            )
        )
    }

    val currentProgress = progress.value
    if (currentProgress < 1f) {
        // Rises from startYRatio up toward top
        val startYPx = containerHeight * reaction.startYRatio
        val yOffset = startYPx - (currentProgress * (startYPx + 80f))
        val xOffset = (containerWidth * reaction.startXRatio) +
                (sin(currentProgress * Math.PI.toFloat() * wobbleFrequency) * horizontalWiggle)

        // Scale pops from 0.3 to 1.25 then settles
        val scale = if (currentProgress < 0.15f) {
            0.4f + (currentProgress / 0.15f) * 0.85f
        } else {
            1.25f - ((currentProgress - 0.15f) * 0.25f)
        }

        // Alpha fade out near top
        val alpha = if (currentProgress < 0.1f) {
            currentProgress / 0.1f
        } else if (currentProgress > 0.65f) {
            (1f - currentProgress) / 0.35f
        } else {
            1f
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = xOffset.toInt(),
                        y = yOffset.toInt()
                    )
                }
                .alpha(alpha.coerceIn(0f, 1f))
                .scale(scale)
                .rotate(rotationOffset * currentProgress)
        ) {
            Text(
                text = reaction.emoji,
                fontSize = reaction.sizeSp.sp
            )
        }
    }
}

@Composable
fun InteractiveReactionRow(
    onReactionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Exactly 5 emojis at the bottom per user request: "tala 5 ota emoji matra rakha"
    val emojis = listOf(
        "❤️" to "Heart",
        "👍" to "Thumbs Up",
        "😂" to "Laugh",
        "🔥" to "Fire",
        "😮" to "Surprised"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { (emoji, label) ->
            ReactionButton(
                emoji = emoji,
                contentDescription = label,
                onTap = { onReactionTap(emoji) }
            )
        }
    }
}

@Composable
fun ReactionButton(
    emoji: String,
    contentDescription: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0x55000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    isPressed = true
                    delay(120)
                    isPressed = false
                }
                onTap()
            }
            .padding(2.dp)
            .testTag("reaction_button_$emoji"),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

/**
 * Full-screen animated burst when emoji is tapped:
 * "ani emoji ma click garda purai screen ma aaos ekxin ko lagi"
 */
@Composable
fun FullScreenEmojiOverlay(
    emoji: String?,
    modifier: Modifier = Modifier
) {
    if (emoji == null) return

    val scaleProgress = remember(emoji) { Animatable(0.2f) }
    val alphaProgress = remember(emoji) { Animatable(1f) }

    LaunchedEffect(emoji) {
        scaleProgress.animateTo(
            targetValue = 2.4f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(emoji) {
        delay(900)
        alphaProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 700, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Center giant pulsing burst
        Box(
            modifier = Modifier
                .scale(scaleProgress.value)
                .alpha(alphaProgress.value),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 110.sp
            )
        }
    }
}
