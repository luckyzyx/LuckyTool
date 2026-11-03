package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeListState
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/** 右区顺序偏好键（宿主 `statusIcons` 容器内的图标顺序，逗号分隔槽位名；空 = 宿主默认顺序） */
const val STATUS_BAR_ICON_ORDER_RIGHT_KEY = "statusbar_icon_order_right"

/** 左区顺序偏好键（被搬到左侧容器的图标顺序，逗号分隔槽位名；空 = 仅时钟在左侧） */
const val STATUS_BAR_ICON_ORDER_LEFT_KEY = "statusbar_icon_order_left"

/** 总开关：关闭时 hooker 不装载，改动需重启作用域（开关本身不做实时同步） */
const val STATUS_BAR_ICON_ORDER_ENABLED_KEY = "statusbar_icon_order_enabled"

/**
 * 时钟伪条目。
 *
 * 宿主的时钟不是 `StatusBarIconList` 槽位，而是左侧容器里的 `clock_for_fake` 子 view，
 * 因此列表用它占一行：它的邻居顺序同样由本功能的区域顺序表达，它本身也可以搬到右区。
 */
const val STATUS_BAR_ICON_ORDER_CLOCK_ITEM = "clock"

/**
 * 通知图标伪条目。
 *
 * 宿主的通知图标不是 `StatusBarIconList` 槽位，而是左侧容器里的 `status_bar_start_side_content_for_fake`
 * （内含 `notification_icon_area` 与实时活动 chips），因此列表用它占一行：和时钟一样是「锚点」，
 * 排在它前面的左区图标在它左边、后面在它右边；它固定留在左区（宿主右区是系统图标簇）。
 */
const val STATUS_BAR_ICON_ORDER_NOTIFY_ITEM = "notifications"

/**
 * 左区为空时的占位伪条目。
 *
 * 只存在于本页的本地列表里（从不写进偏好）：左区被搬空后它占住下标 0，让「左侧区域」始终
 * 可见可落位，同时右区的第一个位置（下标 1 起）照常可用。把真实图标拖到占位行上，
 * 占位条目就被顶替消失（图标变成左区第一项）。
 */
internal const val STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER = "__left_zone_empty__"

/**
 * 排序行高。
 *
 * 拖拽时用位移换算「跨过几行」，因此必须与行的实际高度一致（[OrderItemRowHeight]）。
 */
private val OrderItemHeight = 64.dp

/** 被拖行进入距列表可视区上下边缘这个范围时开始自动滚动 */
private val AutoScrollEdge = 150.dp

/** 自动滚动最快时每帧的位移（贴住边缘时） */
private val AutoScrollMaxSpeed = 12.dp

/**
 * 自动滚动的每帧位移（纯函数，便于单测）。
 *
 * [visualTop] 是此刻被拖行的窗口 Y，[viewportHeight] 是窗口高度，[edge] 是触发自动滚动的边缘宽度，
 * [maxSpeed] 是贴住边缘时的每帧位移。返回值正 = 向列表下方滚（内容上移），负 = 向上滚，0 = 不滚。
 */
internal fun autoScrollStep(
    visualTop: Float,
    viewportHeight: Float,
    edge: Float,
    maxSpeed: Float,
): Float {
    if (edge <= 0f || maxSpeed <= 0f) return 0f
    val overflowTop = edge - visualTop
    if (overflowTop > 0f) return -maxSpeed * (overflowTop / edge).coerceIn(0f, 1f)
    val overflowBottom = visualTop - (viewportHeight - edge)
    if (overflowBottom > 0f) return maxSpeed * (overflowBottom / edge).coerceIn(0f, 1f)
    return 0f
}

/**
 * 状态栏图标槽位表（宿主 `config_statusBarIcons` 的顺序与名字）。
 *
 * 数据来源：ColorOS SystemUI 17 的 `res/values/arrays.xml` + `res/values/strings.xml`
 * （`status_bar_xxx` 字符串的实际值就是 `StatusBarIconList.Slot.mName`，例如
 * `airplane_slot` → `airplane`、`oem_satellite` → `satellite`）。
 */
object StatusBarIconSlots {

