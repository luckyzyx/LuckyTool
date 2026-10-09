package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.graphics.Point
import java.lang.reflect.Field

/**
 * 上游 CorePatch 用于改写静态 final 字段的工具。
 *
 * 迁移自 libxposed 版本 XposedHelper.setStaticBoolean，仅依赖反射与 sun.misc.Unsafe，与钩子框架无关。
 */
internal object UnsafeField {

    private val fieldOffsetValue by lazy { getFieldOffsetOffset() }

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
