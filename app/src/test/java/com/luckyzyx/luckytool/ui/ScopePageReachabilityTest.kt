package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.compose.scopes.related.ApplicationRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.FloatingWindowRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LauncherRelatedPage
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * 冻结「自带整页渲染的特殊页不因 DSL 行为空而被判不可达」这一语义。
 *
 * P4 批次的五个特殊页把 `content` 留成空 `{ }`，整页内容画在 `fullContent` 里（不走共享行渲染），
 * 它们自己声明 `selfRendered = true`：入口可见性只看 `isVisible` + 作用域，**不看** DSL 行索引。
 * 旧判据「空索引 = 该页不可见」会让 `PrefScope.page()` 直接不发射父页入口行 —— 症状就是这些页
 * 在界面上「无法显示」（暗色模式列表 / 意图 / 自定义浮窗 / 应用分身 / MEMC）。
 *
 * 若哪天有人把 `hasVisibleContent` 改回裸 `buildIndex(...).isNotEmpty()`，
 * 或给 FunctionPage 功能树改回 `index.isEmpty()` 即隐藏，本测试会失败并拦住那次改动。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScopePageReachabilityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** `fullContent != null` 且 `content` 恒空 `{ }` 的五个特殊页（全仓仅此五个） */
    private val fullContentPageKeys =
        listOf("dark_mode", "hide_app_intent", "zoom_window", "multi_app", "memc_config")

    @Test
    fun `special pages declare selfRendered so an empty index cannot hide them`() {
        fullContentPageKeys.forEach { key ->
            val spec = ScopePageRegistry[key]
            assertNotNull("$key 应已注册", spec)
            assertNotNull("$key 应自带整页渲染", spec!!.fullContent)
            // 判据来自页面自己的声明，不是「fullContent 非空」之类的结构特征反推
            assertTrue(
                "$key 必须声明 selfRendered：入口可见性只看 isVisible + 作用域",
                spec.selfRendered,
            )
            assertTrue(
                "$key 的 DSL 索引确实为空（行清单画在页面自己的布局里）",
                ScopePageRegistry.buildIndex(context, spec).isEmpty(),
            )
            assertTrue(
                "空索引不得作为不可见依据，否则入口行被误隐藏：$key",
                ScopePageRegistry.hasVisibleContent(context, spec),
            )
        }
    }

    @Test
    fun `page entry rows stay reachable for the fullContent special pages`() {
        // dark_mode 的作用域是 com.android.settings；Robolectric 默认未安装该包
        val settings = PackageInfo().apply { packageName = "com.android.settings" }
        Shadows.shadowOf(context.packageManager).installPackage(settings)
        fullContentPageKeys.forEach { key ->
            assertTrue("父页入口行必须发射：$key", ScopePageRegistry.isPageReachable(context, key))
        }
    }

    @Test
    fun `a page with neither DSL rows nor the selfRendered flag is not visible`() {
        val empty = ScopePageSpec(
            pageKey = "test_empty_spec",
            prefsName = ModulePrefs,
            packName = "",
            scopes = arrayOf(),
            restartEnabled = false,
        ) { }
        assertFalse(
            "既无 DSL 行也没声明自渲染的页仍应按空索引隐藏",
            ScopePageRegistry.hasVisibleContent(context, empty),
        )
    }

    @Test
    fun `the selfRendered flag alone makes an empty page visible`() {
        // 特殊页的判据只有 isVisible + 作用域：声明了自渲染就不再看索引（哪怕一行都没有）
        val special = ScopePageSpec(
            pageKey = "test_self_rendered_spec",
            prefsName = ModulePrefs,
            packName = "",
            scopes = arrayOf(),
            restartEnabled = false,
            selfRendered = true,
        ) { }
        assertTrue(ScopePageRegistry.buildIndex(context, special).isEmpty())
        assertTrue(
            "声明自渲染的页不应被空索引判为不可见",
            ScopePageRegistry.hasVisibleContent(context, special),
        )
    }

    @Test
    fun `unregistered page keys stay reachable`() {
        assertTrue(
            "未注册目标不拦截（保持旧行为）",
            ScopePageRegistry.isPageReachable(context, "not_registered_page"),
        )
    }

    /**
     * 端到端：真正发射入口行的是父页的 `page()` 项。
     * 修复前 dark_mode / hide_app_intent 在「应用」页整行消失（无条件项也不出现）。
     */
    @Test
    fun `parent pages emit the entry rows of the special pages`() {
        Shadows.shadowOf(context.packageManager).installPackage(
            PackageInfo().apply { packageName = "com.android.settings" },
        )

        // 应用页：意图与暗色模式列表无条件发射
        val application = buildIndexKeys(ApplicationRelatedPage.spec)
        assertTrue("意图入口行缺失：$application", "page:hideAppIntentFragment" in application)
        assertTrue("暗色模式入口行缺失：$application", "page:darkModeFragment" in application)

        // 应用页：应用分身仅在 set_multi_app_support_mode == "1" 时发射
        val multiApp = buildIndexKeys(ApplicationRelatedPage.spec) {
            putString("set_multi_app_support_mode", "1")
        }
        assertTrue("应用分身入口行缺失：$multiApp", "page:multiAppFragment" in multiApp)

        // 浮窗页：自定义浮窗支持列表仅在显示模式 == "3" 时发射
        val zoomWindow = buildIndexKeys(FloatingWindowRelatedPage.spec) {
            putString("custom_app_floating_window_display_mode", "3")
        }
        assertTrue("自定义浮窗入口行缺失：$zoomWindow", "page:zoomWindowFragment" in zoomWindow)

        // 设置页的 memcConfigFragment 入口行不做端到端断言：它除了
        // `enable_video_memc_frame_insertion` 开关，还要求 Oplus ROM（getOSVersionCode >= 26），
        // Robolectric 环境下 getOSVersionCode 恒为 0（OplusBuild.getOplusOSVERSION 不可用），
        // 整段（含该开关本身）都不会发射；memc_config 本身的可达性由上面的 isPageReachable 用例覆盖。
    }

    /** 复刻 ScopeScreen 的构建流程，返回本轮条目的索引键集合（page 项 = "page:$target"） */
    private fun buildIndexKeys(
        spec: ScopePageSpec,
        write: SharedPreferences.Editor.() -> Unit = {},
    ): Set<String> {
        val raw = context.getSharedPreferences(spec.prefsName, Context.MODE_PRIVATE)
        raw.edit().clear().apply(write).commit()
        val builder = PrefScopeBuilder(PrefState.ofProvider { raw })
        builder.context = context
        builder.beginBuild()
        spec.content(builder)
        builder.computeSegments()
        return builder.entries.mapNotNull { it.indexKey }.toSet()
    }
}