    /** 宿主默认顺序；也是用户「重置」后的顺序 */
    val defaultOrder: List<String> = listOf(
        "logging",
        "hotspot",
        "data_saver",
        "cast",
        "connected_display",
        "vpn",
        "nfc",
        "headset",
        "high_performance",
        "alarm_clock",
        "volume",
        "bluetooth",
        "sos",
        "location",
        "microphone",
        "camera",
        "rotate",
        "ime",
        "sync_failing",
        "sync_active",
        "tty",
        "speakerphone",
        "cdma_eri",
        "data_connection",
        "phone_evdo_signal",
        "phone_signal",
        "secure",
        "managed_profile",
        "mute",
        "Ethernet",
        "network_speed",
        "vowifi",
        "volte",
        "zen",
        "satellite",
        "scenario_assistant",
        "stacked_mobile",
        "mobile",
        "nosim_all",
        "nosim_one",
        "nosim_two",
        "wifi",
        "dynamic_info",
        "secure_pay",
        "privacy_call",
        "stealth_mode",
        "airplane",
        "oplus_satellite",
        "battery",
    )

    /** 槽位的展示名资源；未收录的槽位（未来版本新增）返回 null，调用点回落成 slot 名 */
    @StringRes
    fun labelRes(slot: String): Int? = when (slot) {
        STATUS_BAR_ICON_ORDER_CLOCK_ITEM -> R.string.statusbar_icon_order_clock
        STATUS_BAR_ICON_ORDER_NOTIFY_ITEM -> R.string.statusbar_icon_order_notifications
        "logging" -> R.string.status_bar_slot_logging
        "hotspot" -> R.string.status_bar_slot_hotspot
        "data_saver" -> R.string.status_bar_slot_data_saver
        "cast" -> R.string.status_bar_slot_cast
        "connected_display" -> R.string.status_bar_slot_connected_display
        "vpn" -> R.string.status_bar_slot_vpn
        "nfc" -> R.string.status_bar_slot_nfc
        "headset" -> R.string.status_bar_slot_headset
        "high_performance" -> R.string.status_bar_slot_high_performance
        "alarm_clock" -> R.string.status_bar_slot_alarm_clock
        "volume" -> R.string.status_bar_slot_volume
        "bluetooth" -> R.string.status_bar_slot_bluetooth
        "sos" -> R.string.status_bar_slot_sos
        "location" -> R.string.status_bar_slot_location
        "microphone" -> R.string.status_bar_slot_microphone
        "camera" -> R.string.status_bar_slot_camera
        "rotate" -> R.string.status_bar_slot_rotate
        "ime" -> R.string.status_bar_slot_ime
        "sync_failing" -> R.string.status_bar_slot_sync_failing
        "sync_active" -> R.string.status_bar_slot_sync_active
        "tty" -> R.string.status_bar_slot_tty
        "speakerphone" -> R.string.status_bar_slot_speakerphone
        "cdma_eri" -> R.string.status_bar_slot_cdma_eri
        "data_connection" -> R.string.status_bar_slot_data_connection
        "phone_evdo_signal" -> R.string.status_bar_slot_phone_evdo_signal
        "phone_signal" -> R.string.status_bar_slot_phone_signal
        "secure" -> R.string.status_bar_slot_secure
        "managed_profile" -> R.string.status_bar_slot_managed_profile
        "mute" -> R.string.status_bar_slot_mute
        "Ethernet" -> R.string.status_bar_slot_ethernet
        "network_speed" -> R.string.status_bar_slot_network_speed
        "vowifi" -> R.string.status_bar_slot_vowifi
        "volte" -> R.string.status_bar_slot_volte
        "zen" -> R.string.status_bar_slot_zen
        "satellite" -> R.string.status_bar_slot_satellite
        "scenario_assistant" -> R.string.status_bar_slot_scenario_assistant
        "stacked_mobile" -> R.string.status_bar_slot_stacked_mobile
        "mobile" -> R.string.status_bar_slot_mobile
        "nosim_all" -> R.string.status_bar_slot_nosim_all
        "nosim_one" -> R.string.status_bar_slot_nosim_one
        "nosim_two" -> R.string.status_bar_slot_nosim_two
        "wifi" -> R.string.status_bar_slot_wifi
        "dynamic_info" -> R.string.status_bar_slot_dynamic_info
        "secure_pay" -> R.string.status_bar_slot_secure_pay
        "privacy_call" -> R.string.status_bar_slot_privacy_call
        "stealth_mode" -> R.string.status_bar_slot_stealth_mode
        "airplane" -> R.string.status_bar_slot_airplane
        "oplus_satellite" -> R.string.status_bar_slot_oplus_satellite
        "battery" -> R.string.status_bar_slot_battery
        else -> null
    }

