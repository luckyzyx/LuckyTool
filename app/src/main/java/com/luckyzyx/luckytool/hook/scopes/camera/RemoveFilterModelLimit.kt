package com.luckyzyx.luckytool.hook.scopes.camera

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFilterModelLimit : YukiBaseHooker() {
    override fun onHook() {
        //Source SystemUtil
        "com.oplus.ocs.camera.ipusdk.processunit.filter.list.SystemUtil".toClass().resolve().apply {
            firstMethod { name = "isMarketNameContainSeriesNum" }.hook {
                intercept(true)
            }
        }
    }
}