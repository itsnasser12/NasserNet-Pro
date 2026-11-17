
package com.nassernet.pro

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class InstalledAppsRepository(
    private val context: Context
) {

    private val packageManager: PackageManager =
        context.packageManager

    fun getUserApps(): List<AppInfo> {
        return getInstalledApps().filter {
            !it.isSystemApp
        }
    }

    fun getSystemApps(): List<AppInfo> {
        return getInstalledApps().filter {
            it.isSystemApp
        }
    }

    private fun getInstalledApps(): List<AppInfo> {

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolveInfo ->

                val appInfo = resolveInfo.activityInfo?.applicationInfo
                    ?: return@mapNotNull null

                val packageName = appInfo.packageName

                if (packageName == context.packageName) {
                    return@mapNotNull null
                }

                val isSystemApp =
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                AppInfo(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    appIcon = resolveInfo.loadIcon(packageManager),
                    isSystemApp = isSystemApp
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }
}
