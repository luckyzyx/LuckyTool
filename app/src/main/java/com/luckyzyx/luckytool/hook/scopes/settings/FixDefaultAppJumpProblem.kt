package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.Context
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object FixDefaultAppJumpProblem : YukiBaseHooker() {
    override fun onHook() {
        //Source DefaultAppManagerPreferenceController
        "com.oplus.settings.feature.appmanager.controller.DefaultAppManagerPreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "handlePreferenceTreeClick" }.hook {
                    before {
                        val preference = firstArg().get() ?: return@before
                        val key =
                            preference.asResolver().firstMethod { name = "getKey";superclass() }
                            .invoke<String>()
                        val context = preference.asResolver().firstField {
                            type = Context::class;superclass()
                        }.get<Context>() ?: return@before
                        if (key == "default_apps_manager") {
                            val intent = Intent("action.oplusos.safecenter.DefaultAppListActivity")
                            safeOfNull { context.startActivity(intent) }
                        }
                    }
                }
            }
    }
}