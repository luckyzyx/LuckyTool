package com.luckyzyx.luckytool.hook.scopes.settings

import android.app.AppOpsManager
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 《自动释放应用空间》默认关闭 - 设置页开关 UI 显示（settings 进程）
 *
 * 配合 framework 侧 DisableAppArchiving（实际归档判定）使用：
 * 原生 UI 显示逻辑 isPackageHibernationExemptByUser：默认态(op=3)且 targetSdk>29 的新应用
 * 显示开关为"开"，与 framework 判定不一致；本 hook 将 UI 显示改为与判定一致——
 * 仅 MODE_ALLOWED(0)（用户明确打开）显示开，默认态与显式关闭均显示关
 */
@Obfuscate
object DisableAppArchivingUI : YukiBaseHooker() {

    override fun onHook() {
        var isEnable = preferences(ModulePrefs).getBoolean("disable_app_archiving", true)
        dataChannel.wait<Boolean>("disable_app_archiving") { isEnable = it }

        //Source HibernationSwitchPreferenceController（设置应用详情页开关 UI）
        "com.android.settings.applications.appinfo.HibernationSwitchPreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "isPackageHibernationExemptByUser" }.intercept {
                    if (!isEnable) return@intercept proceed()
                    val uid = firstField { name = "mPackageUid" }.of(instance).get<Int>()
                        ?: return@intercept proceed()
                    val packageName =
                        firstField { name = "mPackageName" }.of(instance).get<String>()
                            ?: return@intercept proceed()
                    val context = firstField { type = Context::class }.of(instance).get<Context>()
                        ?: return@intercept proceed()
                    val appOps = context.getSystemService(classOf<AppOpsManager>())
                        ?: return@intercept proceed()
                    //默认态(3)与显式关闭(1)均显示关，仅用户明确打开(0)才显示开
                    try {
                        appOps.checkOpNoThrow(
                            "android:auto_revoke_permissions_if_unused", uid, packageName
                        ) != AppOpsManager.MODE_ALLOWED
                    } catch (_: Throwable) {
                        proceed()  //异常兜底：走原生显示
                    }
                }
            }
    }
}
