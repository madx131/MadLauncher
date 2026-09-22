package com.madlauncher.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.madlauncher.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Share the public MAD APK. Only for distributor builds
 * ([BuildConfig.SHOW_APK_SHARE] — requires local MAD_REPO_TOKEN, not baked into public APKs).
 *
 * The GitHub raw URL in [DEFAULT_DOWNLOAD_URL] is private — it only works when signed in.
 * Send the APK file instead: this downloads `releases/MAD-launcher-latest.apk` (public build,
 * no token) with the distributor token and shares that file.
 */
object MadApkShare {
    const val RELEASE_APK_PATH = "releases/MAD-launcher-latest.apk"

    /** Default raw GitHub path for the latest public APK in-repo. Private unless signed in. */
    const val DEFAULT_DOWNLOAD_URL =
        "https://github.com/madx131/MadLauncher/raw/main/releases/MAD-launcher-latest.apk"

    fun isDistributorBuild(): Boolean = BuildConfig.SHOW_APK_SHARE

    fun downloadUrl(): String =
        BuildConfig.MAD_SHARE_APK_URL.trim().ifBlank { DEFAULT_DOWNLOAD_URL }

    fun shareText(): String =
        "MAD Launcher ${BuildConfig.VERSION_NAME}\n${downloadUrl()}"

    fun shareIntent(): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MAD Launcher ${BuildConfig.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, shareText())
        }

    suspend fun preparePublicApk(context: Context): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val dest = File(dir, "MAD-launcher-latest.apk")
            val bytes = RepoContents.getBinaryFile(context, RELEASE_APK_PATH, readTimeoutMs = 120_000)
                .getOrElse { err -> error(err.message ?: "Download failed") }
            if (bytes.size < 50_000 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
                error("Downloaded file is not an APK")
            }
            dest.writeBytes(bytes)
            dest
        }
    }

    fun apkFileIntent(context: Context, apk: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MAD Launcher ${BuildConfig.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, "MAD Launcher ${BuildConfig.VERSION_NAME}")
            clipData = ClipData.newRawUri("apk", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun qrBitmap(sizePx: Int = 512, url: String = downloadUrl()): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
