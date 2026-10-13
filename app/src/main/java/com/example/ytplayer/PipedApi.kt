package com.example.ytplayer

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PipedApi {
    // Puedes cambiar la instancia si esta no funciona:
    // https://github.com/TeamPiped/Piped/wiki/Instances
    private const val BASE = "https://pipedapi.kavin.rocks"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            return response.body!!.string()
        }
    }

    fun search(query: String): List<Video> {
        val json = JSONObject(get("$BASE/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=videos"))
        val items = json.getJSONArray("items")
        val result = mutableListOf<Video>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val url = item.optString("url", "")
            val id = url.substringAfter("v=", "")
            if (id.isBlank()) continue
            result.add(
                Video(
                    id = id,
                    title = item.optString("title", ""),
                    uploader = item.optString("uploaderName", ""),
                    duration = item.optInt("duration", 0),
                    thumbnail = item.optString("thumbnail", "")
                )
            )
        }
        return result
    }

    fun getStreamUrl(videoId: String): String {
        val json = JSONObject(get("$BASE/streams/$videoId"))
        val videoStreams = json.getJSONArray("videoStreams")
        var bestUrl: String? = null
        var bestQuality = -1
        for (i in 0 until videoStreams.length()) {
            val stream = videoStreams.getJSONObject(i)
            val videoOnly = stream.optBoolean("videoOnly", true)
            if (videoOnly) continue // Solo queremos streams combinados (audio + video)
            val quality = stream.optString("quality", "0p").removeSuffix("p").toIntOrNull() ?: 0
            if (quality > bestQuality) {
                bestQuality = quality
                bestUrl = stream.optString("url", "")
            }
        }
        if (bestUrl == null) throw Exception("No se encontró stream reproducible")
        return bestUrl
    }
}
