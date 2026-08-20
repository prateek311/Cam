package com.drfin.timercam.camera

import android.net.Uri
import androidx.camera.video.VideoRecordEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RecordingUiState {
    data object Idle : RecordingUiState()
    data class Recording(val elapsedMs: Long) : RecordingUiState()
}

/**
 * Drives the "tap to start -> tap to stop" video recording loop, mirroring how
 * [BurstCaptureSession] drives the photo countdown/burst loop. Delegates the actual
 * CameraX recording to [CameraController]; this class only tracks UI-facing state.
 */
class VideoRecordingSession(
    private val cameraController: CameraController,
    private val onVideoSaved: (Uri) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val _state = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val state: StateFlow<RecordingUiState> = _state.asStateFlow()

    val isRecording: Boolean
        get() = _state.value != RecordingUiState.Idle

    fun start() {
        if (isRecording) return
        _state.value = RecordingUiState.Recording(elapsedMs = 0L)
        cameraController.startVideoRecording { event ->
            when (event) {
                is VideoRecordEvent.Status -> {
                    _state.value = RecordingUiState.Recording(event.recordingStats.recordedDurationNanos / 1_000_000)
                }
                is VideoRecordEvent.Finalize -> {
                    _state.value = RecordingUiState.Idle
                    if (event.hasError()) {
                        onError(event.cause ?: IllegalStateException("Video recording failed: ${event.error}"))
                    } else {
                        onVideoSaved(event.outputResults.outputUri)
                    }
                }
                else -> Unit
            }
        }
    }

    fun stop() {
        cameraController.stopVideoRecording()
    }
}
