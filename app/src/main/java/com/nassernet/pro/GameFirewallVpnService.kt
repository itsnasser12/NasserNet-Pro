
package com.nassernet.pro

import android.net.VpnService
import android.content.Intent
import android.util.Log

class GameFirewallVpnService : VpnService() {

    companion object {
        const val ACTION_START =
            "com.nassernet.pro.action.START_VPN"

        const val ACTION_STOP =
            "com.nassernet.pro.action.STOP_VPN"

        private const val TAG = "NasserNetVPN"
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "VPN start requested")
            }

            ACTION_STOP -> {
                stopSelf()
                Log.i(TAG, "VPN stop requested")
            }
        }

        return START_NOT_STICKY
    }
}
