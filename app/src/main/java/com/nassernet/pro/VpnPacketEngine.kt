
package com.nassernet.pro

import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.IOException

class VpnPacketEngine(
    private val vpnService: VpnService
) {

    companion object {
        private const val SESSION_NAME = "NasserNet Pro Firewall"
        private const val MTU = 1500

        private const val IPV4_ADDRESS = "10.111.222.1"
        private const val IPV6_ADDRESS = "fd00:1234:5678::1"
    }

    @Volatile
    private var tunnel: ParcelFileDescriptor? = null

    @Volatile
    private var readerThread: Thread? = null

    val isRunning: Boolean
        get() = tunnel != null

    @Synchronized
    fun start(): Boolean {

        if (tunnel != null) {
            return true
        }

        var newTunnel: ParcelFileDescriptor? = null

        try {
            val excludedPackages =
                VpnAppPolicy.getDisallowedPackages(vpnService)

            val builder = vpnService.Builder()
                .setSession(SESSION_NAME)
                .setMtu(MTU)
                .addAddress(IPV4_ADDRESS, 32)
                .addAddress(IPV6_ADDRESS, 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)

            for (packageName in excludedPackages) {
                builder.addDisallowedApplication(packageName)
            }

            newTunnel = builder.establish() ?: return false

            tunnel = newTunnel

            val activeTunnel = newTunnel

            val thread = Thread({

                try {
                    ParcelFileDescriptor.AutoCloseInputStream(
                        activeTunnel
                    ).use { input ->

                        val buffer = ByteArray(32767)

                        while (!Thread.currentThread().isInterrupted) {

                            val bytesRead = input.read(buffer)

                            if (bytesRead < 0) {
                                break
                            }

                            // Packets are intentionally discarded.
                        }
                    }

                } catch (exception: IOException) {
                    // The tunnel may be closed during normal shutdown.

                } finally {
                    synchronized(this) {
                        if (tunnel === activeTunnel) {
                            tunnel = null
                            readerThread = null
                        }
                    }
                }

            }, "NasserNet-Packet-Reader")

            thread.isDaemon = true
            readerThread = thread
            thread.start()

            return true

        } catch (exception: Exception) {

            try {
                newTunnel?.close()
            } catch (_: IOException) {
            }

            tunnel = null
            readerThread = null

            return false
        }
    }

    @Synchronized
    fun stop() {

        val activeTunnel = tunnel
        val activeThread = readerThread

        tunnel = null
        readerThread = null

        activeThread?.interrupt()

        try {
            activeTunnel?.close()
        } catch (_: IOException) {
        }
    }
}
