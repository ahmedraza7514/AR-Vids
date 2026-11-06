package com.arvids.downloader.download

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.arvids.downloader.extract.Http
import com.arvids.downloader.model.DownloadItem
import com.arvids.downloader.model.DownloadRecord
import com.arvids.downloader.model.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer

object DownloadHelper {
    private const val PREFS = "ar_vids_downloads"
    private const val KEY = "records"

    fun enqueue(context: Context, format: MediaFormat, title: String, platform: String): Long {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(60)
            .ifBlank { "AR-Vids" }
        val ext = format.ext.ifBlank { if (format.isAudio) "m4a" else "mp4" }
        val fileName = "$safeTitle.$ext"
        val dirType = if (format.isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val request = DownloadManager.Request(Uri.parse(format.url))
            .setTitle(fileName)
            .setDescription("AR-Vids • $platform")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(dirType, "AR-Vids/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        saveRecord(context, DownloadRecord(id, title, fileName, platform, System.currentTimeMillis()))
        return id
    }

    fun uriFor(context: Context, id: Long): Uri? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return try {
            dm.getUriForDownloadedFile(id)
        } catch (e: Exception) {
            null
        }
    }

    fun loadDownloads(context: Context): List<DownloadItem> {
        val records = loadRecords(context)
        if (records.isEmpty()) return emptyList()
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
            .setFilterById(*records.map { it.id }.toLongArray())
        val out = mutableListOf<DownloadItem>()
        dm.query(query).use { cursor ->
            val idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedCol =
                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rec = records.find { it.id == id } ?: continue
                val status = cursor.getInt(statusCol)
                val downloaded = cursor.getLong(downloadedCol)
                val total = cursor.getLong(totalCol)
                val uri = if (uriCol >= 0) cursor.getString(uriCol) else null
                val progress = if (total > 0) downloaded.toFloat() / total else 0f
                out += DownloadItem(id, rec.title, rec.fileName, rec.platform, status, progress, uri)
            }
        }
        return out.sortedByDescending { it.id }
    }

    fun deleteDownload(context: Context, id: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(id)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kept = loadRecords(context).filter { it.id != id }
        val arr = JSONArray()
        kept.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("fileName", r.fileName)
                    put("platform", r.platform)
                    put("time", r.timeMillis)
                }
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun loadRecords(context: Context): List<DownloadRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                DownloadRecord(
                    o.optLong("id"),
                    o.optString("title"),
                    o.optString("fileName"),
                    o.optString("platform"),
                    o.optLong("time")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRecord(context: Context, rec: DownloadRecord) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = loadRecords(context).toMutableList()
        list.add(0, rec)
        val arr = JSONArray()
        list.take(50).forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("fileName", r.fileName)
                    put("platform", r.platform)
                    put("time", r.timeMillis)
                }
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /** Download a video, pull out its audio track on-device, save as .m4a in Music/AR-Vids. */
    suspend fun downloadAudioFromVideo(
        context: Context,
        videoUrl: String,
        title: String,
        platform: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(60)
                .ifBlank { "AR-Vids" }
            val tmpVideo = File(context.cacheDir, "ar_tmp_video.mp4")
            val tmpAudio = File(context.cacheDir, "ar_tmp_audio.m4a")
            val req = okhttp3.Request.Builder()
                .url(videoUrl)
                .header("User-Agent", Http.UA)
                .build()
            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                tmpVideo.outputStream().use { out ->
                    resp.body?.byteStream()?.copyTo(out)
                }
            }
            if (!extractAudioTrack(tmpVideo, tmpAudio)) return@withContext false
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$safeTitle.m4a")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/AR-Vids")
                put(MediaStore.Audio.Media.TITLE, safeTitle)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return@withContext false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                tmpAudio.inputStream().copyTo(out)
            }
            tmpVideo.delete()
            tmpAudio.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractAudioTrack(input: File, output: File): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(input.absolutePath)
            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(AndroidMediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    break
                }
            }
            if (audioTrack < 0) return false
            extractor.selectTrack(audioTrack)
            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackIndex = muxer.addTrack(extractor.getTrackFormat(audioTrack))
            muxer.start()
            val buffer = ByteBuffer.allocate(1024 * 256)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                muxer.writeSampleData(trackIndex, buffer, info)
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            true
        } catch (e: Exception) {
            false
        } finally {
            extractor.release()
        }
    }
}
