package com.drfin.timercam.camera

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Shutter mode selected by the user before tapping the shutter button. */
enum class ShutterMode { SINGLE, TIMER_BURST }

sealed class CaptureUiState {
    data object Idle : CaptureUiState()

    /** Countdown for shot [shotIndex] of [totalShots], [secondsRemaining] shown in the bottom strip. */
    data class CountingDown(
        val secondsRemaining: Int,
        val shotIndex: Int,
        val totalShots: Int,
    ) : CaptureUiState()

    data class Capturing(val shotIndex: Int, val totalShots: Int) : CaptureUiState()
}

/**
 * Drives the "countdown -> capture -> repeat" loop for timer+burst mode, and the
 * immediate single-shot path. The countdown never covers the whole screen — UI layer
 * is responsible for rendering [CaptureUiState.CountingDown] as a bottom strip only.
 */
class BurstCaptureSession(
    private val scope: CoroutineScope,
    private val takePhoto: suspend () -> Uri,
    private val onPhotoSaved: (Uri) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val _state = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private var job: Job? = null

    val isRunning: Boolean
        get() = _state.value != CaptureUiState.Idle

    fun start(mode: ShutterMode, timerSeconds: Int, shotCount: Int) {
        if (isRunning) return
        job = scope.launch {
            try {
                when (mode) {
                    ShutterMode.SINGLE -> runSingleShot()
                    ShutterMode.TIMER_BURST -> runTimerBurst(timerSeconds, shotCount)
                }
            } catch (t: Throwable) {
                onError(t)
            } finally {
                _state.value = CaptureUiState.Idle
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = CaptureUiState.Idle
    }

    private suspend fun runSingleShot() {
        _state.value = CaptureUiState.Capturing(shotIndex = 1, totalShots = 1)
        val uri = takePhoto()
        onPhotoSaved(uri)
    }

    private suspend fun runTimerBurst(timerSeconds: Int, shotCount: Int) {
        for (shotIndex in 1..shotCount) {
            for (secondsRemaining in timerSeconds downTo 1) {
                _state.value = CaptureUiState.CountingDown(secondsRemaining, shotIndex, shotCount)
                delay(1000)
            }
            _state.value = CaptureUiState.Capturing(shotIndex, shotCount)
            val uri = takePhoto()
            onPhotoSaved(uri)
        }
    }
}
