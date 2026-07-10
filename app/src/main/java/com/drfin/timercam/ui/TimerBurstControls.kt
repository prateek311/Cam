package com.drfin.timercam.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drfin.timercam.camera.ShutterMode
import com.drfin.timercam.ui.theme.SurfaceScrim

val TIMER_OPTIONS = listOf(3, 5, 10)
val SHOT_COUNT_OPTIONS = listOf(3, 5, 10, 15, 20)

/** Mode toggle + timer/shot-count chip selectors, shown above the shutter button. */
@Composable
fun TimerBurstControls(
    mode: ShutterMode,
    onModeChange: (ShutterMode) -> Unit,
    timerSeconds: Int,
    onTimerChange: (Int) -> Unit,
    shotCount: Int,
    onShotCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceScrim, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
            FilterChip(
                selected = mode == ShutterMode.SINGLE,
                onClick = { onModeChange(ShutterMode.SINGLE) },
                label = { Text("Single shot") },
            )
            FilterChip(
                selected = mode == ShutterMode.TIMER_BURST,
                onClick = { onModeChange(ShutterMode.TIMER_BURST) },
                label = { Text("Timer + Burst") },
            )
        }

        AnimatedVisibility(visible = mode == ShutterMode.TIMER_BURST) {
            Column {
                Text(
                    "Timer",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, start = 12.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    TIMER_OPTIONS.forEach { seconds ->
                        FilterChip(
                            selected = timerSeconds == seconds,
                            onClick = { onTimerChange(seconds) },
                            label = { Text("${seconds}s") },
                        )
                    }
                }

                Text(
                    "Photos",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, start = 12.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    SHOT_COUNT_OPTIONS.forEach { count ->
                        FilterChip(
                            selected = shotCount == count,
                            onClick = { onShotCountChange(count) },
                            label = { Text("$count") },
                        )
                    }
                }
            }
        }
    }
}
