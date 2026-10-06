package com.luckyzyx.luckytool.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.ArraySet
import androidx.core.net.toUri
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.data.AppVerInfo
import kotlinx.serialization.json.Json
import org.lsposed.lsparanoid.Obfuscate

@Suppress("MemberVisibilityCanBePrivate")
@Obfuscate
class AppUtils(val context: Context) {

    val packageUtils = PackageUtils(context.packageManager)

    /***
     * 获取APP Commit
     * @receiver Context
     * @param packName String
     * @return String
     */
    fun getAppCommit(packName: String): String {
        val appInfo = packageUtils.getApplicationInfo(packName, PackageManager.GET_META_DATA)
            ?: return ""
        @Suppress("DEPRECATION")
        return appInfo.metaData?.get("versionCommit")?.toString() ?: ""
    }

    /**
     * 获取APP Meta键值
     * @param appInfo ApplicationInfo
     * @param key String
     * @return String
     */
    fun getAppMeta(appInfo: ApplicationInfo, key: String): String {
        @Suppress("DEPRECATION")
        return appInfo.metaData?.get(key)?.toString() ?: ""
    }

    /**
     * 获取APP Meta键值
     * @param packName String
     * @param key String
     * @return String
     */
    fun getAppMeta(packName: String, key: String, default: String = ""): String {
        val appInfo = packageUtils.getApplicationInfo(packName, PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        return appInfo?.metaData?.get(key)?.toString() ?: default
    }

    /**
     * 获取APP版本/版本号/Commit
     * 写入SP xml文件内
     * @return [ArraySet]
     */
    fun getAppVerInfo(packName: String, save: Boolean = true): AppVerInfo? {
        return safeOfNull {
            val packageInfo = packageUtils.getPackageInfo(packName, PackageManager.GET_META_DATA)
                ?: return null
            val appInfo = packageInfo.applicationInfo ?: return null
            val appName = packageUtils.getApplicationLabel(appInfo).toString()
            val versionName = packageInfo.versionName ?: ""
            val versionCode = packageInfo.longVersionCode
            //修复versionCommit获取null
            val versionCommit = getAppMeta(appInfo, "versionCommit")
            val versionDate = getAppMeta(appInfo, "versionDate")
            //Fix the camera's commit is empty
            val commit = versionCommit.ifBlank { versionDate }
            val appVerInfo = AppVerInfo(appName, packName, versionName, versionCode, commit)
            if (save) {
                context.putStringSet(
                    ModulePrefs, packName, listOf(
                        safeOfNull { Json.encodeToString(appVerInfo) } ?: ""
                    ).toSet())
            }
            appVerInfo
        }
    }

    /**
     * 获取所有APP版本/版本号/Commit
     * 写入SP xml文件内
     * @return [ArrayList]
     */
    fun getAllAppVerInfo(packs: Array<String>, save: Boolean = true): ArrayList<AppVerInfo> {
        return ArrayList<AppVerInfo>().apply {
            packs.forEachIndexed { _, packName ->
                safeOfNull {
                    val packageInfo =
                        packageUtils.getPackageInfo(packName, PackageManager.GET_META_DATA)
                            ?: return@forEachIndexed
                    val appInfo = packageInfo.applicationInfo ?: return@forEachIndexed
                    val appName = packageUtils.getApplicationLabel(appInfo).toString()
                    val versionName = packageInfo.versionName ?: ""
                    val versionCode = packageInfo.longVersionCode
                    //修复versionCommit获取null
                    val versionCommit = getAppMeta(appInfo, "versionCommit")
                    val versionDate = getAppMeta(appInfo, "versionDate")
                    //Fix the camera's commit is empty
                    val commit = versionCommit.ifBlank { versionDate }
                    val appVerInfo = AppVerInfo(appName, packName, versionName, versionCode, commit)
                    if (save) {
                        context.putStringSet(
                            ModulePrefs, packName, listOf(
                                safeOfNull { Json.encodeToString(appVerInfo) } ?: ""
                            ).toSet())
                    }
                    appVerInfo
                }?.let { add(it) }
            }
        }
    }

    /**
     * 获取APP图标
     * @receiver Context
     * @param packName String
     * @return Drawable?
     */
    fun getAppIcon(packName: String): Drawable? {
        return packageUtils.getApplicationIcon(packName)
    }

    /**
     * 获取APP版本名
     * @receiver Context
     * @param packName String
     * @return String?
     */
    fun getAppVersionName(packName: String): String? {
        return packageUtils.getPackageInfo(packName, 0)?.versionName
    }

    /**
     * 获取APP版本号
     * @receiver Context
     * @param packName String
     * @return Long?
     */
    fun getAppVersionCode(packName: String): Long? {
        return packageUtils.getPackageInfo(packName, 0)?.longVersionCode
    }

    /**
     * 获取APP名称
     * @receiver Context
     * @param packName String 包名
     * @return CharSequence?  若为Null 返回包名
     */
    fun getAppLabel(packName: String): CharSequence {
        return getAppLabelOrNull(packName).ifBlank { packName }
    }

    /**
     * 获取APP名称
     * @receiver Context
     * @param packName String
     * @return CharSequence?
     */
    fun getAppLabelOrNull(packName: String): CharSequence {
        return packageUtils.getApplicationInfo(packName, 0)?.let {
            packageUtils.getApplicationLabel(it)
        } ?: ""
    }

    /**
     * 禁用组件
     * @receiver Context
     * @param value Boolean
     */
    fun setComponentDisabled(component: ComponentName, value: Boolean) {
        packageUtils.setComponentEnabledSetting(
            component,
            if (value) COMPONENT_ENABLED_STATE_DISABLED else COMPONENT_ENABLED_STATE_ENABLED,
            DONT_KILL_APP
        )
    }

    /**
     * 获取组件状态
     * @receiver Context
     * @param component ComponentName
     * @return Int?
     */
    fun getComponentEnabled(component: ComponentName): Int? {
        return when (packageUtils.getComponentEnabledSetting(component)) {
            COMPONENT_ENABLED_STATE_DEFAULT -> COMPONENT_ENABLED_STATE_DEFAULT
            COMPONENT_ENABLED_STATE_ENABLED -> COMPONENT_ENABLED_STATE_ENABLED
            COMPONENT_ENABLED_STATE_DISABLED -> COMPONENT_ENABLED_STATE_DISABLED
            COMPONENT_ENABLED_STATE_DISABLED_USER -> COMPONENT_ENABLED_STATE_DISABLED_USER
            COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
            else -> null
        }
    }

    /**
     * 跳转到应用详情界面
     * @param packName String
     * @param userId Int?
     */
    fun openAppDetailIntent(packName: String, userId: Int?) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        userId?.let { intent.putExtra("userId", it) }
        context.startActivity(intent)
    }

    /**
     * 跳转商店页面
     * @receiver Context
     * @param packName String
     */
    fun openMarketIntent(packName: String) {
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packName".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        context.startActivity(intent)
    }

    /**
     * 跳转分身页面
     * @receiver Context
     * @param packName String
     */
    fun openMultiAppIntent(label: CharSequence, packName: String) {
        val intent = Intent().apply {
            setClassName(
                "com.oplus.multiapp",
                "com.oplus.multiapp.ui.settings.ActivitySettingsActivity"
            )
            setPackage("com.oplus.multiapp")
            putExtra("title", label)
            putExtra("pkgName", packName)
        }
        context.startActivity(intent)
    }

    /**
     * 跳转耗电控制页面
     * @receiver Context
     * @param packName String
     */
    fun openPowerControlIntent(label: CharSequence, packName: String) {
        val intent = Intent().apply {
            setClassName(
                "com.oplus.battery",
                "com.oplus.powermanager.fuelgaue.PowerControlActivity"
            )
            putExtra("title", "Title")
            putExtra(
                "pkgName",
                BuildConfig.APPLICATION_ID
            )
            putExtra("callingSource", -1)
            putExtra("drainType", "APP")
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}