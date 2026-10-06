@file:Suppress("unused", "NewApi")

package com.luckyzyx.luckytool.utils

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.luckyzyx.luckytool.data.AppInfo
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
class PackageUtils(private val packageManager: PackageManager) {

    /**
     * @see [PackageManager.getPackageArchiveInfo]
     */
    fun getPackageArchiveInfo(archiveFilePath: String, flag: Int): PackageInfo? {
        return try {
            if (SDK < A13) packageManager.getPackageArchiveInfo(archiveFilePath, flag)
            else packageManager.getPackageArchiveInfo(
                archiveFilePath, PackageManager.PackageInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getPackageInfo]
     */
    fun getPackageInfo(packName: String, flag: Int): PackageInfo? {
        return try {
            if (SDK < A13) packageManager.getPackageInfo(packName, flag)
            else packageManager.getPackageInfo(
                packName, PackageManager.PackageInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getNameForUid]
     */
    fun getNameForUid(uid: Int): String? {
        return packageManager.getNameForUid(uid)
    }

    /**
     * @see [PackageManager.getInstallSourceInfo]
     */
    fun getInstallSourceInfo(packName: String): InstallSourceInfo? {
        return try {
            packageManager.getInstallSourceInfo(packName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getPackageUid]
     */
    fun getPackageUid(packName: String, flag: Int): Int? {
        return try {
            if (SDK < A13) packageManager.getPackageUid(packName, flag)
            else packageManager.getPackageUid(
                packName, PackageManager.PackageInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getApplicationInfo]
     */
    fun getApplicationInfo(packName: String, flag: Int): ApplicationInfo? {
        return try {
            if (SDK < A13) packageManager.getApplicationInfo(packName, flag)
            else packageManager.getApplicationInfo(
                packName, PackageManager.ApplicationInfoFlags.of(flag.toLong())
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getApplicationLabel]
     */
    fun getApplicationLabel(applicationInfo: ApplicationInfo): CharSequence {
        return try {
            if (SDK < A13) packageManager.getApplicationLabel(applicationInfo)
            else packageManager.getApplicationLabel(applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    /**
     * @see [PackageManager.getApplicationIcon]
     */
    fun getApplicationIcon(applicationInfo: ApplicationInfo): Drawable? {
        return try {
            if (SDK < A13) packageManager.getApplicationIcon(applicationInfo)
            else packageManager.getApplicationIcon(applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getApplicationIcon]
     */
    fun getApplicationIcon(packName: String): Drawable? {
        return try {
            if (SDK < A13) packageManager.getApplicationIcon(packName)
            else packageManager.getApplicationIcon(packName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * @see [PackageManager.getInstalledPackages]
     */
    fun getInstalledPackages(flag: Int): MutableList<PackageInfo> {
        if (SDK < A13) return packageManager.getInstalledPackages(flag)
        return packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flag.toLong()))
    }

    /**
     * @see [PackageManager.getInstalledApplications]
     */
    fun getInstalledApplications(flag: Int): MutableList<ApplicationInfo> {
        return if (SDK < A13) packageManager.getInstalledApplications(flag)
        else packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flag.toLong()))
    }

    /**
     * @see [PackageManager.resolveActivity]
     */
    fun resolveActivity(intent: Intent, flag: Int): ResolveInfo? {
        return if (SDK < A13) packageManager.resolveActivity(intent, flag)
        else packageManager.resolveActivity(intent, ResolveInfoFlags.of(flag.toLong()))
    }

    /**
     * @see [PackageManager.getApplicationEnabledSetting]
     */
    fun getApplicationEnabledSetting(packName: String): Boolean {
        return packageManager.getApplicationEnabledSetting(packName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    /**
     * @see [PackageManager.getComponentEnabledSetting]
     */
    fun getComponentEnabledSetting(componentName: ComponentName): Int {
        return packageManager.getComponentEnabledSetting(componentName)
    }

    /**
     * @see [PackageManager.setComponentEnabledSetting]
     */
    fun setComponentEnabledSetting(componentName: ComponentName, newState: Int, flags: Int) {
        packageManager.setComponentEnabledSetting(componentName, newState, flags)
    }

    /**
     * @see [PackageManager.getLaunchIntentForPackage]
     */
    fun getLaunchIntentForPackage(packName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packName)
    }

    /**
     * @see [PackageManager.queryIntentActivities]
     */
    fun queryIntentActivities(intent: Intent, int: Int): MutableList<ResolveInfo> {
        return if (SDK < A13) packageManager.queryIntentActivities(intent, int)
        else packageManager.queryIntentActivities(intent, ResolveInfoFlags.of(int.toLong()))
    }

    fun getSupportedAbis(packageName: String): Array<String>? {
        return try {
            val packageInfo = getPackageInfo(packageName, 0)
                ?: return null
            packageInfo.applicationInfo?.nativeLibraryDir?.let { dir ->
                // 解析目录路径获取 ABI
                val abis = mutableListOf<String>()
                when {
                    dir.contains("arm64") -> abis.add("arm64-v8a")
                    dir.contains("arm") -> abis.add("armeabi-v7a")
                    dir.contains("x86_64") -> abis.add("x86_64")
                    dir.contains("x86") -> abis.add("x86")
                    dir.contains("mips64") -> abis.add("mips64")
                    dir.contains("mips") -> abis.add("mips")
                }
                abis.toTypedArray()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun getInstalledAppInfo(packName: String, flag: Int): AppInfo? {
        return getPackageInfo(packName, flag)?.toAppInfo(packageManager)
    }

    fun getInstalledAppInfos(flag: Int): ArrayList<AppInfo> {
        val appInfoList = ArrayList<AppInfo>()
        getInstalledPackages(flag).forEachIndexed { _, info ->
            val appInfo = info.toAppInfo(packageManager)
            if (appInfo != null) appInfoList.add(appInfo)
        }
        return appInfoList
    }

    companion object {
        fun PackageInfo.toAppInfo(pm: PackageManager): AppInfo? {
            return try {
                val appInfo = applicationInfo ?: return null
                val name = appInfo.loadLabel(pm)
                val icon = appInfo.loadIcon(pm)
                val size = FileUtils.getFileSize(File(appInfo.sourceDir))
                val versionName = versionName ?: ""
                val versionCode = longVersionCode
                val installTime = firstInstallTime
                val lastInstallTime = lastUpdateTime
                val target = appInfo.targetSdkVersion
                val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1
                val isOverlay = appInfo.isResourceOverlay
                val isEnable = PackageUtils(pm).getApplicationEnabledSetting(packageName)
                AppInfo(
                    name.toString(), packageName, icon, size, versionName, versionCode,
                    installTime, lastInstallTime, target, isSystem, isOverlay, isEnable
                )
            } catch (e: Exception) {
                LogUtils.e("toAppInfo", packageName, toString())
                null
            }
        }
    }
}

