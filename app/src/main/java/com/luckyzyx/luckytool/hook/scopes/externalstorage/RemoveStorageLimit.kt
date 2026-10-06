package com.luckyzyx.luckytool.hook.scopes.externalstorage

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStorageLimit : YukiBaseHooker() {
    override fun onHook() {
        //Source ExternalStorageProvider
        "com.android.externalstorage.ExternalStorageProvider".toClass().resolve().apply {
            firstMethodOrNull { name = "shouldBlockDirectoryFromTree" }?.hook {
                intercept(false)
            }
        }
    }
}