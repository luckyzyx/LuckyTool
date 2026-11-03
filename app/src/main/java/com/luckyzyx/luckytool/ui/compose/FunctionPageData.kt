package com.luckyzyx.luckytool.ui.compose

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.luckyzyx.luckytool.ui.compose.components.PrefIndexItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AppIconCache
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** 功能树条目（分类页的 packName 为 null，走彩色徽标而不是 App 图标） */
internal data class TreeRow(
    val pageKey: String,
    val title: String,
    val summary: String?,
    val packName: String? = null,
)

/** 全屏搜索条目：一条偏好索引 + 它所属页的 pageKey / 页标题 */
internal data class SearchEntry(val pageKey: String, val pageTitle: String, val item: PrefIndexItem)

/** 分类页彩色徽标配色：固定色（不随深浅色模式变化，与应用图标一致）+ 白色矢量图标 */
internal data class CategoryBadge(val icon: ImageVector, val color: Color)

/**
 * 功能树行图标来源：分类页走 [categoryBadges] 彩色徽标（无包名），应用页用
 * `displayPackName ?: packName` 的应用原图。功能树列表与启动预取共用这一个判据，
 * 避免两边各写一份（拆开写过一次，结果「应用相关」拿到了电池 App 的图标）。
 */
internal fun rowIconPackName(pageKey: String, spec: ScopePageSpec): String? =
    if (pageKey in categoryBadges) null else (spec.displayPackName ?: spec.packName)

/**
 * 功能树行摘要：该页前三个功能标题，空则无摘要。功能树列表与预取快照共用这一个口径，
 * 避免两处各写一份（曾让应用页摘要改显示包名，整屏摘要就只剩包名）。
 */
internal fun rowSummary(index: List<PrefIndexItem>): String? =
    index.mapNotNull { it.title }.take(3).joinToString(" · ").ifEmpty { null }

internal val categoryBadges: Map<String, CategoryBadge> = mapOf(
    "android_related" to CategoryBadge(Icons.Filled.Android, Color(0xFF3DDC84)),
    "statusbar" to CategoryBadge(Icons.Filled.SignalCellularAlt, Color(0xFF4285F4)),
    "launcher" to CategoryBadge(Icons.Filled.Home, Color(0xFFFF9800)),
    "aod" to CategoryBadge(Icons.Filled.BrightnessMedium, Color(0xFF9C27B0)),
    "lock_screen" to CategoryBadge(Icons.Filled.Lock, Color(0xFF00BCD4)),
    "application" to CategoryBadge(Icons.Filled.Apps, Color(0xFF3F51B5)),
    "miscellaneous" to CategoryBadge(Icons.Filled.MoreHoriz, Color(0xFF607D8B)),
)

/** 页标题：登记在覆盖表的页取固定字符串资源，其余取宿主 App 标签（android_related 取 "android" 的标签） */
internal fun pageTitle(context: Context, spec: ScopePageSpec): String {
    ScopePageRegistry.treeTitleRes[spec.pageKey]?.let { return context.getString(it) }
    val pack = spec.displayPackName ?: if (spec.pageKey == "android_related") "android" else spec.packName
    return AppUtils(context).getAppLabel(pack).toString()
}

/**
 * 功能页两类数据的快照：功能树行 + 全屏搜索索引。
 *
 * 两者共用同一轮 [ScopePageRegistry.buildIndex] 结果（每页只跑一次偏好 DSL），
 * 因此「启动期把搜索索引也一起预取」不会让构建成本再翻一倍。
 * data class 是为了让结构相同的重算结果不触发重新组合（[FunctionPageData] 用它去重）。
 */
internal data class FunctionPageSnapshot(
    val rows: List<TreeRow>,
    val searchEntries: List<SearchEntry>,
)

/**
 * 功能页数据的前后台分离：App 打开时就在后台把功能树行与搜索索引构建好，切到「功能」标签页
 * 或全屏搜索时直接用现成数据，避免在标签切换的首帧现场跑 50 页偏好 DSL + PackageManager IPC
 * 造成的空白与 pop-in 割裂感。
 *
 * 线程：构建在内部 IO 作用域串行执行（同一时刻只允许一轮）；快照在主线程交给组合读取，
 * 图标预热通过 [AppIconCache.state] 只在主线程调用（该缓存自身不是线程安全的）。
 *
 * 新鲜度：进入功能页 / 搜索页时调用 [revalidate]，先用旧快照渲染、后台重算、算完替换 ——
 * 数据新鲜度与「每次进入都重算」一致，但不再阻塞首帧。
 */
