@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.components.preference

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.utils.PrefState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 搜索跳转目标：position 为 LazyColumn 槽位（与 [PrefIndexItem.slot] 对应） */
data class ScrollTarget(val key: String, val position: Int)

/**
 * 单个偏好项的搜索索引条目（Function 页全局搜索由 [PrefScopeBuilder.snapshotIndex] 收集，
 * 与旧 PrefsItem 语义对齐：key/title/summary 参与过滤，pageTarget 非空则命中即导航）。
 */
data class PrefIndexItem(
    val key: String,
    val title: String?,
    val summary: String?,
    val visible: () -> Boolean = { true },
    val slot: Int,
    val pageTarget: String? = null,
)

/** 渲染单元：一个 LazyColumn item 槽位 */
internal class PrefEntry(
    val slot: Int,
    val itemKey: String,
    val indexKey: String?,
    val indexTitle: String?,
    val indexSummary: String?,
    val pageTarget: String?,
    val render: @Composable (slot: Int) -> Unit,
)

/**
 * 偏好页声明式 DSL —— 旧 BaseScopePreferenceFeagment 程序化 PreferenceScreen 的 Compose 等价物。
 *
 * 用法：
 * ```
 * ScopeScreen(state, sendValue = { k, v -> context.sendPrefsValue(pack, k, v) }) {
 *     category("基础")
 *     switch("key1", "标题", "摘要")
 *     if (state.getBoolean("master", false)) {   // 条件可见性：任意偏好变更自动重求值
 *         slider("key2", "滑条", 1..100)
 *     }
 *     page("其他页面", "statusbar_clock")
 * }
 * ```
 *
 * 构建 lambda 是普通（非 Composable）函数：每次重组重跑（等价旧 View 方案“重进页面刷新”，
 * 但自动触发），因此条件判断用 PrefState 同步 getter 即天然响应式。搜索索引同样来自构建结果：
 * Function 页可不在 UI 中渲染，直接用同一 lambda 收集 [PrefIndexItem]。
 *
 * 写值语义对齐旧代码：`state.set(key, value)` 先落 SharedPreferences（host 进程 hook 经
 * remote prefs 同源可见），再经 [sendValue] 触发 `Context.sendPrefsValue(packName, key, value)`。
 */
