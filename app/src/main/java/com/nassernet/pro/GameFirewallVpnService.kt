
package com.nassernet.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

        const val ACTION_STATUS =
            "com.nassernet.pro.action.VPN_STATUS"

        const val EXTRA_STATUS = "vpn_status"
        const val EXTRA_MESSAGE = "vpn_message"

        const val STATUS_STARTING = "STARTING"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_STOPPED = "STOPPED"

        private const val TAG = "NasserNetVPN"
        private const val CHANNEL_ID = "nassernet_vpn_channel"
        private const val NOTIFICATION_ID = 2001
        private const val STOP_PENDING_INTENT_ID = 2002
        private const val OPEN_PENDING_INTENT_ID = 2003
    }

    private var packetEngine: VpnPacketEngine? = null

    @Volatile
    private var currentStatus = STATUS_STOPPED

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

            ACTION_STATUS -> {
                Log.i(TAG, "Status requested")

                val message = when (currentStatus) {
                    STATUS_ACTIVE -> "VPN protection is active"
                    STATUS_STARTING -> "VPN protection is starting"
                    STATUS_FAILED -> "VPN protection failed"
                    else -> "VPN protection is stopped"
                }

                publishStatus(currentStatus, message)

                // If this was only a status query and the VPN
                // is stopped, do not leave an idle service running.
                if (
                    currentStatus == STATUS_STOPPED ||
                    currentStatus == STATUS_FAILED
                ) {
                    stopSelf(startId)
                }
            }

            else -> {
                Log.w(TAG, "Unknown VPN action")

                publishStatus(
                    currentStatus,
                    "Current VPN status"
                )
            }
        }

        return START_NOT_STICKY
    }

    private fun setCurrentStatus(
        status: String,
        message: String
    ) {
        currentStatus = status
        publishStatus(status, message)
    }

    private fun startProtection(startId: Int) {

        if (packetEngine?.isRunning == true) {
            Log.i(TAG, "VPN is already running")

            setCurrentStatus(
                STATUS_ACTIVE,
                "VPN protection is active"
            )

            updateForegroundNotification(
                "Gaming network protection is active"
            )

            return
        }

        var engine: VpnPacketEngine? = null

        setCurrentStatus(
            STATUS_STARTING,
            "Starting VPN protection"
        )

        try {

            startForegroundNotification(
                "Starting network protection..."
            )

            engine = VpnPacketEngine(this)

            if (!engine.start()) {
                throw IllegalStateException(
                    "Unable to establish VPN tunnel"
                )
            }

            packetEngine = engine

            updateForegroundNotification(
                "Gaming network protection is active"
            )

            Log.i(
                TAG,
                "VPN tunnel established successfully"
            )

            setCurrentStatus(
                STATUS_ACTIVE,
                "VPN protection is active"
            )

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "VPN startup failed",
                exception
            )

            try {
                engine?.stop()
            } catch (stopException: Exception) {
                Log.e(
                    TAG,
                    "Tunnel cleanup failed",
                    stopException
                )
            }

            packetEngine = null
            removeForegroundNotification()

            setCurrentStatus(
                STATUS_FAILED,
                exception.localizedMessage
                    ?: "VPN startup failed"
            )

            stopSelf(startId)
        }
    }

    private fun startForegroundNotification(
        message: String
    ) {

        val notification = buildNotification(message)

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

    private fun updateForegroundNotification(
        message: String
    ) {

        val notification = buildNotification(message)

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        message: String
    ): Notification {

        val openIntent = Intent(
            this,
            MainActivity::class.java
        )

        val openPendingIntent = PendingIntent.getActivity(
            this,
            OPEN_PENDING_INTENT_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(
            this,
            GameFirewallVpnService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_PENDING_INTENT_ID,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopAction = Notification.Action.Builder(
            null,
            "STOP",
            stopPendingIntent
        ).build()

        val builder = Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("NasserNet Pro")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_stat_nassernet)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(stopAction)

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(
                Notification.FOREGROUND_SERVICE_IMMEDIATE
            )
        }

        return builder.build()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "NasserNet VPN Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "Persistent NasserNet Pro VPN protection status"

                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun publishStatus(
        status: String,
        message: String
    ) {

        val intent = Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_MESSAGE, message)
        }

        sendBroadcast(intent)

        Log.i(
            TAG,
            "VPN status: $status - $message"
        )
    }

    private fun stopProtection() {

        cleanupProtection()

        setCurrentStatus(
            STATUS_STOPPED,
            "VPN protection stopped"
        )

        Log.i(TAG, "VPN protection stopped")
    }

    private fun cleanupProtection() {

        try {
            packetEngine?.stop()
            packetEngine = null

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Error stopping VPN",
                exception
            )

        } finally {
            removeForegroundNotification()
        }
    }

    private fun removeForegroundNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onRevoke() {

        Log.w(TAG, "VPN permission revoked")

        stopProtection()
        stopSelf()

        super.onRevoke()
    }

    override fun onDestroy() {

        cleanupProtection()

        if (currentStatus != STATUS_STOPPED) {
            setCurrentStatus(
                STATUS_STOPPED,
                "VPN service stopped"
            )
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
