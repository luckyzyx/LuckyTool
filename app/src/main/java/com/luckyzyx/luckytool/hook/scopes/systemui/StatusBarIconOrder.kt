package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.resolver.MethodResolver
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 状态栏图标位置排序（左 / 右两区域，区域内可排序、可跨区搬运）。
 *
 * ## 顺序（宿主唯一来源）
 * `StatusBarIconList.mSlots`（`ArrayList<Slot>`，下标即顺序）：
 * - `StatusBarIconList(String[])` 构造器按 `config_statusBarIcons` 逐项 `mSlots.add(new Slot(name))`；
 * - `mViewOnlySlots` 是 `mSlots` 的 `unmodifiableList` 视图，**原地重排 `mSlots` 立即对 `getSlots()` 生效**；
 * - 图标 view 的插入下标由 `getViewIndex(tag, slot)` 从 `mSlots` 顺序推出来，所以重排 `mSlots` 即改变顺序。
 *
 * 本模块把 `mSlots` 排成 `[左区槽位…] + [右区槽位…] + [未收录槽位（宿主默认顺序）]`，
 * 于是「虚拟顺序」既是宿主索引空间，也是每个容器内部的显示顺序：
 * 左区容器里只会有左区图标（按左区顺序），右区容器里只会有右区图标（按右区顺序）。
 *
 * ## 左 / 右分区（宿主没有原生机制，只能自己搬 view）
 * 宿主 `IconManager.mGroup` 的子 view 必须与「当前存在的图标的全局索引空间」严格一一对应：
 * `onSetIcon(i)` / `onRemoveIcon(i)` 都直接 `mGroup.getChildAt(i)` / `removeViewAt(i)`，
 * 因此**把 view 搬出 mGroup 会让索引错位**（取到错误的 view 甚至 IndexOutOfBoundsException）。
 *
 * 解法：hook `DarkIconManager` 构造器，把第一个参数（真实的 `statusIcons` 容器）换成
 * [ZoneIconGroup]——一个只记录虚拟子 view 列表、并把每个图标按槽位转发到左/右真实容器的
 * `LinearLayout`。它对宿主而言仍是 `LinearLayout`（`createDemoStatusIcons` 会强转），
 * `getChildAt(i)` 永远返回正确的图标 view（不论其真实父容器在哪），`addView` 仍按宿主给的全局下标入列。
 * 虚拟容器本身不在布局树里，不参与测量。
 *
 * ## 宿主布局契约（动这里之前先看，全是踩过的坑）
 *
 * | 容器 | 类型 / 布局参数 | 约束 |
 * | --- | --- | --- |
 * | `status_bar_start_side_except_heads_up` | StartSideExceptHeadsUpLayout（LinearLayout），子 view 全 `wrap_content`、**无 `layout_weight`** | 插段容器安全；`onLayout` 只做 pivot + `super`（StartSideExceptHeadsUpLayout.java:136）→ 子 view 顺序即布局顺序 |
 * | `system_icons` | LinearLayout（`wrap_content`），内含 `statusIcons`（**`0dp` + `layout_weight = 1`**）+ 电池 include | **不能随便插容器**：`statusIcons` 靠权重吃剩余宽度，旁边多一个容器就抢它的宽度 |
 * | `statusIcons` | StatusIconContainer（AlphaOptimizedLinearLayout） | 子 view 必须是 `StatusIconDisplayable` 且带 `R.id.status_bar_view_state_tag`（StatusIconContainer.java:440 / :442 强转 + 无 null 检查）→ **绝不能把我们的段容器放进去** |
 *
 * `StatusIconContainer` 的定位方式（StatusIconContainer.java:276-286 / :423-494）：`onLayout` 把每个子 view
 * 都 layout 到 `x = 0`，再由 `updateStates()` 按「**子 view 顺序 + 自己的宽度**」重算每个图标的
 * `translationX`（:438-451，从右往左累加）→ ①子 view 顺序就是布局顺序，原地重排是安全的；
 * ②容器宽度一变所有图标位置跟着变（宽度被抢成 0 时全叠在一处）；③图标一旦离开 `statusIcons`，
 * 就没有人再更新它的 state，那个 `translationX` 会留着（看上去就是「图标全叠在右上角」），
 * 必须自己清掉 → [resetHostIconTransform]。
 *
 * 生命周期：宿主不少 view 挂在 ColorOS `SysuiLifecycle` 的 attach/detach 回调上，普通 `removeView`
 * 会把它们打成 DESTROYED（再 attach 抛 `State is 'DESTROYED' and cannot be moved to RESUMED`）
 * → 搬 view 一律走 [detachQuietly] / [moveChildInPlace]。
 *
 * ## 锚点（时钟 / 通知图标）
 * 时钟（`clock_for_fake`）与通知图标组（`status_bar_start_side_content_for_fake`）都不是槽位，
 * 而是宿主布局里的独立 view，顺序里的 `clock` / `notifications` 伪条目代表它们。
 * 锚点把**自己所在的那个区**切成「锚点数 + 1」段（[leftSegGroups] / [rightSegGroups]）：
 * - 左区：`[段0][锚点0][段1][锚点1][段2]`，插在锚点原本在左侧容器里的位置；
 * - 右区：`system_icons` 内、宿主 `statusIcons` 之前 → `[段0][锚点0][段1][锚点1][statusIcons][电池]`；
 * - 两个锚点都能跨区：写进哪一区的顺序，就摆到哪一区的对应段之间（视图拿不到时不参与分段）。
 *
 * 运行期改动由 dataChannel 推送，重排后调用 `refreshStatusBarIconList()` 重建图标 view 立即生效。
 */
@SuppressLint("StaticFieldLeak")
@Obfuscate
object StatusBarIconOrder : YukiBaseHooker() {

    /** 右区顺序（逗号分隔的槽位名；空 = 宿主默认顺序） */
    private const val RIGHT_KEY = "statusbar_icon_order_right"

    /** 左区顺序（逗号分隔的槽位名，可含 `clock` 伪条目；空 = 仅时钟在左侧） */
    private const val LEFT_KEY = "statusbar_icon_order_left"

