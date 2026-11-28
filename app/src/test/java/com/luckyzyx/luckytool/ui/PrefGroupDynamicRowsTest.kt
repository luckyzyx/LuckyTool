package com.luckyzyx.luckytool.ui

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PageScreen
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
 * 回归测试：[PrefGroup] 的行登记必须在状态变化后重新落到卡片上。
 *
 * 对应真机缺陷：强制刷新率页的 displayMode 列表由 root IPC 异步返回，首帧 modes 为空、
 * 回来之后数据卡一直是空的（只剩下面的开关卡片）。
 *
 * 根因：`PrefGroup` 的 content 若是可组合 lambda，它就自带独立组合作用域 —— 状态变化时只有它自己
 * 重跑，新登记的条目进不了已经画出来的那批行（卡片停在首帧的空内容）。content 必须是普通登记块，
 * 读取落在 `PrefGroup` 自己的作用域里，状态一变整组重新登记。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefGroupDynamicRowsTest {

    private companion object {
        const val PREFS_NAME = "pref_group_dynamic_rows_test"

        /** 视口高度：远大于测试条目高度，保证条目落在视口内被组合。 */
        const val VIEWPORT_HEIGHT = 20_000
    }

    @Test
    fun `rows registered after a state change reach the card`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        PrefState.invalidate(PREFS_NAME)
        val state = PrefState.ofProvider { raw }

        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val composeView = ComposeView(activity)
        val show = mutableStateOf(false)
        var renders = 0

        activity.setContentView(composeView)
        composeView.setContent {
            MaterialTheme {
                PageScreen(state = state) {
                    custom(key = "body") { _ ->
                        PrefGroup {
                            if (show.value) {
                                item {
                                    renders++
                                    Text("dynamic")
                                }
                            }
                        }
                    }
                }
            }
        }
        layout(composeView)

        assertEquals("首帧没有行，宿主必须先渲染出卡片", 0, renders)

        show.value = true
        Snapshot.sendApplyNotifications()
        repeat(4) { layout(composeView) }

        assertTrue(
            "状态变化后登记的条目不进卡片（实际渲染 $renders 次，期望至少 1 次）",
            renders > 0,
        )
    }

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
