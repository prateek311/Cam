package com.drfin.timercam.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Saves captured photos and videos into the public Gallery via MediaStore, same as any camera app. */
object MediaStoreSaver {

    private const val RELATION_PATH = "Pictures/TimerCam"
    private const val VIDEO_RELATION_PATH = "Movies/TimerCam"

    fun newOutputOptions(context: Context): ImageCapture.OutputFileOptions {
        val name = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATION_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.DATA, "${dir}/TimerCam/$name.jpg")
            }
        }
        return ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    }

    suspend fun capture(imageCapture: ImageCapture, context: Context): Uri =
        suspendCancellableCoroutine { continuation ->
            val outputOptions = newOutputOptions(context)
            imageCapture.takePicture(
                outputOptions,
                Runnable::run,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri
                        if (uri != null) {
                            continuation.resume(uri)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Saved photo but no URI returned"))
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }

    fun videoOutputOptions(context: Context): MediaStoreOutputOptions {
        val name = "VID_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, VIDEO_RELATION_PATH)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.DATA, "${dir}/TimerCam/$name.mp4")
            }
        }
        return MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(contentValues).build()
    }
}
