package com.luckyzyx.luckytool.hook.scopes.otherapp

import android.app.Activity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.query.enums.StringMatchType

@Obfuscate
object HookADM : YukiBaseHooker() {
    override fun onHook() {
        //解锁Pro
        if (preferences(ModulePrefs).getBoolean("adm_unlock_pro", false)) {
            loadHooker(UnlockAdmPro)
        }
        //解锁线程数
        loadHooker(UnlockAdmThreads)
    }

    @Obfuscate
    object UnlockAdmPro : YukiBaseHooker() {
        override fun onHook() {
            //Search Beta / Pro -> EVENT_DISA / hua_voices
            "com.dv.get.Main".toClass().resolve().apply {
                firstMethod { name = "onCreate" }.hook {
                    after {
                        val activity = instance<Activity>()
                        val sp = PreferenceManager.getDefaultSharedPreferences(activity)
                        sp.edit(commit = true) {
                            putBoolean("EVENT_DISA", false)
                            putBoolean("hua_voices", false)
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object UnlockAdmThreads : YukiBaseHooker() {
        override fun onHook() {
            val threads = preferences(ModulePrefs).getString("adm_unlock_more_threads", "0")
                .toIntOrNull() ?: 0
            if (threads <= 0) return

            //Source Main -> S215
            DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
                dexKitBridge.findClass {
                    matcher {
                    addFieldForType(classOf<Int>())
                        methods {
                            add {
                                name("call", StringMatchType.Contains)
                                paramCount(0);returnType(classOf<Int>())
                                usingNumbers(15)
                            }
                            add {
                                name("call", StringMatchType.Contains)
                                paramCount(0);returnType(classOf<Boolean>())
                            }
                        }
                    }
                }.apply {
                    checkDataList("UnlockAdmThreads")
                    single().name.toClass().resolve().apply {
                        firstMethod {
                            name { it.contains("call") }
                            emptyParameters()
                            returnType = Int::class
                        }.hook {
                            after {
//                                val type = field { type = IntType }.get(instance).int()
                                val res = result<Int>() ?: return@after
                                if (res == 15) result = threads - 1
                            }
                        }
                    }
                }
            }
        }
    }
}