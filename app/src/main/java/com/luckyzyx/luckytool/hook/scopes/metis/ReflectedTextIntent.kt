package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.resolver.FieldResolver
import com.highcapable.kavaref.resolver.MethodResolver
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 TextIntent bean 的反射访问封装。
 *
 * 卡片 bean 是宿主私有类，只能按成员名访问。优先用公开 getter/setter（getPackageName / setPackageName /
 * getIntent / getTextType），不存在时再沿继承链回落 packageName 字段访问。
 * 访问器按 Class 缓存：卡片点击是高频路径，每次重新查找方法与字段太浪费。
 *
 * 缓存里只放解析结果：解析器绑定实例后不能复用，每次取值都用 `copy()` 重新绑定。
 */
@Obfuscate
class ReflectedTextIntent private constructor(
    private val bean: Any,
    private val accessors: Accessors
) {
    private class Accessors(
        val getPackageName: MethodResolver<Any>?,
        val setPackageName: MethodResolver<Any>?,
        val packageNameField: FieldResolver<Any>?,
        val getIntent: MethodResolver<Any>?,
        val getTextType: MethodResolver<Any>?
    )

    /** 当前包名，读不到返回 null */
    fun packageName(): String? {
        accessors.getPackageName?.let { resolver ->
            runCatching { resolver.copy().of(bean).invoke<Any?>() as? String }.getOrNull()?.let { return it }
        }
        val field = accessors.packageNameField ?: return null
        return runCatching { field.copy().of(bean).get<Any?>() as? String }.getOrNull()
    }

    /**
     * 写入包名。
     *
     * 这里必须「有 setter 就只走 setter 并立刻返回」，不能写成 setter?.invoke(x) ?: 字段赋值：
     * void 方法的 invoke 返回 null，?: 会永远落到字段分支，等于 setter 白调、字段也没写对。
     */
    fun setPackageName(value: String) {
        accessors.setPackageName?.let { resolver ->
            if (runCatching { resolver.copy().of(bean).invoke<Any?>(value) }.isSuccess) return
        }
        accessors.packageNameField?.let { field ->
            runCatching { field.copy().of(bean).set(value) }
        }
    }

    /** 卡片自带的 Intent，读不到返回 null；改写动作都作用在它上面 */
    fun intent(): Any? = accessors.getIntent?.let { resolver ->
        runCatching { resolver.copy().of(bean).invoke<Any?>() }.getOrNull()
    }

    /** 卡片类型标记，textType == 29 是文本搜索卡 */
    fun textType(): Int? = accessors.getTextType?.let { resolver ->
        runCatching { (resolver.copy().of(bean).invoke<Any?>() as? Number)?.toInt() }.getOrNull()
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

        private fun build(type: Class<Any>): Accessors = Accessors(
            getPackageName = findMethod(type, "getPackageName"),
            setPackageName = findMethod(type, "setPackageName", String::class.java),
            packageNameField = findField(type, "packageName"),
            getIntent = findMethod(type, "getIntent"),
            getTextType = findMethod(type, "getTextType")
        )

        /**
         * 沿继承链找方法：宿主可能把 getter 放在父类里，只看本类会漏。
         *
         * 参数表必须精确匹配（按 Class 逐位相等），无参方法用 `emptyParameters()` 而不是留空——留空等于不限制参数个数。
         */
        private fun findMethod(
            type: Class<Any>,
            methodName: String,
            vararg parameterTypes: Class<*>
        ): MethodResolver<Any>? = type.resolve().firstMethodOrNull {
            name = methodName
            if (parameterTypes.isEmpty()) emptyParameters() else parameters(*parameterTypes)
            superclass()
        }

        /** 沿继承链找字段；宿主字段都是 private，取值时解析器会自己放开访问权限 */
        private fun findField(type: Class<Any>, fieldName: String): FieldResolver<Any>? =
            type.resolve().firstFieldOrNull {
                name = fieldName
                superclass()
            }
    }
}
