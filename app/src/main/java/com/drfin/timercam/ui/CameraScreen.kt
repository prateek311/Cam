package com.drfin.timercam.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.drfin.timercam.camera.BurstCaptureSession
import com.drfin.timercam.camera.CameraController
import com.drfin.timercam.camera.CaptureUiState
import com.drfin.timercam.camera.FlashSetting
import com.drfin.timercam.camera.ShutterMode
import com.drfin.timercam.ui.theme.AccentAmber
import com.drfin.timercam.ui.theme.SurfaceScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val ZOOM_PRESETS = listOf(0.5f, 1f, 2f, 3f)
private const val FOCUS_RING_MS = 900L
private const val EXPOSURE_SLIDER_IDLE_MS = 3000L

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { CameraController(context, lifecycleOwner) }

    var flashSetting by remember { mutableStateOf(FlashSetting.OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureIndex by remember { mutableStateOf(0) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var lastThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Mirrors of CameraController's CameraInfo-derived ranges, refreshed via
    // onCameraBound below (fires on initial bind AND every lens switch).
    var exposureRange by remember { mutableStateOf(0..0) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }

    var mode by remember { mutableStateOf(ShutterMode.SINGLE) }
    var timerSeconds by remember { mutableStateOf(TIMER_OPTIONS.first()) }
    var shotCount by remember { mutableStateOf(SHOT_COUNT_OPTIONS.first()) }

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusRingVisible by remember { mutableStateOf(false) }
    var exposureSliderVisible by remember { mutableStateOf(false) }
    var focusNonce by remember { mutableIntStateOf(0) }
    var exposureNonce by remember { mutableIntStateOf(0) }

    val session = remember {
        BurstCaptureSession(
            scope = scope,
            takePhoto = { cameraController.takePhoto() },
            onPhotoSaved = { uri ->
                lastPhotoUri = uri
                scope.launch {
                    lastThumbnail = withContext(Dispatchers.IO) { loadThumbnail(context, uri) }
                }
            },
            onError = { errorMessage = it.message ?: "Failed to capture photo" },
        )
    }
    val captureState by session.state.collectAsState()
    val isIdle = captureState == CaptureUiState.Idle

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        cameraController.bind(
            providerFuture = providerFuture,
            executor = ContextCompat.getMainExecutor(context),
            previewView = previewView,
            onCameraBound = {
                val newRange = cameraController.exposureRange()
                exposureRange = newRange
                exposureIndex = exposureIndex.coerceIn(newRange.start, newRange.endInclusive.coerceAtLeast(newRange.start))
                minZoom = cameraController.minZoomRatio()
                maxZoom = cameraController.maxZoomRatio()
                zoomRatio = cameraController.currentZoomRatio()
            },
        )
    }

    LaunchedEffect(focusNonce) {
        if (focusNonce > 0) {
            focusRingVisible = true
            delay(FOCUS_RING_MS)
            focusRingVisible = false
        }
    }
    LaunchedEffect(exposureNonce) {
        if (exposureNonce > 0) {
            exposureSliderVisible = true
            delay(EXPOSURE_SLIDER_IDLE_MS)
            exposureSliderVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        zoomRatio = (zoomRatio * zoomChange).coerceIn(
                            cameraController.minZoomRatio(),
                            cameraController.maxZoomRatio(),
                        )
                        cameraController.setZoomRatio(zoomRatio)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        cameraController.focusOnPoint(previewView, offset.x, offset.y)
                        focusPoint = offset
                        focusNonce++
                        exposureNonce++
                    }
                },
        )

        // Top controls: flash + lens switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { flashSetting = cameraController.cycleFlash() }) {
                Icon(
                    imageVector = when (flashSetting) {
                        FlashSetting.AUTO -> Icons.Filled.FlashAuto
                        FlashSetting.ON -> Icons.Filled.FlashOn
                        FlashSetting.TORCH -> Icons.Filled.FlashlightOn
                        FlashSetting.OFF -> Icons.Filled.FlashOff
                    },
                    contentDescription = "Flash: ${flashSetting.name}",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = {
                cameraController.toggleLensFacing(previewView)
            }) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Tap-to-focus ring + drag-to-adjust exposure slider, anchored at the tap point.
        focusPoint?.let { point ->
            FocusRing(
                visible = focusRingVisible,
                modifier = Modifier.offset {
                    val half = with(density) { 36.dp.roundToPx() }
                    IntOffset(point.x.roundToInt() - half, point.y.roundToInt() - half)
                },
            )

            if (exposureRange.endInclusive > exposureRange.start) {
                ExposureSlider(
                    exposureIndex = exposureIndex,
                    range = exposureRange,
                    onChange = { newIndex ->
                        exposureIndex = newIndex
                        cameraController.setExposureCompensation(newIndex)
                    },
                    onInteraction = { exposureNonce++ },
                    modifier = Modifier.offset {
                        val offsetX = with(density) { 48.dp.roundToPx() }
                        val offsetY = with(density) { 80.dp.roundToPx() }
                        IntOffset(point.x.roundToInt() + offsetX, point.y.roundToInt() - offsetY)
                    },
                )
            }
        }

        // Bottom-left thumbnail of the last captured photo.
        lastThumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "View last photo",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .size(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                    .clickable {
                        lastPhotoUri?.let { uri -> openPhoto(context, uri) }
                    },
            )
        }

        // Bottom controls: while idle, zoom presets + mode/timer/shot-count chips + shutter.
        // While capturing, everything collapses to a single capsule (countdown + cancel).
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isIdle) {
                ZoomPresetRow(
                    currentRatio = zoomRatio,
                    minRatio = minZoom,
                    maxRatio = maxZoom,
                    onSelect = { ratio ->
                        zoomRatio = ratio
                        cameraController.setZoomRatio(ratio)
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                TimerBurstControls(
                    mode = mode,
                    onModeChange = { mode = it },
                    timerSeconds = timerSeconds,
                    onTimerChange = { timerSeconds = it },
                    shotCount = shotCount,
                    onShotCountChange = { shotCount = it },
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                )
                IconButton(
                    onClick = { session.start(mode, timerSeconds, shotCount) },
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(AccentAmber, CircleShape),
                    )
                }
            } else {
                CaptureStatusCapsule(state = captureState, onCancel = { session.cancel() })
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .background(SurfaceScrim, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            )
        }
    }
}

