package com.arvids.downloader.extract

import com.arvids.downloader.model.DetectedMedia
import com.arvids.downloader.model.MediaFormat
import com.arvids.downloader.model.Platform
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamExtractor

class OkHttpDownloader(private val client: okhttp3.OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (name, values) ->
            values.forEach { builder.addHeader(name, it) }
        }
        val method = request.httpMethod()
        val body = request.dataToSend()
        val reqBody = when {
            body != null -> body.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            method == "POST" || method == "PUT" || method == "PATCH" ->
                ByteArray(0).toRequestBody(null)
            else -> null
        }
        builder.method(method, reqBody)
        client.newCall(builder.build()).execute().use { resp ->
            val headers = resp.headers.toMultimap()
            val respBody = resp.body?.string() ?: ""
            return Response(resp.code, resp.message, headers, respBody, request.url())
        }
    }
}

object YouTubeExtractor {
    private var initialized = false

    fun extract(url: String): DetectedMedia? {
        return try {
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(OkHttpDownloader(Http.client))
                    initialized = true
                }
            }
            val service = ServiceList.YouTube
            val extractor: StreamExtractor = service.getStreamExtractor(url)
            extractor.fetchPage()
            val name = try {
                extractor.name
            } catch (e: Exception) {
                "YouTube Video"
            }
            val thumbUrl = try {
                extractor.thumbnails.maxByOrNull { it.height }?.url
            } catch (e: Exception) {
                null
            }
            val formats = mutableListOf<MediaFormat>()
            try {
                extractor.videoStreams.forEach { vs ->
                    val res = vs.resolution
                    val label = if (vs.isVideoOnly) "$res • video only" else res
                    val ext = vs.format?.suffix ?: "mp4"
                    formats += MediaFormat(
                        label = label,
                        ext = ext,
                        url = vs.url ?: return@forEach,
                        isAudio = false,
                        sortKey = vs.height * 10 + if (vs.isVideoOnly) 0 else 1
                    )
                }
            } catch (e: Exception) {
                // keep whatever formats we already have
            }
            try {
                extractor.audioStreams.forEach { aus ->
                    val kbps = aus.averageBitrate / 1000
                    val ext = aus.format?.suffix ?: "m4a"
                    formats += MediaFormat(
                        label = "Audio • $kbps kbps",
                        ext = ext,
                        url = aus.url ?: return@forEach,
                        isAudio = true,
                        sortKey = 5
                    )
                }
            } catch (e: Exception) {
                // keep whatever formats we already have
            }
            if (formats.isEmpty()) return null
            formats.sortByDescending { it.sortKey }
            DetectedMedia(Platform.YOUTUBE, url, name, thumbUrl, formats)
        } catch (e: Exception) {
            null
        }
    }
}
