package com.luckyzyx.luckytool.hook.utils.appcompat.dialog

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
class COUISwitch {

    private val clazzString = "com.coui.appcompat.couiswitch.COUISwitch"
    var context: Context
    var clazz: Class<*>
    var builder: Any?

    constructor(context: Context, classloader: ClassLoader?) {
        this.context = context
        this.clazz = clazzString.toClass(classloader)
        builder = clazz.createInstance(context, isPublic = false)
    }

    constructor(context: Context, attributeSet: AttributeSet, classloader: ClassLoader?) {
        this.context = context
        this.clazz = clazzString.toClass(classloader)
        builder = clazz.createInstance(context, attributeSet, isPublic = false)
    }

    constructor(context: Context, attributeSet: AttributeSet, int: Int, classloader: ClassLoader?) {
        this.context = context
        this.clazz = clazzString.toClass(classloader)
        builder = clazz.createInstance(context, attributeSet, int, isPublic = false)
    }

    fun setTag(tag: Any): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setTag"
            parameters(Any::class)
            superclass()
        }?.invoke(tag)
    }

    fun setClickable(clickable: Boolean): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setClickable"
            parameters(Boolean::class)
            superclass()
        }?.invoke(clickable)
    }

    fun setChecked(bool: Boolean): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setChecked"
            parameters(Boolean::class)
        }?.invoke(bool)
    }

    fun setEnabled(bool: Boolean): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setEnabled"
            parameters(Boolean::class)
            superclass()
        }?.invoke(bool)
    }

    fun setChecked(bool: Boolean, bool2: Boolean): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setChecked"
            parameters(Boolean::class, Boolean::class)
        }?.invoke(bool, bool2)
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener): Any? {
        return builder?.asResolver()?.firstMethod {
            name = "setOnCheckedChangeListener"
            parameters(classOf<CompoundButton.OnCheckedChangeListener>())
            superclass()
        }?.invoke(listener)
    }
}