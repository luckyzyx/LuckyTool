package com.luckyzyx.luckytool.ui.theme

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import com.itos.coui.theme.CouiTheme

/**
 * 回归测试：两条外观线都必须给 material3 的 LocalContentColor 下发主题色。
 *
 * material3 的 MaterialTheme / MaterialExpressiveTheme 本身不下发 LocalContentColor，
 * 只有 Surface / Scaffold / TopAppBar / Button 这类组件才会下发。COUI 外观线里 material3
 * 组件挂在 COUI 容器下（COUI 只下发自己那份同名 CompositionLocal），取不到就退回
 * material3 默认的黑 —— 深色模式（深底 + 黑图标）下顶栏 actions 里的「菜单图标」会
 * 整片看不见，这也是旧 XML 的 android:tint 改动对这些调用点无效的原因。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeContentColorTest {

    @Test
    fun `coui line hands the coui content color to material3`() {
        val appSettings = AppSettings(colorMode = ColorMode.DARK)

        var couiContentColor: Color? = null
        var materialContentColor: Color? = null
        compose {
            CouiAppTheme(appSettings = appSettings) {
                couiContentColor = CouiTheme.colorScheme.onSurface
                materialContentColor = MaterialLocalContentColor.current
            }
        }

        assertNotNull("Robolectric 下 ComposeView 没有完成首次组合", materialContentColor)
        assertEquals(couiContentColor, materialContentColor)
        assertNotEquals(
            "COUI 线上 material3 的内容色退化成了默认黑，深色模式下图标会看不见",
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
            MaterialAppTheme(appSettings = appSettings) {
                schemeContentColor = MaterialTheme.colorScheme.onBackground
                materialContentColor = MaterialLocalContentColor.current
            }
        }

        assertNotNull("Robolectric 下 ComposeView 没有完成首次组合", materialContentColor)
        assertEquals(schemeContentColor, materialContentColor)
    }

    /**
     * Android 14 以下没有 COUI：请求 COUI 必须实际落到 Material 线且不崩。
     *
     * 判定方式是同一份内容在两种请求下取到的 material3 内容色相同 —— 请求 COUI 时若真的
     * 组合了 COUI 容器，这个值会变成 COUI 的 onSurface；这里只看它是否与请求 Material 时
     * 一致，不依赖任一条线内部的具体颜色值。
     */
    @Test
    @Config(sdk = [32])
    fun `coui request below android 14 renders the material line`() {
        assertFalse(UiMode.Coui.isAvailable)
        assertEquals(UiMode.Material, UiMode.Coui.coerceAvailable())

        val appSettings = AppSettings(colorMode = ColorMode.DARK)
        var couiRequested: Color? = null
        var materialRequested: Color? = null
        compose {
            AppTheme(appSettings = appSettings, uiMode = UiMode.Coui) {
                Text("coui 回落")
                couiRequested = MaterialLocalContentColor.current
            }
        }
        compose {
            AppTheme(appSettings = appSettings, uiMode = UiMode.Material) {
                Text("material")
                materialRequested = MaterialLocalContentColor.current
            }
        }

        assertNotNull("Robolectric 下 ComposeView 没有完成首次组合", couiRequested)
        assertEquals(materialRequested, couiRequested)
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
