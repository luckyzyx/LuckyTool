package com.luckyzyx.luckytool.hook.scopes.metis

import java.lang.reflect.Field
import java.lang.reflect.Method
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 TextIntent bean 的反射访问封装。
 *
 * 卡片 bean 是宿主私有类，只能反射读写。优先用公开 getter/setter（getPackageName / setPackageName /
 * getIntent / getTextType），不存在时再沿继承链回落 getDeclaredField("packageName") 这类字段访问。
 * 访问器按 Class 缓存：卡片点击是高频路径，每次重新查找方法与字段太浪费。
 */
@Obfuscate
class ReflectedTextIntent private constructor(
    private val bean: Any,
    private val accessors: Accessors
) {
    private class Accessors(
        val getPackageName: Method?,
        val setPackageName: Method?,
        val packageNameField: Field?,
        val getIntent: Method?,
        val getTextType: Method?
    )

    /** 当前包名，读不到返回 null */
    fun packageName(): String? {
        accessors.getPackageName?.let { method ->
            runCatching { method.invoke(bean) as? String }.getOrNull()?.let { return it }
        }
        val field = accessors.packageNameField ?: return null
        return runCatching { field.get(bean) as? String }.getOrNull()
    }

    /**
     * 写入包名。
     *
     * 这里必须「有 setter 就只走 setter 并立刻返回」，不能写成 setter?.invoke(x) ?: 字段赋值：
     * void 方法的 invoke 返回 null，?: 会永远落到字段分支，等于 setter 白调、字段也没写对。
     */
    fun setPackageName(value: String) {
        accessors.setPackageName?.let { method ->
            if (runCatching { method.invoke(bean, value) }.isSuccess) return
        }
        accessors.packageNameField?.let { field ->
            runCatching { field.set(bean, value) }
        }
    }

    /** 卡片自带的 Intent，读不到返回 null；改写动作都作用在它上面 */
    fun intent(): Any? = accessors.getIntent?.let { method ->
        runCatching { method.invoke(bean) }.getOrNull()
    }

    /** 卡片类型标记，textType == 29 是文本搜索卡 */
    fun textType(): Int? = accessors.getTextType?.let { method ->
        runCatching { (method.invoke(bean) as? Number)?.toInt() }.getOrNull()
    }

    companion object {
        private val ACCESSORS = java.util.concurrent.ConcurrentHashMap<Class<*>, Accessors>()

        /** 包装任意 bean；bean 为 null 时返回 null */
        fun of(bean: Any?): ReflectedTextIntent? {
            val target = bean ?: return null
            val type = target.javaClass
            val cached = ACCESSORS[type] ?: build(type).also { ACCESSORS[type] = it }
            return ReflectedTextIntent(target, cached)
        }

        private fun build(type: Class<*>): Accessors = Accessors(
            getPackageName = findMethod(type, "getPackageName"),
            setPackageName = findMethod(type, "setPackageName", String::class.java),
            packageNameField = findField(type, "packageName"),
            getIntent = findMethod(type, "getIntent"),
            getTextType = findMethod(type, "getTextType")
        )

        /** 沿继承链找方法：宿主可能把 getter 放在父类里，只看本类会漏 */
        private fun findMethod(type: Class<*>, name: String, vararg parameterTypes: Class<*>): Method? {
            var current: Class<*>? = type
            while (true) {
                val cls = current ?: return null
                if (cls == Any::class.java) return null
                val method = runCatching { cls.getDeclaredMethod(name, *parameterTypes) }.getOrNull()
                if (method != null) {
                    method.isAccessible = true
                    return method
                }
                current = cls.superclass
            }
        }

        /** 沿继承链找字段；isAccessible 必须置 true，宿主字段都是 private */
        private fun findField(type: Class<*>, name: String): Field? {
            var current: Class<*>? = type
            while (true) {
                val cls = current ?: return null
                if (cls == Any::class.java) return null
                val field = runCatching { cls.getDeclaredField(name) }.getOrNull()
                if (field != null) {
                    field.isAccessible = true
                    return field
                }
                current = cls.superclass
            }
        }
    }
}