class PrefScopeBuilder internal constructor(
    internal val state: PrefState,
) {
    internal var sendValue: (key: String, value: Any) -> Unit = { _, _ -> }
    internal var navigate: ((target: String, title: String?) -> Unit)? = null
    internal var restart: (() -> Unit)? = null
    internal var context: Context? = null
    internal val highlightSlot = mutableStateOf<Int?>(null)

    internal val entries = ArrayList<PrefEntry>()

    /** 每次重建前清空（ScopeScreen 每次重组前调用；外部索引收集同样先调用） */
    fun beginBuild() {
        entries.clear()
    }

    /** 当前构建轮次的搜索索引快照 */
    fun snapshotIndex(): List<PrefIndexItem> = entries.mapNotNull { entry ->
        entry.indexKey?.let { key ->
            PrefIndexItem(key, entry.indexTitle, entry.indexSummary, { true }, entry.slot, entry.pageTarget)
        }
    }

    @Composable
    internal fun highlightColor(slot: Int): Color {
        val target = highlightSlot.value == slot
        return animateColorAsState(
            targetValue = if (target) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ).value
    }

    private fun emit(
        itemKey: String?,
        indexKey: String?,
        indexTitle: String?,
        indexSummary: String?,
        pageTarget: String? = null,
        render: @Composable (slot: Int) -> Unit,
    ) {
        val slot = entries.size
        entries += PrefEntry(slot, itemKey ?: "slot-$slot", indexKey, indexTitle, indexSummary, pageTarget, render)
    }

    // ---------------- DSL 项 ----------------

    /** 分类标题（对应旧 addCategory / categoryPreference） */
    fun category(title: String) = emit(null, null, null, null) { _ ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        )
    }

    /** 开关（对应 SwitchPreference + setOnPreferenceChangeListener） */
    fun switch(
        key: String,
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((Boolean) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val checked by state.booleanFlow(key).collectAsStateWithLifecycle()
        val bg = highlightColor(slot)
        fun apply(newValue: Boolean) {
            state.set(key, newValue)
            if (notify) sendValue(key, newValue)
            onChange?.invoke(newValue)
        }
        ListItem(
            onClick = { if (enabled) apply(!checked) },
            supportingContent = summary?.let { { Text(it) } },
            trailingContent = {
                Switch(checked = checked, enabled = enabled, onCheckedChange = ::apply)
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(bg),
            enabled = enabled,
        ) { Text(title) }
    }

    /** 单选列表（对应 DropDownPreference：条目文字 + entryValues 存储值） */
    fun list(
        key: String,
        title: String,
        entries: Array<String>,
        entryValues: Array<String>,
        default: String = "",
        summary: String? = null,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((String) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val current by state.stringFlow(key, default).collectAsStateWithLifecycle()
        var showDialog by remember { mutableStateOf(false) }
        val currentLabel = entries.getOrNull(entryValues.indexOf(current)) ?: current.orEmpty()
        val bg = highlightColor(slot)
        ListItem(
            onClick = { showDialog = true },
            supportingContent = { Text(summary?.replace("%s", currentLabel) ?: currentLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .background(bg),
            enabled = enabled,
        ) { Text(title) }
        if (showDialog) {
            var selected by remember { mutableStateOf(entryValues.indexOf(current).coerceAtLeast(0)) }
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(title) },
                text = {
                    Column {
                        entries.forEachIndexed { i, label ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(selected = i == selected, onClick = { selected = i })
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = i == selected, onClick = null)
                                Text(label, Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            val newValue = entryValues.getOrNull(selected) ?: return@TextButton
                            state.set(key, newValue)
                            if (notify) sendValue(key, newValue)
                            onChange?.invoke(newValue)
                        },
                    ) { Text("确定") /* TODO P5: 迁入 stringResource */ }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("取消") }
                },
            )
        }
    }

    /** 整数滑条（对应 SeekBarPreference；拖动结束才落盘，避免 commit() 拖拽风暴） */
    fun slider(
        key: String,
        title: String,
        valueRange: IntRange,
        step: Int = 1,
        default: Int? = null,
        summary: String? = null,
        valueLabel: ((Int) -> String)? = null,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((Int) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val stored by state.intFlow(key, default ?: valueRange.first).collectAsStateWithLifecycle()
        val sliderState = rememberSliderState(
            value = stored.coerceIn(valueRange.first, valueRange.last).toFloat(),
            steps = ((valueRange.last - valueRange.first) / step - 1).coerceAtLeast(0),
            trackRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        )
        var dragging by remember { mutableStateOf(false) }
        // 外部值变化同步（拖动期间不覆盖用户手势）
        if (!dragging) {
            sliderState.value = stored.coerceIn(valueRange.first, valueRange.last).toFloat()
        }
        val current = sliderState.value.roundToInt()
        val bg = highlightColor(slot)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    valueLabel?.invoke(current) ?: current.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            summary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                state = sliderState,
                onValueChange = {
                    dragging = true
                    sliderState.value = it
                },
                onValueChangeFinished = {
                    dragging = false
                    val min = valueRange.first
                    val stepped = (((sliderState.value - min) / step).roundToInt() * step + min)
                        .coerceIn(valueRange.first, valueRange.last)
                    sliderState.value = stepped.toFloat()
                    state.set(key, stepped)
                    if (notify) sendValue(key, stepped)
                    onChange?.invoke(stepped)
                },
                enabled = enabled,
            )
        }
    }

    /** 文本输入（对应 EditTextPreference，M3 对话框 + OutlinedTextField） */
    fun editText(
        key: String,
        title: String,
        summary: String? = null,
        hint: String? = null,
        dialogMessage: String? = null,
        default: String = "",
        keyboardType: KeyboardType = KeyboardType.Text,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((String) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val current by state.stringFlow(key, default).collectAsStateWithLifecycle()
        var showDialog by remember { mutableStateOf(false) }
        val bg = highlightColor(slot)
        ListItem(
            onClick = { showDialog = true },
            supportingContent = { Text(current.ifBlank { summary ?: "" }) },
            modifier = Modifier
                .fillMaxWidth()
                .background(bg),
            enabled = enabled,
        ) { Text(title) }
        if (showDialog) {
            var text by remember { mutableStateOf(current.orEmpty()) }
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(title) },
                text = {
                    Column {
                        dialogMessage?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = hint?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            state.set(key, text)
                            if (notify) sendValue(key, text)
                            onChange?.invoke(text)
                        },
                    ) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("取消") }
                },
            )
        }
    }

    /** 页面入口（对应旧 addFragmentPreference：跳转其他作用域页；同时登记搜索索引） */
    fun page(
        title: String,
        target: String,
        summary: String? = null,
        enabled: Boolean = true,
    ) = emit("page:$target", "page:$target", title, summary, pageTarget = target) { slot ->
        ListItem(
            onClick = { navigate?.invoke(target, title) },
            supportingContent = summary?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightColor(slot)),
            enabled = enabled,
        ) { Text(title) }
    }

    /** 逃生舱：任意自定义 Composable（ColorPicker、应用选择器等特殊控件用），receiver 可访问 state/restart 等 */
    fun custom(
        key: String? = null,
        title: String? = null,
        summary: String? = null,
        content: @Composable PrefScopeBuilder.(slot: Int) -> Unit,
    ) = emit(key, key, title, summary, render = { slot -> content(this, slot) })
}

