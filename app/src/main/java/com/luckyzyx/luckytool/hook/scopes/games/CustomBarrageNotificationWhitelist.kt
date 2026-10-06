package com.luckyzyx.luckytool.hook.scopes.games

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomBarrageNotificationWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val set = preferences(ModulePrefs).getStringSet(
            "custom_barrage_notification_whitelist_list", ArraySet()
        )

        //Source GameBarrageUtil
        "com.coloros.gamespaceui.module.barrage.GameBarrageUtil".toClass().resolve().apply {
            firstMethod { name = "initAppState" }.hook {
                before {
                    if (set.isEmpty()) return@before

                    val gameBarrageApplicationState = firstMethod {
                        name = "getGameBarrageApplicationState"
                    }.invoke<HashMap<String, String>>() ?: return@before
                    if (gameBarrageApplicationState.isEmpty() || gameBarrageApplicationState.size != set.size) {
                        set.forEachIndexed { _, s ->
                            if (!gameBarrageApplicationState.containsKey(s)) {
                                gameBarrageApplicationState[s] = "1"
                            }
                        }
                        firstMethod { name = "setGameBarrageApplicationState" }.invoke(
                            gameBarrageApplicationState
                        )
                    }
                    result = gameBarrageApplicationState
                }
            }
            firstMethod { name = "getGameBarrageAppSwitchMap" }.hook {
                before {
                    if (set.isEmpty()) return@before

                    val hashMap = java.util.HashMap<String, String>()
                    val gameBarrageApplicationState = firstMethod {
                        name = "getGameBarrageApplicationState"
                    }.invoke<HashMap<String, String>>() ?: return@before
                    set.forEachIndexed { _, s ->
                        hashMap[s] = gameBarrageApplicationState.getOrDefault(s, "1")
                    }
                    result = hashMap
                }
            }
        }
    }
}