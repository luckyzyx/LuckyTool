package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.FunctionPageData
import com.luckyzyx.luckytool.ui.compose.categoryBadges
import com.luckyzyx.luckytool.ui.compose.PageRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * 冻结功能页数据快照（[FunctionPageData]）的两条语义：
 * 1. 功能树行仍按 `treeOrder` 顺序、仍按「空索引 + 未声明 selfRendered 就隐藏」筛选；
 * 2. 搜索索引与功能树行共用同一轮构建，两者对「页是否入索引」的判断必须一致。
 *
 * 这套逻辑从 FunctionPage 的 produceState 搬到 FunctionPageData 后被 App 启动预取复用，
 * 任何一处判据被改动（例如改回 `index.isEmpty()` 即隐藏）都会让本测试失败。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FunctionPageDataTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun installScopeApps() {
        // 功能树里大部分页的作用域是这几个包，Robolectric 默认未安装
        listOf("com.android.settings", "com.android.systemui", "com.oplus.launcher").forEach { pkg ->
            Shadows.shadowOf(context.packageManager).installPackage(
                PackageInfo().apply { packageName = pkg },
            )
        }
    }

    @Test
    fun `rows keep treeOrder and category pages carry no packName`() {
        val rows = FunctionPageData.buildSnapshot(context).rows
        assertTrue("功能树不应为空", rows.isNotEmpty())
        val positions = rows.map { PageRegistry.treeOrder.indexOf(it.pageKey) }
        assertTrue("行必须是已注册的 treeOrder 页：$positions", positions.none { it < 0 })
        assertEquals("行顺序必须与 treeOrder 一致", positions.sorted(), positions)
        rows.forEach { row ->
            assertEquals(
                "分类页走彩色徽标（packName 为 null），应用页必须带包名：${row.pageKey}",
                row.pageKey in categoryBadges,
                row.packName == null,
            )
        }
    }

    @Test
    fun `app page summaries show function titles instead of the package name`() {
        // 摘要取该页前三个功能标题；16b0c07cf 曾让应用页摘要改显示包名，功能页整屏就只剩包名
        val appRows = FunctionPageData.buildSnapshot(context).rows
            .filter { it.packName != null && it.summary != null }
        assertTrue("应有带包名的应用页行可供校验", appRows.isNotEmpty())
        appRows.forEach { row ->
            assertNotEquals("应用页摘要不得是自身的包名：${row.pageKey}", row.packName, row.summary)
        }
    }

    @Test
    fun `an empty index hides a page unless it renders itself`() {
        FunctionPageData.buildSnapshot(context).rows.forEach { row ->
            val spec = PageRegistry[row.pageKey] ?: return@forEach
            if (spec.selfRendered) return@forEach
            assertTrue(
                "空索引且未声明 selfRendered 的页不得入树：${row.pageKey}",
                PageRegistry.buildIndex(context, spec).isNotEmpty(),
            )
        }
    }

    @Test
    fun `search entries cover the same pages as the tree rows`() {
        val snapshot = FunctionPageData.buildSnapshot(context)
        val indexedPageKeys = snapshot.searchEntries.map { it.pageKey }.toSet()
        snapshot.rows.forEach { row ->
            val spec = PageRegistry[row.pageKey] ?: return@forEach
            if (PageRegistry.buildIndex(context, spec).isEmpty()) return@forEach
            assertTrue(
                "有索引的功能树页必须同样出现在搜索索引里：${row.pageKey}",
                row.pageKey in indexedPageKeys,
            )
        }
        val registered = PageRegistry.all().map { it.pageKey }.toSet()
        assertTrue("搜索索引只能来自已注册页", registered.containsAll(indexedPageKeys))
    }

    @Test
    fun `two builds of unchanged data compare equal so recomposition can be skipped`() {
        // FunctionPageData 用结构相等决定要不要发布新快照（相等就不发布 → 不触发整棵树重组）
        assertEquals(FunctionPageData.buildSnapshot(context), FunctionPageData.buildSnapshot(context))
    }
}
