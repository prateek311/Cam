package com.drfin.timercam.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drfin.timercam.ui.theme.AccentAmber
import com.drfin.timercam.ui.theme.SurfaceScrim
import kotlin.math.roundToInt

private val EXPOSURE_TRACK_HEIGHT = 160.dp
private val EXPOSURE_TRACK_WIDTH = 40.dp

/** Amber square that briefly appears where the user tapped to focus, then fades. */
@Composable
fun FocusRing(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(2.dp, AccentAmber, RoundedCornerShape(8.dp)),
        )
    }
}

/**
 * Vertical drag slider for exposure ("light"), anchored near the last focus tap —
 * same interaction as tap-to-focus + drag-to-adjust-brightness in stock camera apps.
 * The exposure value it sets persists after the slider fades; this control is only
 * for the visible drag affordance.
 */
@Composable
fun ExposureSlider(
    exposureIndex: Int,
    range: ClosedRange<Int>,
    onChange: (Int) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val trackHeightPx = with(density) { EXPOSURE_TRACK_HEIGHT.toPx() }
    val span = (range.endInclusive - range.start).coerceAtLeast(1)

    fun indexToOffsetPx(index: Int): Float {
        val fraction = (index - range.start).toFloat() / span
        return (1f - fraction) * trackHeightPx
    }

    var thumbOffsetPx by remember(range) { mutableFloatStateOf(indexToOffsetPx(exposureIndex)) }
    LaunchedEffect(exposureIndex, range) { thumbOffsetPx = indexToOffsetPx(exposureIndex) }

    Box(
        modifier = modifier
            .size(width = EXPOSURE_TRACK_WIDTH, height = EXPOSURE_TRACK_HEIGHT)
            .background(SurfaceScrim, RoundedCornerShape(20.dp))
            .pointerInput(range) {
                detectVerticalDragGestures(
                    onDragStart = { onInteraction() },
                ) { change, dragAmount ->
                    change.consume()
                    thumbOffsetPx = (thumbOffsetPx + dragAmount).coerceIn(0f, trackHeightPx)
                    val fraction = 1f - (thumbOffsetPx / trackHeightPx)
                    val newIndex = (range.start + (fraction * span))
                        .roundToInt()
                        .coerceIn(range.start, range.endInclusive)
                    onChange(newIndex)
                    onInteraction()
                }
            },
    ) {
        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = "Adjust brightness",
            tint = AccentAmber,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopCenter)
                .offset {
                    val halfIconPx = with(density) { 12.dp.roundToPx() }
                    IntOffset(0, thumbOffsetPx.roundToInt() - halfIconPx)
                },
        )
    }
}
