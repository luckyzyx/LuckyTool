package com.luckyzyx.luckytool.hook.scopes.android

import android.util.ArrayMap
import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.data.DarkModeInfo
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.safeOfNull
import com.oplus.darkmode.OplusDarkModeData
import kotlinx.serialization.json.Json
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DarkModeService : YukiBaseHooker() {

    var isEnable = false
    val list = ArraySet<DarkModeInfo>()

    fun loadData() {
        isEnable = preferences(ModulePrefs).getBoolean("dark_mode_list_enable", false)
        dataChannel.wait<Boolean>("dark_mode_list_enable") {
            isEnable = it
            YLog.debug("update dark mode service configs status -> $it")
        }

        list.clear()
        val enabled = preferences(ModulePrefs).getStringSet("dark_mode_support_list", ArraySet())
        list.addAll(enabled.mapNotNull {
            safeOfNull { Json.decodeFromString<DarkModeInfo>(it) }
        })
        dataChannel.wait("dark_mode_support_list") {
            val new = preferences(ModulePrefs).getStringSet("dark_mode_support_list", ArraySet())
            YLog.debug("update dark mode service whitelist configs -> ${list.size} | ${new.size}")
            list.clear()
            list.addAll(new.mapNotNull {
                safeOfNull { Json.decodeFromString<DarkModeInfo>(it) }
            })
        }
        YLog.debug("init dark mode service configs success -> ${list.size}")
    }

    override fun onHook() {
        loadData()

        //Source OplusDarkModeServiceManager
        "com.android.server.OplusDarkModeServiceManager".toClass().resolve().apply {
            firstMethod {
                name { it.startsWith("updateList") }
                parameterCount = 1
            }.hook {
                after {
                    if (!isEnable) return@after
                    val dataMap = ArrayMap<String, OplusDarkModeData>()
                    list.forEach {
                        if (it.curType == 0) dataMap[it.packName] = OplusDarkModeData()
                        else dataMap[it.packName] = OplusDarkModeData().apply {
                            mCurType = it.curType
                        }
                    }
                    firstField { name = "mRusAppMap" }.of(instance).set(dataMap.toMap())
//                    field { name = "mOpenApp" }.get(instance).set(supportlist)
//                    field { name = "mClickApp" }.get(instance).set(supportlist)
                }
            }
        }
    }
}