    /** 总开关：关闭时不装载任何 hook（与页面上的开关同名） */
    private const val ENABLE_KEY = "statusbar_icon_order_enabled"

    /** 时钟伪条目：代表 `clock_for_fake`，可以在左区，也可以搬到右区 */
    private const val CLOCK_ITEM = "clock"

    /**
     * 通知图标伪条目：代表左侧容器里的 `status_bar_start_side_content_for_fake`
     * （内含 `notification_icon_area` 通知图标 + 实时活动 chips，整组一起移动）。
     *
     * 它和 [CLOCK_ITEM] 一样是「锚点」：排在它前面 / 后面的图标各进一段容器，因此它能停在所属区域的任意位置，
     * 并且**两个锚点都能跨区**——写进左区顺序就摆在左侧容器里，写进右区顺序就摆进 `system_icons`。
     */
    private const val NOTIFY_ITEM = "notifications"

    /** 锚点伪条目；id → 宿主 view 的 id 名。归属哪个区只看它出现在哪一区的顺序里 */
    private val anchorViewIds = linkedMapOf(
        CLOCK_ITEM to "clock_for_fake",
        NOTIFY_ITEM to "status_bar_start_side_content_for_fake",
    )

    /** ColorOS 的状态栏根布局（StatusBarRoot 里 inflate `R.layout.status_bar` 的那个 View） */
    private const val STATUS_BAR_VIEW_CLASS =
        "com.android.systemui.statusbar.phone.PhoneStatusBarView"

    /** A15+ 把 StatusBarIconList/StatusBarIconControllerImpl 挪到 phone.ui 包，两代名字都兼容 */
    private val iconListClassNames = arrayOf(
        "com.android.systemui.statusbar.phone.ui.StatusBarIconList",
        "com.android.systemui.statusbar.phone.StatusBarIconList",
    )
    private val iconControllerClassNames = arrayOf(
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
        "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
    )

    /** 持有 `mGroup` 的图标管理器（状态栏用的是 DarkIconManager；shade / keyguard 另有实例） */
    private val iconManagerClassNames = arrayOf(
        "com.android.systemui.statusbar.phone.ui.DarkIconManager",
        "com.android.systemui.statusbar.phone.DarkIconManager",
    )

    /** 左区顺序（含 `clock` 时表示时钟也在左区） */
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var leftOrder: List<String> = listOf(CLOCK_ITEM)

    /** 右区顺序（含 `clock` 时表示时钟被搬到右区） */
    @Volatile
    private var rightOrder: List<String> = emptyList()

    /** 构造时捕获的宿主默认顺序：未收录槽位的回落顺序 */
    @Volatile
    private var defaultOrder: List<String> = emptyList()

    // StatusBarIconList / StatusBarIconControllerImpl / PhoneStatusBarView 与 SystemUI 进程同生命周期，强引用不会泄漏
    private var iconList: Any? = null
    private var iconController: Any? = null
    private var statusBarView: View? = null

    /** 状态栏自己的 `statusIcons` 容器（用于分辨哪个 IconManager 属于状态栏） */
    private var statusIconsView: ViewGroup? = null

    /** 被替换过 `mGroup` 的图标管理器（运行期重新对账用） */
    private val zoneGroups = ArrayList<ZoneIconGroup>(2)

    /**
     * 左区分段容器：左区按**锚点**（时钟 / 通知图标）切成「锚点数 + 1」段，图标按所在段装进来。
     *
     * 下标 0 = 所有锚点之前、1 = 两个锚点之间、2 = 所有锚点之后（锚点最多 2 个）。
     */
    private val leftSegGroups = arrayOfNulls<LinearLayout>(3)

    /**
     * 右区分段容器：右区同样按落在右区的**锚点**（时钟 / 通知图标）切段，
     * 下标 0 = 第一个锚点之前、1 = 两个锚点之间；最后一段永远是宿主的 `statusIcons` 本身。
     */
    private val rightSegGroups = arrayOfNulls<LinearLayout>(2)

    /** 本轮真正摆好的右区锚点（伪条目，按 [rightOrder] 顺序）：[targetOf] 据此分段 */
    @Volatile
    private var placedRightAnchors: List<String> = emptyList()

    /** 时钟原始布局参数：搬回左区时原样还原 */
    private var clockParams: ViewGroup.LayoutParams? = null

    /** 锚点在左侧容器里的原始下标（首次见到时记录）：锚点右移回来时按它还原原位 */
    private val anchorStartIndex = HashMap<String, Int>(2)

    /** 本轮真正摆好的左区锚点（伪条目，按 [leftOrder] 顺序）：[leftSegmentOf] 据此分段 */
    @Volatile
    private var placedLeftAnchors: List<String> = emptyList()

    /** 两区容器是否生效（容器已就绪 + 拿到的是状态栏容器） */
    @Volatile
    private var zonesActive = false

    /** 图标 view 类的 `getSlot()` 解析结果缓存：按 Class 解析一次、每个 view 调用时再绑定实例 */
    private val slotMethodCache = HashMap<Class<*>, MethodResolver<View>>(8)

    /**
     * `ViewGroup.detachViewFromParent(View)`：同容器内移动子 view 的第一步（不触发 detach 回调）。
     *
     * 解析结果只保留 resolver，调用时用 `copy().of(parent)` 绑定实例：一个 resolver 只能绑定一次。
     */
    private val detachViewFromParentResolver: MethodResolver<ViewGroup>? by lazy {
        ViewGroup::class.java.resolve()
            .firstMethodOrNull {
                name = "detachViewFromParent"
                parameters(View::class)
            }
    }

    /**
     * `ViewGroup.attachViewToParent(View, int, LayoutParams)`：挂子 view 但不触发 attach 回调。
     *
     * 参数类型按 primitives 精确匹配（int 用 `Int::class`），与宿主声明一致。
     */
    private val attachViewToParentResolver: MethodResolver<ViewGroup>? by lazy {
        ViewGroup::class.java.resolve()
            .firstMethodOrNull {
                name = "attachViewToParent"
                parameters(View::class, Int::class, ViewGroup.LayoutParams::class)
            }
    }