    fun label(context: Context, slot: String): String =
        labelRes(slot)?.let { context.getString(it) } ?: slot

    /** 是否被本版本收录（未收录的槽位不参与排序，保持宿主默认顺序） */
    fun isKnown(slot: String): Boolean = slot in defaultOrder
}

/**
 * 偏好值 → (左区列表, 右区列表)。
 *
 * - 非锚点槽位：保存顺序里的合法项，去重；未写进任何一区的已收录槽位按宿主默认顺序补进右区
 *   （本版本新增的槽位因此默认仍排在右侧尾部）。
 * - 锚点（[STATUS_BAR_ICON_ORDER_CLOCK_ITEM] / [STATUS_BAR_ICON_ORDER_NOTIFY_ITEM]）可以跨区：
 *   写进哪一区的顺序就落在哪一区（两边都写了以左区为准）；两边都没写时按宿主默认落在左区，
 *   时钟补在左区最前、通知图标补在左区最后。锚点在原区里的相对位置会保留。
 * - 未收录的槽位名一律丢弃（宿主里本来就不存在，或未来版本才有）。
 */
internal fun zoneSplit(leftRaw: String, rightRaw: String): Pair<List<String>, List<String>> {
    val known = StatusBarIconSlots.defaultOrder
    val clock = STATUS_BAR_ICON_ORDER_CLOCK_ITEM
    val notify = STATUS_BAR_ICON_ORDER_NOTIFY_ITEM
    fun parse(raw: String) = raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() && (it == clock || it == notify || it in known) }
        .distinct()

    val leftSaved = parse(leftRaw)
    val rightSaved = parse(rightRaw)
    val left = leftSaved.filterNot { it == clock || it == notify }.toMutableList()
    val right = rightSaved.filterNot { it == clock || it == notify }.toMutableList()
    for (slot in known) if (slot !in left && slot !in right) right += slot

    for (anchor in listOf(clock, notify)) {
        val inLeft = anchor in leftSaved
        val inRight = anchor in rightSaved
        val toLeft = if (inLeft || inRight) inLeft else true
        val source = if (toLeft) leftSaved else rightSaved
        val target = if (toLeft) left else right
        val at = when {
            // 保留它原本在所属区里的相对位置
            anchor in source ->
                source.take(source.indexOf(anchor)).count { it in target }
                    .coerceIn(0, target.size)
            // 补位：通知图标补在左区最后（宿主默认），其余补在该区最前
            toLeft && anchor == notify -> target.size
            else -> 0
        }
        target.add(at, anchor)
    }
    return left to right
}

/**
 * 松手落位：把 [items] 里 [from] 位置的项移到 [to]（两者都是移动前的下标），返回新的左区项数。
 *
 * 用「先摘后插」，所以 [to] 也用移动前的下标：`[A,B,C,D]` 把 1 移到 3 得到 `[A,C,D,B]`。
 * 跨区（左区项落到右区下标，或反之）时区界随之增减；时钟与通知图标都能跨区，因此左区可以被搬空。
 *
 * 左区为空时列表由 [STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER] 占住下标 0（此时 [leftCount] = 1）：
 * - 拖到占位槽（[to] = 0）= 放进左侧：图标顶替占位条目（占位被顶出下标 0 后移除）；
 * - 拖到 [to] ≥ 1 = 右区排序，第一个右区位置（下标 1）照常可用；
 * - 左区被搬空时自动补回占位条目，保证左区在列表里始终占一行。
 *
 * 下标越界或原地不动时不改动 [items]，原样返回 [leftCount]。
 */
