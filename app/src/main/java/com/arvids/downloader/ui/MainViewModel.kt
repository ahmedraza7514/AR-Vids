package com.arvids.downloader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvids.downloader.download.DownloadHelper
import com.arvids.downloader.extract.InstagramSession
import com.arvids.downloader.extract.LinkExtractor
import com.arvids.downloader.model.DetectedMedia
import com.arvids.downloader.model.DownloadItem
import com.arvids.downloader.model.MediaFormat
import com.arvids.downloader.model.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DetectState {
    data object Idle : DetectState
    data object Loading : DetectState
    data class Ready(val media: DetectedMedia) : DetectState
    data class Error(val message: String) : DetectState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _link = MutableStateFlow("")
    val link: StateFlow<String> = _link

    private val _detect = MutableStateFlow<DetectState>(DetectState.Idle)
    val detect: StateFlow<DetectState> = _detect

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads

    private val _statusMsg = MutableStateFlow("")
    val statusMsg: StateFlow<String> = _statusMsg

    private val _igLoggedIn = MutableStateFlow(false)
    val igLoggedIn: StateFlow<Boolean> = _igLoggedIn

    private val _showIgLogin = MutableStateFlow(false)
    val showIgLogin: StateFlow<Boolean> = _showIgLogin

    init {
        InstagramSession.init(getApplication())
        _igLoggedIn.value = InstagramSession.isLoggedIn
        refreshDownloads()
    }

    fun setShowIgLogin(value: Boolean) {
        _showIgLogin.value = value
    }

    fun refreshInstagramLogin() {
        _igLoggedIn.value = InstagramSession.isLoggedIn
    }

    fun logoutInstagram() {
        InstagramSession.clear()
        _igLoggedIn.value = false
        _statusMsg.value = "Instagram se logout ho gaya."
    }

    fun setLink(value: String) {
        _link.value = value
    }

    fun detectLink(url: String) {
        val clean = url.trim()
        if (clean.isBlank()) {
            _detect.value = DetectState.Error("Pehle link paste karo.")
            return
        }
        _detect.value = DetectState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                LinkExtractor.extract(clean)
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                _detect.value = if (result != null && result.formats.isNotEmpty()) {
                    DetectState.Ready(result)
                } else if (LinkExtractor.detectPlatform(clean) == Platform.INSTAGRAM &&
                    !InstagramSession.isLoggedIn
                ) {
                    DetectState.Error("Is link se video nahi mila. Instagram ke liye pehle upar Instagram Login se login karo.")
                } else {
                    DetectState.Error("Is link se video nahi mila. Link public hona chahiye — login wali ya private video nahi chalegi.")
                }
            }
        }
    }

    fun downloadFormat(format: MediaFormat, media: DetectedMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadHelper.enqueue(getApplication(), format, media.title, media.platform.name)
            withContext(Dispatchers.Main) {
                _statusMsg.value = "Download shuru ho gaya — ${format.label}. File Gallery mein aayegi."
                refreshDownloads()
            }
        }
    }

    fun downloadAudioFromVideo(media: DetectedMedia) {
        viewModelScope.launch {
            _statusMsg.value = "Audio nikaal rahe hain, thora wait karo..."
            val ok = withContext(Dispatchers.IO) {
                val best = media.formats.filter { !it.isAudio }.maxByOrNull { it.sortKey }
                    ?: media.formats.firstOrNull()
                if (best == null) false
                else DownloadHelper.downloadAudioFromVideo(
                    getApplication(), best.url, media.title, media.platform.name
                )
            }
            _statusMsg.value = if (ok) {
                "Audio save ho gaya — Music/AR-Vids mein dekho."
            } else {
                "Audio nahi nikaal sakay. Dobara try karo."
            }
        }
    }

    fun refreshDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = DownloadHelper.loadDownloads(getApplication())
            withContext(Dispatchers.Main) { _downloads.value = list }
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadHelper.deleteDownload(getApplication(), id)
            refreshDownloads()
        }
    }
}
