package com.drfin.timercam.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

enum class FlashSetting { AUTO, ON, OFF, TORCH }

/**
 * Thin wrapper around CameraX bind/rebind + the controls this app exposes
 * (flash, zoom, exposure, focus, lens facing). Holds no UI state itself.
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    var imageCapture: ImageCapture? = null
        private set

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashSetting = FlashSetting.OFF

    fun bind(
        providerFuture: ListenableFuture<ProcessCameraProvider>,
        executor: Executor,
        previewView: PreviewView,
        onReady: () -> Unit,
    ) {
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            rebind(previewView)
            onReady()
        }, executor)
    }

    private fun rebind(previewView: PreviewView) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val newPreview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val newImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(toImageCaptureFlashMode(flashSetting))
            .build()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        camera = provider.bindToLifecycle(lifecycleOwner, selector, newPreview, newImageCapture)
        preview = newPreview
        imageCapture = newImageCapture

        applyFlashSetting(flashSetting)
    }

    fun toggleLensFacing(previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        rebind(previewView)
    }

    fun isFrontFacing(): Boolean = lensFacing == CameraSelector.LENS_FACING_FRONT

    fun cycleFlash(): FlashSetting {
        flashSetting = when (flashSetting) {
            FlashSetting.OFF -> FlashSetting.AUTO
            FlashSetting.AUTO -> FlashSetting.ON
            FlashSetting.ON -> FlashSetting.TORCH
            FlashSetting.TORCH -> FlashSetting.OFF
        }
        applyFlashSetting(flashSetting)
        return flashSetting
    }

    private fun applyFlashSetting(setting: FlashSetting) {
        imageCapture?.flashMode = toImageCaptureFlashMode(setting)
        camera?.cameraControl?.enableTorch(setting == FlashSetting.TORCH)
    }

    private fun toImageCaptureFlashMode(setting: FlashSetting): Int = when (setting) {
        FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
        FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
        FlashSetting.OFF, FlashSetting.TORCH -> ImageCapture.FLASH_MODE_OFF
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(minZoomRatio(), maxZoomRatio()))
    }

    fun minZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
    fun maxZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    fun currentZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    fun exposureRange(): ClosedRange<Int> {
        val state = camera?.cameraInfo?.exposureState ?: return 0..0
        return state.exposureCompensationRange.lower..state.exposureCompensationRange.upper
    }

    fun setExposureCompensation(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
        val meteringPointFactory = previewView.meteringPointFactory
        val point = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    suspend fun takePhoto(): Uri {
        val capture = imageCapture ?: error("Camera not ready")
        return MediaStoreSaver.capture(capture, context)
    }
}
