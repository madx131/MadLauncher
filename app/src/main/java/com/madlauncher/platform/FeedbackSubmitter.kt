package com.madlauncher.platform

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.madlauncher.action.SearchableAppRequest
import com.madlauncher.data.RepoContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compatibility reports → GitHub `reports/compat/`.
 * Help-improve / bug notes → GitHub `reports/messages/`.
 * Searchable-app requests → GitHub `reports/searchable/`.
 * Failures stay local; do not open a share sheet (that is not the review path).
 */
object FeedbackSubmitter {
    private const val TAG = "FeedbackSubmitter"

    const val GITHUB_COMPAT_DIR = "reports/compat"
    const val GITHUB_MESSAGES_DIR = "reports/messages"
    const val GITHUB_SEARCHABLE_DIR = "reports/searchable"

    enum class SubmitOutcome {
        GITHUB_OK,
        SAVED_LOCAL,
    }

    data class SubmitResult(
        val outcome: SubmitOutcome,
        val savedFile: File?,
        val message: String,
        val htmlUrl: String? = null,
    )

    suspend fun submitCompatibilityReport(
        context: Context,
        report: CompatibilityReport,
    ): SubmitResult = withContext(Dispatchers.IO) {
        val file = saveReportFile(context, report)
        val jsonName = file.nameWithoutExtension + ".json"
        val jsonBody = report.toJson().toString(2)
        val txtPut = RepoContents.putText(
            context,
            "$GITHUB_COMPAT_DIR/${file.name}",
            report.toPlainText(),
            "compat: ${file.nameWithoutExtension}",
        )
        if (txtPut.ok) {
            runCatching {
                RepoContents.putText(
                    context,
                    "$GITHUB_COMPAT_DIR/$jsonName",
                    jsonBody,
                    "compat json: ${file.nameWithoutExtension}",
                )
            }
            return@withContext SubmitResult(
                SubmitOutcome.GITHUB_OK,
                file,
                "Compatibility report uploaded",
                txtPut.htmlUrl,
            )
        }
        Log.w(TAG, "GitHub compat upload failed: ${txtPut.error}")
        val err = txtPut.error.take(80)
        SubmitResult(
            SubmitOutcome.SAVED_LOCAL,
            file,
            if (txtPut.error.contains("token", ignoreCase = true)) {
                "Could not upload — GitHub token missing"
            } else {
                "Could not upload to GitHub ($err)"
            },
        )
    }

    suspend fun submitHelpImprove(
        context: Context,
        madVersion: String,
        name: String,
        email: String,
        reportBody: String,
    ): SubmitResult = withContext(Dispatchers.IO) {
        val text = buildString {
            appendLine("MAD Launcher message")
            appendLine("Version: $madVersion")
            if (name.isNotBlank()) appendLine("Name: $name")
            if (email.isNotBlank()) appendLine("Email: $email")
            appendLine()
            append(reportBody.trim())
        }
        val dir = File(context.filesDir, "help-improve").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val file = File(dir, "message-$stamp.txt")
        runCatching { file.writeText(text) }
        val put = RepoContents.putText(
            context,
            "$GITHUB_MESSAGES_DIR/${file.name}",
            text,
            "message: ${file.nameWithoutExtension}",
        )
        if (put.ok) {
            return@withContext SubmitResult(
                SubmitOutcome.GITHUB_OK,
                file,
                "Sent — thank you",
                put.htmlUrl,
            )
        }
        Log.w(TAG, "GitHub message upload failed: ${put.error}")
        SubmitResult(
            SubmitOutcome.SAVED_LOCAL,
            file,
            if (put.error.contains("token", ignoreCase = true)) {
                "Could not send — GitHub token missing"
            } else {
                "Could not upload to GitHub (${put.error.take(80)})"
            },
        )
    }

    suspend fun submitSearchableRequest(
        context: Context,
        request: SearchableAppRequest,
    ): SubmitResult = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "searchable-requests").apply { mkdirs() }
        val base = request.fileBase()
        val file = File(dir, "$base.txt")
        runCatching { file.writeText(request.toPlainText()) }
        val txtPut = RepoContents.putText(
            context,
            "$GITHUB_SEARCHABLE_DIR/${file.name}",
            request.toPlainText(),
            "searchable: ${request.packageName}",
        )
        if (txtPut.ok) {
            runCatching {
                RepoContents.putText(
                    context,
                    "$GITHUB_SEARCHABLE_DIR/$base.json",
                    request.toJson(),
                    "searchable json: ${request.packageName}",
                )
            }
            return@withContext SubmitResult(
                SubmitOutcome.GITHUB_OK,
                file,
                "Sent ${request.appLabel} — we'll try to wire it",
                txtPut.htmlUrl,
            )
        }
        Log.w(TAG, "GitHub searchable upload failed: ${txtPut.error}")
        val err = txtPut.error.take(80)
        SubmitResult(
            SubmitOutcome.SAVED_LOCAL,
            file,
            if (txtPut.error.contains("token", ignoreCase = true)) {
                "Could not upload — GitHub token missing"
            } else {
                "Could not upload to GitHub ($err)"
            },
        )
    }

    fun saveReportFile(context: Context, report: CompatibilityReport): File {
        val dir = File(context.filesDir, "compatibility-reports").apply { mkdirs() }
        val safeModel = report.device.model.replace(Regex("[^A-Za-z0-9._-]"), "_").take(24)
        val file = File(dir, "mad-compat-${safeModel}-${report.probedAtMillis}.txt")
        file.writeText(report.toPlainText())
        runCatching {
            File(dir, "mad-compat-latest.json").writeText(report.toJson().toString(2))
        }
        return file
    }

    fun shareReportFile(context: Context, file: File, chooserTitle: String = "Share report") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "MAD Launcher report: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun shareReportText(context: Context, text: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "MAD Launcher report")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
