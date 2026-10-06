package com.luckyzyx.luckytool.hook.scopes.android

import android.app.AppOpsManager
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 《自动释放应用空间》默认关闭（空间不足时自动卸载应用但保留数据）
 *
 * 每应用的自动释放开关存储于 AppOps OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED（c17 = 97）：
 *  - MODE_ALLOWED(0)  = 用户明确打开（允许该应用被自动归档）
 *  - MODE_ERRORED(1)  = 用户关闭（豁免）
 *  - MODE_DEFAULT(3)  = 默认态（原生视为允许归档）
 *
 * 原生判定（PackageArchiver.isAppOptedOutOfArchiving）：op == MODE_ERRORED(1) 才豁免，
 * 即新装应用默认参与自动归档。
 *
 * 本 hook 将判定改为：仅 MODE_ALLOWED(0) 允许，默认态与显式关闭均豁免——
 * 即《自动释放应用空间》对所有应用默认关闭，用户仍可在应用详情页手动打开单个应用。
 *
 * 设置侧开关（OplusHibernationSwitchPreferenceController）写的是同一个 op，
 * 用户手动打开后 op = 0，本判定放行，行为闭环。
 */
@Obfuscate
object DisableAppArchiving : YukiBaseHooker() {

    private const val TAG = "LuckyTool_DisableAppArchiving"

    override fun onHook() {
        var isEnable = preferences(ModulePrefs).getBoolean("disable_app_archiving", true)
        dataChannel.wait<Boolean>("disable_app_archiving") { isEnable = it }

        //Source PackageArchiver
        "com.android.server.pm.PackageArchiver".toClass().resolve().apply {
            firstMethod { name = "isAppOptedOutOfArchiving" }.intercept {
                if (!isEnable) return@intercept proceed()
                val packageName = arg<String>(0) ?: return@intercept proceed()
                val uid = arg<Int>(1) ?: return@intercept proceed()
                val context = firstField { type = Context::class }.of(instance).get<Context>()
                    ?: return@intercept proceed()
                val appOps =
                    context.getSystemService(classOf<AppOpsManager>()) ?: return@intercept proceed()
                //默认态(3)与显式关闭(1)均豁免，仅用户明确打开(0)才允许归档
                try {
                    appOps.checkOpNoThrow(
                        "android:auto_revoke_permissions_if_unused", uid, packageName
                    ) != AppOpsManager.MODE_ALLOWED
                } catch (_: Throwable) {
                    proceed()  //异常兜底：默认关闭
                }
            }
        }
    }
}
