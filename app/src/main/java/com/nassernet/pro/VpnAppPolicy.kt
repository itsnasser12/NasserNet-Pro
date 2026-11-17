
package com.nassernet.pro

import android.content.Context
import android.content.pm.ApplicationInfo

object VpnAppPolicy {

    /**
     * Returns packages that must bypass the local VPN.
     *
     * Bypass policy:
     * 1. NasserNet Pro itself.
     * 2. All system applications.
     * 3. User-selected games.
     *
     * Other installed user applications will use the VPN tunnel.
     */
    fun getDisallowedPackages(context: Context): Set<String> {

        val packageManager = context.packageManager

        val selectedGames = AppPrefs.getSelectedGames(context)

        val installedApplications =
            packageManager.getInstalledApplications(0)

        val excludedPackages = mutableSetOf<String>()

        // Always exclude NasserNet Pro itself.
        excludedPackages.add(context.packageName)

        for (appInfo in installedApplications) {

            val packageName = appInfo.packageName

            val isSystemApp =
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // System applications must retain normal connectivity.
            if (isSystemApp) {
                excludedPackages.add(packageName)
            }

            // Selected games must retain direct internet access.
            if (selectedGames.contains(packageName)) {
                excludedPackages.add(packageName)
            }
        }

        return excludedPackages
    }
}
