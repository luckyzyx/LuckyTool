package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.yukihookapi.hook.param.PackageParam
import java.lang.reflect.Field
import org.lsposed.lsparanoid.Obfuscate

/**
 * 卡片文案模板：四段模板（标题 / 副标题 / 按钮 / 胶囊）、占位符替换、目标应用名解析。
 *
 * 两个文案层（卡片构建与安装面板）共用这一套模板，保证同一张卡片两处文案一致。
 * 约定：任一模板为空 = 保留 metis 原文案；{app} 解不出应用名时整条不改，避免卡片上出现「打开 」这种半截文案。
 */
@Obfuscate
object CardTextRules {
    /** 目标应用名占位符 */
    const val PLACEHOLDER_APP = "{app}"

    /** 各段文案在卡片对象上的候选字段名，按顺序取第一个存在的 */
    private val TITLE_FIELDS = listOf("cardTitle", "title", "mainTitle")
    private val SUBTITLE_FIELDS = listOf("cardSubTitle", "subTitle", "subtitle", "secondTitle")
    private val BUTTON_FIELDS = listOf("button", "buttonText", "positiveText", "btnText")
    private val CAPSULE_FIELDS = listOf("capsTitle", "capsDirectText", "capsuleText", "capsText")

    /** 卡片对象上表示目标包名的候选字段名，用于在没有 getTextIntent() 时反查应用名 */
    private val PACKAGE_FIELDS = listOf("packageName", "mPackageName", "pkgName")

    data class Templates(
        val title: String?,
        val subtitle: String?,
        val button: String?,
        val capsule: String?
    ) {
        /** 四段都空说明用户没配任何文案，文案层可以整体跳过 */
        fun isEmpty(): Boolean =
            title.isNullOrBlank() && subtitle.isNullOrBlank() && button.isNullOrBlank() && capsule.isNullOrBlank()
    }

    /** 按当前偏好组装模板；每次调用都重读偏好，改完即时生效 */
    fun templates(param: PackageParam): Templates = Templates(
        title = MetisSettings.cardTitle(param),
        subtitle = MetisSettings.cardSubtitle(param),
        button = MetisSettings.cardButton(param),
        capsule = MetisSettings.cardCapsule(param)
    )

    /** 套模板：空模板返回 null 表示这一项不改 */
    fun render(template: String?, appLabel: String): String? {
        val text = template?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (!text.contains(PLACEHOLDER_APP)) return text
        if (appLabel.isEmpty()) return null
        return text.replace(PLACEHOLDER_APP, appLabel)
    }

    /**
     * 目标应用显示名。
     * 先按替换表把包名换成目标 App 再查名字，这样卡片文案与点击后真正打开的 App 一致；
     * 查不到（目标未安装）回落包名，保证文案里总有点东西。
     */
    @Suppress("DEPRECATION")
    fun appLabel(param: PackageParam, packageName: String?): String {
        val target = PackageSwapRules.targetOf(param, packageName) ?: packageName?.trim().orEmpty()
        if (target.isEmpty()) return ""
        val manager = MetisSettings.contextOf(param)?.let { runCatching { it.packageManager }.getOrNull() }
        val label = manager?.let { pm ->
            runCatching {
                val info = pm.getApplicationInfo(target, 0)
                pm.getApplicationLabel(info).toString()
            }.getOrNull()
        }
        return label ?: target
    }

    /** 把四段模板写进卡片对象，返回是否有任一字段被改；字段不存在就跳过，不新建字段 */
    fun applyTemplates(target: Any?, templates: Templates, appLabel: String): Boolean {
        val card = target ?: return false
        var changed = setFieldText(card, TITLE_FIELDS, render(templates.title, appLabel))
        changed = setFieldText(card, SUBTITLE_FIELDS, render(templates.subtitle, appLabel)) || changed
        changed = setFieldText(card, BUTTON_FIELDS, render(templates.button, appLabel)) || changed
        changed = setFieldText(card, CAPSULE_FIELDS, render(templates.capsule, appLabel)) || changed
        return changed
    }

    /** 从卡片对象上读出目标包名，优先走 bean 的 getTextIntent() */
    fun packageNameOf(bean: Any?): String? {
        val card = bean ?: return null
        ReflectedTextIntent.of(card)?.intent()?.let { intent ->
            (intent as? android.content.Intent)?.let { plain ->
                plain.getPackage()?.let { return it }
                plain.component?.packageName?.let { return it }
            }
        }
        PACKAGE_FIELDS.forEach { name ->
            val field = findField(card.javaClass, name) ?: return@forEach
            val value = runCatching { field.get(card) as? String }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    /** 只写 String 类型的同名字段；类型对不上直接跳过，避免把整数/对象字段写坏 */
    private fun setFieldText(target: Any, names: List<String>, value: String?): Boolean {
        if (value == null) return false
        val field = names.firstNotNullOfOrNull { findField(target.javaClass, it) } ?: return false
        if (field.type != String::class.java) return false
        return runCatching { field.set(target, value) }.isSuccess
    }

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
