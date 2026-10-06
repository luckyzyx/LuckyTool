package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalPmsFeature : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        val list = ArrayMap<String, Boolean>().apply {

        }
        loadHooker(PmsFeature(list))
    }

    @Obfuscate
    class PmsFeature(private val features: Map<String, Boolean>) : YukiBaseHooker() {
        override fun onHook() {
            //Source PackageManagerService
            "com.android.server.pm.PackageManagerService".toClass().resolve().apply {
                firstMethod {
                    name = "hasSystemFeature"
                    parameters(String::class, Int::class)
                    returnType = Boolean::class
                }.hook {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        val value = features[key]
                        if (value != null) result = value
                    }
                }
            }
        }
    }
}