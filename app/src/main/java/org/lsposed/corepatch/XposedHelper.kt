package org.lsposed.corepatch

import android.annotation.SuppressLint
import android.graphics.Point
import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method

typealias BeforeCallback = (XposedHelper.BeforeHookCallback) -> Unit
typealias AfterCallback = (XposedHelper.AfterHookCallback) -> Unit

object XposedHelper {
    private val TAG = "LuckyTool-CorePatch"

    lateinit var xposedModule: XposedInterface
        private set
    lateinit var hostClassLoader: ClassLoader
        private set
    //LuckyTool：配置统一存 ModulePrefs 组，键名沿用上游
    val prefs by lazy { xposedModule.getRemotePreferences("ModulePrefs") }
    private val fieldOffsetValue by lazy { getFieldOffsetOffset() }

    fun setXposedModule(module: XposedInterface) {
        xposedModule = module
    }

    fun setHostClassLoader(classLoader: ClassLoader) {
        hostClassLoader = classLoader
    }

    val isHostClassLoaderInitialized: Boolean
        get() = ::hostClassLoader.isInitialized

    class BeforeHookCallback(private val chain: XposedInterface.Chain) {
        val thisObject: Any? get() = chain.thisObject
        val args: Array<Any?> = chain.args.toTypedArray()
        private var skipped = false
        private var skipResult: Any? = null

        fun returnAndSkip(result: Any?) {
            skipped = true
            skipResult = result
        }

        fun isSkipped() = skipped
        fun getSkipResult() = skipResult
    }

    class AfterHookCallback(
        private val chain: XposedInterface.Chain,
        var result: Any?,
        var throwable: Throwable?
    ) {
        val thisObject: Any? get() = chain.thisObject
        val args: Array<Any?> get() = chain.args.toTypedArray()
    }

    internal class CustomHooker(
        val beforeCallback: BeforeCallback = {},
        val afterCallback: AfterCallback = {},
    ) : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            var result: Any? = null
            var throwable: Throwable? = null
            var skipped = false

            val bcb = BeforeHookCallback(chain)
            beforeCallback(bcb)
            if (bcb.isSkipped()) {
                result = bcb.getSkipResult()
                skipped = true
            }

            if (!skipped) {
                try {
                    result = chain.proceed(bcb.args)
                } catch (t: Throwable) {
                    throwable = t
                }
            }

            val acb = AfterHookCallback(chain, result, throwable)
            afterCallback(acb)
            result = acb.result
            throwable = acb.throwable

            if (throwable != null) {
                throw throwable
            }
            return result
        }
    }

    fun hookBefore(
        member: Executable, callback: BeforeCallback
    ): XposedInterface.HookHandle {
        return xposedModule.hook(member).intercept(CustomHooker(beforeCallback = callback))
    }

    fun hookAfter(
        executable: Executable, callback: AfterCallback
    ): XposedInterface.HookHandle {
        return xposedModule.hook(executable).intercept(CustomHooker(afterCallback = callback))
    }

    fun log(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            xposedModule.log(Log.ERROR, TAG, message, throwable)
        } else if (com.luckyzyx.luckytool.BuildConfig.DEBUG) {
            xposedModule.log(Log.DEBUG, TAG, message)
        }
    }

    fun deoptimize(method: Method): Boolean {
        return xposedModule.deoptimize(method)
    }

    fun getOriginInvoker(method: Method): XposedInterface.Invoker<*, Method?>? =
        xposedModule.getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN)

    fun findClassIfExists(name: String): Class<*>? {
        return try {
            hostClassLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    fun setStaticBoolean(field: Field, value: Boolean) {
        try {
            // Resolve the target field before reading its internal ART offset.
            field.isAccessible = true
            field.get(null)
        } catch (_: IllegalAccessException) {
        }

        val offset = UnsafeAccess.getInt(field, fieldOffsetValue).toLong()
        UnsafeAccess.putBoolean(field.declaringClass, offset, value)
    }

    @SuppressLint("DiscouragedPrivateApi")
    private object UnsafeAccess {
        private val unsafeClass = Class.forName("sun.misc.Unsafe")
        private val unsafeInstance = unsafeClass.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null)
        }
        private val getIntMethod = unsafeClass.getMethod(
            "getInt", Any::class.java, Long::class.javaPrimitiveType
        )
        private val putIntMethod = unsafeClass.getMethod(
            "putInt", Any::class.java, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        private val putBooleanMethod = unsafeClass.getMethod(
            "putBoolean",
            Any::class.java,
            Long::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        private val objectFieldOffsetMethod = unsafeClass.getMethod(
            "objectFieldOffset", Field::class.java
        )

        fun getInt(target: Any, offset: Long) =
            getIntMethod.invoke(unsafeInstance, target, offset) as Int

        fun putInt(target: Any, offset: Long, value: Int) {
            putIntMethod.invoke(unsafeInstance, target, offset, value)
        }

        fun putBoolean(target: Any, offset: Long, value: Boolean) {
            putBooleanMethod.invoke(unsafeInstance, target, offset, value)
        }

        fun objectFieldOffset(field: Field) =
            objectFieldOffsetMethod.invoke(unsafeInstance, field) as Long
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SoonBlockedPrivateApi")
    private fun getFieldOffsetOffset(): Long {
        var noSuchFieldException: NoSuchFieldException? = null
        try {
            val offsetField = Field::class.java.getDeclaredField("offset")
            offsetField.isAccessible = true
            offsetField.getInt(offsetField)
            return UnsafeAccess.objectFieldOffset(offsetField)
        } catch (e: NoSuchFieldException) {
            noSuchFieldException = e
        } catch (_: IllegalAccessException) {
        } catch (_: UnsupportedOperationException) {
        }

        val probeField = Point::class.java.getDeclaredField("x")
        probeField.getInt(Point())
        val fieldOffset = UnsafeAccess.objectFieldOffset(probeField).toInt()
        for (offset in 8 until 256 step 4) {
            val offsetLong = offset.toLong()
            if (UnsafeAccess.getInt(probeField, offsetLong) != fieldOffset) continue

            val modifiedOffset = fieldOffset.inv()
            UnsafeAccess.putInt(probeField, offsetLong, modifiedOffset)
            val currentOffset = UnsafeAccess.objectFieldOffset(probeField).toInt()
            UnsafeAccess.putInt(probeField, offsetLong, fieldOffset)
            if (currentOffset == modifiedOffset) return offsetLong
        }
        throw noSuchFieldException ?: NoSuchFieldException("Field.offset")
    }
}
