package com.madlauncher.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.madlauncher.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * GitHub Contents API for `madx131/MadLauncher` (private).
 * Token from BuildConfig / gitignored `repo_token.txt` — never log it.
 * Lists `lang/` only from the app; reports are write paths for feedback.
 */
object RepoContents {
    private const val TAG = "RepoContents"
    const val OWNER = "madx131"
    const val REPO = "MadLauncher"
    const val USER_AGENT = "MAD-Launcher"
    private const val API = "https://api.github.com"
    private const val CONNECT_MS = 15_000
    private const val READ_MS = 20_000

    data class Entry(
        val name: String,
        val path: String,
        val sha: String,
        val type: String,
        val downloadUrl: String?,
    )

    data class PutResult(
        val ok: Boolean,
        val htmlUrl: String? = null,
        val error: String = "",
    )

    fun token(context: Context): String {
        val fromBuild = BuildConfig.MAD_REPO_TOKEN.trim()
        if (fromBuild.isNotEmpty()) return fromBuild
        return runCatching {
            context.assets.open("repo_token.txt").bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                    .orEmpty()
            }
        }.getOrDefault("")
    }

    fun hasToken(context: Context): Boolean = token(context).isNotBlank()

    fun listDir(context: Context, path: String): Result<List<Entry>> = runCatching {
        val encoded = path.trim('/').split('/').joinToString("/") { encodePathSeg(it) }
        val text = httpJson(context, "GET", "/repos/$OWNER/$REPO/contents/$encoded")
        val arr = JSONArray(text)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    Entry(
                        name = o.optString("name"),
                        path = o.optString("path"),
                        sha = o.optString("sha"),
                        type = o.optString("type"),
                        downloadUrl = o.optString("download_url").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    fun getBytes(context: Context, path: String, readTimeoutMs: Int = READ_MS): Result<ByteArray> = runCatching {
        val encoded = path.trim('/').split('/').joinToString("/") { encodePathSeg(it) }
        val raw = httpBytes(
            context,
            "GET",
            "/repos/$OWNER/$REPO/contents/$encoded",
            extraHeaders = mapOf("Accept" to "application/vnd.github.raw"),
            readTimeoutMs = readTimeoutMs,
        )
        if (!looksLikeGithubContentsMeta(raw)) return@runCatching raw
        val meta = JSONObject(raw.toString(StandardCharsets.UTF_8))
        val downloadUrl = meta.optString("download_url")
        if (downloadUrl.isNotBlank()) {
            return@runCatching httpAbsolute(context, downloadUrl, readTimeoutMs)
        }
        val sha = meta.optString("sha")
        if (sha.isBlank()) error("GitHub file missing sha")
        val blobBytes = httpBytes(
            context,
            "GET",
            "/repos/$OWNER/$REPO/git/blobs/$sha",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
            readTimeoutMs = readTimeoutMs,
        )
        val b64 = JSONObject(blobBytes.toString(StandardCharsets.UTF_8))
            .optString("content")
            .replace("\n", "")
        if (b64.isBlank()) error("GitHub blob empty")
        Base64.decode(b64, Base64.DEFAULT)
    }

    private fun looksLikeGithubContentsMeta(bytes: ByteArray): Boolean {
        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) return false
        val head = bytes.toString(StandardCharsets.UTF_8).trimStart().take(240)
        return head.startsWith("{") &&
            head.contains("\"sha\"") &&
            (head.contains("\"encoding\"") || head.contains("\"git_url\""))
    }

    fun getText(context: Context, path: String): Result<String> =
        getBytes(context, path).map { it.toString(StandardCharsets.UTF_8) }

    /** Contents metadata + authenticated download. Use for APKs and other files over 1 MB. */
    fun getBinaryFile(context: Context, path: String, readTimeoutMs: Int = 120_000): Result<ByteArray> = runCatching {
        val encoded = path.trim('/').split('/').joinToString("/") { encodePathSeg(it) }
        val meta = JSONObject(
            httpBytes(
                context,
                "GET",
                "/repos/$OWNER/$REPO/contents/$encoded",
                extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
                readTimeoutMs = readTimeoutMs,
            ).toString(StandardCharsets.UTF_8),
        )
        val downloadUrl = meta.optString("download_url")
        if (downloadUrl.isNotBlank()) {
            return@runCatching httpAbsolute(context, downloadUrl, readTimeoutMs)
        }
        val sha = meta.optString("sha")
        if (sha.isBlank()) error("GitHub file missing sha")
        val blobBytes = httpBytes(
            context,
            "GET",
            "/repos/$OWNER/$REPO/git/blobs/$sha",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
            readTimeoutMs = readTimeoutMs,
        )
        val b64 = JSONObject(blobBytes.toString(StandardCharsets.UTF_8))
            .optString("content")
            .replace("\n", "")
        if (b64.isBlank()) error("GitHub blob empty")
        Base64.decode(b64, Base64.DEFAULT)
    }

    fun putText(
        context: Context,
        path: String,
        content: String,
        message: String,
    ): PutResult = putBytes(context, path, content.toByteArray(StandardCharsets.UTF_8), message)

    fun putBytes(
        context: Context,
        path: String,
        content: ByteArray,
        message: String,
    ): PutResult {
        val encoded = path.trim('/').split('/').joinToString("/") { encodePathSeg(it) }
        val sha = existingSha(context, encoded)
        val blobSha = gitBlobSha(content)
        if (!sha.isNullOrBlank() && sha == blobSha) {
            return PutResult(ok = true)
        }
        val body = JSONObject()
            .put("message", message)
            .put("content", Base64.encodeToString(content, Base64.NO_WRAP))
        if (!sha.isNullOrBlank()) body.put("sha", sha)
        return runCatching {
            val text = httpJson(
                context,
                "PUT",
                "/repos/$OWNER/$REPO/contents/$encoded",
                body.toString().toByteArray(StandardCharsets.UTF_8),
            )
            val json = JSONObject(text)
            val contentObj = json.optJSONObject("content")
            PutResult(
                ok = true,
                htmlUrl = contentObj?.optString("html_url")?.takeIf { it.isNotBlank() }
                    ?: json.optJSONObject("commit")?.optString("html_url"),
            )
        }.getOrElse { e ->
            Log.w(TAG, "PUT $path failed: ${e.message}")
            PutResult(false, error = e.message ?: "put_failed")
        }
    }

    private fun existingSha(context: Context, encodedPath: String): String? {
        return runCatching {
            val text = httpJson(context, "GET", "/repos/$OWNER/$REPO/contents/$encodedPath")
            JSONObject(text).optString("sha").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun encodePathSeg(seg: String): String =
        java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20")

    private fun gitBlobSha(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(StandardCharsets.UTF_8)
        val md = java.security.MessageDigest.getInstance("SHA-1")
        md.update(header)
        md.update(bytes)
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private fun httpJson(
        context: Context,
        method: String,
        apiPath: String,
        body: ByteArray? = null,
    ): String = httpBytes(context, method, apiPath, body).toString(StandardCharsets.UTF_8)

    private fun httpBytes(
        context: Context,
        method: String,
        apiPath: String,
        body: ByteArray? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        readTimeoutMs: Int = READ_MS,
    ): ByteArray {
        val tok = token(context)
        if (tok.isBlank()) error("GitHub token missing")
        val url = API + apiPath
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_MS
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            doInput = true
            setRequestProperty("Authorization", "Bearer $tok")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", extraHeaders["Accept"] ?: "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Content-Length", body.size.toString())
            }
        }
        try {
            if (body != null) {
                BufferedOutputStream(conn.outputStream).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.readBytes() ?: ByteArray(0)
            if (code !in 200..299) {
                val preview = bytes.toString(StandardCharsets.UTF_8).take(180).replace('\n', ' ')
                error("GitHub HTTP $code: $preview")
            }
            return bytes
        } finally {
            conn.disconnect()
        }
    }

    private fun httpAbsolute(context: Context, absoluteUrl: String, readTimeoutMs: Int): ByteArray {
        val tok = token(context)
        if (tok.isBlank()) error("GitHub token missing")
        val conn = (URL(absoluteUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_MS
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            doInput = true
            setRequestProperty("Authorization", "Bearer $tok")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.readBytes() ?: ByteArray(0)
            if (code !in 200..299) {
                val preview = bytes.toString(StandardCharsets.UTF_8).take(180).replace('\n', ' ')
                error("GitHub HTTP $code: $preview")
            }
            return bytes
        } finally {
            conn.disconnect()
        }
    }
}
