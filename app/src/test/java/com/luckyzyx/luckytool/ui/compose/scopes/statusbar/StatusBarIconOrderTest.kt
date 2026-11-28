package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefPageBuilder
import com.luckyzyx.luckytool.ui.compose.PageRegistry
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * 状态栏图标排序的槽位表与排序回落逻辑。
 *
 * 槽位名/顺序取自宿主 ColorOS SystemUI 17 的 `config_statusBarIcons`（arrays.xml 49 项，
 * 每项经 strings.xml 解析成 `StatusBarIconList.Slot.mName`）；这里锁住数量、首尾、易错项
 * （`ethernet_slot` → `Ethernet` 带大写、`oem_satellite` → `satellite`、`satellite_slot` → `oplus_satellite`）
 * 与「每个槽位都有非空展示名」，避免以后加槽位时漏翻译。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatusBarIconOrderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `host slot table keeps 49 unique slots`() {
        val order = StatusBarIconSlots.defaultOrder
        assertEquals(49, order.size)
        assertEquals(order.size, order.toSet().size)
    }

    @Test
    fun `host slot table keeps the config_statusBarIcons sequence`() {
        assertEquals("logging", StatusBarIconSlots.defaultOrder.first())
        assertEquals("battery", StatusBarIconSlots.defaultOrder.last())
        assertEquals("Ethernet", StatusBarIconSlots.defaultOrder[29])
        assertEquals("satellite", StatusBarIconSlots.defaultOrder[34])
        assertEquals("airplane", StatusBarIconSlots.defaultOrder[46])
        assertEquals("oplus_satellite", StatusBarIconSlots.defaultOrder[47])
    }

    @Test
    fun `every host slot has a non blank label`() {
        StatusBarIconSlots.defaultOrder.forEach { slot ->
            val res = StatusBarIconSlots.labelRes(slot)
            assertNotNull("missing label resource for $slot", res)
            assertTrue("blank label for $slot", StatusBarIconSlots.label(context, slot).isNotBlank())
        }
    }

    @Test
    fun `unknown slot falls back to its raw name`() {
        assertNull(StatusBarIconSlots.labelRes("unknown_slot"))
        assertEquals("unknown_slot", StatusBarIconSlots.label(context, "unknown_slot"))
        assertTrue(!StatusBarIconSlots.isKnown("unknown_slot"))
    }

    @Test
    fun `blank preference keeps the clock and notification icons on the left`() {
        val (left, right) = zoneSplit("", "")
        // 通知图标固定左区，缺失时补到末尾（宿主默认：通知区排在左区内容之后）
        assertEquals(listOf("clock", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM), left)
        assertEquals(StatusBarIconSlots.defaultOrder, right)
    }

    /** 「重置」写回的偏好（左区只剩时钟、右区清空）必须等于全新安装时的状态 */
    @Test
    fun `reset writes the same zones as a fresh install`() {
        assertEquals(zoneSplit("", ""), zoneSplit(STATUS_BAR_ICON_ORDER_CLOCK_ITEM, ""))
    }

    /** 通知图标在所属区里的位置由保存顺序决定：可以排到时钟前面，也可以落在右区 */
    @Test
    fun `notification icons keep their saved place inside their zone`() {
        val (left, _) = zoneSplit("$STATUS_BAR_ICON_ORDER_NOTIFY_ITEM,clock,network_speed", "")
        assertEquals(
            listOf(STATUS_BAR_ICON_ORDER_NOTIFY_ITEM, "clock", "network_speed"),
            left,
        )
        // 写进右区的通知图标留在右区：它和时钟一样是锚点，不再被拉回左区
        val (pulledLeft, pulledRight) =
            zoneSplit("clock", "$STATUS_BAR_ICON_ORDER_NOTIFY_ITEM,battery")
        assertEquals(listOf("clock"), pulledLeft)
        assertEquals(STATUS_BAR_ICON_ORDER_NOTIFY_ITEM, pulledRight[0])
        assertEquals("battery", pulledRight[1])
    }

    @Test
    fun `saved zones are restored in place and never overlap`() {
        val (left, right) = zoneSplit("network_speed,clock,mobile", "battery,vowifi")
        assertEquals(
            listOf("network_speed", "clock", "mobile", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM),
            left,
        )
        assertEquals("battery", right[0])
        assertEquals("vowifi", right[1])
        // 49 个宿主槽位里只有 network_speed / mobile 被搬到左区；时钟/通知是伪条目，不占宿主槽位
        assertEquals(47, right.size)
        assertTrue("network_speed" !in right)
        assertTrue("mobile" !in right)
    }

    @Test
    fun `clock is prepended when the saved left zone lost it`() {
        val (left, _) = zoneSplit("network_speed", "")
        assertEquals(
            listOf("clock", "network_speed", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM),
            left,
        )
    }

    /** 时钟可以搬到右区：此时左区只剩通知图标，右区里时钟按保存顺序停在其它槽位之间 */
    @Test
    fun `clock can be moved to the right zone`() {
        val (left, right) = zoneSplit("", "clock,battery")
        assertEquals(listOf(STATUS_BAR_ICON_ORDER_NOTIFY_ITEM), left)
        assertEquals("clock", right[0])
        assertEquals("battery", right[1])
        // 49 个宿主槽位 + `clock` 伪条目（clock 本身不是宿主槽位）
        assertEquals(StatusBarIconSlots.defaultOrder.size + 1, right.size)
        assertTrue("clock" !in StatusBarIconSlots.defaultOrder)
    }

    /** 两边都写了时钟时以左区为准，右区里的时钟被清掉 */
    @Test
    fun `the clock prefers the left zone when both zones claim it`() {
        val (left, right) = zoneSplit("clock,network_speed", "clock,battery")
        assertEquals(
            listOf("clock", "network_speed", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM),
            left,
        )
        assertTrue("clock" !in right)
        assertEquals("battery", right[0])
    }

    @Test
    fun `unknown entries are dropped from both zones`() {
        val (left, right) = zoneSplit("ghost,clock", "ghost,battery")
        assertEquals(listOf("clock", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM), left)
        assertEquals("battery", right[0])
        assertTrue("ghost" !in left)
        assertTrue("ghost" !in right)
    }

    /** 松手落位：同区内换位、跨区时区界同步增减（时钟也能跨区，因此左区可以被搬空） */
    @Test
    fun `zone drop reorders inside a zone and carries across the border`() {
        // 左区内下移：clock 与 network_speed 互换，左区项数不变
        val inside = mutableListOf("clock", "network_speed", "battery", "wifi")
        assertEquals(2, zoneDrop(inside, 2, 0, 1))
        assertEquals(listOf("network_speed", "clock", "battery", "wifi"), inside)

        // 左区项搬到右区末尾：区界左移
        val toRight = mutableListOf("clock", "network_speed", "battery", "wifi")
        assertEquals(1, zoneDrop(toRight, 2, 1, 3))
        assertEquals(listOf("clock", "battery", "wifi", "network_speed"), toRight)

        // 右区项搬到左区最前：区界右移
        val toLeft = mutableListOf("clock", "battery", "wifi")
        assertEquals(2, zoneDrop(toLeft, 1, 1, 0))
        assertEquals(listOf("battery", "clock", "wifi"), toLeft)

        // 时钟搬到右区：左区被搬空，自动补回占位条目（占位不算进偏好，也不参与区头计数）
        val clockToRight = mutableListOf("clock", "battery", "wifi")
        assertEquals(1, zoneDrop(clockToRight, 1, 0, 1))
        assertEquals(
            listOf(STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER, "battery", "clock", "wifi"),
            clockToRight,
        )
    }

    /** 左区为空时占位条目占住下标 0：右区第一个位置照常可用，只有拖到占位行才放回左侧 */
    @Test
    fun `zone drop keeps the first right slot reachable when the left zone is empty`() {
        val marker = STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER

        // 拖到占位槽（to = 0）＝放进左侧：占位被顶替后消失
        val onto = mutableListOf(marker, "clock", "battery", "wifi")
        assertEquals(1, zoneDrop(onto, 1, 1, 0))
        assertEquals(listOf("clock", "battery", "wifi"), onto)

        // 拖到右区第一个位置（to = 1）：占位保留，右区照常排序
        val firstRight = mutableListOf(marker, "clock", "battery")
        assertEquals(1, zoneDrop(firstRight, 1, 2, 1))
        assertEquals(listOf(marker, "battery", "clock"), firstRight)

        // 右区第一项原地不动（from = to = 1）：无操作，不会误入左区
        val noOp = mutableListOf(marker, "clock", "battery")
        assertEquals(1, zoneDrop(noOp, 1, 1, 1))
        assertEquals(listOf(marker, "clock", "battery"), noOp)
    }

    /** 通知图标和时钟一样能跨区：拖到右区就落在右区，拖回左区还能与时钟互换 */
    @Test
    fun `notification icons can be carried across the border`() {
        val notify = STATUS_BAR_ICON_ORDER_NOTIFY_ITEM
        // 拖到右区（to = 3）：区界左移，通知图标离开左区
        val toRight = mutableListOf("clock", notify, "battery", "wifi")
        assertEquals(1, zoneDrop(toRight, 2, 1, 3))
        assertEquals(listOf("clock", "battery", "wifi", notify), toRight)

        // 左区内仍可与时钟互换位置
        val inside = mutableListOf("clock", notify, "battery")
        assertEquals(2, zoneDrop(inside, 2, 1, 0))
        assertEquals(listOf(notify, "clock", "battery"), inside)

        // 右区里的通知图标再拖回左区最前：区界右移
        val back = mutableListOf("clock", "battery", notify)
        assertEquals(2, zoneDrop(back, 1, 2, 0))
        assertEquals(listOf(notify, "clock", "battery"), back)
    }

    @Test
    fun `zone drop ignores a no-op or an invalid target`() {
        val items = mutableListOf("clock", "battery")
        // 原地不动：不改列表，也不触发写回
        assertEquals(1, zoneDrop(items, 1, 0, 0))
        assertEquals(listOf("clock", "battery"), items)
        // 下标越界
        assertEquals(1, zoneDrop(items, 1, 0, 5))
        assertEquals(1, zoneDrop(items, 1, 5, 0))
        assertEquals(listOf("clock", "battery"), items)
        // 空列表
        assertEquals(0, zoneDrop(mutableListOf(), 0, 0, 1))
    }

    /**
     * 排序列表独立成页：功能页只留入口行，排序页自己带开关与列表。
     * `com.android.systemui` 是排序页的唯一作用域，Robolectric 下未安装时入口行会被可达性判断拦掉，
     * 因此先装一个包信息（与 `PageReachabilityTest` 同一手法）。
     */
    @Test
    fun `the icon page links to the dedicated order page`() {
        Shadows.shadowOf(context.packageManager).installPackage(
            PackageInfo().apply { packageName = "com.android.systemui" },
        )

        val spec = PageRegistry[StatusBarIconOrderPage.PAGE_KEY]
        assertNotNull("排序页应已注册", spec)
        assertEquals("statusbar_icon_order", StatusBarIconOrderPage.PAGE_KEY)
        assertTrue(
            "排序页必须有可见条目（开关无条件发射），否则入口行会被误隐藏",
            PageRegistry.hasVisibleContent(context, spec!!),
        )

        val raw = context.getSharedPreferences(ModulePrefs, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        val builder = PrefPageBuilder(PrefState.ofProvider { raw })
        builder.context = context
        builder.beginBuild()
        StatusBarIconPage.spec.content(builder)
        val keys = builder.entries.mapNotNull { it.indexKey }.toSet()
        assertTrue(
            "图标页必须发射排序页入口行，实际索引键：$keys",
            "page:${StatusBarIconOrderPage.PAGE_KEY}" in keys,
        )
        // 排序的行不再长在功能页里：旧的排序行键与左区键都不该出现在图标页索引中
        assertTrue("排序列不应再留在图标页：$keys", STATUS_BAR_ICON_ORDER_RIGHT_KEY !in keys)
        assertTrue("排序列不应再留在图标页：$keys", STATUS_BAR_ICON_ORDER_LEFT_KEY !in keys)
    }

    /**
     * 拖到列表上下边缘自动滚动：远离边缘不滚，越靠边越快，方向由落在哪一侧决定。
     * `visualTop` 是被拖行的窗口 Y，`viewport` 是窗口高度，`edge` 是触发自动滚动的边缘宽度。
     */
    @Test
    fun `auto scroll only kicks in near the viewport edges`() {
        val viewport = 2000f
        val edge = 300f
        val max = 40f
        // 中间：不滚
        assertEquals(0f, autoScrollStep(1000f, viewport, edge, max), 0.001f)
        // 上边缘区内：向上滚（负），越靠上越快
        val upper = autoScrollStep(150f, viewport, edge, max)
        assertTrue("上边缘应向上滚，实际 $upper", upper < 0f)
        assertTrue("越靠上越快", autoScrollStep(90f, viewport, edge, max) < upper)
        // 下边缘区内：向下滚（正），越靠下越快
        val lower = autoScrollStep(1800f, viewport, edge, max)
        assertTrue("下边缘应向下滚，实际 $lower", lower > 0f)
        assertTrue("越靠下越快", autoScrollStep(1900f, viewport, edge, max) > lower)
    }

    @Test
    fun `auto scroll speed is clamped and invalid geometry never scrolls`() {
        // 贴住边缘 = 满速；越界（手指拖出屏幕）也只维持满速
        assertEquals(-40f, autoScrollStep(0f, 2000f, 300f, 40f), 0.001f)
        assertEquals(-40f, autoScrollStep(-500f, 2000f, 300f, 40f), 0.001f)
        assertEquals(40f, autoScrollStep(2000f, 2000f, 300f, 40f), 0.001f)
        // 边缘宽度/速度非法时不滚（窗口高度拿不到时调用点也会直接跳过）
        assertEquals(0f, autoScrollStep(10f, 2000f, 0f, 40f), 0.001f)
        assertEquals(0f, autoScrollStep(10f, 2000f, 300f, 0f), 0.001f)
    }

    @Test
    fun `zone display counts accurately reports item counts and ignores empty marker`() {
        val marker = STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER
        val default49 = StatusBarIconSlots.defaultOrder

        // 默认状态：左区时钟+通知（2项），右区49项
        val defaultList = listOf("clock", STATUS_BAR_ICON_ORDER_NOTIFY_ITEM) + default49
        assertEquals(2 to 49, zoneDisplayCounts(defaultList, 2))

        // 左区清空：占位条目占住下标 0，展示计数应为左 0、右 49，不可重复扣减占位符
        val leftEmptyList = listOf(marker) + default49
        assertEquals(0 to 49, zoneDisplayCounts(leftEmptyList, 1))

        // 时钟挪到右区且左区为空：右区共 50 项（49 + 时钟），展示计数为左 0、右 50
        val clockInRightList = listOf(marker, "clock") + default49
        assertEquals(0 to 50, zoneDisplayCounts(clockInRightList, 1))
    }

    @Test
    fun `extrapolateRowTops linearly populates coordinates for all undisturbed rows`() {
        val items = listOf("clock", "notifications", "wifi", "battery")
        val step = 100f

        // 目标行有实测坐标（如 "wifi" 在 300f），其余未位移的行应全部被推算补齐
        val measured = mapOf("wifi" to 300f)
        val (targetTop, extrapolated) = extrapolateRowTops(items, measured, "wifi", step)
        assertEquals(300f, targetTop, 0.001f)
        assertEquals(100f, extrapolated["clock"]!!, 0.001f)
        assertEquals(200f, extrapolated["notifications"]!!, 0.001f)
        assertEquals(300f, extrapolated["wifi"]!!, 0.001f)
        assertEquals(400f, extrapolated["battery"]!!, 0.001f)

        // 目标行未测量（如 "battery" 为 null），但列表中存在已有锚点（"clock" 在 100f）
        val anchorOnly = mapOf("clock" to 100f)
        val (targetEstimated, extrapolated2) = extrapolateRowTops(items, anchorOnly, "battery", step)
        assertEquals(400f, targetEstimated, 0.001f)
        assertEquals(400f, extrapolated2["battery"]!!, 0.001f)
        assertEquals(200f, extrapolated2["notifications"]!!, 0.001f)

        // 全部行均未测量：安全回退为 0f，不抛异常
        val (targetFallback, emptyResult) = extrapolateRowTops(items, emptyMap(), "ghost", step)
        assertEquals(0f, targetFallback, 0.001f)
        assertTrue(emptyResult.isEmpty())
    }
}
