package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefPageBuilder
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结分段端点语义：组内收起的行不占高度，卡片端点（圆角/组间距）只按可见行算，
 * 所以夹在中间的条件行显隐不会把同组可见行切成两张卡（如 OtherPage 的「远程 ADB 调试」）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefSegmentationTest {

    private lateinit var context: Context
    private lateinit var raw: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        raw = context.getSharedPreferences("scope_segment_test", Context.MODE_PRIVATE)
        raw.edit().clear().commit()
    }

    private fun build(block: PrefPageBuilder.() -> Unit): PrefPageBuilder {
        val builder = PrefPageBuilder(PrefState.ofProvider { raw })
        builder.context = context
        builder.beginBuild()
        builder.block()
        builder.computeSegments()
        return builder
    }

    private fun PrefPageBuilder.cardRows() = entries.filter { it.groupable && it.visible }

    @Test
    fun `hidden row keeps its neighbours in the same card`() {
        val builder = build {
            group {
                click(title = "a", onClick = {})
                click(title = "b", itemKey = "hidden", visible = false, onClick = {})
                click(title = "c", onClick = {})
            }
        }

        val rows = builder.cardRows()
        assertEquals(listOf(0, 1), rows.map { it.segIndex })
        assertEquals(listOf(2, 2), rows.map { it.segCount })
        // 收起态沿用相邻可见行的位置，展开动画中途不会闪出单独的圆角卡片
        assertEquals(2, builder.entries.first { it.itemKey == "hidden" }.segCount)
    }

    @Test
    fun `leading hidden row leaves the first visible row as the card top`() {
        val builder = build {
            group {
                click(title = "a", itemKey = "hidden", visible = false, onClick = {})
                click(title = "b", onClick = {})
                click(title = "c", onClick = {})
            }
        }

        val rows = builder.cardRows()
        assertEquals(listOf(0, 1), rows.map { it.segIndex })
        assertEquals(listOf(2, 2), rows.map { it.segCount })
    }

    @Test
    fun `group markers still split cards`() {
        val builder = build {
            group {
                click(title = "a", onClick = {})
            }
            group {
                click(title = "b", onClick = {})
            }
        }

        val rows = builder.cardRows()
        assertEquals(listOf(0, 0), rows.map { it.segIndex })
        assertEquals(listOf(1, 1), rows.map { it.segCount })
    }
}