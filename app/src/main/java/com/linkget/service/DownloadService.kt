package com.linkget.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.linkget.LinkGetApp
import com.linkget.data.download.DownloadSummary
import com.linkget.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Foreground service that carries the queue beyond the UI lifecycle. */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var container: AppContainer
    private lateinit var notifier: DownloadNotifier
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        container = (application as LinkGetApp).container
        notifier = DownloadNotifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (foregroundStarted) return START_NOT_STICKY

        try {
            ServiceCompat.startForeground(
                this,
                DownloadNotifier.PROGRESS_ID,
                notifier.progressNotification(DownloadSummary()),
                foregroundType()
            )
            foregroundStarted = true
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            val monitor = launch {
                container.downloads.observeSummary().collect(notifier::notifyProgress)
            }
            val completedBefore = container.dao.completedCount()
            val failedBefore = container.dao.failedCount()
            try {
                container.dao.resetRunning()
                while (true) {
                    container.downloads.processPending()
                    if (container.dao.pending().isEmpty()) {
                        delay(700)
                        if (container.dao.pending().isEmpty()) break
                    }
                }
            } finally {
                monitor.cancel()
            }
            val saved = (container.dao.completedCount() - completedBefore).coerceAtLeast(0)
            val failed = (container.dao.failedCount() - failedBefore).coerceAtLeast(0)
            ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
            notifier.notifyFinished(saved, failed)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

    companion object {
        const val ACTION_START = "com.linkget.action.START_DOWNLOAD"
    }
}
