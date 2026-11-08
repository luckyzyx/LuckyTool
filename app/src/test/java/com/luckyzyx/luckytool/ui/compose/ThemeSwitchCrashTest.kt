package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.itos.coui.effect.CouiFloatingRenderer
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.RootScaffold
import com.luckyzyx.luckytool.ui.compose.components.TabsScaffold
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.appPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeSwitchCrashTest {
    @Test
    fun `switching ui mode from material to coui at runtime does not crash`() {
        val host = ComposeGestureHost()
        host.activity.appPrefs(SettingsPrefs).edit().clear()
            .putString(ThemeController.KEY_UI_MODE, UiMode.Material.value)
            .putString("dark_theme", ColorMode.LIGHT.value.toString())
            .putBoolean("use_dynamic_color", false)
            .commit()
        host.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                AppTheme {
                    val context = LocalContext.current
                    val settings = remember { PrefState.of(context, SettingsPrefs) }
                    PageScaffold(title = "设置") { padding ->
                        ScopeScreen(
                            state = settings,
                            modifier = Modifier.fillMaxSize().padding(padding),
                        ) {
                            group()
                            list(
                                key = ThemeController.KEY_UI_MODE,
                                title = "界面风格",
                                entries = UiMode.entries.map { it.name }.toTypedArray(),
                                entryValues = UiMode.entries.map { it.value }.toTypedArray(),
                                default = UiMode.DEFAULT_VALUE,
                                onChange = { ThemePrefs.notifyChanged() },
                            )
                        }
                    }
                }
            }
        }
        assertEquals(UiMode.Material, ThemeController.getUiMode(host.activity))

        host.clickText("界面风格")
        host.advance()
        host.clickText("Coui")
        host.advance(3000)

        assertEquals(UiMode.Coui, ThemeController.getUiMode(host.activity))
        assertNotNull("COUI 页壳应渲染出页面标题", host.textNode("设置"))
        assertNotNull("切换后行尾应显示当前值 Coui", host.textNode("Coui"))
    }

    @Test
    fun `coui floating renderer constructs in robolectric`() {
        val host = ComposeGestureHost()
        var rendererOk = false
        var message = "none"
        host.setContent {
            AppTheme {
                val renderer = remember {
                    runCatching { CouiFloatingRenderer() }.onFailure { message = it.toString() }.getOrNull()
                }
                SideEffect { rendererOk = renderer != null }
                Text(if (renderer != null) "renderer-ok" else "renderer-null")
            }
        }
        println("COUI_RENDERER_OK=$rendererOk MESSAGE=$message")
    }

    @Test
    fun `switching ui mode with tabs scaffold does not crash`() {
        val host = ComposeGestureHost()
        host.activity.appPrefs(SettingsPrefs).edit().clear()
            .putString(ThemeController.KEY_UI_MODE, UiMode.Material.value)
            .putString("dark_theme", ColorMode.LIGHT.value.toString())
            .putBoolean("use_dynamic_color", false)
            .commit()
        host.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                AppTheme {
                    val pagerState = rememberPagerState(initialPage = MainTab.Home.ordinal) { MainTab.entries.size }
                    val mainPagerState = rememberMainPagerState(pagerState)
                    MainTabs(
                        mainPagerState = mainPagerState,
                        isActive = true,
                    ) { tab, _ ->
                        Box(Modifier.fillMaxSize()) { Text(tab.name) }
                    }
                }
            }
        }
        assertEquals(UiMode.Material, ThemeController.getUiMode(host.activity))
        assertNotNull("Material 标签外壳应渲染出首页", host.textNode("Home"))

        host.activity.appPrefs(SettingsPrefs).edit()
            .putString(ThemeController.KEY_UI_MODE, UiMode.Coui.value).commit()
        ThemePrefs.notifyChanged()
        host.advance(3000)

        assertEquals(UiMode.Coui, ThemeController.getUiMode(host.activity))
        assertNotNull("COUI 标签外壳应仍渲染首页", host.textNode("Home"))
    }

    @Test
    fun `switching ui mode with full shell nesting does not crash`() {
        val host = ComposeGestureHost()
        host.activity.appPrefs(SettingsPrefs).edit().clear()
            .putString(ThemeController.KEY_UI_MODE, UiMode.Material.value)
            .putString("dark_theme", ColorMode.LIGHT.value.toString())
            .putBoolean("use_dynamic_color", false)
            .commit()
        host.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                AppTheme {
                    RootScaffold { _ ->
                        TabsScaffold(bottomBar = { Text("底栏") }) { tabPadding ->
                            PageScaffold(title = "设置") { pagePadding ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(tabPadding)
                                        .padding(pagePadding),
                                ) { Text("页面内容") }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(UiMode.Material, ThemeController.getUiMode(host.activity))
        assertNotNull("Material 全壳应渲染页面内容", host.textNode("页面内容"))

        host.activity.appPrefs(SettingsPrefs).edit()
            .putString(ThemeController.KEY_UI_MODE, UiMode.Coui.value).commit()
        ThemePrefs.notifyChanged()
        host.advance(3000)

        assertEquals(UiMode.Coui, ThemeController.getUiMode(host.activity))
        assertNotNull("COUI 全壳应仍渲染页面内容", host.textNode("页面内容"))
        assertNotNull("COUI 全壳应渲染出页面标题", host.textNode("设置"))
    }
}
