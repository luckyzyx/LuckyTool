package com.luckyzyx.luckytool.hook.scopes.ridermode

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.ArrayClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

/**
 * 高能户外模式 App 侧白名单，让选应用页能显示任意可启动应用：
 * - 旧版 UI 的名单来自 SupportAppHelper.a(Context)，直接替换为全部可启动应用；
 * - 新版 17.x workbench 选应用页的名单来自 AppOpenModeUtils 的配置 Map 解析，
 *   这里保留官方名单条目（含原分区），把不在名单内的可启动应用追加到「其他」分区。
 * 混淆目标全部用 DexKit 按类内唯一字符串运行时定位，不写死混淆后的类名/方法名。
 */
@Obfuscate
class RiderModeAppListWhitelist(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        hookSupportAppHelper()
        hookWorkbenchConfigParser()
    }

    /** 旧版支持应用名单：SupportAppHelper.a(Context) 返回「名称,包名,图标名」数组。 */
    private fun hookSupportAppHelper() {
        dexKitBridge.findMethod {
            matcher {
                paramTypes(classOf<Context>())
                usingStrings("system properties country")
            }
        }.apply {
            checkDataList("RiderModeAppListWhitelist SupportAppHelper")
            single().className.toClass().resolve().apply {
                firstMethod {
                    name = single().methodName
                    parameters(Context::class)
                    returnType = ArrayClass(classOf<String>())
                }.intercept {
                    val context = firstArg<Context>() ?: return@intercept proceed()
                    buildSupportApps(context)
                }
            }
        }
    }

    /**
     * 新版 workbench 选应用页：保留官方名单条目，把不在名单内的可启动应用
     * 追加进配置 Map（value 0 = openMode 0 / classification 0，即「其他」分区），
     * 再交给原解析逻辑生成最终列表。
     */
    private fun hookWorkbenchConfigParser() {
        dexKitBridge.findMethod {
            matcher {
                paramTypes(classOf<Map<*, *>>())
                usingStrings("parseAppConfigList: input=")
            }
        }.apply {
            checkDataList("RiderModeAppListWhitelist AppOpenModeUtils.parseAppConfigList")
            single().className.toClass().resolve().apply {
                firstMethod {
                    name = single().methodName
                    parameters(Map::class)
                    returnType = List::class
                }.intercept {
                    val configMap = firstArg<Map<*, *>>() ?: return@intercept proceed()
                    val merged = LinkedHashMap<String, Any?>()
                    configMap.forEach { (key, value) -> merged[key as String] = value }
                    val existing = merged.keys.map { it.substringBefore('#') }.toHashSet()
                    val context = hostApplication ?: return@intercept proceed()
                    queryLaunchablePackages(context).forEach { pkg ->
                        if (pkg !in existing) merged[pkg] = 0
                    }
                    proceed(arrayOf(merged))
                }
            }
        }
    }

    /** 查询全部可启动应用的包名。 */
    private fun queryLaunchablePackages(context: Context): List<String> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            context.packageManager.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
                .mapNotNull { it.activityInfo?.applicationInfo?.packageName }
                .distinct()
        }.getOrDefault(emptyList())
    }

    /** 返回所有可启动应用，条目格式与名单数组一致：「名称,包名,图标名」，图标名统一复用美团骑手图标名。 */
    private fun buildSupportApps(context: Context): Array<String> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching {
            pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
                .mapNotNull { resolve ->
                    val activity = resolve.activityInfo ?: return@mapNotNull null
                    val appInfo = activity.applicationInfo ?: return@mapNotNull null
                    val pkg = appInfo.packageName ?: return@mapNotNull null
                    val label = activity.loadLabel(pm)
                        .toString().replace(",", " ")
                    pkg to label
                }
                .distinctBy { it.first }
        }.getOrDefault(emptyList())
        return apps.sortedBy { it.second.lowercase() }
            .map { (pkg, label) -> "$label,$pkg,icon_meituan_rider" }
            .toTypedArray()
    }
}