    override fun onHook() {
        // 需要重启作用域才会装载/卸载本 hooker
        if (!preferences(ModulePrefs).getBoolean(ENABLE_KEY, false)) return
        // 装载时先把偏好读进来：**重启作用域后不需要任何 dataChannel 推送，按存好的顺序生效**
        //（dataChannel 只负责「运行期改一次就实时生效」，不是唯一入口）
        val initial = normalizeOrders(
            parseOrder(preferences(ModulePrefs).getString(LEFT_KEY, "")),
            parseOrder(preferences(ModulePrefs).getString(RIGHT_KEY, "")),
        )
        leftOrder = initial.first
        rightOrder = initial.second
//        YLog.debug("StatusBarIconOrder: loaded left=${leftOrder} right=${rightOrder}")
        hookIconList()
        hookIconController()
        hookIconManager()
        hookStatusBarView()
        // 重排 + refreshStatusBarIconList 都要在主线程做
        dataChannel.wait<String>(RIGHT_KEY) { value ->
            val orders = normalizeOrders(leftOrder, parseOrder(value), PushSide.RIGHT)
            leftOrder = orders.first
            rightOrder = orders.second
            Handler(Looper.getMainLooper()).post { applyZones(refresh = true) }
        }
        dataChannel.wait<String>(LEFT_KEY) { value ->
            val orders = normalizeOrders(parseOrder(value), rightOrder, PushSide.LEFT)
            leftOrder = orders.first
            rightOrder = orders.second
            Handler(Looper.getMainLooper()).post { applyZones(refresh = true) }
        }
    }

    private fun hookIconList() {
        val clazz = classOrNull(*iconListClassNames) ?: return
        clazz.resolve().apply {
            firstConstructor { parameterCount = 1 }.hook {
                after {
                    val list = instance
                    iconList = list
                    defaultOrder = namesOf(list)
                    applyOrder(list, refresh = false)
                }
            }
        }
    }

    private fun hookIconController() {
        val clazz = classOrNull(*iconControllerClassNames) ?: return
        clazz.resolve().apply {
            // 构造器最早拿到实例；参数个数按版本放宽（A13 ~ A15 之间在 8~11 之间浮动）
            firstConstructor { parameterCount { it in 8..11 } }.hook {
                after { iconController = instance }
            }
            // addIconGroup 在状态栏初始化时必然被调用一次；setIcon 兜底（模块晚加载时）
            firstMethodOrNull { name = "addIconGroup" }?.hook {
                before { iconController = instance }
            }
            firstMethodOrNull { name = "setIcon" }?.hook {
                before { if (iconController == null) iconController = instance }
            }
        }
    }

    /**
     * 把 `DarkIconManager.mGroup` 换成虚拟容器。
     *
     * 直接替换构造器第一个参数（而不是反射写 final 字段）：`mGroup = viewGroup` 仍是宿主自己的赋值，
     * 不依赖 final 字段的可写性，也不受 AOT 内联影响。原始容器留作右区容器。
     */
    private fun hookIconManager() {
        val clazz = classOrNull(*iconManagerClassNames) ?: return
        clazz.resolve().apply {
            firstConstructor { parameterCount { it in 7..10 } }.hook {
                before {
                    val original = args.getOrNull(0) as? ViewGroup ?: return@before
                    val group = ZoneIconGroup(original.context, original) { child, index, params ->
                        attachChild(child, index, params)
                    }
                    zoneGroups += group
                    args[0] = group
                }
            }
        }
    }

    /** 状态栏布局 inflate 完成后才能拿到左侧容器 / 时钟 / 状态栏自己的 statusIcons */
    private fun hookStatusBarView() {
        val clazz = classOrNull(STATUS_BAR_VIEW_CLASS) ?: return
        clazz.resolve().apply {
            firstMethod { name = "onFinishInflate" }.hook {
                after {
                    val view = instance<View>()
                    statusBarView = view
                    runCatching { applyZones(refresh = false) }
                        .onFailure { YLog.debug("StatusBarIconOrder: layout apply failed -> $it") }
                    // 布局装好后再按偏好重建一次图标：**重启作用域后不需要任何推送**，
                    // 直接按存好的左右区顺序生效（与 dataChannel 运行期推送走同一条 applyZones）
                    Handler(Looper.getMainLooper()).post {
                        // 再读一次偏好：装载到 inflate 之间若又改过，以最新偏好为准（prefs 是唯一真相）
                        val orders = normalizeOrders(
                            parseOrder(preferences(ModulePrefs).getString(LEFT_KEY, "")),
                            parseOrder(preferences(ModulePrefs).getString(RIGHT_KEY, "")),
                        )
                        leftOrder = orders.first
                        rightOrder = orders.second
                        YLog.debug("StatusBarIconOrder: startup apply left=${leftOrder} right=${rightOrder}")
                        runCatching { applyZones(refresh = true) }
                            .onFailure { YLog.debug("StatusBarIconOrder: startup apply failed -> $it") }
                    }
                }
            }
        }
    }

    /**
     * 重新对账：先按区域把图标 view 摆到对应容器，再把容器与时钟摆回布局树里的位置。
     *
     * @param refresh true 时额外重建图标 view（顺序变化必须重建：虚拟顺序来自宿主的插入顺序）
     */
    private fun applyZones(refresh: Boolean) {
        // hook 里任何异常都不能带走 SystemUI（宿主结构不符时只记日志、保持原样）
        runCatching {
            applyOrder(iconList, refresh = false)
            ensureGroups()
            reconcile()
            if (refresh) refreshIcons()
            // 布局跑完再打结构日志：这时 statusIcons 的宽度才是真实值
            statusIconsView?.post { dumpStructure("applied(refresh=$refresh)") }
        }.onFailure { YLog.debug("StatusBarIconOrder: applyZones failed -> $it") }
    }

