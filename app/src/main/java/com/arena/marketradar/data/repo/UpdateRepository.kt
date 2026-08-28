package com.arena.marketradar.data.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.arena.marketradar.data.model.GitHubAsset
import com.arena.marketradar.data.model.GitHubRelease
import com.arena.marketradar.domain.util.Version
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app self-update from GitHub Releases. It reads the published releases
 * (via the public API), finds the newest one that is newer than the installed
 * version, and downloads the attached APK so the user can install it.
 *
 * No backend, no Play Store — purely open-source/self-hosted style updates.
 */
class UpdateRepository(context: Context) {

    // Configurable: repo owner/name (must be public for anonymous API access).
    private val repoOwner = "hojjatrad"
    private val repoName = "MarketRadar"
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=20"

    private val appContext = context.applicationContext

    /** The currently installed version code (from the manifest). */
    fun installedVersionCode(): Int = try {
        val pm = appContext.packageManager
        pm.getPackageInfo(appContext.packageName, 0).let { if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toInt() else it.versionCode }
    } catch (e: Exception) { 0 }

    fun installedVersionName(): String = try {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) { "1.0" }

    /**
     * Returns the newest available release strictly newer than the installed one,
     * or null if up to date. Compares semantic versions (e.g. "1.4" > "1.3"),
     * so it works regardless of the numeric versionCode.
     */
    suspend fun checkForUpdate(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000; conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "MarketRadar/1.4")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            // Respect GitHub rate-limit headers: fall back to etag-less plain fetch.
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val releases = parseReleases(body)
            val current = installedVersionName()
            releases.firstOrNull { Version.isNewer(it.versionName(), current) }
        } catch (e: Exception) { null }
    }

    private fun parseReleases(body: String): List<GitHubRelease> {
        val list = mutableListOf<GitHubRelease>()
        try {
            val root = JsonParser.parseString(body).asJsonArray
            for (el in root) {
                val o = el.asJsonObject
                if (o.get("draft")?.asBoolean == true) continue
                if (o.get("prerelease")?.asBoolean == true) continue
                val tag = o.get("tag_name")?.asString ?: ""
                val name = o.get("name")?.asString ?: tag
                val published = o.get("published_at")?.asString ?: ""
                val bodyStr = o.get("body")?.asString
                val assets = o.get("assets")?.asJsonArray?.map { a ->
                    val ao = a.asJsonObject
                    GitHubAsset(
                        name = ao.get("name")?.asString ?: "",
                        downloadUrl = ao.get("browser_download_url")?.asString ?: "",
                        size = ao.get("size")?.asLong ?: 0L
                    )
                } ?: emptyList()
                // Extract versionCode from the tag (e.g. "v1.3" -> 1.3) or infer from version name.
                val vc = tag.replace("v", "").let { t ->
                    val parts = t.split('.', '-').mapNotNull { it.toIntOrNull() }
                    if (parts.isNotEmpty()) parts.fold(0) { acc, p -> acc * 10 + p } else 0
                }
                list += GitHubRelease(tagName = tag, name = name, publishedAt = parseDateMs(published), body = bodyStr, assets = assets, versionCode = vc)
            }
        } catch (e: Exception) { /* tolerate */ }
        return list
    }

    private fun parseDateMs(iso: String): Long = try {
        java.time.Instant.parse(iso).toEpochMilli()
    } catch (e: Exception) { 0L }

    /** Finds the APK asset attached to a release (prefers the release build). */
    fun apkAsset(release: GitHubRelease): GitHubAsset? =
        release.assets.firstOrNull { it.name.endsWith(".apk", true) && it.name.contains("release", true) }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", true) }

    /** Downloads the APK to the app cache and returns the file (no install needed). */
    suspend fun download(asset: GitHubAsset, status: (Float) -> Unit = {}): File? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(asset.downloadUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000; conn.readTimeout = 60000
            conn.setRequestProperty("User-Agent", "MarketRadar/1.3")
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            val out = File(appContext.cacheDir, "update_${asset.name}")
            conn.inputStream.use { ins ->
                out.outputStream().use { os ->
                    val buf = ByteArray(8192)
                    var read: Int; var done = 0L
                    while (ins.read(buf).also { read = it } > 0) {
                        os.write(buf, 0, read)
                        done += read
                        status((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            out
        } catch (e: Exception) { null }
    }

    /** Returns a content URI so the downloaded APK can be opened by the installer. */
    fun apkUri(file: File): Uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    /** Opens the system package installer for a downloaded APK. */
    fun install(file: File) {
        try {
            val uri = apkUri(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (e: Exception) { /* no installer */ }
    }
}
