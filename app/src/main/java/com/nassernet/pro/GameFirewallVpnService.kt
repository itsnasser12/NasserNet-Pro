
package com.nassernet.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log

class GameFirewallVpnService : VpnService() {

    companion object {
        const val ACTION_START =
            "com.nassernet.pro.action.START_VPN"

        const val ACTION_STOP =
            "com.nassernet.pro.action.STOP_VPN"

        private const val TAG = "NasserNetVPN"
        private const val CHANNEL_ID = "nassernet_vpn_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private var packetEngine: VpnPacketEngine? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> startProtection(startId)

            ACTION_STOP -> {
                stopProtection()
                stopSelf()
            }

            else -> {
                Log.w(TAG, "Unknown VPN action")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startProtection(startId: Int) {

        if (packetEngine?.isRunning == true) {
            Log.i(TAG, "VPN is already running")
            return
        }

        var engine: VpnPacketEngine? = null

        try {
            engine = VpnPacketEngine(this)

            // Establish the VPN tunnel first.
            if (!engine.start()) {
                Log.e(TAG, "Unable to establish VPN tunnel")
                engine.stop()
                stopSelf(startId)
                return
            }

            packetEngine = engine

            // Promote the service after successful tunnel creation.
            startForegroundNotification()

            Log.i(TAG, "VPN tunnel established successfully")

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "VPN startup failed",
                exception
            )

            try {
                engine?.stop()
            } catch (stopException: Exception) {
                Log.e(TAG, "Tunnel cleanup failed", stopException)
            }

            packetEngine = null
            stopProtection()
            stopSelf(startId)
        }
    }

    private fun startForegroundNotification() {

        val notification = Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("NasserNet Pro")
            .setContentText("Gaming network protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "NasserNet VPN Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "NasserNet Pro network protection status"
                setShowBadge(false)
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun stopProtection() {

        try {
            packetEngine?.stop()
            packetEngine = null

            Log.i(TAG, "VPN protection stopped")

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Error stopping VPN",
                exception
            )

        } finally {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked")

        stopProtection()
        stopSelf()

        super.onRevoke()
    }

    override fun onDestroy() {
        stopProtection()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
