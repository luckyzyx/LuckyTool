package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefPageBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.related.ApplicationRelatedPage
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结「条件条目随偏好写入自动重算」这一语义（不再依赖 Activity recreate）。
 *
 * [PrefPageBuilder] 的构建 lambda 是普通（非 Composable）函数，由 `PageScreen` 每次重组重跑；
 * 而 `list(...)` 的 onItemSelected 与 `switch(...)` 的 apply 内部**先 `state.set`**
 * （PrefPage.kt:404 / PrefPage.kt:346）→ [PrefState.revision] 前进 → 页面重组 → 构建 lambda 重跑。
 * 因此「按偏好值决定是否显示的行」会自己出现/消失。
 *
 * 正因如此，ApplicationRelatedPage / AodRelatedPage 已删除对应的 `restart?.invoke()`
 * （旧行为：写完偏好顺手重建整个 LuckyTool Activity）。若这条 revision 链哪天断了
 * （例如改回裸 SharedPreferences 写入、不再走 `state.set`），本测试会失败并拦住那次改动。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PageConditionalVisibilityTest {

    private lateinit var context: Context
    private lateinit var raw: SharedPreferences
    private lateinit var state: PrefState

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        raw = context.getSharedPreferences("scope_conditional_test", Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        state = PrefState.ofProvider { raw }
    }

    /** 复刻 PageScreen 的构建流程：beginBuild → content → computeSegments，返回本轮条目 key 集合 */
    private fun buildApplicationPage(): Set<String> {
        val builder = PrefPageBuilder(state)
        builder.context = context
        builder.beginBuild()
        ApplicationRelatedPage.spec.content(builder)
        builder.computeSegments()
        return builder.entries.mapNotNull { it.indexKey }.toSet()
    }

    @Test
    fun `conditional rows stay hidden while the driving pref holds the default`() {
        val keys = buildApplicationPage()
        assertTrue("驱动条目本身始终可见", "set_wlan_sla_whitelist_mode" in keys)
        assertFalse("\"0\" 时不应出现黑名单条目", "remove_wlan_sla_blacklist" in keys)
        assertFalse("custom_wlan_sla_whitelist" in keys)
        assertFalse("custom_wlan_sla_game_whitelist" in keys)
    }

    @Test
    fun `writing the driving pref recomputes the conditional rows`() {
        val before = state.revision.value
        assertTrue(state.set("set_wlan_sla_whitelist_mode", "1"))
        assertTrue(
            "state.set 必须让 revision 前进（PageScreen 据此重组重跑构建 lambda）",
            state.revision.value > before,
        )

        val keys = buildApplicationPage()
        assertTrue("条件条目应在重跑后出现，无需 recreate", "remove_wlan_sla_blacklist" in keys)
        assertTrue("custom_wlan_sla_whitelist" in keys)
        assertTrue("custom_wlan_sla_game_whitelist" in keys)
    }
}
