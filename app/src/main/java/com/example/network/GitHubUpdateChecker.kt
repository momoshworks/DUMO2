package com.example.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubUpdateChecker(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class ReleaseInfo(
        val tagName: String,
        val releaseTitle: String,
        val changelog: String,
        val downloadUrl: String?,
        val isNewer: Boolean
    )

    suspend fun checkForUpdates(repo: String, currentVersion: String = "1.0"): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DUMU-Updater")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GitHubUpdateChecker", "GitHub response code: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "v1.0")
            val name = json.optString("name", "DUMU Release")
            val releaseNotes = json.optString("body", "Bug fixes and performance enhancements for Delta University students.")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val isNewer = compareVersions(tagName.removePrefix("v"), currentVersion.removePrefix("v")) > 0

            ReleaseInfo(
                tagName = tagName,
                releaseTitle = name,
                changelog = releaseNotes,
                downloadUrl = apkUrl ?: json.optString("html_url", ""),
                isNewer = isNewer
            )
        } catch (e: Exception) {
            Log.e("GitHubUpdateChecker", "Error checking for updates", e)
            null
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    fun startDownload(downloadUrl: String, fileName: String = "DUMU-latest.apk"): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle("DUMU APK Update")
            setDescription("Downloading latest DUMU Delta University assistant update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        return downloadManager.enqueue(request)
    }
}
