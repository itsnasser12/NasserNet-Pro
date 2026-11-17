
package com.nassernet.pro

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.provider.Settings
import android.app.PendingIntent

class GameMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "nassernet_monitor"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NasserNet Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NasserNet Pro")
            .setContentText("Gaming protection monitor is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(
            Context.APP_OPS_SERVICE
        ) as android.app.AppOpsManager

        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )

        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
