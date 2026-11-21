package com.luckyzyx.luckytool.hook.utils

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
class OplusMultiAppManagerUtils(val classLoader: ClassLoader?) {

    val clazz = "com.oplus.multiapp.OplusMultiAppManager".toClass(classLoader)

    fun getInstance(): Any? {
        return clazz.resolve().firstMethod {
            name = "getInstance";emptyParameters()
        }.invoke()
    }

    fun isMultiAppUserId(instance: Any, userId: Int): Boolean? {
        return instance.asResolver().firstMethod {
            name = "isMultiAppUserId";parameterCount = 1
        }.invoke<Boolean>(userId)
    }
}