/**
 * 作用域偏好页主体（无 Scaffold，供 ComposeView 容器 / NavHost 内容插槽使用，标题栏由宿主提供）。
 *
 * 与旧 BaseScopePreferenceFeagment 行为对齐：
 * - [sendValue] 对应 `Context.sendPrefsValue(packName, key, value)`（dataChannel 通知宿主）
 * - [scrollTarget] 对应搜索跳转（scrollKey/scrollPosition）：滚动到槽位 + 高亮闪烁
 * - [onNavigate] 对应页面入口跳转（page DSL 项）
 */
@Composable
fun ScopeScreen(
    state: PrefState,
    modifier: Modifier = Modifier,
    sendValue: (key: String, value: Any) -> Unit = { _, _ -> },
    scrollTarget: ScrollTarget? = null,
    onNavigate: ((target: String, title: String?) -> Unit)? = null,
    onRestart: (() -> Unit)? = null,
    onRefresh: (suspend () -> Unit)? = null,
    fullContent: (@Composable LazyItemScope.(PrefScopeBuilder) -> Unit)? = null,
    content: PrefScopeBuilder.() -> Unit,
) {
    // 订阅 revision：任何偏好写入都会重组本页 → 构建 lambda 重跑 → 条件可见性自动重求值
    state.revision.collectAsStateWithLifecycle()

    val builder = remember(state) { PrefScopeBuilder(state) }
    builder.sendValue = sendValue
    builder.navigate = onNavigate
    builder.restart = onRestart
    builder.context = LocalContext.current.applicationContext
    builder.beginBuild()
    builder.content()

    val listState = rememberLazyListState()
    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        val slot = if (!target.key.isNullOrBlank()) {
            // 按索引 key 解析槽位（条目在构建后位置可能因条件可见性变化而偏移）
            builder.entries.firstOrNull { it.indexKey == target.key }?.slot ?: return@LaunchedEffect
        } else {
            target.position.takeIf { it >= 0 } ?: return@LaunchedEffect
        }
        listState.animateScrollToItem(slot)
        builder.highlightSlot.value = slot
        delay(2500)
        if (builder.highlightSlot.value == slot) builder.highlightSlot.value = null
    }

    val list: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(modifier = listModifier, state = listState) {
            if (fullContent != null) {
                item(key = "full") { fullContent(this, builder) }
            } else {
                builder.entries.forEach { entry ->
                    item(key = entry.itemKey) { entry.render(entry.slot) }
                }
            }
        }
    }
    if (onRefresh != null) {
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    try {
                        onRefresh()
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = modifier,
        ) { list(Modifier.fillMaxSize()) }
    } else {
        list(modifier)
    }
}