    /** 解析状态栏自己的 `statusIcons`，并准备分区容器（按需才会挂到布局上） */
    private fun ensureGroups() {
        val root = statusBarView ?: return
        val systemIcons = root.findViewById<View>(idOf(root, "system_icons"))
        statusIconsView = systemIcons?.findViewById(idOf(root, "statusIcons"))
        for (index in leftSegGroups.indices) {
            if (leftSegGroups[index] == null) leftSegGroups[index] = createZoneGroup(root.context)
        }
        for (index in rightSegGroups.indices) {
            if (rightSegGroups[index] == null) rightSegGroups[index] = createZoneGroup(root.context)
        }
    }

    private fun createZoneGroup(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
        ).apply { gravity = Gravity.CENTER_VERTICAL }
    }

    /**
     * 左区图标落在第几段：数 [leftOrder] 里排在该 slot 之前、且**已摆好**的锚点个数。
     *
     * 只看 [placedLeftAnchors]（而不是 [leftOrder] 里的全部锚点）：锚点视图拿不到时不会参与摆放，
     * 用没摆的锚点数会算出不存在的段、图标被塞进没挂到布局里的容器而消失。
     */
    private fun leftSegmentOf(slot: String): Int {
        val slotIndex = leftOrder.indexOf(slot)
        if (slotIndex < 0) return 0
        var count = 0
        for (index in 0 until slotIndex) if (leftOrder[index] in placedLeftAnchors) count++
        return count
    }

    /**
     * 把分段容器与锚点摆到位。
     *
     * - 左区：`[第0段][锚点0][第1段][锚点1][第2段]`，锚点 = 落在左区顺序里的时钟 / 通知图标；
     * - 右区：宿主 `system_icons` 内、`statusIcons` 之前 → `[第0段][锚点0][第1段][锚点1][statusIcons][电池]`；
     * - **锚点视图拿不到**（宿主换实现 / 结构变化）：该区退化成「只有第 0 段」，
     *   图标照样能搬过去，只是无法再按锚点分段。
     *
     * 只摆「位置不对」的 view：宿主很多 view 挂在 ColorOS `SysuiLifecycle` 的 attach 回调上，
     * 反复 removeView/addView 会把它们的生命周期打成 DESTROYED，再 attach 就抛
     * `State is 'DESTROYED' and cannot be moved to RESUMED`（applyZones 会整条中断）。
     *
     * `system_icons` 仍是 `status_bar_end_side_content` 的唯一子 view，因此右区整体布局逻辑不变。
     *
     * @return 分区容器是否摆好；false 表示宿主结构完全不符（调用方回落到「不分区」）
     */
    private fun placeGroups(): Boolean {
        val root = statusBarView ?: return false
        val startSide =
            root.findViewById<ViewGroup>(idOf(root, "status_bar_start_side_except_heads_up"))
        if (startSide == null || leftSegGroups[0] == null) return false
        val systemIcons = root.findViewById<ViewGroup>(idOf(root, "system_icons"))
        val statusIcons = statusIconsView
        val clock = root.findViewById<View>(idOf(root, "clock_for_fake"))
        val notify = root.findViewById<View>(idOf(root, "status_bar_start_side_content_for_fake"))
        if (clock != null && clockParams == null) clockParams = clock.layoutParams

        val anchorViews = HashMap<String, View>(2)
        clock?.let { anchorViews[CLOCK_ITEM] = it }
        notify?.let { anchorViews[NOTIFY_ITEM] = it }
        // 锚点归属哪个区完全由顺序决定：伪条目写在左区顺序里就摆到左区、写在右区顺序里就摆到右区
        val leftAnchors = leftOrder.mapNotNull { item -> anchorViews[item]?.let { item to it } }
        val rightAnchors = rightOrder.mapNotNull { item -> anchorViews[item]?.let { item to it } }
        placedLeftAnchors = leftAnchors.map { it.first }
        // 右区锚点摆不进 `system_icons` 时不能声称已摆好：否则右区图标会被路由进不在布局树里的段容器
        placedRightAnchors =
            if (systemIcons != null && statusIcons != null) rightAnchors.map { it.first } else emptyList()
        for ((item, view) in leftAnchors) {
            if (view.parent === startSide && item !in anchorStartIndex) {
                anchorStartIndex[item] = startSide.indexOfChild(view)
            }
        }

        // 左区：[第0段][锚点0][第1段][锚点1][第2段]，整体插在第一个左区锚点原来的位置
        val leftSequence = ArrayList<View>(5)
        var segment = 0
        while (true) {
            leftSegGroups.getOrNull(segment)?.let { leftSequence += it }
            val anchor = leftAnchors.getOrNull(segment)?.second ?: break
            leftSequence += anchor
            segment++
        }
        val firstLeftAnchor = leftAnchors.firstOrNull()?.second
        val leftInsertAt = (
                firstLeftAnchor?.takeIf { it.parent === startSide }
                    ?.let { startSide.indexOfChild(it) }
                    ?: leftAnchors.mapNotNull { anchorStartIndex[it.first] }.minOrNull()
                    ?: 0
                ).coerceIn(0, startSide.childCount)
        placeSequence(startSide, leftSequence, leftInsertAt)

        // 右区：`system_icons` 内、`statusIcons` 之前 → [第0段][锚点0][第1段][锚点1][statusIcons][电池]
        // 每个段容器只在「它后面确实还有一个右区锚点」时才需要，最后一段就是宿主 `statusIcons` 本身
        val rightSequence = ArrayList<View>(4)
        for (index in rightAnchors.indices) {
            rightSegGroups.getOrNull(index)?.let { rightSequence += it }
            rightSequence += rightAnchors[index].second
        }
        if (systemIcons != null && statusIcons != null) {
            placeSequence(
                systemIcons,
                rightSequence,
                systemIcons.indexOfChild(statusIcons).coerceAtLeast(0),
            )
        }
        // 用不到的段容器摘掉：锚点变少时多出来的段留在布局树里，会占着父容器的宽度
        for (index in (leftAnchors.size + 1) until leftSegGroups.size) {
            detachViews(leftSegGroups[index])
        }
        for (index in rightAnchors.size until rightSegGroups.size) {
            detachViews(rightSegGroups[index])
        }
        return true
    }

    /**
     * 把 [sequence] 里的 view 按顺序摆到 [parent] 的 [startIndex] 起，只动「位置不对」的：
     * 同父容器内走 [moveChildInPlace]（不触发 attach/detach 回调，理由见 [placeGroups] 的注释），
     * 换父容器时也用 [detachQuietly] 摘掉再 `addView`（普通 `removeView` 会触发 detach 回调，
     * 把宿主的 `SysuiLifecycle` 打成 DESTROYED，下一次 attach 就抛异常、整次对账中断）。
     */
    private fun placeSequence(parent: ViewGroup, sequence: List<View>, startIndex: Int) {
        var index = startIndex.coerceIn(0, parent.childCount)
        for (view in sequence) {
            if (!isChildAt(parent, index, view)) {
                val currentParent = view.parent as? ViewGroup
                if (currentParent === parent) {
                    if (!moveChildInPlace(parent, view, index)) {
                        runCatching {
                            parent.removeView(view)
                            parent.addView(view, index.coerceIn(0, parent.childCount))
                        }.onFailure { YLog.debug("StatusBarIconOrder: move child failed -> $it") }
                    }
                } else {
                    currentParent?.let { detachQuietly(it, view) }
                    runCatching {
                        parent.addView(view, index.coerceIn(0, parent.childCount))
                    }.onFailure { YLog.debug("StatusBarIconOrder: attach child failed -> $it") }
                }
            }
            index = parent.indexOfChild(view).let { if (it >= 0) it + 1 else index + 1 }
        }
    }

    /**
     * 把 [view] 从 [parent] 摘掉但**不触发 detach 回调**（内部接口拿不到时退回普通 `removeView`）。
     *
     * `ViewGroup.detachViewFromParent` 同样会清掉 `mParent`，所以摘完就能 `addView` 到别的容器；
     * 宿主不少 view 挂在 ColorOS `SysuiLifecycle` 的 attach/detach 回调上，普通 `removeView`
     * 会把它们打成 DESTROYED，再 attach 就抛
     * `State is 'DESTROYED' and cannot be moved to RESUMED`。
     */
    private fun detachQuietly(parent: ViewGroup, view: View) {
        val detach = detachViewFromParentResolver
        if (detach != null) {
            runCatching { detach.copy().of(parent).invoke<Any?>(view) }
                .onSuccess { return }
                .onFailure { YLog.debug("StatusBarIconOrder: detachQuietly failed -> $it") }
        }
        runCatching { parent.removeView(view) }
    }

    /**
     * 同一父容器内把 [view] 移到 [index]：直接用 `ViewGroup.detachViewFromParent` /
     * `attachViewToParent`（Android 内部移动子 view 用的就是这两个），**不触发
     * `onDetachedFromWindow` / `onAttachedToWindow`**，因此不会碰坏宿主挂在 attach 回调上的生命周期。
     */
    private fun moveChildInPlace(parent: ViewGroup, view: View, index: Int): Boolean = runCatching {
        val detach = detachViewFromParentResolver ?: return@runCatching false
        val attach = attachViewToParentResolver ?: return@runCatching false
        detach.copy().of(parent).invoke<Any?>(view)
        attach.copy().of(parent).invoke<Any?>(
            view,
            index.coerceIn(0, parent.childCount),
            view.layoutParams
                ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
        )
        // 这两个内部接口不会自己请求布局，移动完必须手动触发，否则子 view 位置不刷新
        parent.requestLayout()
        parent.invalidate()
        view.requestLayout()
        true
    }.getOrElse {
        YLog.debug("StatusBarIconOrder: moveChildInPlace failed -> $it")
        false
    }

    /** 父容器的第 [index] 个子 view 是否就是 [view]（越界返回 false） */
    private fun isChildAt(parent: ViewGroup, index: Int, view: View): Boolean =
        index in 0 until parent.childCount && parent.getChildAt(index) === view

    /**
     * 图标离开宿主 `statusIcons`、进入我们自己的段容器时，清掉它身上的 `translationX`。
     *
     * `StatusIconContainer` 的 `StatusIconState`（ViewState）会给每个子 view 设 `translationX`，值是按
     * **`statusIcons` 自己的宽度**算出来的（StatusIconContainer.java:119 / :438-451）。图标被搬走之后
     * 没有人再更新它的 state，这个位移就一直留着，在新的容器里把图标整体推到一起 → 看上去就是
     * 「移动数据 / WIFI / 电池全叠在右上角」。
     *
     * 只清位移：`alpha` / `visibleState` 是宿主表示「这个图标该不该显示」的状态，不能碰。
     */
    private fun resetHostIconTransform(group: ZoneIconGroup, view: View, parent: ViewGroup) {
        if (group.rightGroup !== statusIconsView || parent === statusIconsView) return
        view.translationX = 0f
    }

    private fun detachViews(vararg groups: ViewGroup?) {
        for (group in groups) (group?.parent as? ViewGroup)?.removeView(group)
    }

    /**
     * 某个图标 view 的目标容器；null = 该 group 自己的容器（状态栏即宿主 `statusIcons`）。
     *
     * **只对状态栏那个 group 做分区路由**（`rightGroup === statusIconsView`）：锁屏/通知栏各自还有
     * 一个 `DarkIconManager`，它们的图标槽位同名，若也按槽位路由就会搬进状态栏的左侧容器，
     * 互相顶掉（左区看起来「搬过去没用」）。
     * 左区按锚点（时钟 / 通知图标）分成「锚点数 + 1」段，图标落在它所属的段容器里；
     * 右区同样按落在右区的锚点分段（最后一段即宿主 `statusIcons`，返回 null = 留在原地）。
     */
    private fun targetOf(group: ZoneIconGroup, child: View): ViewGroup? {
        if (!zonesActive) return null
        if (group.rightGroup !== statusIconsView) return null
        val slot = slotOf(child) ?: return null
        if (slot in anchorViewIds) return null
        if (slot in leftOrder) {
            // 左区：按「排在该 slot 之前的已摆好锚点数」选段；锚点全拿不到时退化成第 0 段
            return leftSegGroups.getOrNull(leftSegmentOf(slot)) ?: leftSegGroups[0]
        }
        if (slot !in rightOrder) return null
        // 右区：段号 = 排在该 slot 之前的已摆好锚点数。**只有排在某个右区锚点之前的图标**
        // 才需要额外容器；段号 ≥ 锚点数（即最后一段）时返回 null，落回宿主自己的容器 `statusIcons`。
        // 右区一个锚点都没有时必须全部返回 null：宿主 `statusIcons` 是 `layout_weight = 1` 的，
        // 旁边多出一个容器就会抢宽度，系统图标（信号 / WIFI / 电池）会被挤成一团叠在右上角。
        val segment = rightSegmentOf(slot)
        if (segment >= placedRightAnchors.size) return null
        return rightSegGroups.getOrNull(segment)
    }

    /** 右区图标落在第几段：数 [rightOrder] 里排在该 slot 之前的已摆好右区锚点个数 */
    private fun rightSegmentOf(slot: String): Int {
        val slotIndex = rightOrder.indexOf(slot)
        if (slotIndex < 0) return 0
        var count = 0
        for (index in 0 until slotIndex) if (rightOrder[index] in placedRightAnchors) count++
        return count
    }

    /** 把整个虚拟列表按当前区域配置摆好（先把要换容器的摘掉，再按顺序放回去） */
    private fun reconcile() {
        val statusIcons = statusIconsView
        zonesActive = leftSegGroups[0] != null && statusIcons != null &&
                zoneGroups.any { it.rightGroup === statusIcons }
        var placed = false
        if (zonesActive) {
            for (group in zoneGroups) reorderGroup(group)
            placed = placeGroups()
        }
        if (!placed) {
            // 容器没能挂进布局树（宿主结构变化）：图标全部回宿主 statusIcons，
            // 否则它们会被搬进不在布局树里的容器 = 直接看不见
            zonesActive = false
            for (group in zoneGroups) reorderGroup(group)
            detachViews(*leftSegGroups, *rightSegGroups)
        }
    }

    private fun reorderGroup(group: ZoneIconGroup) {
        val containers = ArrayList<ViewGroup>(8)
        containers += group.rightGroup
        for (segment in leftSegGroups) segment?.let { containers += it }
        for (segment in rightSegGroups) segment?.let { containers += it }

        // 1) 计算每个容器期望的子 view 顺序
        val desired = HashMap<ViewGroup, MutableList<View>>(4)
        for (child in group.snapshot()) {
            val target = targetOf(group, child) ?: group.rightGroup
            desired.getOrPut(target) { ArrayList(4) }.add(child)
        }
        // 2) 先把不属于该容器的子 view 摘掉（re-parent 前必须先解除旧父容器）。
        //    只摘本 group 自己的子 view：左区容器被状态栏 group 独占，别的 IconManager 的图标不归它管
        for (container in containers) {
            val want = desired[container] ?: emptyList()
            for (child in childrenOf(container)) {
                // 摘掉不该待在这个容器里的：走安静接口，避免每次对账都把子 view detach/attach 一遍
                if (group.contains(child) && child !in want) detachQuietly(container, child)
            }
        }
        // 3) 再按期望顺序依次摆好
        for (container in containers) {
            val want = desired[container] ?: continue
            for (index in want.indices) {
                val child = want[index]
                // 必须用 getChildAt（越界返回 null）：betterandroid 的 container.child(index)
                // 在容器为空 / 下标越界时会直接抛 IllegalStateException，把 SystemUI 带走
                if (isChildAt(container, index, child)) continue
                val currentParent = child.parent as? ViewGroup
                if (currentParent === container) {
                    // 同容器内换位：走安静接口，避免每次对账都把图标 detach/attach 一遍
                    if (!moveChildInPlace(container, child, index)) {
                        runCatching {
                            container.removeView(child)
                            container.addView(
                                child,
                                index.coerceIn(0, container.childCount),
                                child.layoutParams,
                            )
                        }.onFailure { YLog.debug("StatusBarIconOrder: reorder move failed -> $it") }
                    }
                } else {
                    // 换容器：安静摘掉再 attach（普通 removeView 会触发 detach 回调，
                    // 把宿主 SysuiLifecycle 打成 DESTROYED），失败只记日志（回落到宿主容器）
                    currentParent?.let { detachQuietly(it, child) }
                    runCatching {
                        container.addView(
                            child,
                            index.coerceIn(0, container.childCount),
                            child.layoutParams,
                        )
                    }.onFailure { YLog.debug("StatusBarIconOrder: reorder add failed -> $it") }
                }
                // 搬进我们自己的段容器后，宿主留下的位移必须清掉（理由见 resetHostIconTransform）
                resetHostIconTransform(group, child, container)
            }
        }
    }

    /** 虚拟容器收到子 view 时：记进虚拟列表，然后按区域转发到真实容器 */
    private fun attachChild(child: View, virtualIndex: Int, params: ViewGroup.LayoutParams?) {
        val group = zoneGroups.firstOrNull { it.contains(child) } ?: return
        val target = targetOf(group, child) ?: group.rightGroup
        val index = indexWithin(group, virtualIndex, target)
        runCatching {
            target.addView(
                child,
                index.coerceIn(0, target.childCount),
                params ?: child.layoutParams
            )
            resetHostIconTransform(group, child, target)
        }.onFailure {
            YLog.debug("StatusBarIconOrder: attach icon failed -> $it")
            runCatching { group.rightGroup.addView(child, params ?: child.layoutParams) }
        }
    }

    /** 虚拟列表中排在该图标之前、且同属 [target] 的图标个数 = 目标容器内的插入下标 */
    private fun indexWithin(group: ZoneIconGroup, virtualIndex: Int, target: ViewGroup): Int {
        var count = 0
        val children = group.virtualChildren
        for (index in 0 until virtualIndex.coerceAtMost(children.size)) {
            val sibling = children[index]
            if ((targetOf(group, sibling) ?: group.rightGroup) === target) count++
        }
        return count
    }

    private fun childrenOf(group: ViewGroup): List<View> =
        (0 until group.childCount).mapNotNull { group.getChildAt(it) }

    /** view 的可读名字：优先用资源 id 名（宿主 view 都有），拿不到就退回类名 */
    private fun viewName(view: View): String = runCatching {
        val id = view.id
        if (id != View.NO_ID) view.resources.getResourceEntryName(id) else view.javaClass.simpleName
    }.getOrElse { view.javaClass.simpleName }

    private fun childSummary(group: ViewGroup?): String =
        if (group == null) "null" else (0 until group.childCount).joinToString(",") { viewName(group.getChildAt(it)) }

    /**
     * 结构自检：把真实布局树的关键部分打进日志。
     *
     * 真机上「没反应 / 图标叠在一起」这类问题只能靠真实结构判断（开发机没有设备），
     * 所以每次对账后都打一行：顺序、实际摆好的锚点、两个容器的子 view、`statusIcons` 的宽度与槽位。
     * 出问题时直接看这一行就知道是顺序没生效、还是容器被塞坏（宽度变 0 就会叠在一起）。
     */
    private fun dumpStructure(note: String) {
        runCatching {
            val icons = statusIconsView ?: return
            val root = statusBarView ?: return
            val systemIcons = root.findViewById<ViewGroup>(idOf(root, "system_icons"))
            val startSide = root.findViewById<ViewGroup>(idOf(root, "status_bar_start_side_except_heads_up"))
            val slots = (0 until icons.childCount).joinToString(",") { slotOf(icons.getChildAt(it)) ?: "?" }
            YLog.debug(
                "StatusBarIconOrder: $note zonesActive=$zonesActive" +
                    " left=$leftOrder placedL=$placedLeftAnchors" +
                    " right=$rightOrder placedR=$placedRightAnchors" +
                    " startSide=[${childSummary(startSide)}]" +
                    " systemIcons=[${childSummary(systemIcons)}]" +
                    " statusIcons(w=${icons.width},n=${icons.childCount})=[$slots]"
            )
        }.onFailure { YLog.debug("StatusBarIconOrder: dumpStructure failed -> $it") }
    }

    /** 宿主资源 id 只能按名字取（模块里没有 com.android.systemui 的 R） */
    private fun idOf(view: View, name: String): Int =
        view.resources.getIdentifier(name, "id", view.context.packageName)

    /**
     * 图标 view 自报槽位名（`StatusIconDisplayable.getSlot()`）。
     *
     * 只认 public 方法，并沿父类与接口找：实现类常常只继承 `getSlot` 而不复写。
     * 解析失败不写缓存，下一次调用会重新解析（宿主可能晚一点才把实现类补上）。
     */
    private fun slotOf(view: View): String? = runCatching {
        val clazz = view.javaClass
        val resolver = slotMethodCache[clazz] ?: clazz.resolve()
            .firstMethodOrNull {
                name = "getSlot"
                modifiers(Modifiers.PUBLIC)
                superclass(interfaces = true)
            }
            ?.also { slotMethodCache[clazz] = it }
        resolver?.copy()?.of(view)?.invoke<Any?>() as? String
    }.getOrNull()

    /**
     * 按区域顺序原地重排 `mSlots`：左区在前、右区在后，未收录槽位按宿主默认顺序稳定排在最后。
     *
     * 左区在前是为了让左区图标按左区顺序进入虚拟列表；两区各自的相对顺序都会原样保留。
     */
    private fun applyOrder(list: Any?, refresh: Boolean) {
        val slots = (list ?: ensureIconList())?.let { slotsOf(it) } ?: return
        val rank = HashMap<String, Int>(leftOrder.size + rightOrder.size)
        var next = 0
        leftOrder.forEach { if (it !in anchorViewIds && rank.put(it, next) == null) next++ }
        rightOrder.forEach { if (it !in anchorViewIds && rank.put(it, next) == null) next++ }
        val fallback = HashMap<String, Int>(defaultOrder.size)
        defaultOrder.forEachIndexed { index, name -> fallback[name] = index }

        val comparator = Comparator<Any?> { left, right ->
            val nameLeft = left?.let { slotName(it) }
            val nameRight = right?.let { slotName(it) }
            if (nameLeft == null || nameRight == null) return@Comparator 0
            val rankLeft = rank[nameLeft]
            val rankRight = rank[nameRight]
            when {
                rankLeft != null && rankRight != null -> rankLeft - rankRight
                rankLeft != null -> -1
                rankRight != null -> 1
                else -> (fallback[nameLeft] ?: Int.MAX_VALUE) - (fallback[nameRight]
                    ?: Int.MAX_VALUE)
            }
        }
        // java.util.List.sort 是稳定排序：同序槽位保持原有相对位置
        slots.sortWith(comparator)

        if (refresh) refreshIcons()
    }

    /**
     * 重建全部图标 view（宿主 OplusStatusBarIconControllerEx 会在前后保存/恢复图标状态）。
     *
     * `refreshStatusBarIconList()` 在 oplus 实现里是 public final 且无重载，按「无参、沿父类」定位。
     */
    private fun refreshIcons() {
        val controller = iconController ?: return
        val resolver = controller.javaClass.resolve()
            .firstMethodOrNull {
                name = "refreshStatusBarIconList"
                parameterCount = 0
                superclass()
            }
        if (resolver == null) {
            YLog.debug("StatusBarIconOrder: refreshStatusBarIconList not found, restart SystemUI to apply")
            return
        }
        runCatching { resolver.copy().of(controller).invoke<Any?>() }
            .onFailure { YLog.debug("StatusBarIconOrder: refreshStatusBarIconList failed -> $it") }
    }

    /**
     * 兜底获取 `StatusBarIconList`：模块晚加载时 `StatusBarIconList` 的构造器 hook 不会触发，
     * 但 `StatusBarIconControllerImpl.mStatusBarIconList` 是 public 字段，可直接取。
     */
    private fun ensureIconList(): Any? {
        iconList?.let { return it }
        val controller = iconController ?: return null
        val list = runCatching {
            controller.javaClass.resolve()
                .firstFieldOrNull {
                    name = "mStatusBarIconList"
                    superclass()
                }
                ?.let { it.copy().of(controller).get<Any?>() }
        }.getOrNull() ?: return null
        iconList = list
        if (defaultOrder.isEmpty()) defaultOrder = namesOf(list)
        return list
    }

    private fun namesOf(list: Any): List<String> =
        slotsOf(list)?.mapNotNull { slot -> slot?.let { slotName(it) } } ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun slotsOf(list: Any): MutableList<Any?>? = runCatching {
        list.javaClass.resolve()
            .firstFieldOrNull {
                name = "mSlots"
                superclass()
            }
            ?.let { it.copy().of(list).get<Any?>() } as? MutableList<Any?>
    }.getOrNull()

    /** Slot 是宿主内部类，直接按字段名取值，避免写死 `StatusBarIconList$Slot` */
    private fun slotName(slot: Any): String? = runCatching {
        slot.javaClass.resolve()
            .firstFieldOrNull {
                name = "mName"
                superclass()
            }
            ?.let { it.copy().of(slot).get<Any?>() } as? String
    }.getOrNull()

    private fun classOrNull(vararg names: String): Class<*>? =
        names.firstNotNullOfOrNull { runCatching { it.toClassOrNull() }.getOrNull() }

    private fun parseOrder(value: String?): List<String> =
        value?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()

    /**
     * 归一化两区顺序。
     *
     * 时钟（[CLOCK_ITEM]）与通知图标（[NOTIFY_ITEM]）都是**可跨区的锚点**，各自必须恰好落在一个区域：
     * - 推送方明确的归属优先（[pushed]）：页面写回是两个键分两次推送，只有「刚写的那个键」说了算，
     *   否则把锚点从一区搬到另一区时会被先到的旧值拽回去；
     * - 两个键都写了（初始读取、[PushSide.NONE]）时以左区为准；
     * - 两个键都没写：刚推送的那个键把它从自己这区摘走了（[PushSide] 取反），没有推送信息时沿用上一轮归属，
     *   首次安装则两个锚点都落在左区（宿主默认：时钟在前、通知图标在左区最后）。
     *
     * 锚点原本在所属区里的相对位置会保留；新落进某区的锚点插在该区最前（等下一次推送带来准确位置）。
     */
    private fun normalizeOrders(
        left: List<String>,
        right: List<String>,
        pushed: PushSide = PushSide.NONE,
    ): Pair<List<String>, List<String>> {
        val resultLeft = left.filterNot { it in anchorViewIds }.toMutableList()
        val resultRight = right.filterNot { it in anchorViewIds }.toMutableList()

        for (anchor in anchorViewIds.keys) {
            val inLeft = left.contains(anchor)
            val inRight = right.contains(anchor)
            val toLeft = when {
                inLeft && inRight -> pushed != PushSide.RIGHT
                inLeft -> true
                inRight -> false
                // 两个键都没写：刚推送的那个键把它摘走了，所以归属是对侧
                pushed == PushSide.LEFT -> false
                pushed == PushSide.RIGHT -> true
                else -> !(anchor in rightOrder && anchor !in leftOrder)
            }
            val target = if (toLeft) resultLeft else resultRight
            if (anchor in target) continue
            val source = if (toLeft) left else right
            val at = when {
                // 保留它在原区里的相对位置
                anchor in source ->
                    source.take(source.indexOf(anchor)).count { it in target }
                        .coerceIn(0, target.size)
                // 补位：通知图标补在左区最后（宿主默认），其余补在该区最前
                toLeft && anchor == NOTIFY_ITEM -> target.size
                else -> 0
            }
            target.add(at, anchor)
        }
        return resultLeft to resultRight
    }

    /** 归一化时以哪一区的推送为准（页面写回是两个键分两次推送） */
    private enum class PushSide { NONE, LEFT, RIGHT }
}

