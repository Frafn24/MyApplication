package com.example.myapplication.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** A swipe must start this close to the left edge. */
private val EdgeWidth = 24.dp

/** How far across the screen (as a fraction of its width) the swipe must go to count as "back". */
private const val BACK_FRACTION = 1f / 3f

private val IndicatorSize = 40.dp

/**
 * Swiping from the left edge towards the middle goes back, like the phone's back
 * button. Phones with gesture navigation already do this themselves, but phones
 * with 3-button navigation do not, so the app handles it too.
 *
 * While dragging, a back arrow follows the finger; it turns dark once releasing
 * would go back. Taps and vertical scrolling that start at the edge still reach
 * the screen below.
 */
@Composable
fun EdgeSwipeBack(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentOnBack by rememberUpdatedState(onBack)
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val edgePx = with(LocalDensity.current) { EdgeWidth.toPx() }
    val indicatorPx = with(LocalDensity.current) { IndicatorSize.toPx() }

    val swipeModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                // Initial pass: see the touch before the screen's buttons and lists do.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x > edgePx) return@awaitEachGesture

                val slop = viewConfiguration.touchSlop
                var dx = 0f
                var dy = 0f
                var isSwipe = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (isSwipe && dx >= size.width * BACK_FRACTION) currentOnBack()
                            break
                        }
                        val delta = change.positionChange()
                        dx += delta.x
                        dy += delta.y
                        if (!isSwipe) {
                            // Mostly vertical: a scroll, leave it to the screen.
                            if (abs(dy) > slop && abs(dy) > abs(dx)) break
                            if (dx > slop) isSwipe = true
                        }
                        if (isSwipe) {
                            // Keep the screen's buttons from also reacting to this touch.
                            change.consume()
                            dragging = true
                            dragX = dx.coerceAtLeast(0f)
                            dragY = change.position.y
                            progress = (dx / (size.width * BACK_FRACTION)).coerceIn(0f, 1f)
                        }
                    }
                } finally {
                    dragging = false
                    progress = 0f
                }
            }
        }
    }

    Box(modifier.then(swipeModifier)) {
        content()
        if (dragging) {
            val ready = progress >= 1f
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragX / 2 - indicatorPx / 2).coerceAtMost(indicatorPx).roundToInt(),
                                y = (dragY - indicatorPx / 2).roundToInt()
                            )
                        }
                        .size(IndicatorSize)
                        .alpha(0.4f + 0.6f * progress)
                        .clip(CircleShape)
                        .background(if (ready) Color(0xFF111827) else Color(0xFFDCE0E6))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = if (ready) Color.White else Color(0xFF111827)
                    )
                }
            }
        }
    }
}
