package com.luckyzyx.luckytool.ui.compose.scopes

import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder

/**
 * 一个 Compose 作用域页的声明——旧 [BaseScopePreferenceFeagment] 子类中
 * scopes/currentPrefsName/loadPreferences 等字段的注册单元等价物。
 *
 * @param pageKey        页面注册键（nav_container.xml 中 page_key 参数值）
 * @param prefsName      SharedPreferences 文件名（ModulePrefs 等）
 * @param packName       宿主包名（sendPrefsValue 通知目标）
 * @param scopes         Xposed 作用域包名列表（重启作用域对话框用）
 * @param restartEnabled 是否显示"重启作用域"菜单
 * @param content        页面内容 DSL（每次重组重跑，条件可见性写 Kotlin if）
 */
class ScopePageSpec(
    val pageKey: String,
    val prefsName: String,
    val packName: String,
    val scopes: Array<String>,
    val restartEnabled: Boolean,
    val content: PrefScopeBuilder.() -> Unit,
)

/** Compose 页注册表：ComposeScopeFragment 按 page_key 查表渲染 */
object ScopePageRegistry {
    private val pages = LinkedHashMap<String, ScopePageSpec>()

    fun register(spec: ScopePageSpec) {
        pages[spec.pageKey] = spec
    }

    operator fun get(key: String): ScopePageSpec? = pages[key]

    init {
        register(StatusBarClockPage.spec)
        register(StatusBarRelatedPage.spec)
    }
}
