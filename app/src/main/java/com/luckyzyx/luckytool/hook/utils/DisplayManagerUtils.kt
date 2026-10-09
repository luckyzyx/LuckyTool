package com.luckyzyx.luckytool.hook.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.DisplayInfo
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("MemberVisibilityCanBePrivate")
class DisplayManagerUtils(val classLoader: ClassLoader?) {

    val clazz = "android.hardware.display.DisplayManager".toClass(classLoader)
    val displayInfoClazz = "android.view.DisplayInfo".toClass(classLoader)

    fun getDisplayManagerService(context: Context): DisplayManager {
        return context.getSystemService(classOf<DisplayManager>())
    }

    fun Display.getDisplayInfo(outDisplayInfo: DisplayInfo?): Boolean {
        return asResolver().firstMethod {
            name = "getDisplayInfo"
            parameters(displayInfoClazz)
        }.invoke<Boolean>(outDisplayInfo) ?: false
    }

    fun getDynamicDisplayInfo(displayInfo: DisplayInfo): Any? {
        // ColorOS 17 主屏的 address 可能是 StablePhysical,它是 Physical 的兄弟类而非子类,
        // 因此不能再用 instanceof Physical 判断,直接反射获取 physicalDisplayId。
        val physicalDisplayId = displayInfo.address?.let { getPhysicalDisplayId(it) } ?: -1L
        return SurfaceControlUtils(classLoader).let {
            if (it.isDisplayToken()) {
                val token = if (physicalDisplayId > 0L) it.getPhysicalDisplayToken(physicalDisplayId)
                else it.getInternalDisplayToken()
                it.getDynamicDisplayInfo(token)
            } else {
                it.getDynamicDisplayInfo(physicalDisplayId)
            }
        }
    }

    fun getPhysicalDisplayId(address: Any): Long? {
        return address.asResolver().firstMethodOrNull {
            name = "getPhysicalDisplayId"
            emptyParameters()
        }?.invoke<Long>()
    }
}