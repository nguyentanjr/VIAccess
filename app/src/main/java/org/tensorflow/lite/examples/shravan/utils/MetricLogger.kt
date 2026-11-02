package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MetricLogger {
    private const val TAG = "VIA_METRIC"
    private val events = ArrayDeque<MetricEvent>()

    data class MetricEvent(
        val timestampUtc: String,
        val elapsedRealtimeNs: Long,
        val frameId: Long?,
        val event: String,
        val classId: Int?,
        val confidence: Float?,
        val latencyMs: Long?,
        val baselineMode: Boolean,
        val detail: String
    )

    @Synchronized
    fun log(
        event: String,
        frameId: Long? = null,
        classId: Int? = null,
        confidence: Float? = null,
        latencyMs: Long? = null,
        baselineMode: Boolean = false,
        detail: String = ""
    ) {
        val metric = MetricEvent(
            timestampUtc = utcTimestamp(),
            elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos(),
            frameId = frameId,
            event = event,
            classId = classId,
            confidence = confidence,
            latencyMs = latencyMs,
            baselineMode = baselineMode,
            detail = detail
        )
        events.addLast(metric)
        while (events.size > FeedbackConfig.METRIC_MAX_ROWS) events.removeFirst()
        Log.i(TAG, metric.toCsvRow())
    }

    fun exportCsv(context: Context, uri: Uri): Int {
        val snapshot = synchronized(this) { events.toList() }
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open the selected CSV destination")
        BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { writer ->
            writer.write(
                "timestamp_utc,elapsed_realtime_ns,frame_id,event,class_id,confidence," +
                    "latency_ms,baseline_mode,detail"
            )
            writer.newLine()
            snapshot.forEach { metric ->
                writer.write(metric.toCsvRow())
                writer.newLine()
            }
        }
        return snapshot.size
    }

    private fun utcTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    private fun MetricEvent.toCsvRow(): String = listOf(
        timestampUtc,
        elapsedRealtimeNs.toString(),
        frameId?.toString().orEmpty(),
        event,
        classId?.toString().orEmpty(),
        confidence?.toString().orEmpty(),
        latencyMs?.toString().orEmpty(),
        baselineMode.toString(),
        detail
    ).joinToString(",") { it.toCsvCell() }

    private fun String.toCsvCell(): String =
        if (contains(',') || contains('"') || contains('\n') || contains('\r')) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
}
