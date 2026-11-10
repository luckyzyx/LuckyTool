package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.collection.ArrayMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.DarkModeInfo
import com.luckyzyx.luckytool.ui.compose.components.AppSearchPlaceholder
import com.luckyzyx.luckytool.ui.compose.components.AppSearchBar
import com.luckyzyx.luckytool.ui.compose.components.AppSlider
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialSwitch
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageAction
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.sendPrefsKey
import com.luckyzyx.luckytool.utils.sendPrefsValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * P4 特殊页：暗色模式应用列表。
 * 使用 ScopeScreen 的 fullContent 全屏自定义渲染 + onRefresh 下拉刷新。
 *
 * 单文件承载：状态/加载/写值逻辑只有一份，列表容器由 [DarkModeContent] 渲染、单行由
 * [DarkModeAppRow] 渲染，两处统一使用 Material 组件。
 *
 * 「打开暗色模式设置」是对整页数据的一次性动作，声明为 [ScopePageAction] 由页面宿主渲染进顶栏
 * （不占页面正文的卡片）；「重启作用域」由 `restartEnabled` 提供。
 */
object DarkModePage {

    internal const val ENABLE_KEY = "dark_mode_list_enable"
    internal const val SUPPORT_KEY = "dark_mode_support_list"

    /** 顶栏动作 key：打开暗色模式设置（旧菜单 id 2） */
    internal const val ACTION_OPEN = "open"

    internal var refresh: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "dark_mode",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf("com.android.settings"),
        restartEnabled = true,
        // 特殊页：整页由 fullContent 自渲染，入口可见性只看 isVisible + 作用域
        selfRendered = true,
        onRefresh = { refresh?.invoke() },
        fullContent = { builder -> DarkModeContent(builder) },
        topActions = listOf(
            ScopePageAction(
                key = ACTION_OPEN,
                labelRes = R.string.open,
                icon = R.drawable.baseline_open_in_new_24,
                showAsAction = true,
            ),
        ),
    ) { }
}