internal fun zoneDrop(items: MutableList<String>, leftCount: Int, from: Int, to: Int): Int {
    if (from !in items.indices || to !in items.indices || from == to) return leftCount
    val moved = items.removeAt(from)
    items.add(to, moved)
    var next = when (leftCount) {
        in (from + 1)..to -> leftCount - 1
        in (to + 1)..from -> leftCount + 1
        else -> leftCount
    }
    // 占位条目被顶出下标 0 = 左区已有真实图标，占位不再需要
    val markerIndex = items.indexOf(STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER)
    if (markerIndex > 0) {
        items.removeAt(markerIndex)
        next = (next - 1).coerceAtLeast(0)
    }
    // 左区被搬空：补回占位条目，让左区在列表里始终占一行
    if (next == 0) {
        items.add(0, STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER)
        next = 1
    }
    return next
}

/**
 * 两区域排序列表（左区 / 右区，各自带序号）。
 *
 * 拖动方式两种，走同一套状态机：按住右侧手柄立即拖动（48dp 热区），或长按任意一行拖动。
 *
 * 与手感有关的两条硬约束（也是早期版本「一拖就丢状态」的原因）：
 * - **拖动期间不改动列表结构**：只有被拖行的 `translationY` 跟随手指（绘制阶段读取，
 *   逐帧拖动不触发重组）。按行交换列表时，行在左右两个 `forEach` 之间
 *   搬家时组合节点被销毁重建，手势检测器随之取消——表现就是拖到半路状态丢失。
 * - **落位用实测位置而不是固定行距**：[targetFromPosition] 按「手指落在哪些行的中心之上」计数。
 *   区头/分隔线本来就不占行数，按行距估算会在跨区时把区头高度算进行程，出现
 *   「视觉上已经拖到左区、松手却还在右区」的错位；实测位置则不受区头高度影响。
 *
 * 拖动期间其余行不再让位（之前的 ±一行偏移会越过区头/列表两端造成布局溢出），
 * 改为**高亮落位槽**：被拖行浮在列表上方，落位行以底色标出，松手才真正换位。
 */
