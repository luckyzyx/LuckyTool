package com.luckyzyx.luckytool.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.ComposeGestureHost
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.related.LockScreenRelatedPage
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = Application::class,
    qualifiers = "en-rUS-w411dp-h880dp-xhdpi",
    shadows = [LockScreenCredentialShortcutPageTest.ShadowBuildVersionUtils::class],
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockScreenCredentialShortcutPageTest {
    private lateinit var context: Context
    private lateinit var raw: SharedPreferences
    private lateinit var state: PrefState

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        raw = context.getSharedPreferences(ModulePrefs, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        PrefState.invalidate(ModulePrefs)
        state = PrefState.ofProvider { raw }
        ShadowBuildVersionUtils.osCode = 37
        assertEquals(37, getOSVersionCode)
    }

    @Test
    fun `shortcut starts off and both toggles persist before notifying the host`() {
        val sent = mutableListOf<Pair<String, Any>>()
        val title = context.getString(R.string.lock_screen_credential_shortcut)
        ComposeGestureHost().use { ui ->
            ui.setContent {
                CompositionLocalProvider(LocalUiMode provides UiMode.Material) {
                    MaterialTheme {
                        ScopeScreen(
                            state = state,
                            scrollTarget = ScrollTarget(KEY, 0),
                            sendValue = { key, value ->
                                assertEquals("通知前必须先保存偏好", value, raw.all[key])
                                sent += key to value
                            },
                            content = LockScreenRelatedPage.spec.content,
                        )
                    }
                }
            }
            ui.advance()
            assertTrue("打开页面不应写入默认值", raw.all.isEmpty())
            assertTrue("打开页面不应发送通知", sent.isEmpty())

            ui.clickText(title)
            ui.advance()
            assertTrue("首次点击应从默认关闭切换为开启", state.getBoolean(KEY))
            assertEquals(listOf(KEY to true), sent)

            ui.clickText(title)
            ui.advance()
            assertFalse(state.getBoolean(KEY))
            assertEquals(listOf(KEY to true, KEY to false), sent)
        }
    }

    @Test
    fun `search index gates the shortcut at OS code 37 and explains password entry`() {
        for ((osCode, visible) in listOf(36 to false, 37 to true, 40 to true)) {
            ShadowBuildVersionUtils.osCode = osCode
            assertEquals(osCode, getOSVersionCode)
            val entries = ScopePageRegistry.buildIndex(context, LockScreenRelatedPage.spec)
                .filter { it.key == KEY }
            assertEquals("osCode=$osCode", if (visible) 1 else 0, entries.size)
            if (visible) {
                assertEquals("Shortcut to password unlock", entries.single().title)
                assertEquals(
                    "Swipe the shortcut opposite the camera to open the pattern, PIN or password screen. " +
                        "A plain swipe up no longer opens it while fingerprint unlock is available. " +
                        "Replaces the shortcut in that position.\nNeed to restart scope",
                    entries.single().summary,
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
    fun `Chinese search index describes the opposite shortcut and the replaced gesture`() {
        val entry = ScopePageRegistry.buildIndex(context, LockScreenRelatedPage.spec)
            .single { it.key == KEY }
        assertEquals("快捷入口打开密码页", entry.title)
        assertEquals(
            "滑动相机对侧的快捷按钮进入图案、PIN 或密码页；指纹可用时，普通上滑不再进入。" +
                "会替换该位置原有的快捷方式。\n需要重启作用域",
            entry.summary,
        )
    }

    // OplusBuild 仅作为 compileOnly stub 提供，测试直接替换页面读取的版本 getter。
    @Implements(
        className = "com.luckyzyx.luckytool.utils.BuildVersionUtilsKt",
        isInAndroidSdk = false,
    )
    class ShadowBuildVersionUtils {
        companion object {
            var osCode = 37

            @JvmStatic
            @Implementation
            fun getGetOSVersionCode(): Int = osCode
        }
    }

    private companion object {
        const val KEY = "lock_screen_credential_shortcut"
    }
}