internal object FunctionPageData {

    private val _snapshot = MutableStateFlow<FunctionPageSnapshot?>(null)

    /** null = 还没算过一次（此时界面按空数据处理，相当于加载中） */
    val snapshot: StateFlow<FunctionPageSnapshot?> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 构建闸门：已有构建在跑时不再排队，避免频繁切页堆叠多轮重算 */
    private val building = AtomicBoolean(false)

    /** App 启动时调用：提前把功能页数据准备好（重复调用等价于一次 [revalidate]） */
    fun preload(context: Context, iconSizePx: Int) {
        refresh(context.applicationContext, iconSizePx)
    }

    /** 进入功能页 / 全屏搜索时调用：旧快照先渲染，后台重算后替换 */
    fun revalidate(context: Context, iconSizePx: Int) {
        refresh(context.applicationContext, iconSizePx)
    }

    private fun refresh(context: Context, iconSizePx: Int) {
        if (!building.compareAndSet(false, true)) return
        scope.launch {
            try {
                val built = buildSnapshot(context)
                // 内容没变就不发布：结构相等的新对象会让整棵树重新组合一次
                if (_snapshot.value != built) _snapshot.value = built
                // 图标预热必须在主线程调用（AppIconCache.state 的线程约定），这里只登记不加载
                withContext(Dispatchers.Main.immediate) { warmIcons(context, built.rows, iconSizePx) }
            } catch (t: Throwable) {
                // buildIndex 内部逐页兜底，这里兜住 isVisible / 标题 / 图标包名等外层逻辑，
                // 失败时保留上一份快照，不让整棵树变空
                LogUtils.e("FunctionPageData", "build", t.toString(), true)
            } finally {
                building.set(false)
            }
        }
    }

    /** 单轮构建：功能树行 + 全屏搜索索引（每页只跑一次 [ScopePageRegistry.buildIndex]） */
    internal fun buildSnapshot(context: Context): FunctionPageSnapshot {
        val specs = ScopePageRegistry.all()
        val indexed = LinkedHashMap<String, IndexedPage>(specs.size)
        specs.forEach { spec ->
            // 手动隐藏 / 单 App 作用域未安装的页不入索引：功能树与全屏搜索用的是同一个判据
            runCatching {
                if (!spec.isVisible(context)) return@runCatching
                if (!ScopePageRegistry.isScopeAppPresent(context, spec)) return@runCatching
                indexed[spec.pageKey] =
                    IndexedPage(pageTitle(context, spec), ScopePageRegistry.buildIndex(context, spec))
            }.onFailure { LogUtils.e("FunctionPageData", "index $" + "{spec.pageKey}", it.toString(), true) }
        }
        val rows = ScopePageRegistry.treeOrder.mapNotNull { key ->
            val spec = ScopePageRegistry[key] ?: return@mapNotNull null
            val page = indexed[key] ?: return@mapNotNull null
            // 自带整页渲染的特殊页（暗色模式列表/意图/自定义浮窗等）不声明共享行、索引恒空，
            // 空索引不代表不可见；判据与 ScopePageRegistry.hasVisibleContent 一致
            if (page.index.isEmpty() && !spec.selfRendered) return@mapNotNull null
            val summary = rowSummary(page.index)
            val packName = rowIconPackName(key, spec)
            TreeRow(key, page.title, summary, packName)
        }
        val searchEntries = specs.flatMap { spec ->
            val page = indexed[spec.pageKey] ?: return@flatMap emptyList()
            page.index.map { item -> SearchEntry(spec.pageKey, page.title, item) }
        }
        return FunctionPageSnapshot(rows, searchEntries)
    }

    private fun warmIcons(context: Context, rows: List<TreeRow>, iconSizePx: Int) {
        rows.forEach { row ->
            row.packName?.takeIf { it.isNotEmpty() }?.let { AppIconCache.state(context, it, iconSizePx) }
        }
    }

    private class IndexedPage(val title: String, val index: List<PrefIndexItem>)
}
