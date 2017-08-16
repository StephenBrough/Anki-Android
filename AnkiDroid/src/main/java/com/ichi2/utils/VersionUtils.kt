package com.ichi2.utils

import android.content.pm.PackageManager
import com.ichi2.anki.AnkiDroidApp
import timber.log.Timber


object VersionUtils {


    /**
     * Get package name as defined in the manifest.
     */
    val appName: String
        get() {
            var pkgName = AnkiDroidApp.TAG
            val context = AnkiDroidApp.getInstance()

            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pkgName = context.getString(pInfo.applicationInfo.labelRes)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e, "Couldn't find package named %s", context.packageName)
            }

            return pkgName
        }

    /**
     * Get the package versionName as defined in the manifest.
     */
    val pkgVersionName: String
        get() {
            var pkgVersion = "?"
            val context = AnkiDroidApp.getInstance()
            if (context != null) {
                try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pkgVersion = pInfo.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e, "Couldn't find package named %s", context.packageName)
                }

            }
            return pkgVersion
        }


    /**
     * Get the package versionCode as defined in the manifest.
     */
    val pkgVersionCode: Int
        get() {
            val context = AnkiDroidApp.getInstance()
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return pInfo.versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e, "Couldn't find package named %s", context.packageName)
            }

            return 0
        }

    /**
     * Return whether the package version code is set to that for release version
     * @return whether build number in manifest version code is '3'
     */
    val isReleaseVersion: Boolean
        get() {
            val versionCode = Integer.toString(pkgVersionCode)
            return versionCode[versionCode.length - 3] == '3'
        }
}
