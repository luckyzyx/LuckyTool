package com.luckyzyx.luckytool.hook.scopes.weather

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RestoreRainfallCloudMapPage : YukiBaseHooker() {
    override fun onHook() {
        //Source IndexOperationsManager
        "com.oplus.weather.indexoperations.IndexOperationsManager".toClassOrNull()?.resolve()
            ?.apply {
                firstMethod { name = "supportIndexOperationsFeature" }.hook {
                    intercept(false)
                }
            }
    }
}