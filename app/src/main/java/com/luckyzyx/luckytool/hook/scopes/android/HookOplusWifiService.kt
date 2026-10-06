package com.luckyzyx.luckytool.hook.scopes.android

import android.os.Message
import android.util.ArraySet
import com.android.internal.os.SystemServerClassLoaderFactory
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.ArrayClass
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookOplusWifiService : YukiBaseHooker() {

    //C17 起 oplus-wifi-service 被 APK 化，设备上的实际路径/后缀可能变化，
    //缓存查找按路径关键字匹配，避免与系统真实使用的 loader 分裂
    private const val OPLUS_WIFI_SERVICE_PATH_FLAG = "oplus-wifi-service"
    private const val OPLUS_WIFI_SERVICE_CLASS_FLAG = "com.oplus.server.wifi"

    @Volatile
    private var hooksApplied = false

    override fun onHook() {
        //1. 等系统启动 wifi 服务时，从 startServiceFromJar 返回的实例上拿真实 loader
        hookStartServiceFromJar()
        //2. 若系统已创建过 loader（模块 hook 晚于系统启动），直接从工厂缓存复用
        findLoaderFromCache()?.let { applyHooks(it) }
    }

    //不 hook getOrCreateClassLoader：它是启动期热路径（每个 jar 各调一次），
    //改为 hook 低频的 startServiceFromJar，返回实例的 classLoader 即系统真实使用的 loader
    private fun hookStartServiceFromJar() {
        try {
            //Source SystemServiceManager
            "com.android.server.SystemServiceManager".toClass().resolve()
                .firstMethod { name = "startServiceFromJar"; parameterCount(2) }.hook {
                    after {
                        val className = firstArg().get<String>() ?: return@after
                        if (!className.contains(OPLUS_WIFI_SERVICE_CLASS_FLAG)) return@after
                        val service = result<Any>() ?: return@after
                        val classLoader = service.javaClass.classLoader ?: return@after
                        applyHooks(classLoader)
                    }
                }
        } catch (t: Throwable) {
            YLog.error("Hook SystemServiceManager.startServiceFromJar Error!", t)
        }
    }

    private fun findLoaderFromCache(): ClassLoader? {
        return try {
            val cache = classOf<SystemServerClassLoaderFactory>().resolve().firstField {
                name = "sLoadedPaths"
            }.get<Map<*, *>>() ?: return null
            cache.entries.firstOrNull { (k, _) ->
                k is String && k.contains(OPLUS_WIFI_SERVICE_PATH_FLAG)
            }?.value as? ClassLoader
        } catch (t: Throwable) {
            YLog.debug("find oplus wifi service classloader from cache error", t)
            null
        }
    }

    private fun applyHooks(loader: ClassLoader) {
        if (hooksApplied) return
        synchronized(HookOplusWifiService) {
            if (hooksApplied) return
            hooksApplied = true
        }
        //Source_ext oplus-wifi-service OplusTetheringNotification showSoftapEnabledDurationNotification
        //Channel DurationNotification -> Notification id -> 4
        if (preferences(ModulePrefs).getBoolean(
                "remove_hotspot_power_consumption_notification",
                false
            )
        ) {
            loadHooker(HookOplusSoftAp(loader))
        }
        //Source_ext oplus-wifi-service OplusWifiRomUpdateHelper getSlaWhiteListApps
        loadHooker(HookSlaAppList(loader))
    }

    @Obfuscate
    class HookOplusSoftAp(val classLoader: ClassLoader?) : YukiBaseHooker() {
        override fun onHook() {
            //C13-C15：startSoftapEnableTimer 触发热点启用时长统计
            try {
                "com.oplus.server.wifi.hotspot.OplusSoftapStatistics".toClass(classLoader).resolve()
                    .firstMethod { name = "startSoftapEnableTimer" }.hook {
                        intercept()
                    }
            } catch (_: Throwable) {
//                YLog.debug("startSoftapEnableTimer not found on this version", t)
            }
            //C17：逻辑并入 SoftapHandler.handleMessage，what==1 时发送热点时长通知
            try {
                "com.oplus.server.wifi.hotspot.OplusSoftapStatistics\$SoftapHandler"
                    .toClass(classLoader).resolve().firstMethod { name = "handleMessage" }.hook {
                        before {
                            val msg = firstArg().get<Message>()
                            if (msg?.what == 1) result = null
                        }
                    }
            } catch (_: Throwable) {
//                YLog.debug("SoftapHandler.handleMessage not found on this version", t)
            }
        }
    }

    @Obfuscate
    class HookSlaAppList(val classLoader: ClassLoader?) : YukiBaseHooker() {

        private val whitelistKey = "custom_wlan_sla_whitelist"
        private val gameWhitelistKey = "custom_wlan_sla_game_whitelist"

        var mode = "0"
        var rmBlack = false
        val whitelist = ArraySet<String>()
        val gameWhitelist = ArraySet<String>()

        private fun initData() {
            mode = preferences(ModulePrefs).getString("set_wlan_sla_whitelist_mode", "0")
            dataChannel.wait<String>("set_wlan_sla_whitelist_mode") {
                mode = it
                YLog.debug("update oplus wifi configs status -> $it")
            }
            rmBlack = preferences(ModulePrefs).getBoolean("remove_wlan_sla_blacklist", false)
            dataChannel.wait<Boolean>("remove_wlan_sla_blacklist") { rmBlack = it }

            whitelist.clear()
            whitelist.addAll(preferences(ModulePrefs).getStringSet(whitelistKey, ArraySet()))
            dataChannel.wait(whitelistKey) {
                val new = preferences(ModulePrefs).getStringSet(whitelistKey, ArraySet())
                YLog.debug("update oplus wifi whitelist configs -> ${whitelist.size} | ${new.size}")
                whitelist.clear()
                whitelist.addAll(new)
            }

            gameWhitelist.clear()
            gameWhitelist.addAll(
                preferences(ModulePrefs).getStringSet(
                    gameWhitelistKey,
                    ArraySet()
                )
            )
            dataChannel.wait(gameWhitelistKey) {
                val new = preferences(ModulePrefs).getStringSet(gameWhitelistKey, ArraySet())
                YLog.debug("update oplus wifi game whitelist configs -> ${gameWhitelist.size} | ${new.size}")
                gameWhitelist.clear()
                gameWhitelist.addAll(new)
            }
            YLog.debug("init oplus wifi configs success -> ${whitelist.size} | ${gameWhitelist.size}")
        }

        override fun onHook() {
            initData()

            if (mode == "0") return

            //Source OplusSlaApps
            VariousClass(
                "com.oplus.server.wifi.OplusSlaApps", //C13
                "com.oplus.server.wifi.sla.OplusSlaApps" //C14 C15 C17
            ).toClass(classLoader).resolve().apply {
                firstMethod {
                    name = "getSlaWhiteListAppsFromRus"
                    returnType = ArrayClass(classOf<String>())
                }.hook {
                    after {
                        if (mode == "0") return@after
                        val res = result<Array<String>>() ?: return@after
                        result = when (mode) {
                            "1" -> res.toMutableList().apply {
                                whitelist.forEachIndexed { _, new ->
                                    if (!contains(new)) add(new)
                                }
                            }.toTypedArray()

                            "2" -> whitelist.toTypedArray()
                            else -> return@after
                        }
                    }
                }
                firstMethod {
                    name = "getSlaGameAppsFromRus"
                    returnType = ArrayClass(classOf<String>())
                }.hook {
                    after {
                        if (mode == "0") return@after
                        val res = result<Array<String>>() ?: return@after
                        result = when (mode) {
                            "1" -> res.toMutableList().apply {
                                gameWhitelist.forEachIndexed { _, new ->
                                    if (!contains(new)) add(new)
                                }
                            }.toTypedArray()

                            "2" -> gameWhitelist.toTypedArray()
                            else -> return@after
                        }
                    }
                }
                firstMethod { name = "getSlaBlackListAppsFromRus" }.hook {
                    before {
                        if (mode == "0") return@before
                        if (rmBlack) result = null
                    }
                }
            }
        }
    }
}