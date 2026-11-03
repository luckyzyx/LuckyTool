package com.luckyzyx.luckytool.hook.utils

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusMirageDisplayManagerUtils(val classLoader: ClassLoader?) {

    val service = "com.android.server.display.OplusMirageDisplayManagerService".toClass(classLoader)

    fun getInstance(): Any? {
        return service.resolve().firstMethod {
            name = "getInstance";emptyParameters()
        }.invoke()
    }

    fun notifyCastSuccess(instance: Any, displayId: Int) {
        instance.asResolver().firstMethod { name = "notifyCastSuccess";parameters(Int::class) }
            .invoke(displayId)
    }

    //释放后台串流的虚拟屏（后台挂起恢复时用）
    fun releaseVirtualDisplay(instance: Any, displayId: Int) {
        instance.asResolver().firstMethodOrNull {
            name = "releaseVirtualDisplayDevice";parameterCount = 1
        }?.invoke<Any?>(displayId)
    }
}