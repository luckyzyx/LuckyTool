package com.luckyzyx.luckytool.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 记录 Compose 快照读取追踪的一个关键行为：**`derivedStateOf` 会追踪其计算块内
 * 「普通（非 @Composable）lambda」对状态对象的读取**。
 *
 * `ScopeScreen` 的条目重建依赖同一机制：只有把 `builder.content()` 放进
 * LazyColumn 的 content lambda（由 LazyList 的 derivedStateOf 包裹），
 * 其中对页面级 `mutableStateOf`（systemInfo / rows / filtered 等）的读取才会进入
 * 快照订阅；放在 `ScopeScreen` 函数体里则不会刷新列表
 * （旧症状：功能树白屏、搜索结果不出现、SYSTEMINFO 切页后才显示）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeReadTrackingTest {

    @Test
    fun `derivedStateOf tracks a state read made inside a plain lambda`() {
        val state = mutableStateOf(0)
        var computations = 0

        // 非 @Composable 的普通函数读取状态对象
        fun readPlain(): Int = state.value
        val derived = derivedStateOf {
            computations++
            readPlain()
        }

        assertEquals(0, derived.value)
        assertEquals(1, computations)

        state.value = 7
        Snapshot.sendApplyNotifications()

        assertEquals(7, derived.value)
        assertEquals("状态变化后 derivedStateOf 必须重算（说明普通 lambda 内的读取被追踪）", 2, computations)
    }

    @Test
    fun `plain lambda read alone does not make the caller a snapshot dependency`() {
        val state = mutableStateOf(0)
        // 仅仅是「调用普通 lambda 读取状态」不会给自己建立任何订阅：
        // 没有 derivedStateOf / 组合作用域包裹时，值变化后再次读取仍是新值但无回调。
        var observed = 0
        fun readPlain(): Int = state.value
        observed = readPlain()
        assertEquals(0, observed)

        state.value = 3
        observed = readPlain()
        assertEquals(3, observed)
    }
}
