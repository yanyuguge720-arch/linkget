package com.linkget.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.linkget.MainActivity
import com.linkget.R
import com.linkget.core.FileNames
import com.linkget.data.download.DownloadSummary

/** Builds the foreground progress notification and the final result notification. */
class DownloadNotifier(private val context: Context) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannel()
    }

    fun progressNotification(summary: DownloadSummary): Notification {
        val title = if (summary.active > 0) {
            "正在下载 " + summary.active + " 个文件"
        } else {
            "正在准备下载"
        }
        val speed = if (summary.speed > 0) " · " + FileNames.formatBytes(summary.speed) + "/s" else ""
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_save)
            .setContentTitle(title)
            .setContentText((summary.currentTitle ?: "文件会保存到手机媒体库") + speed)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setShowWhen(false)

        if (summary.percent >= 0) {
            builder.setProgress(100, summary.percent, false)
        } else {
            builder.setProgress(100, 0, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    fun notifyProgress(summary: DownloadSummary) {
        manager.notify(PROGRESS_ID, progressNotification(summary))
    }

    fun notifyFinished(saved: Int, failed: Int) {
        manager.cancel(PROGRESS_ID)
        val text = when {
            saved > 0 && failed > 0 -> "已保存 " + saved + " 个，" + failed + " 个失败"
            saved > 0 -> "已保存 " + saved + " 个文件，点击查看"
            failed > 0 -> "$failed 个文件下载失败，点击查看"
            else -> "任务已结束"
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_save)
            .setContentTitle(if (saved > 0) "下载完成" else "下载任务结束")
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        manager.notify(DONE_ID, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_downloads),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_downloads_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "downloads"
        const val PROGRESS_ID = 1001
        const val DONE_ID = 1002
    }
}
