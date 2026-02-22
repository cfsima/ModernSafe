package io.github.cfsima.modernsafe.util

import android.content.Context
import android.content.pm.PackageManager
import io.github.cfsima.modernsafe.R

object VersionUtils {
    @JvmStatic
    fun getVersionNumber(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    @JvmStatic
    fun getApplicationName(context: Context): String {
        return context.getString(R.string.app_name)
    }
}
