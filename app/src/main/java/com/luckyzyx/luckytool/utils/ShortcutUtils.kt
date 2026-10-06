package com.luckyzyx.luckytool.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.ShortcutBean
import com.luckyzyx.luckytool.ui.activity.ShortcutActivity
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
class ShortcutUtils(val context: Context) {

    var shortcutManager = context.getSystemService(classOf<ShortcutManager>())

    private val existOplusGame =
        context.checkPackName("com.oplus.games") && context.checkResolveActivity(
            Intent().setClassName(
                "com.oplus.games",
                "business.compact.activity.GameBoxCoverActivity"
            )
        )

    /**
     * 获取内置快捷方式支持列表
     * @receiver Context
     * @return ArrayList<ShortcutBean>
     */
    fun getDefaultShortcutBean(): ArrayList<ShortcutBean> {
        return ArrayList<ShortcutBean>().apply {
            add(
                ShortcutBean(
                    "module_shortcut_status_lsposed", "LSPosed", Icon.createWithResource(
                        context.packageName, android.R.mipmap.sym_def_app_icon
                    )
                )
            )
            if (existOplusGame) {
                add(
                    ShortcutBean(
                        "module_shortcut_status_oplusgames",
                        AppUtils(context).getAppLabel("com.oplus.games").toString(),
                        Icon.createWithResource(context.packageName, R.mipmap.oplusgames_icon)
                    )
                )
            }
            add(
                ShortcutBean(
                    "module_shortcut_status_chargingtest",
                    context.getString(R.string.charging_test),
                    Icon.createWithResource(
                        context.packageName, R.drawable.ic_baseline_charging_station_24
                    )
                )
            )
            add(
                ShortcutBean(
                    "module_shortcut_status_processmanager",
                    context.getString(R.string.process_manager),
                    Icon.createWithResource(context.packageName, android.R.mipmap.sym_def_app_icon)
                )
            )
            add(
                ShortcutBean(
                    "module_shortcut_status_performance",
                    context.getString(R.string.high_performance_mode),
                    Icon.createWithResource(
                        context.packageName, R.drawable.baseline_device_thermostat_24
                    )
                )
            )
            forEachIndexed { _, bean ->
                val intent = when (bean.key) {
                    "module_shortcut_status_oplusgames" -> Intent(Intent.ACTION_VIEW).apply {
                        putExtra("Shortcut", bean.key)
                        setClassName(
                            "com.oplus.games",
                            "business.compact.activity.GameBoxCoverActivity"
                        )
                    }

                    else -> Intent(Intent.ACTION_VIEW).apply {
                        setClass(context, classOf<ShortcutActivity>())
                        putExtra("Shortcut", bean.key)
                    }
                }
                bean.intent = intent
            }
            removeIf { it.key.isBlank() || it.label.isBlank() }
        }
    }

    /**
     * 获取已开启的快捷方式
     * @return List<ShortcutInfo>
     */
    fun getEnabledShortcutList(): List<ShortcutInfo> {
        return shortcutManager.dynamicShortcuts
    }

    /**
     * 设置快捷方式启用状态
     * @param bean ShortcutBean
     * @param status Boolean
     */
    fun setShortcutStatus(defBeans: ArrayList<ShortcutBean>, bean: ShortcutBean, status: Boolean) {
        val enabledList = getEnabledShortcutList()
        val newList = ArrayList<ShortcutInfo>()

        defBeans.forEachIndexed { _, shortcutBean ->
            val findEnabled = enabledList.find { it.id == shortcutBean.key }
            val findCurrent = enabledList.find { it.id == bean.key }
            if (findEnabled != null || findCurrent == null) {
                newList.add(shortcutBean.toShortcutInfo(context))
            }
        }

        if (status) updateDynamicShortcuts(newList)
        else removeDynamicShortcuts(arrayListOf(bean.key))
    }

    /**
     * 获取模块图标状态
     * @receiver Context
     * @return Boolean
     */
    fun getAppIconStatus(): Boolean {
        return when (AppUtils(context).getComponentEnabled(
            ComponentName(context.packageName, "${context.packageName}.Hide")
        )) {
            0 -> true
            1 -> true
            else -> false
        }
    }

    /**
     * 创建快捷方式信息
     * @receiver Context
     * @param id String
     * @param label String
     * @param icon Icon
     * @param intent Intent
     * @return ShortcutInfo
     */
    fun createShortcutInfo(id: String, label: String, icon: Icon?, intent: Intent?): ShortcutInfo {
        return ShortcutInfo.Builder(context, id).apply {
            setShortLabel(label)
            setIcon(icon)
            intent?.let { setIntent(it) }
        }.build()
    }

    /**
     * @see [ShortcutManager.requestPinShortcut]
     * @param shortcutInfo ShortcutInfo
     */
    fun requestPinShortcut(shortcutInfo: ShortcutInfo) {
        val intent = shortcutManager.createShortcutResultIntent(shortcutInfo)
        val callback = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        shortcutManager.requestPinShortcut(shortcutInfo, callback.intentSender)
    }

    /**
     * 设置动态快捷方式
     * @receiver Context
     * @param list Array<out ShortcutInfo>
     */
    private fun updateDynamicShortcuts(list: ArrayList<ShortcutInfo>) = try {
        shortcutManager.dynamicShortcuts = list
    } catch (t: Throwable) {
        context.showToast("Set Dynamic Shortcuts Error!")
        LogUtils.e("updateDynamicShortcuts", "${list.toList()}", t.toString(), true)
    }

    /**
     * 根据ID移除快捷方式
     * @receiver Context
     * @param list Array<out String>
     */
    private fun removeDynamicShortcuts(list: ArrayList<String>) {
        shortcutManager.removeDynamicShortcuts(list)
    }
}