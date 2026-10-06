package com.luckyzyx.luckytool.hook.scopes.games

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableDeveloperPage : YukiBaseHooker() {
    override fun onHook() {
        //Source GameDevelopOptionsActivity
        "business.compact.activity.GameDevelopOptionsActivity".toClassOrNull()?.resolve()?.apply {
            firstMethod {
                name = "onCreate"
                parameterCount = 1
            }.hook {
                before {
                    instance<Activity>().intent.apply {
                        putExtra("gameDevelopOptions", "GameDevelopOptionsActivity")
                        putExtra("openAutomation", -1)
                    }
                    firstArg().set(null)
                }
            }
        }
    }
}