@Composable
fun StatusBarIconOrderZones(
    leftRaw: String,
    rightRaw: String,
    onApply: (left: String, right: String) -> Unit,
) {
    val context = LocalContext.current
    val session = remember(leftRaw, rightRaw) { Any() }
    val initial = remember(session) { zoneSplit(leftRaw, rightRaw) }
    val items = remember(session) {
        mutableStateListOf<String>().apply {
            // 左区为空时用占位条目占住下标 0：左区始终可见可落位，右区第一个位置（下标 1）照常可用
            if (initial.first.isEmpty()) add(STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER)
            addAll(initial.first)
            addAll(initial.second)
        }
    }
    var leftCount by remember(session) { mutableIntStateOf(if (initial.first.isEmpty()) 1 else initial.first.size) }
    var draggingItem by remember(session) { mutableStateOf<String?>(null) }
    var dragFrom by remember(session) { mutableIntStateOf(-1) }
    val dragDelta = remember(session) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val step = with(density) { OrderItemHeight.toPx() }

    // 页面列表状态（拖到边缘时要主动滚动它）与自动滚动的几何参数
    val pageListState = LocalScopeListState.current
    val viewportHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val autoScrollEdge = with(density) { AutoScrollEdge.toPx() }
    val autoScrollSpeed = with(density) { AutoScrollMaxSpeed.toPx() }

    // 每行的窗口 Y（普通 map，不参与快照，避免逐帧重组）
    val rowTops = remember(session) { HashMap<String, Float>() }

    /** 拖动开始时全部行的窗口 Y 快照；页面滚动期间统一减去 [scrolledBy] 即得当前位置 */
    val startTops = remember(session) { HashMap<String, Float>() }

    /** 拖动开始时被拖行的窗口 Y */
    val dragRowTop = remember(session) { FloatArray(1) }

    /** 自动滚动累计量（正 = 内容上移）：要补进被拖行的位移，行才不会在滚动时脱离手指 */
    val scrolledBy = remember(session) { FloatArray(1) }

    /**
     * 落位下标：数「手指 Y 之下有多少行的中心」，即插入位置。
     *
     * 行在拖动期间不发生布局位移（除被拖行外没有视觉偏移），页面滚动让所有行统一移动
     * [scrolledBy]，所以 `startTops[item] - scrolledBy` 就是该行此刻的窗口 Y。
     */
    fun targetFromPosition(fingerY: Float): Int {
        var count = 0
        for (index in items.indices) {
            // 快照在拖动开始时由 onGloballyPositioned 填满；缺失的行跳过（宁可少算也不给错位置）
            val top = (startTops[items[index]] ?: continue) - scrolledBy[0]
            if (fingerY > top + step / 2f) count++
        }
        return count.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    }

    /** 当前落位下标（只在数值变化时通知，行的高亮因此只在换槽那一刻重组） */
    val dragTarget by remember(session) {
        derivedStateOf {
            val item = draggingItem
            if (item == null) -1
            else targetFromPosition(dragRowTop[0] + (dragDelta.floatValue - scrolledBy[0]))
        }
    }

    /** 行的绘制偏移：只有被拖行跟随手指，其余行保持原位（落位槽由高亮标出） */
    fun offsetFor(index: Int): Float {
        val item = draggingItem ?: return 0f
        val from = dragFrom
        if (from !in items.indices) return 0f
        // 位移里含自动滚动的补偿量，不再夹取：夹住会让行在端点/长距离滚动后「拖不动、只能原路拖回」
        return if (index == from) dragDelta.floatValue else 0f
    }

    // 区头计数：显示口径不含占位条目，拖动期间实时预览落位结果，只在跨区那一刻才变化
    val liveCounts by remember(session) {
        derivedStateOf {
            val from = dragFrom
            val item = draggingItem
            val hasMarker = items.firstOrNull() == STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER
            // 右区实际项数（占位条目不计入任何一区）
            val rightSize = items.size - leftCount - (if (hasMarker) 1 else 0)
            if (item == null || from !in items.indices) {
                (if (hasMarker) 0 else leftCount) to rightSize
            } else {
                val to = dragTarget
                val left = when {
                    // 左区只有占位：落在占位槽（to=0）则左区变为 1，否则仍为空
                    hasMarker -> if (to == 0) 1 else 0
                    leftCount in (from + 1)..to -> leftCount - 1
                    leftCount in (to + 1)..from -> leftCount + 1
                    else -> leftCount
                }
                // 被拖项从右区落入左区（含占位槽）时右区少一项
                val leavesRight = leftCount in (to + 1)..from
                left to (rightSize - if (leavesRight) 1 else 0)
            }
        }
    }

    fun resetDrag() {
        draggingItem = null
        dragFrom = -1
        dragDelta.floatValue = 0f
        scrolledBy[0] = 0f
    }

    /** 松手落位：换位（跨区时区界同步增减），然后写回偏好（占位条目不落偏好），宿主侧实时生效 */
    fun commitDrag() {
        val from = dragFrom
        val to = targetFromPosition(dragRowTop[0] + (dragDelta.floatValue - scrolledBy[0]))
        resetDrag()
        if (from !in items.indices || to !in items.indices || from == to) return
        leftCount = zoneDrop(items, leftCount, from, to)
        val leftItems =
            items.take(leftCount).filterNot { it == STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER }
        onApply(leftItems.joinToString(","), items.drop(leftCount).joinToString(","))
    }

    // afterLongPress = true：长按整行开始拖动；false：按住手柄立即拖动。
    // pointerInput 的 key 与位置无关，拖动期间即使发生重组也不会重启手势检测器。
    val dragModifierFor: (String, Boolean) -> Modifier = { item, afterLongPress ->
        Modifier
            // 记住每一行的窗口 Y：拖动开始时快照全部行位置，落位下标按实测位置计算
            .onGloballyPositioned { rowTops[item] = it.positionInWindow().y }
            .pointerInput(session, afterLongPress) {
                val onStart = {
                    draggingItem = item
                    dragFrom = items.indexOf(item)
                    dragDelta.floatValue = 0f
                    dragRowTop[0] = rowTops[item] ?: 0f
                    scrolledBy[0] = 0f
                    startTops.clear()
                    startTops.putAll(rowTops)
                }
                val onDrag: (PointerInputChange, Offset) -> Unit = { change, amount ->
                    change.consume()
                    dragDelta.floatValue += amount.y
                }
                if (afterLongPress) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onStart() },
                        onDragCancel = { resetDrag() },
                        onDragEnd = { commitDrag() },
                        onDrag = onDrag,
                    )
                } else {
                    detectDragGestures(
                        onDragStart = { onStart() },
                        onDragCancel = { resetDrag() },
                        onDragEnd = { commitDrag() },
                        onDrag = onDrag,
                    )
                }
            }
    }

    // 拖到列表上下边缘自动滚动：指针停在边缘区就按「离边缘多近」逐帧滚页面，越靠边越快。
    // 页面滚动 delta 后内容整体上移 delta，被拖行必须补同样位移（见 scrolledBy），否则它会脱离手指；
    // 位移同样参与落位下标计算，因此长距离搬运在一次手势里就能完成。
    LaunchedEffect(draggingItem, session) {
        val listState = pageListState ?: return@LaunchedEffect
        if (draggingItem == null || viewportHeight <= 0f) return@LaunchedEffect
        while (isActive && draggingItem != null) {
            withFrameNanos { }
            // 窗口 Y = 拖动开始时的行位置 + 手指位移（自动滚动的补偿量在两者里抵消）
            val visualTop = dragRowTop[0] + (dragDelta.floatValue - scrolledBy[0])
            val dy = autoScrollStep(visualTop, viewportHeight, autoScrollEdge, autoScrollSpeed)
            if (dy == 0f) continue
            val consumed = listState.scrollBy(dy)
            if (consumed != 0f) {
                dragDelta.floatValue += consumed
                scrolledBy[0] += consumed
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OrderResetRow(
            onReset = {
                // 重置 = 全新安装状态：左区「时钟 + 通知图标」、右区宿主默认顺序。
                // 必须用 zoneSplit 推出来的列表（含通知图标伪条目），不能只塞时钟——
                // 只塞时钟会让本地列表缺掉「通知图标」那一行，而写回的左区键又和旧值相同时
                // 不会触发 session 重建，那一行就再也回不来了。
                val fresh = zoneSplit(STATUS_BAR_ICON_ORDER_CLOCK_ITEM, "")
                items.clear()
                items.addAll(fresh.first)
                items.addAll(fresh.second)
                leftCount = fresh.first.size
                onApply(fresh.first.joinToString(","), "")
            },
        )
        ZoneDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ZoneHeader(
            text = stringResource(R.string.statusbar_icon_order_zone_left, liveCounts.first),
            topPadding = 12.dp,
        )
        items.take(leftCount).forEachIndexed { index, item ->
            if (item == STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER) {
                // 左区被搬空时的占位行：占住下标 0，让左区始终可见、右区第一位置（下标 1）照常可用
                LeftZoneEmptyRow(
                    modifier = Modifier.onGloballyPositioned {
                        rowTops[STATUS_BAR_ICON_ORDER_LEFT_EMPTY_MARKER] = it.positionInWindow().y
                    },
                    highlighted = draggingItem != null && dragTarget == index,
                )
            } else {
                key(item) {
                    OrderItemRow(
                        number = index + 1,
                        label = StatusBarIconSlots.label(context, item),
                        slot = item,
                        dragging = draggingItem == item,
                        highlighted = draggingItem != item && dragTarget == index,
                        rowOffset = { offsetFor(index) },
                        rowDragModifier = dragModifierFor(item, true),
                        handleDragModifier = dragModifierFor(item, false),
                    )
                }
            }
        }
        ZoneDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ZoneHeader(
            text = stringResource(R.string.statusbar_icon_order_zone_right, liveCounts.second),
            topPadding = 8.dp,
        )
        items.drop(leftCount).forEachIndexed { index, item ->
            key(item) {
                OrderItemRow(
                    number = index + 1,
                    label = StatusBarIconSlots.label(context, item),
                    slot = item,
                    dragging = draggingItem == item,
                    highlighted = draggingItem != item && dragTarget == leftCount + index,
                    rowOffset = { offsetFor(leftCount + index) },
                    rowDragModifier = dragModifierFor(item, true),
                    handleDragModifier = dragModifierFor(item, false),
                )
            }
        }
    }
}

/** 重置行：两个区域一起恢复宿主原生顺序 */
@Composable
private fun OrderResetRow(onReset: () -> Unit) {
    val title = stringResource(R.string.statusbar_icon_order_reset)
    val summary = stringResource(R.string.statusbar_icon_order_reset_summary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(OrderItemHeight)
            .clickable(onClick = onReset)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> {
                Column(modifier = Modifier.weight(1f)) {
                    MiuixText(text = title, style = MiuixTheme.textStyles.main)
                    MiuixText(
                        text = summary,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
                MiuixIcon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }

            UiMode.Material -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 区头：「左侧区域(共 N 项)」（N 随拖拽实时变化） */
@Composable
private fun ZoneHeader(text: String, topPadding: Dp) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 4.dp)
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixText(
            text = text,
            modifier = modifier,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 区界分隔线（列表内的分组线，两侧留 16dp 内缩） */
@Composable
private fun ZoneDivider(modifier: Modifier = Modifier) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixHorizontalDivider(modifier)
    } else {
        HorizontalDivider(modifier)
    }
}

/**
 * 左区被搬空时的占位行：让「左侧区域」始终可见，并充当「把图标拖回左侧」的落位区。
 *
 * [highlighted] 由拖动状态传入：落位下标为 0 时高亮，与普通行的落位槽高亮同色。
 * [modifier] 由调用点传入（用于把占位行的窗口位置登记进 rowTops，落位计算按实测位置走）。
 */
@Composable
private fun LeftZoneEmptyRow(modifier: Modifier, highlighted: Boolean) {
    val text = stringResource(R.string.statusbar_icon_order_left_empty)
    when (LocalUiMode.current) {
        UiMode.Miuix -> Row(
            modifier = Modifier
                .then(modifier)
                .fillMaxWidth()
                .height(OrderItemHeight)
                .then(
                    if (highlighted) {
                        Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixText(
                text = text,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
        }

        UiMode.Material -> Row(
            modifier = Modifier
                .then(modifier)
                .fillMaxWidth()
                .height(OrderItemHeight)
                .then(
                    if (highlighted) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 一行条目：序号 + 展示名 + 槽位名 + 右侧拖拽手柄；高度固定，供拖拽换算行数。
 *
 * [rowOffset] 是这一行相对静止位置的绘制偏移（被拖行跟随手指，其余行让位），只在绘制阶段读取
 * （`graphicsLayer` 的 lambda），因此拖动期间不会逐帧重组。
 * [rowDragModifier] 挂在整行（长按拖动），[handleDragModifier] 挂在手柄 48dp 热区（立即拖动）。
 */
@Composable
private fun OrderItemRow(
    number: Int,
    label: String,
    slot: String,
    dragging: Boolean,
    highlighted: Boolean,
    rowOffset: () -> Float,
    rowDragModifier: Modifier,
    handleDragModifier: Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixOrderItemRow(
            number,
            label,
            slot,
            dragging,
            highlighted,
            rowOffset,
            rowDragModifier,
            handleDragModifier
        )

        UiMode.Material -> MaterialOrderItemRow(
            number,
            label,
            slot,
            dragging,
            highlighted,
            rowOffset,
            rowDragModifier,
            handleDragModifier
        )
    }
}

@Composable
private fun MiuixOrderItemRow(
    number: Int,
    label: String,
    slot: String,
    dragging: Boolean,
    highlighted: Boolean,
    rowOffset: () -> Float,
    rowDragModifier: Modifier,
    handleDragModifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(OrderItemHeight)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = rowOffset() }
            .then(
                when {
                    dragging -> Modifier.background(MiuixTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    highlighted -> Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f))
                    else -> Modifier
                }
            )
            .then(rowDragModifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = number.toString(),
            modifier = Modifier.width(28.dp),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.body2,
        )
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(text = label, style = MiuixTheme.textStyles.main)
            MiuixText(
                text = slot,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(handleDragModifier),
            contentAlignment = Alignment.Center,
        ) {
            MiuixIcon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}

@Composable
private fun MaterialOrderItemRow(
    number: Int,
    label: String,
    slot: String,
    dragging: Boolean,
    highlighted: Boolean,
    rowOffset: () -> Float,
    rowDragModifier: Modifier,
    handleDragModifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(OrderItemHeight)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = rowOffset() }
            .then(
                when {
                    dragging -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    highlighted -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    else -> Modifier
                }
            )
            .then(rowDragModifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = slot,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(handleDragModifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
