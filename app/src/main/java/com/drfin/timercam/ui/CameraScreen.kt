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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { CameraController(context, lifecycleOwner) }

    var flashSetting by remember { mutableStateOf(FlashSetting.OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureIndex by remember { mutableStateOf(0) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var lastThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var mode by remember { mutableStateOf(ShutterMode.SINGLE) }
    var timerSeconds by remember { mutableStateOf(TIMER_OPTIONS.first()) }
    var shotCount by remember { mutableStateOf(SHOT_COUNT_OPTIONS.first()) }

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

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        cameraController.bind(
            providerFuture = providerFuture,
            executor = ContextCompat.getMainExecutor(context),
            previewView = previewView,
            onReady = {
                exposureIndex = 0
            },
        )
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
                zoomRatio = 1f
            }) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Exposure ("lighting") slider, docked below the top controls row.
        val range = cameraController.exposureRange()
        if (range.endInclusive > range.start) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 88.dp, end = 12.dp)
                    .background(SurfaceScrim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Light", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = {
                        exposureIndex = it.toInt()
                        cameraController.setExposureCompensation(exposureIndex)
                    },
                    valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
                    steps = (range.endInclusive - range.start - 1).coerceAtLeast(0),
                    modifier = Modifier.size(width = 140.dp, height = 32.dp),
                )
            }
        }

        // Bottom countdown strip - overlays the LOWER part only, preview stays visible above it.
        val state = captureState
        if (state is CaptureUiState.CountingDown) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 220.dp)
                    .background(SurfaceScrim, RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text = "${state.secondsRemaining}",
                    color = AccentAmber,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "  shot ${state.shotIndex}/${state.totalShots}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
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

        // Bottom controls: mode/timer/shot-count chips + shutter button.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimerBurstControls(
                mode = mode,
                onModeChange = { mode = it },
                timerSeconds = timerSeconds,
                onTimerChange = { timerSeconds = it },
                shotCount = shotCount,
                onShotCountChange = { shotCount = it },
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            )

            if (state == CaptureUiState.Idle) {
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
                IconButton(
                    onClick = { session.cancel() },
                    modifier = Modifier
                        .size(76.dp)
                        .background(SurfaceScrim, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
