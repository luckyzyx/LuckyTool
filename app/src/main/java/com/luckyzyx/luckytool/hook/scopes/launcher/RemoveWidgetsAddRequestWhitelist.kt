package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveWidgetsAddRequestWhitelist : YukiBaseHooker() {
    override fun onHook() {
        //Source WidgetControlHelper
        "com.android.launcher3.widget.WidgetControlHelper".toClassOrNull() ?: return
        //Source AddItemActivity
        "com.android.launcher3.dragndrop.AddItemActivity".toClass().resolve().apply {
            firstMethodOrNull {
                name = "isAllowedAddWidget"
                parameterCount { it in 1..2 }
            }?.hook {
                intercept(true)
            }
        }
    }
}