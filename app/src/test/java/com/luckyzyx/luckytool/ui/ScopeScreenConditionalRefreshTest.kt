package com.luckyzyx.luckytool.ui

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 回归测试：偏好写入（[PrefState.revision] 前进）后，构建 lambda 里的**条件条目**必须真正生效。
 *
 * 对应真机缺陷：ApplicationRelatedPage 把「WLAN SLA 白名单模式」切到 1 之后，
 * `remove_wlan_sla_blacklist` 与两个白名单条目不出现（此前靠 `restart?.invoke()` 重建
 * Activity 才显示，去掉重启后缺陷暴露）。
 *
 * 根因：`ScopeScreen` 的重组只重建函数体里的 `builder.entries`；`LazyColumn` 的条目内容
 * lambda 只订阅 systemInfo / rows / 搜索结果等状态，不依赖 revision，列表不会拿新条目
 * 重算——函数体重建的结果没人用，条件条目因此一直不出现。修复：把 revision 的 State
 * 提出来后，在 `listItems` 内部也读一次，让列表内容直接依赖它。
 *
 * 断言口径：条件条目「进入条目列表、列表用它重算」这一步在 Robolectric 的强制测量宿主
 * 里可以直接观察到——写入偏好后无条件条目必须被**重新渲染**（列表内容重算 → 条目内容
 * lambda 换新 → 已有条目重组）。实测未修复时该计数停在 1（列表从未重算），修复后变为 2。
 * 至于新条目最终被组合出来，依赖 Robolectric 下子组合的时序，观测不稳定，故不作为断言；
 * 「条件条目会进入 entries」本身由 `ScopeConditionalVisibilityTest` 在同一条构建链上覆盖。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScopeScreenConditionalRefreshTest {

    private companion object {
        const val PREFS_NAME = "scope_conditional_refresh_test"

        /** 视口高度：远大于测试条目高度，保证新增条目落在视口内被组合。 */
        const val VIEWPORT_HEIGHT = 20_000
    }

    @Test
    fun `conditional entry makes the list rebuild after a prefs write`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        PrefState.invalidate(PREFS_NAME)
        val state = PrefState.ofProvider { raw }

        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val composeView = ComposeView(activity)
        var alwaysRendered = 0
        var conditionalRendered = 0

        activity.setContentView(composeView)
        composeView.setContent {
            MaterialTheme {
                ScopeScreen(state = state) {
                    custom(key = "always", title = "always") { _ -> alwaysRendered++ }
                    if (state.getString("mode", "0") == "1") {
                        custom(key = "conditional", title = "conditional") { _ -> conditionalRendered++ }
                    }
                }
            }
        }
        layout(composeView)

        // 前提断言：无条件条目先渲染出来，否则说明测试宿主（Compose + LazyColumn）没跑起来
        assertTrue("无条件条目必须被渲染，实际 $alwaysRendered", alwaysRendered >= 1)
        assertEquals("默认值 0 时条件条目不应渲染", 0, conditionalRendered)
        val renderedBeforeWrite = alwaysRendered

        state.set("mode", "1")
        Snapshot.sendApplyNotifications()
        repeat(4) { layout(composeView) }

        assertTrue(
            "写入偏好后列表必须重算（条目内容换新 → 无条件条目重组），" +
                "实际写入前 $renderedBeforeWrite、写入后 $alwaysRendered",
            alwaysRendered > renderedBeforeWrite,
        )
    }

    /**
     * Robolectric 下把 ComposeView 强制走完重组 + 测量 + 布局，让 LazyColumn 真正组合条目。
     *
     * 视口取 [VIEWPORT_HEIGHT] 这么高是必须的：LazyColumn 只组合落在视口内的条目，
     * 视口不够高时新增的条目根本不会被组合，测试会把「没组合」误判成「没刷新」。
     */
    private fun layout(view: View) {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            view.forceLayout()
            view.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEWPORT_HEIGHT, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, 1080, VIEWPORT_HEIGHT)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }
}
