package com.arvids.downloader.model

enum class Platform { INSTAGRAM, FACEBOOK, X, YOUTUBE, UNKNOWN }

data class MediaFormat(
    val label: String,
    val ext: String,
    val url: String,
    val isAudio: Boolean,
    val sizeBytes: Long = -1,
    val sortKey: Int = 0
)

data class DetectedMedia(
    val platform: Platform,
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val formats: List<MediaFormat>
)

data class DownloadRecord(
    val id: Long,
    val title: String,
    val fileName: String,
    val platform: String,
    val timeMillis: Long
)

data class DownloadItem(
    val id: Long,
    val title: String,
    val fileName: String,
    val platform: String,
    val status: Int,
    val progress: Float,
    val localUri: String?
)
