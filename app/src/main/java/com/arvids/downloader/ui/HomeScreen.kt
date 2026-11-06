package com.arvids.downloader.ui

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arvids.downloader.download.DownloadHelper
import com.arvids.downloader.model.DetectedMedia
import com.arvids.downloader.model.DownloadItem
import com.arvids.downloader.model.MediaFormat
import com.arvids.downloader.model.Platform

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val link by viewModel.link.collectAsState()
    val detectState by viewModel.detect.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val statusMsg by viewModel.statusMsg.collectAsState()
    val igLoggedIn by viewModel.igLoggedIn.collectAsState()
    val showIgLogin by viewModel.showIgLogin.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<MediaFormat?>(null) }

    if (showIgLogin) {
        InstagramLoginScreen(
            onBack = { viewModel.setShowIgLogin(false) },
            onLoggedIn = {
                viewModel.refreshInstagramLogin()
                viewModel.setShowIgLogin(false)
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B1E))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AR-Vids",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Instagram • Facebook • X • YouTube downloader",
                color = Color(0xFFB0AEC9),
                fontSize = 13.sp
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (igLoggedIn) Color(0xFF1B3D2E) else Color(0xFF241B3D)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (igLoggedIn) "Instagram login ho gaya" else "Instagram Login",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            if (igLoggedIn) "Reels download ke liye tayyar ho."
                            else "Reels download karne ke liye login zaroori hai.",
                            color = Color(0xFFB0AEC9),
                            fontSize = 13.sp
                        )
                    }
                    if (igLoggedIn) {
                        OutlinedButton(onClick = { viewModel.logoutInstagram() }) {
                            Text("Logout")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.setShowIgLogin(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            )
                        ) {
                            Text("Login")
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF241B3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Video link paste karo",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = link,
                        onValueChange = { viewModel.setLink(it) },
                        placeholder = {
                            Text(
                                "https://www.instagram.com/reel/...",
                                color = Color(0xFF8A87A3)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color(0xFF4A4460),
                            cursorColor = Color(0xFF7C3AED)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.getText()?.text?.let { viewModel.setLink(it) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Paste")
                        }
                        Button(
                            onClick = { viewModel.detectLink(link) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            )
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Find Video")
                        }
                    }
                }
            }
        }
        when (val s = detectState) {
            is DetectState.Loading -> item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF7C3AED))
                    Spacer(Modifier.width(12.dp))
                    Text("Video dhoond rahe hain...", color = Color.White)
                }
            }
            is DetectState.Error -> item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1B2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(s.message, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            is DetectState.Ready -> item {
                LaunchedEffect(s.media.sourceUrl) {
                    selectedFormat = s.media.formats.firstOrNull()
                }
                MediaCard(
                    media = s.media,
                    selected = selectedFormat,
                    onSelect = { selectedFormat = it },
                    onDownload = { fmt -> viewModel.downloadFormat(fmt, s.media) },
                    onAudio = { viewModel.downloadAudioFromVideo(s.media) }
                )
            }
            else -> {}
        }
        if (statusMsg.isNotBlank()) {
            item {
                Text(statusMsg, color = Color(0xFF9EE6B8), fontSize = 14.sp)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Downloads",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.refreshDownloads() }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        }
        if (downloads.isEmpty()) {
            item {
                Text("Abhi koi download nahi hai.", color = Color(0xFF8A87A3), fontSize = 14.sp)
            }
        } else {
            items(downloads) { d ->
                DownloadRow(
                    item = d,
                    onOpen = {
                        val uri = DownloadHelper.uriFor(context, d.id)
                        if (uri != null) {
                            val mime = if (d.fileName.endsWith(".m4a", true) ||
                                d.fileName.endsWith(".mp3", true)
                            ) "audio/*" else "video/*"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mime)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                    },
                    onDelete = { viewModel.deleteDownload(d.id) }
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    media: DetectedMedia,
    selected: MediaFormat?,
    onSelect: (MediaFormat) -> Unit,
    onDownload: (MediaFormat) -> Unit,
    onAudio: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF241B3D)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                AsyncImage(
                    model = media.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B1430))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        media.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 3
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(media.platform.name, color = Color(0xFFB0AEC9), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Quality choose karo:",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(6.dp))
            media.formats.forEach { fmt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(fmt) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == fmt,
                        onClick = { onSelect(fmt) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF7C3AED)
                        )
                    )
                    Icon(
                        if (fmt.isAudio) Icons.Filled.Audiotrack else Icons.Filled.Movie,
                        contentDescription = null,
                        tint = Color(0xFFB0AEC9),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(fmt.label, color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(fmt.ext.uppercase(), color = Color(0xFFB0AEC9), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { selected?.let { onDownload(it) } },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Download")
            }
            if (media.platform != Platform.YOUTUBE) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onAudio, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Filled.Audiotrack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Audio only (M4A)")
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF241B3D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.fileName.endsWith(".m4a", true) ||
                        item.fileName.endsWith(".mp3", true)
                    ) Icons.Filled.Audiotrack else Icons.Filled.Movie,
                    contentDescription = null,
                    tint = Color(0xFFB0AEC9),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        "${item.fileName} • ${item.platform}",
                        color = Color(0xFFB0AEC9),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.Filled.OpenInNew,
                        contentDescription = "Open",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF8A80)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (item.status == DownloadManager.STATUS_RUNNING ||
                item.status == DownloadManager.STATUS_PENDING
            ) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF7C3AED),
                    trackColor = Color(0xFF4A4460)
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                statusText(item.status, item.progress),
                color = if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                    Color(0xFF9EE6B8)
                } else {
                    Color(0xFFB0AEC9)
                },
                fontSize = 12.sp
            )
        }
    }
}

private fun statusText(status: Int, progress: Float): String = when (status) {
    DownloadManager.STATUS_SUCCESSFUL -> "Complete"
    DownloadManager.STATUS_RUNNING -> "Downloading ${(progress * 100).toInt()}%"
    DownloadManager.STATUS_PENDING -> "Waiting..."
    DownloadManager.STATUS_PAUSED -> "Paused"
    DownloadManager.STATUS_FAILED -> "Failed"
    else -> "Unknown"
}
