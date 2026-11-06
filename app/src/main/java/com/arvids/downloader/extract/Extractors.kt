package com.arvids.downloader.extract

import com.arvids.downloader.model.DetectedMedia
import com.arvids.downloader.model.MediaFormat
import com.arvids.downloader.model.Platform
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    fun get(url: String, headers: Map<String, String> = emptyMap()): String? {
        val builder = Request.Builder().url(url).header("User-Agent", UA)
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }
}

/** Unescape a JSON string body (the part between the quotes). */
fun unescapeJson(s: String): String {
    return try {
        JSONObject("{\"k\":\"$s\"}").optString("k", s)
    } catch (e: Exception) {
        s.replace("\\/", "/").replace("\\\"", "\"")
    }
}

private fun jsonString(html: String, key: String): String? {
    val regex = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
    return regex.find(html)?.groupValues?.getOrNull(1)?.let { unescapeJson(it) }
}

/** Unescape HTML entities found in scraped page markup (imginn writes & as &#38;). */
private fun unescapeHtmlEntities(s: String): String {
    return s.replace("&#38;", "&").replace("&amp;", "&")
}

object InstagramExtractor {
    private const val IG_APP_ID = "936619743392459"

    /** Headers for Instagram requests; adds the login session when logged in. */
    private fun authHeaders(): Map<String, String> {
        val headers = mutableMapOf("Accept-Language" to "en-US,en;q=0.9")
        InstagramSession.cookieHeader()?.let { headers["Cookie"] = it }
        InstagramSession.csrfToken()?.let { headers["X-CSRFToken"] = it }
        return headers
    }

    private fun apiHeaders(): Map<String, String> {
        val headers = authHeaders().toMutableMap()
        headers["X-IG-App-ID"] = IG_APP_ID
        headers["X-Requested-With"] = "XMLHttpRequest"
        headers["Accept"] = "application/json"
        return headers
    }

