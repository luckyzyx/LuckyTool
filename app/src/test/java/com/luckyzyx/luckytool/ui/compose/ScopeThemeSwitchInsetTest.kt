package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.appPrefs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 回归测试：运行时切换界面风格后，设置页的行两侧内缩必须与同一风格冷启动时一致。
 *
 * 运行时切换只改 LocalUiMode，内容子树由 AppTheme 的 movableContentOf 搬移而不重建，
 * 所以「按外观线选列表分支」的判定一旦被 remember 冻住，列表就继续按旧线排版：
 * COUI 切 Material 时 material 分组内缩叠上行内缩（两侧间距变大），
 * Material 切 COUI 时逐行分支不给内缩（行贴边），两种情况都要重启应用才恢复。
 *
 * 断言口径：被测行标题文本的左边距，运行时切过去后与冷启动该风格时的取值相等。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScopeThemeSwitchInsetTest {

    private companion object {
        /** 被测行的标题文本：行容器的横向内缩全落在这段文本的左边距上。 */
        const val ROW_TITLE = "界面风格"
    }

    /** 按 [uiMode] 冷启动一个设置页宿主，页面内容就是一条会显示文本的分组行。 */
    private fun mount(uiMode: UiMode): ComposeGestureHost {
        val host = ComposeGestureHost()
        host.activity.appPrefs(SettingsPrefs).edit().clear()
            .putString(ThemeController.KEY_UI_MODE, uiMode.value)
            .putString("dark_theme", ColorMode.LIGHT.value.toString())
            .putBoolean("use_dynamic_color", false)
            .commit()
        host.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                AppTheme {
                    // 每个用例一个 Application：绕开 PrefState 按 prefs 名的进程级缓存，
                    // 免得缓存实例把 provider 绑在上一个用例的 prefs 文件上。
                    val settings = remember { PrefState.ofProvider { host.activity.appPrefs(SettingsPrefs) } }
                    PageScaffold(title = "设置") { padding ->
                        ScopeScreen(
                            state = settings,
                            modifier = Modifier.fillMaxSize().padding(padding),
                        ) {
                            group(title = "外观") {
                                list(
                                    key = ThemeController.KEY_UI_MODE,
                                    title = ROW_TITLE,
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
        }
        return host
    }

    /** 行标题文本的左边距；以语义节点在根坐标里的左边界取值。 */
    private fun rowInset(host: ComposeGestureHost): Float {
        val node = requireNotNull(host.textNode(ROW_TITLE)) { "缺少行标题「$ROW_TITLE」" }
        return node.boundsInRoot.left
    }

    /** 与主题页一致：写入 ui_mode 偏好后通知主题重读，等待重组与重测量完成。 */
    private fun switchUiMode(host: ComposeGestureHost, uiMode: UiMode) {
        host.activity.appPrefs(SettingsPrefs).edit()
            .putString(ThemeController.KEY_UI_MODE, uiMode.value)
            .commit()
        ThemePrefs.notifyChanged()
        host.advance(3000)
        assertEquals(uiMode, ThemeController.getUiMode(host.activity))
    }

    @Test
    fun `runtime switch from material to coui keeps the coui row inset`() {
        val cold = mount(UiMode.Coui)
        val expected = rowInset(cold)
        cold.close()

        val host = mount(UiMode.Material)
        switchUiMode(host, UiMode.Coui)
        assertEquals(
            "Material 切到 COUI 后行两侧内缩必须与冷启动 COUI 一致",
            expected,
            rowInset(host),
            0.5f,
        )
        host.close()
    }

    @Test
    fun `runtime switch from coui to material keeps the material row inset`() {
        val cold = mount(UiMode.Material)
        val expected = rowInset(cold)
        cold.close()

        val host = mount(UiMode.Coui)
        switchUiMode(host, UiMode.Material)
        assertEquals(
            "COUI 切到 Material 后行两侧内缩必须与冷启动 Material 一致",
            expected,
            rowInset(host),
            0.5f,
        )
        host.close()
    }
}