@Composable
internal fun LazyItemScope.DarkModeContent(builder: PrefScopeBuilder) {
    val context = LocalContext.current
    val packageUtils = remember { PackageUtils(context.packageManager) }
    val scope = rememberCoroutineScope()

    var isReverse by remember { mutableStateOf(false) }
    var sortMode by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var allAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var filterAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var allEnabledInfos by remember { mutableStateOf(ArrayMap<String, DarkModeInfo>()) }

    var showSortSheet by remember { mutableStateOf(false) }

    fun applyQuery(q: String): ArrayList<AppInfo> =
        if (q.isBlank()) allAppInfos
        else ArrayList(
            allAppInfos.filter {
                it.name.contains(q) || it.packageName.lowercase().contains(q)
            }
        )

    fun saveEnableList() {
        val data = allEnabledInfos.mapNotNull { (_, v) ->
            safeOfNull { Json.encodeToString(v) }
        }
        context.putStringSet(ModulePrefs, DarkModePage.SUPPORT_KEY, data.toSet())
        context.sendPrefsKey("android", DarkModePage.SUPPORT_KEY)
        context.sendPrefsKey("com.android.settings", DarkModePage.SUPPORT_KEY)
    }

    // 写值一律「先拷贝、再改、最后整体赋值」：就地改 state 当前持有的 map 后再赋一个
    // 内容相等的副本，MutableState 的结构相等策略会认定"值没变"而丢弃写入，
    // 界面不重组（表现为点了没反应、重开页面才刷新）。
    fun setEnabled(packName: String, enable: Boolean) {
        val next: ArrayMap<String, DarkModeInfo> = ArrayMap(allEnabledInfos)
        if (enable) next[packName] = DarkModeInfo(packName) else next.remove(packName)
        allEnabledInfos = next
        saveEnableList()
    }

    fun setType(packName: String, type: Int) {
        // DarkModeInfo 的 curType 是 var：就地改字段会连旧值一起变，
        // 必须换成一个新的 data class 实例，新 map 才与旧值不相等。
        val next: ArrayMap<String, DarkModeInfo> = ArrayMap(allEnabledInfos)
        next[packName]?.let { next[packName] = DarkModeInfo(it.packName, type) }
        allEnabledInfos = next
        saveEnableList()
    }

    suspend fun reload() {
        loading = true
        query = ""
        withContext(Dispatchers.IO) {
            val enableData =
                context.getStringSet(ModulePrefs, DarkModePage.SUPPORT_KEY, ArraySet())
            val appInfos = packageUtils.getInstalledAppInfos(0)
            appInfos.removeIf { it.isOverlay }
            val enabledInfos = ArrayMap<String, DarkModeInfo>()
            enableData.forEach { js ->
                val info = safeOfNull { Json.decodeFromString<DarkModeInfo>(js) }
                if (info != null &&
                    appInfos.find { it.packageName == info.packName } != null
                ) {
                    enabledInfos[info.packName] = info
                }
            }
            appInfos.apply {
                when (sortMode) {
                    0 -> sortBy { it.name }
                    1 -> sortBy { it.packageName }
                    2 -> sortBy { it.size }
                    3 -> sortBy { it.installTime }
                    4 -> sortBy { it.lastInstallTime }
                    5 -> sortBy { it.target }
                }
                if (isReverse) reverse()
            }
            val sortDatas = ArrayList<AppInfo>()
            enabledInfos.keys.forEach { k ->
                val find = appInfos.find { it.packageName == k } ?: return@forEach
                sortDatas.add(find)
            }
            sortDatas.apply {
                when (sortMode) {
                    0 -> sortBy { it.name }
                    1 -> sortBy { it.packageName }
                    2 -> sortBy { it.size }
                    3 -> sortBy { it.installTime }
                    4 -> sortBy { it.lastInstallTime }
                    5 -> sortBy { it.target }
                }
                if (isReverse) reverse()
            }
            appInfos.removeAll(sortDatas.toSet())
            appInfos.addAll(0, sortDatas)
            allEnabledInfos = enabledInfos
            allAppInfos = appInfos
            filterAppInfos = appInfos
        }
        loading = false
    }

    DarkModePage.refresh = { reload() }
    DarkModePage.spec.actionHandler = { key ->
        if (key == DarkModePage.ACTION_OPEN) IntentUtils(context).jumpDarkMode()
    }

    LaunchedEffect(Unit) {
        if (allAppInfos.isEmpty()) reload()
    }

    AppListLoadingContainer(
        visible = loading,
        modifier = Modifier.fillParentMaxHeight(),
    ) {
        Column(Modifier.fillParentMaxHeight()) {
            // COUI 线的分组卡片自带 16dp 水平内缩（CouiScopeCard），页级 padding 只补给 Material 线，
            // 否则开关卡左右各多缩 16dp，与搜索框 / 列表对不齐。
            PrefGroup(
                modifier = if (LocalUiMode.current == UiMode.Coui) Modifier else Modifier.padding(horizontal = 16.dp),
            ) {
                item {
                    PrefSwitchRow(
                        title = stringResource(R.string.enable_dark_mode_list),
                        checked = context.getBoolean(ModulePrefs, DarkModePage.ENABLE_KEY, false),
                        onCheckedChange = { v ->
                            context.putBoolean(ModulePrefs, DarkModePage.ENABLE_KEY, v)
                            context.sendPrefsValue("system", DarkModePage.ENABLE_KEY, v)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSearchBar(
                    query = query,
                    onQueryChange = { q ->
                        query = q
                        filterAppInfos = applyQuery(q)
                    },
                    placeholder = AppSearchPlaceholder,
                    enabled = !loading,
                    filterActive = showSortSheet,
                    onFilterClick = { showSortSheet = true },
                )
            }
            Spacer(Modifier.height(AppListContentGap))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.ItemGap),
            ) {
                itemsIndexed(filterAppInfos, key = { _, info -> info.packageName }) { index, info ->
                    AppItem(index = index, count = filterAppInfos.size) {
                        DarkModeAppRow(
                            info = info,
                            enabled = allEnabledInfos.containsKey(info.packageName),
                            curType = allEnabledInfos[info.packageName]?.curType ?: 0,
                            onToggle = { v -> setEnabled(info.packageName, v) },
                            onTypeChange = { t -> setType(info.packageName, t) },
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortFilterSheet(
            reverse = isReverse,
            sortMode = sortMode,
            sortLabels =
                context.resources.getStringArray(R.array.sort_selector_chips).toList(),
            onReverseChange = {
                isReverse = !isReverse
                scope.launch { reload() }
            },
            onSortChange = { mode ->
                sortMode = mode
                scope.launch { reload() }
            },
            filterContent = null,
            onDismiss = { showSortSheet = false },
        )
    }
}

/**
 * 暗色模式应用行：图标 + 应用名 + 包名 + 只读开关，整行点击切换；启用后在本卡片内追加
 * 滑条（0..4 五档）与数值文本。
 *
 * 两条线只差最外层的卡片载体与行内文字原语：COUI 线的卡片由调用点的 AppItem 分段卡片提供，
 * 这里只做纵向排布；Material 线由 MaterialItemContainer 绘制卡片。行内容只写一份，
 * 避免两处排版各自漂移。
 */
@Composable
internal fun DarkModeAppRow(
    info: AppInfo,
    enabled: Boolean,
    curType: Int,
    onToggle: (Boolean) -> Unit,
    onTypeChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val coui = LocalUiMode.current == UiMode.Coui
    val body: @Composable () -> Unit = {
        Column {
            AppListItem(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onToggle(!enabled)
                },
                interactionSource = interactionSource,
                headlineContent = {
                    if (coui) {
                        CouiText(
                            text = info.name,
                            kind = CouiTextKind.ItemTitle,
                            color = CouiTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            info.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                supportingContent = {
                    if (coui) {
                        CouiText(
                            text = info.packageName,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            info.packageName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                leadingContent = {
                    info.icon?.let { d ->
                        Image(
                            remember(info) { d.toBitmap().asImageBitmap() },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                },
                trailingContent = {
                    // 两条线都只显示状态、不接手势：点击与触感由整行统一处理
                    if (coui) {
                        CouiSwitch(
                            checked = enabled,
                            onCheckedChange = null,
                            enabled = true,
                            interactionSource = interactionSource,
                            dark = CouiTheme.isDark,
                        )
                    } else {
                        MaterialSwitch(
                            checked = enabled,
                            onCheckedChange = null,
                            interactionSource = interactionSource,
                        )
                    }
                },
            )
            if (enabled) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (LocalUiMode.current) {
                        UiMode.Material -> {
                            var sliderValue by remember(info) { mutableFloatStateOf(curType.toFloat()) }
                            // material3 的 SliderState 是非受控语义，值必须由调用方写回
                            val sliderState = rememberSliderState(
                                value = curType.toFloat(),
                                steps = 3,
                                trackRange = 0f..4f,
                            )
                            // 列表复用行时把滑条同步到当前应用的类型
                            LaunchedEffect(info.packageName) {
                                sliderState.value = curType.toFloat()
                            }
                            AppSlider(
                                state = sliderState,
                                valueRange = 0f..4f,
                                steps = 3,
                                onValueChange = {
                                    sliderValue = it
                                    sliderState.value = it
                                },
                                modifier = Modifier.weight(1f),
                                onValueChangeFinished = { onTypeChange(sliderValue.toInt()) },
                            )
                            Text(
                                curType.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        UiMode.Coui -> {
                            // COUI 滑条是值式受控：手势期间的值只留在页内，松手才把类型写回
                            var sliderValue by remember(info) { mutableFloatStateOf(curType.toFloat()) }
                            LaunchedEffect(info.packageName) { sliderValue = curType.toFloat() }
                            val dark = CouiTheme.isDark
                            CouiSlider(
                                modifier = Modifier.weight(1f),
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                valueRange = 0f..4f,
                                steps = 3,
                                enabled = true,
                                colors = CouiSliderDefaults.colors(CouiTheme.colorScheme.accent, dark, true),
                                dark = dark,
                                onValueChangeFinished = { onTypeChange(sliderValue.toInt()) },
                            )
                            CouiText(
                                text = curType.toString(),
                                kind = CouiTextKind.ItemValue,
                                color = CouiTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
    if (coui) {
        Column(modifier = Modifier.fillMaxWidth()) { body() }
    } else {
        MaterialItemContainer { body() }
    }
}