    fun extract(url: String): DetectedMedia? {
        val id = Regex("/(?:reel|reels|p)/([A-Za-z0-9_-]+)")
            .find(url)?.groupValues?.getOrNull(1) ?: return null
        val headers = authHeaders()
        val loggedIn = InstagramSession.isLoggedIn
        // 1) Direct Instagram page — with a login session this carries media data.
        var html = Http.get("https://www.instagram.com/reel/$id/", headers)
        if (html == null || !html.contains("video_url")) {
            html = Http.get("https://www.instagram.com/p/$id/embed/", headers)
        }
        var videoUrl = html?.let { jsonString(it, "video_url") }
        var thumb = html?.let { jsonString(it, "display_url") }
        var title: String? = html?.let {
            Regex(""""caption"\s*:\s*\{[^}]*"text"\s*:\s*"((?:\\.|[^"\\])*)"""")
                .find(it)?.groupValues?.getOrNull(1)?.let { t -> unescapeJson(t) }
        }?.trim()?.take(80)?.ifBlank { null }
        // 2) Logged-in media info endpoint (best quality from video_versions).
        if (videoUrl == null && loggedIn) {
            val apiJson = Http.get(
                "https://i.instagram.com/api/v1/media/$id/info/",
                apiHeaders()
            )
            if (apiJson != null) {
                try {
                    val item = JSONObject(apiJson).optJSONArray("items")?.optJSONObject(0)
                    val versions = item?.optJSONArray("video_versions")
                    var best: String? = null
                    var bestWidth = 0
                    if (versions != null) {
                        for (i in 0 until versions.length()) {
                            val v = versions.optJSONObject(i) ?: continue
                            val u = v.optString("url")
                            val w = v.optInt("width", 0)
                            if (u.isNotBlank() && w >= bestWidth) {
                                bestWidth = w
                                best = u
                            }
                        }
                    }
                    videoUrl = best
                    val apiThumb = item?.optJSONObject("image_versions2")
                        ?.optJSONArray("candidates")?.optJSONObject(0)?.optString("url")
                        ?.takeIf { it.isNotBlank() }
                    if (apiThumb != null) thumb = apiThumb
                    val apiTitle = item?.optJSONObject("caption")?.optString("text")
                        ?.trim()?.take(80)?.ifBlank { null }
                    if (apiTitle != null) title = apiTitle
                } catch (e: Exception) {
                    // fall through to the mirror fallback
                }
            }
        }
        // 3) Fallback: public mirror page for the same post.
        if (videoUrl == null) {
            val mirror = Http.get(
                "https://imginn.com/p/$id/",
                mapOf("Accept-Language" to "en-US,en;q=0.9")
            )
            if (mirror != null) {
                videoUrl = Regex("""<video[^>]*\ssrc="([^"]+)"""")
                    .find(mirror)?.groupValues?.getOrNull(1)
                    ?.let { unescapeHtmlEntities(it) }
                thumb = Regex("""<meta property="og:image" content="([^"]+)"""")
                    .find(mirror)?.groupValues?.getOrNull(1)
                    ?.let { unescapeHtmlEntities(it) }
                    ?: Regex("""<video[^>]*\sposter="([^"]+)"""")
                        .find(mirror)?.groupValues?.getOrNull(1)
                        ?.let { unescapeHtmlEntities(it) }
                title = Regex("""<meta property="og:title" content="([^"]+)"""")
                    .find(mirror)?.groupValues?.getOrNull(1)
                    ?.let { unescapeHtmlEntities(it) }
                    ?.removeSuffix(" instagram post download - imginn.com")
                    ?.trim()?.take(80)?.ifBlank { null }
            }
        }
        videoUrl ?: return null
        return DetectedMedia(
            platform = Platform.INSTAGRAM,
            sourceUrl = url,
            title = title ?: "Instagram Reel",
            thumbnailUrl = thumb,
            formats = listOf(
                MediaFormat("Video • MP4", "mp4", videoUrl, false, sortKey = 100)
            )
        )
    }
}

object FacebookExtractor {
    fun extract(url: String): DetectedMedia? {
        val html = Http.get(url, mapOf("Accept-Language" to "en-US,en;q=0.9")) ?: return null
        val hd = jsonString(html, "playable_url_quality_hd")
        val sd = jsonString(html, "playable_url")
        if (hd == null && sd == null) return null
        val thumb = Regex(""""preferred_thumbnail"\s*:\s*\{[^}]*"image"\s*:\s*\{[^}]*"uri"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(html)?.groupValues?.getOrNull(1)?.let { unescapeJson(it) }
            ?: Regex(""""og:image"\s+content="([^"]+)"""")
                .find(html)?.groupValues?.getOrNull(1)
        val title = Regex(""""og:title"\s+content="([^"]+)"""")
            .find(html)?.groupValues?.getOrNull(1)?.take(80) ?: "Facebook Video"
        val formats = mutableListOf<MediaFormat>()
        hd?.let { formats += MediaFormat("HD • MP4", "mp4", it, false, sortKey = 100) }
        sd?.let { formats += MediaFormat("SD • MP4", "mp4", it, false, sortKey = 50) }
        return DetectedMedia(Platform.FACEBOOK, url, title, thumb, formats)
    }
}

object XExtractor {
    fun extract(url: String): DetectedMedia? {
        val id = Regex("/status/(\\d+)").find(url)?.groupValues?.getOrNull(1) ?: return null
        val json = Http.get(
            "https://cdn.syndication.twimg.com/tweet?id=$id",
            mapOf("Accept" to "application/json")
        ) ?: return null
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            return null
        }
        val video = obj.optJSONObject("video") ?: return null
        val variants = video.optJSONArray("variants") ?: return null
        val formats = mutableListOf<MediaFormat>()
        for (i in 0 until variants.length()) {
            val v = variants.optJSONObject(i) ?: continue
            if (v.optString("content_type") != "video/mp4") continue
            val vurl = v.optString("src")
            if (vurl.isBlank()) continue
            val bitrate = v.optInt("bitrate", 0)
            val label = if (bitrate > 0) "Video • ${bitrate / 1000} kbps" else "Video • MP4"
            formats += MediaFormat(label, "mp4", vurl, false, sortKey = bitrate)
        }
        if (formats.isEmpty()) return null
        formats.sortByDescending { it.sortKey }
        val text = obj.optString("text").trim().take(80).ifBlank { "X Video" }
        var thumb: String? = null
        val photos = obj.optJSONArray("photos")
        if (photos != null && photos.length() > 0) {
            thumb = photos.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
        }
        return DetectedMedia(Platform.X, url, text, thumb, formats)
    }
}

object LinkExtractor {
    fun detectPlatform(url: String): Platform = when {
        url.contains("instagram.com") -> Platform.INSTAGRAM
        url.contains("facebook.com") || url.contains("fb.watch") -> Platform.FACEBOOK
        url.contains("x.com") || url.contains("twitter.com") -> Platform.X
        url.contains("youtube.com") || url.contains("youtu.be") -> Platform.YOUTUBE
        else -> Platform.UNKNOWN
    }

    fun extract(url: String): DetectedMedia? = when (detectPlatform(url)) {
        Platform.INSTAGRAM -> InstagramExtractor.extract(url)
        Platform.FACEBOOK -> FacebookExtractor.extract(url)
        Platform.X -> XExtractor.extract(url)
        Platform.YOUTUBE -> YouTubeExtractor.extract(url)
        Platform.UNKNOWN -> null
    }
}
