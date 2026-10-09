package com.luckyzyx.luckytool.ui.theme

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor

/**
 * 回归测试：两条外观线都必须给 material3 的 LocalContentColor 下发主题色。
 *
 * material3 的 MaterialTheme / MaterialExpressiveTheme 本身不下发 LocalContentColor，
 * 只有 Surface / Scaffold / TopAppBar / Button 这类组件才会下发。Miuix 外观线里 material3
 * 组件挂在 miuix 容器下（miuix 只下发自己那份同名 CompositionLocal），取不到就退回
 * material3 默认的黑 —— 深色模式（深底 + 黑图标）下顶栏 actions 里的「菜单图标」会
 * 整片看不见，这也是旧 XML 的 android:tint 改动对这些调用点无效的原因。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeContentColorTest {

    @Test
    fun `miuix line hands the miuix content color to material3`() {
        val appSettings = AppSettings(colorMode = ColorMode.DARK)

        var miuixContentColor: Color? = null
        var materialContentColor: Color? = null
        compose {
            MiuixLuckyTheme(appSettings = appSettings) {
                miuixContentColor = MiuixLocalContentColor.current
                materialContentColor = MaterialLocalContentColor.current
            }
        }

        assertNotNull("Robolectric 下 ComposeView 没有完成首次组合", materialContentColor)
        assertEquals(miuixContentColor, materialContentColor)
        assertNotEquals(
            "Miuix 线上 material3 的内容色退化成了默认黑，深色模式下图标会看不见",
            Color.Black,
            materialContentColor,
        )
    }

    @Test
    fun `material line hands its own scheme color to material3`() {
        val appSettings = AppSettings(colorMode = ColorMode.DARK)

        var schemeContentColor: Color? = null
        var materialContentColor: Color? = null
        compose {
            MaterialLuckyTheme(appSettings = appSettings) {
                schemeContentColor = MaterialTheme.colorScheme.onBackground
                materialContentColor = MaterialLocalContentColor.current
            }
        }

        assertNotNull("Robolectric 下 ComposeView 没有完成首次组合", materialContentColor)
        assertEquals(schemeContentColor, materialContentColor)
    }

    /** 把 [content] 组合进一个真实的 Robolectric Activity，并驱动到首次组合完成 */
    private fun compose(content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val composeView = ComposeView(activity)
        activity.setContentView(composeView)
        composeView.setContent(content)

        val looper = shadowOf(Looper.getMainLooper())
        repeat(3) {
            looper.idle()
            looper.idleFor(100, TimeUnit.MILLISECONDS)
        }
    }
}
