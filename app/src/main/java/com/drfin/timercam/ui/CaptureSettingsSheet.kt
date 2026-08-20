package com.drfin.timercam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drfin.timercam.camera.PhotoQuality
import com.drfin.timercam.camera.VideoQuality

/**
 * Bottom sheet for the video and photo quality/bitrate tiers. Bitrate — not resolution
 * alone — is what mainly drives file size, so each [VideoQuality] label spells it out
 * (e.g. "1080p · Recommended") so the tradeoff is visible while picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSettingsSheet(
    videoQuality: VideoQuality,
    onVideoQualityChange: (VideoQuality) -> Unit,
    photoQuality: PhotoQuality,
    onPhotoQualityChange: (PhotoQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "Video quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Higher bitrate looks sharper but makes much bigger files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                VideoQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = quality == videoQuality,
                        onClick = { onVideoQualityChange(quality) },
                        label = { Text(quality.label) },
                    )
                }
            }

            Text(
                text = "Photo quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Standard keeps photos small with no visible difference for sharing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                PhotoQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = quality == photoQuality,
                        onClick = { onPhotoQualityChange(quality) },
                        label = { Text(quality.label) },
                    )
                }
            }
        }
    }
}