/**
 * 虚拟图标容器：只维护「虚拟子 view 列表」，每个图标按槽位被转发到左区或右区真实容器，
 * 自己从不持有真实子 view，也不在布局树里。
 *
 * 宿主 `IconManager` 对 `mGroup` 的所有用法都能被满足：
 * - `addView(view, index, params)`：按宿主给的全局下标入列，再转发到目标容器；
 * - `getChildAt(i)` / `getChildCount()` / `indexOfChild(view)`：返回虚拟列表，索引空间与宿主一致；
 * - `removeViewAt(i)` / `removeView(view)` / `removeAllViews()`：同步从真实容器摘除。
 */
private class ZoneIconGroup(
    context: Context,
    /** 宿主原本的图标容器（右区容器，仍在布局树里） */
    val rightGroup: ViewGroup,
    private val onAttach: (View, Int, ViewGroup.LayoutParams?) -> Unit,
) : LinearLayout(context) {

    val virtualChildren = ArrayList<View>(16)

    fun snapshot(): List<View> = ArrayList(virtualChildren)

    fun contains(child: View): Boolean = virtualChildren.contains(child)

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        val at = (if (index < 0) virtualChildren.size else index).coerceIn(0, virtualChildren.size)
        virtualChildren.add(at, child)
        onAttach(child, at, params)
    }

    override fun getChildAt(index: Int): View? = virtualChildren.getOrNull(index)

    override fun getChildCount(): Int = virtualChildren.size

    override fun indexOfChild(child: View): Int = virtualChildren.indexOf(child)

    override fun removeView(child: View) {
        virtualChildren.remove(child)
        (child.parent as? ViewGroup)?.removeView(child)
    }

    override fun removeViewAt(index: Int) {
        val child = virtualChildren.getOrNull(index) ?: return
        virtualChildren.removeAt(index)
        (child.parent as? ViewGroup)?.removeView(child)
    }

    override fun removeViews(start: Int, count: Int) {
        for (index in (start + count - 1) downTo start) removeViewAt(index)
    }

    override fun removeAllViews() {
        for (child in snapshot()) (child.parent as? ViewGroup)?.removeView(child)
        virtualChildren.clear()
    }
}
