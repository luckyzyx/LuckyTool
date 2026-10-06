package com.luckyzyx.luckytool.hook.scopes.otherapp

import android.content.Context
import android.content.SharedPreferences
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookKsWeb : YukiBaseHooker() {
    override fun onHook() {
        val isPro = preferences(ModulePrefs).getBoolean("ksweb_remove_check_license", false)
        if (!isPro) return
        //Source EXTEND TO PRO VERSION / CHECK SERIAL KEY / KSWEB PRO / KSWEB STANDARD
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    fields {
                    addForType(classOf<Int>())
                        addForType(classOf<Boolean>())
                        addForType(classOf<SharedPreferences>())
                    }
                    methods {
                    add { paramCount(0);returnType(classOf<Int>()) }
                        add { paramCount(0);returnType(classOf<Boolean>()) }
                        add { paramTypes(classOf<Int>());returnType(Void.TYPE) }
                        add { paramTypes(classOf<Context>());returnType(Void.TYPE) }
                    }
                    usingStrings(
                        "EXTEND TO PRO VERSION",
                        "CHECK SERIAL KEY",
                        "KSWEB PRO",
                        "KSWEB STANDARD"
                    )
                }
            }.apply {
                checkDataList("HookKsWeb")
                single().name.toClass().resolve().apply {
                    method {
                        emptyParameters()
                        returnType = Boolean::class
                    }.hookAll {
                        before {
                            firstField { type = Boolean::class }.of(instance).set(true)
                            firstField { type = Int::class }.of(instance).set(2)
                        }
                    }
                }
            }
        }
    }
}