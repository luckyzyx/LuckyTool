package com.luckyzyx.luckytool.hook.scopes.android

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOfNull
import kotlinx.serialization.json.Json
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableVideoMemcFrameInsertion : YukiBaseHooker() {

    private val allConfigPackages = ArrayList<MemcConfigPackage>()
    private val allConfigActivitys = ArrayList<MemcConfigActivity>()

    private val configAppList = java.util.ArrayList<String>()
    private val appScreenRate = HashMap<String, String>()
    private val sdr2hdrCommand = HashMap<String, String>()
    private val configActivityList = java.util.ArrayList<String>()
    private val configMemcCommand = HashMap<String, String>()

    override fun onHook() {
        if (getOSVersionCode < 26) return
        val isEnable = preferences(ModulePrefs).getBoolean("enable_video_memc_frame_insertion", false)
        val configPackages =
            preferences(ModulePrefs).getStringSet("memc_config_package_list", ArraySet())
        val configActivitys =
            preferences(ModulePrefs).getStringSet("memc_config_activity_list", ArraySet())

        //Source OplusMemcHelper
        VariousClass(
            "com.android.server.display.memc.OplusMemcHelper", //C14
            "com.android.server.display.feature.vrr.memc.OplusMemcHelper" //C16
        ).toClass().resolve().apply {
            firstMethod { name = "init" }.hook {
                after {
                    if (!isEnable) return@after
                    init(configPackages, configActivitys)
                }
            }
            firstMethod { name = "getConfigAppList" }.hook {
                after {
                    if (!isEnable || configAppList.isEmpty()) return@after
                    result = configAppList
                }
            }
            firstMethod { name = "getAppScreenRateMap" }.hook {
                after {
                    if (!isEnable || appScreenRate.isEmpty()) return@after
                    result = appScreenRate
                }
            }
            firstMethod { name = "getSdr2hdrCommandMap" }.hook {
                after {
                    if (!isEnable || sdr2hdrCommand.isEmpty()) return@after
                    result = sdr2hdrCommand
                }
            }
            firstMethod { name = "getConfigActivityList" }.hook {
                after {
                    if (!isEnable || configActivityList.isEmpty()) return@after
                    result = configActivityList
                }
            }
            firstMethod { name = "getMemcCommandMap" }.hook {
                after {
                    if (!isEnable || configMemcCommand.isEmpty()) return@after
                    result = configMemcCommand
                }
            }
        }
    }

    fun init(configPackages: Set<String>, configActivitys: Set<String>) {
        allConfigPackages.clear()
        allConfigActivitys.clear()
        configPackages.forEach {
            val configPackageInfo = safeOfNull {
                Json.decodeFromString<MemcConfigPackage>(it)
            }
            if (configPackageInfo != null) allConfigPackages.add(configPackageInfo)
        }
        configActivitys.forEach {
            val configActivityInfo = safeOfNull {
                Json.decodeFromString<MemcConfigActivity>(it)
            }
            if (configActivityInfo != null) allConfigActivitys.add(configActivityInfo)
        }

        configAppList.clear()
        appScreenRate.clear()
        sdr2hdrCommand.clear()
        configActivityList.clear()
        configMemcCommand.clear()

        allConfigPackages.forEach {
            configAppList.add(it.packName)
            appScreenRate[it.packName] = it.rate
            sdr2hdrCommand[it.packName] = it.type
        }
        allConfigActivitys.forEach {
            configActivityList.add(it.activity)
            configMemcCommand["${it.packName}/${it.activity}"] = it.type
        }
    }
}