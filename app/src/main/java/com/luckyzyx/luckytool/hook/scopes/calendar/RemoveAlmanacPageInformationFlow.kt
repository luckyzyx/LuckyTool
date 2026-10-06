package com.luckyzyx.luckytool.hook.scopes.calendar

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAlmanacPageInformationFlow : YukiBaseHooker() {
    override fun onHook() {
        //Source AlmanacPagesAdapter -> H5InterfaceHelper getAlmanacUrl
        "com.android.calendar.module.subscription.almanac.adapter.AlmanacPagesAdapter".toClass()
            .resolve().apply {
                firstMethod { name = "onCreateViewHolder" }.hook {
                    before {
                        lastArg().set(0)
                    }
                }
            }
    }
}