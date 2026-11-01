package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import org.lsposed.lsparanoid.Obfuscate

/**
 * 卡片文案自定义的 hook 层。
 *
 * 两个文案层共用 metis_card_text_enabled 开关与同一套模板：
 * - Seedling$Builder.build()：主路径，返回的卡片对象上直接改标题 / 副标题 / 按钮 / 胶囊；
 * - SingleAppInstallPanel.onPositiveText() / onTitle()：兜底，安装引导面板的按钮与标题单独生成，
 *   只在该面板文案里含「安装」时替换，避免把无关提示也一起改掉。
 *
 * 开关关闭时这里完全不改文案（点击重定向照常），所以在拦截器里还要再确认一次开关：
 * 改开关不重启进程时 hooker 已经装载，只能靠这一步生效。
 */
@Obfuscate
object MetisCardTextHook : YukiBaseHooker() {

    private const val SEEDLING_BUILDER = "com.oplus.textintent.pantanal.Seedling\$Builder"
    private const val INSTALL_PANEL = "com.oplus.textintent.distribution.ui.SingleAppInstallPanel"

    /** 面板文案里的「安装」字样：只有安装引导面板的文案才替换 */
    private const val INSTALL_KEYWORD = "安装"

    override fun onHook() {
        val param = this
        hookSeedlingBuilder(param)
        hookInstallPanel(param)
    }

    /** E 主路径：build() 无参，返回卡片对象，拿到返回值的副本再反射改字段 */
    private fun hookSeedlingBuilder(param: PackageParam) {
        runCatching {
            SEEDLING_BUILDER.toClass().resolve().apply {
                firstMethod {
                    name = "build"
                    emptyParameters()
                }.intercept {
                    val card = proceed()
                    rewriteCard(param, card)
                    card
                }
            }
        }.onFailure { MetisLog.warn("SeedlingBuilder.build", "hook 安装失败: $it") }
    }

    /** F 兜底路径：面板的按钮与标题各自生成，两个都挂 */
    private fun hookInstallPanel(param: PackageParam) {
        runCatching {
            INSTALL_PANEL.toClass().resolve().apply {
                firstMethod {
                    name = "onPositiveText"
                    emptyParameters()
                }.intercept {
                    val panel = instanceOrNull<Any>()
                    val original = proceed()
                    rewritePanelText(param, panel, original, "SingleAppInstallPanel.onPositiveText") { it.button }
                }
                firstMethod {
                    name = "onTitle"
                    emptyParameters()
                }.intercept {
                    val panel = instanceOrNull<Any>()
                    val original = proceed()
                    rewritePanelText(param, panel, original, "SingleAppInstallPanel.onTitle") { it.title }
                }
            }
        }.onFailure { MetisLog.warn("SingleAppInstallPanel", "hook 安装失败: $it") }
    }

    /** 卡片对象上四段一起改；模板全空则整体跳过，省掉反射开销 */
    private fun rewriteCard(param: PackageParam, card: Any?) {
        if (card == null) return
        if (!MetisSettings.cardTextEnabled(param)) return
        val templates = CardTextRules.templates(param)
        if (templates.isEmpty()) return
        val label = CardTextRules.appLabel(param, CardTextRules.packageNameOf(card))
        if (CardTextRules.applyTemplates(card, templates, label)) {
            MetisLog.debug(param, "SeedlingBuilder.build", "卡片文案已替换 appLabel=$label")
        }
    }

    /** 面板文案替换：原文不含「安装」或模板为空都原样返回 */
    private fun rewritePanelText(
        param: PackageParam,
        panel: Any?,
        original: Any?,
        from: String,
        templateOf: (CardTextRules.Templates) -> String?
    ): Any? {
        val text = (original as? CharSequence)?.toString() ?: return original
        if (!MetisSettings.cardTextEnabled(param)) return original
        if (!text.contains(INSTALL_KEYWORD)) return original
        val templates = CardTextRules.templates(param)
        val template = templateOf(templates)
        if (template.isNullOrBlank()) return original
        val label = CardTextRules.appLabel(param, panelPackageName(panel))
        val replacement = CardTextRules.render(template, label) ?: return original
        MetisLog.debug(param, from, "$text -> $replacement")
        return replacement
    }

    /** 面板上没有直接的包名 getter，只能调它的 getTextIntent() 再读 bean 的包名 */
    private fun panelPackageName(panel: Any?): String? {
        val target = panel ?: return null
        var current: Class<*>? = target.javaClass
        while (true) {
            val cls = current ?: return null
            if (cls == Any::class.java) return null
            val method = runCatching { cls.getDeclaredMethod("getTextIntent") }.getOrNull()
            if (method != null) {
                method.isAccessible = true
                val bean = runCatching { method.invoke(target) }.getOrNull() ?: return null
                return ReflectedTextIntent.of(bean)?.packageName()
            }
            current = cls.superclass
        }
    }
}