/** Quick zoom-ratio presets (0.5x/1x/2x/3x), filtered to what this lens actually supports. */
@Composable
private fun ZoomPresetRow(
    currentRatio: Float,
    minRatio: Float,
    maxRatio: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidates = ZOOM_PRESETS.filter { it in minRatio..maxRatio }
    if (candidates.size < 2) return

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        candidates.forEach { ratio ->
            val selected = abs(currentRatio - ratio) < 0.05f
            Box(
                modifier = Modifier
                    .size(if (selected) 44.dp else 36.dp)
                    .background(if (selected) AccentAmber else SurfaceScrim, CircleShape)
                    .clickable { onSelect(ratio) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatZoomLabel(ratio),
                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Single bottom pill shown while counting down / capturing: status text + cancel, together. */
@Composable
private fun CaptureStatusCapsule(state: CaptureUiState, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .background(SurfaceScrim, RoundedCornerShape(28.dp))
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        when (state) {
            is CaptureUiState.CountingDown -> {
                Text(
                    text = "${state.secondsRemaining}",
                    color = AccentAmber,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "shot ${state.shotIndex}/${state.totalShots}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
            }
            is CaptureUiState.Capturing -> {
                Text(
                    text = "Capturing ${state.shotIndex}/${state.totalShots}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                )
            }
            CaptureUiState.Idle -> Unit
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun formatZoomLabel(ratio: Float): String =
    if (ratio == ratio.toInt().toFloat()) "${ratio.toInt()}x" else "${ratio}x"

private fun openPhoto(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private suspend fun loadThumbnail(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
    } else {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeStream(input, null, options)
        }
    }
} catch (t: Throwable) {
    null
}
