package com.drfin.timercam.camera

import android.content.Context
import androidx.camera.video.Quality

/**
 * Video quality/bitrate tiers. Resolution alone isn't what makes phone video huge —
 * stock camera apps default to very high bitrates. Pinning an explicit bitrate per
 * tier (instead of leaving it to the encoder default) is what keeps files "reel-sized".
 */
enum class VideoQuality(val label: String, val quality: Quality, val bitRate: Int) {
    DATA_SAVER("720p · Data saver", Quality.HD, 4_000_000),
    RECOMMENDED("1080p · Recommended", Quality.FHD, 8_000_000),
    HIGH("1080p · High", Quality.FHD, 14_000_000),
}

/** JPEG compression tiers for photos, applied via ImageCapture.Builder.setJpegQuality. */
enum class PhotoQuality(val label: String, val jpegQuality: Int) {
    STANDARD("Standard", 80),
    HIGH("High", 92),
    MAX("Max", 100),
}

/** Persists the user's chosen quality tiers across app launches. */
class CaptureSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getVideoQuality(): VideoQuality =
        prefs.getString(KEY_VIDEO_QUALITY, null)?.let { name ->
            runCatching { VideoQuality.valueOf(name) }.getOrNull()
        } ?: VideoQuality.RECOMMENDED

    fun setVideoQuality(quality: VideoQuality) {
        prefs.edit().putString(KEY_VIDEO_QUALITY, quality.name).apply()
    }

    fun getPhotoQuality(): PhotoQuality =
        prefs.getString(KEY_PHOTO_QUALITY, null)?.let { name ->
            runCatching { PhotoQuality.valueOf(name) }.getOrNull()
        } ?: PhotoQuality.STANDARD

    fun setPhotoQuality(quality: PhotoQuality) {
        prefs.edit().putString(KEY_PHOTO_QUALITY, quality.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "capture_settings"
        const val KEY_VIDEO_QUALITY = "video_quality"
        const val KEY_PHOTO_QUALITY = "photo_quality"
    }
}
