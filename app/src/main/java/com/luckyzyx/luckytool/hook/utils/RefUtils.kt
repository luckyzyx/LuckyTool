@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName", "FunctionName")

package com.luckyzyx.luckytool.hook.utils

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class RefUtils(val classLoader: ClassLoader?) {

    val BooleanRefCls = "kotlin.jvm.internal.Ref\$BooleanRef"
    val FloatRefCls = "kotlin.jvm.internal.Ref\$FloatRef"
    val IntRefCls = "kotlin.jvm.internal.Ref\$IntRef"
    val LongRefCls = "kotlin.jvm.internal.Ref\$LongRef"
    val ObjectRefCls = "kotlin.jvm.internal.Ref\$ObjectRef"

    fun BooleanRef(): Any {
        return BooleanRef(null)
    }

    fun BooleanRef(element: Boolean? = null): Any {
        return BooleanRefCls.toClass(classLoader).createInstance(isPublic = false).apply {
            asResolver().firstField { name = "element";type = Boolean::class }.set(element)
        }
    }

    fun FloatRef(): Any {
        return FloatRef(null)
    }

    fun FloatRef(element: Float? = null): Any {
        return FloatRefCls.toClass(classLoader).createInstance(isPublic = false).apply {
            asResolver().firstField { name = "element";type = Float::class }.set(element)
        }
    }

    fun IntRef(): Any {
        return IntRef(null)
    }

    fun IntRef(element: Int? = null): Any {
        return IntRefCls.toClass(classLoader).createInstance(isPublic = false).apply {
            asResolver().firstField { name = "element";type = Int::class }.set(element)
        }
    }

    fun LongRef(): Any {
        return LongRef(null)
    }

    fun LongRef(element: Long? = null): Any {
        return LongRefCls.toClass(classLoader).createInstance(isPublic = false).apply {
            asResolver().firstField { name = "element";type = Long::class }.set(element)
        }
    }

    inline fun <reified T : Any> ObjectRef(): Any {
        return ObjectRef<T>(null)
    }

    inline fun <reified T : Any> ObjectRef(element: T? = null): Any {
        return ObjectRefCls.toClass(classLoader).createInstance(isPublic = false).apply {
            asResolver().firstField { name = "element";type = classOf<T>() }.set(element)
        }
    }

    inline fun <reified T : Any> getRefElement(ref: Any): T? {
        return ref.asResolver().firstField { name = "element";type = classOf<T>() }.get<T>()
    }

}