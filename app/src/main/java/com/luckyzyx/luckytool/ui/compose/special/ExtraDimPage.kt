package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppSlider
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.ExtraDimUtils
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.checkResolveActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 极暗模式页：直接读写框架监听的那三个 Settings.Secure 键，绕开系统设置页。
 *
 * ## 为什么不用系统设置页
 * 部分 ROM 的极暗设置页里，每个控制器都以「颜色变换未加速」判为不支持而被过滤，进页是空白；
 * 但框架侧的显示服务仍无条件监听这三个键，键一变就激活滤镜，所以 root 写键即可控制该功能。
 *
 * ## 状态模型
 * 真相在 [android.provider.Settings.Secure]（[ExtraDimUtils] 的三个键），页面状态只做镜像：
 * 每次读/写后都回读真实键值，不做 prefs 落盘，也不长期依赖乐观值。
 * 键未设置时强度读作 null，此时滑块显示 0 并在说明里写明「跟随系统最弱档」，用户拖动前不写键。
 *
 * ## 门禁（关键，别删）
 * 颜色变换未加速（[ExtraDimUtils.isSupported] 为 false）时框架套用的是全零颜色矩阵，
 * 把激活键写成 1 会直接黑屏（只能再写 0 恢复）。因此该情况下两个开关一律 disabled：
 * 页面禁用 + [ExtraDimUtils.writeActivated] 内部再挡一次，UI 无法绕过。
 *
 * ## 加载时机
 * 1. 进入页面：`LaunchedEffect(Unit)`；
 * 2. 回到前台：`LifecycleEventEffect(ON_RESUME)` —— 系统设置或磁贴可能改过同样的键；
 * 3. 下拉刷新：`spec.onRefresh` → [reloader]。
 *
 * 水平内缩由外层 Column 的 16dp padding 提供；滑条用 material3 `Slider` 包在自绘卡片里。
 */
object ExtraDimPage {

    /** 由内容组合时注册的加载器驱动 onRefresh */
    private var reloader: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "extra_dim",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { reloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "extra_dim_body", bare = true) {
            // 未读到真实能力前按「不支持」渲染：宁可短暂禁用，也不给黑屏留窗口
            var supported by remember { mutableStateOf(false) }
            var activated by remember { mutableStateOf<Boolean?>(null) }
            var level by remember { mutableStateOf<Int?>(null) }
            var persist by remember { mutableStateOf<Boolean?>(null) }
            var loading by remember { mutableStateOf(true) }

            suspend fun load() {
                val supportedNow = withContext(Dispatchers.IO) { ExtraDimUtils.isSupported(c) }
                val activatedNow = withContext(Dispatchers.IO) { ExtraDimUtils.readActivated() }
                val levelNow = withContext(Dispatchers.IO) { ExtraDimUtils.readLevel() }
                val persistNow = withContext(Dispatchers.IO) { ExtraDimUtils.readPersist() }
                supported = supportedNow
                activated = activatedNow
                level = levelNow
                persist = persistNow
                loading = false
            }

            // 写入后统一回读：键可能被系统策略拦下，界面只反映读回来的真实值
            suspend fun writeAndReload(write: () -> Boolean) {
                withContext(Dispatchers.IO) { write() }
                load()
            }

            // onRefresh 在组合之外调用加载器：用 SideEffect 注册，保证注册的是本次成功组合的实例
            SideEffect { reloader = ::load }
            DisposableEffect(Unit) {
                onDispose { reloader = null }
            }
            LaunchedEffect(Unit) { load() }
            // LifecycleEventEffect 的回调不是 suspend：经 rememberCoroutineScope 起协程
            val scope = rememberCoroutineScope()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { load() } }

            // 系统页可解析才给入口：极暗页在部分 ROM 上是空白页，这里只作为兜底
            val systemPageAvailable = remember(c) {
                c.checkResolveActivity(
                    "com.android.settings",
                    "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity",
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrefGroup {
                    item {
                        PrefSwitchRow(
                            title = stringResource(R.string.extra_dim_enable),
                            summary = stringResource(R.string.extra_dim_enable_summary),
                            checked = activated == true,
                            enabled = supported,
                            onCheckedChange = { value ->
                                activated = value
                                scope.launch { writeAndReload { ExtraDimUtils.writeActivated(c, value) } }
                            },
                        )
                    }
                    item {
                        PrefSwitchRow(
                            title = stringResource(R.string.extra_dim_persist),
                            summary = stringResource(R.string.extra_dim_persist_summary),
                            checked = persist == true,
                            enabled = supported,
                            onCheckedChange = { value ->
                                persist = value
                                scope.launch { writeAndReload { ExtraDimUtils.writePersist(value) } }
                            },
                        )
                    }
                }

                // 强度：拖动结束才写键（每帧写 shell 会卡手势）；键缺失时显示 0 且不写
                if (LocalUiMode.current == UiMode.Coui) {
                    CouiLevelCard(
                        level = level,
                        supported = supported,
                        onCommit = { stepped ->
                            level = stepped
                            scope.launch {
                                writeAndReload { ExtraDimUtils.writeLevel(stepped) }
                            }
                        },
                    )
                } else {
                    val sliderState = rememberSliderState(
                        value = (level ?: ExtraDimUtils.MIN_LEVEL).toFloat(),
                        steps = (ExtraDimUtils.MAX_LEVEL - ExtraDimUtils.MIN_LEVEL - 1).coerceAtLeast(0),
                        trackRange = ExtraDimUtils.MIN_LEVEL.toFloat()..ExtraDimUtils.MAX_LEVEL.toFloat(),
                    )
                    // 外部值变化同步：拖动期间 level 不变，因此不会覆盖用户手势
                    LaunchedEffect(level) {
                        sliderState.value = (level ?: ExtraDimUtils.MIN_LEVEL).toFloat()
                    }
                    // 水平 16dp 已由外层 Column 的 padding 提供，这里再叠一层会让滑条卡片比 PrefGroup 窄
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MaterialItemContainer(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.extra_dim_level),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        sliderState.value.roundToInt().toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (level == null) {
                                    Text(
                                        stringResource(R.string.extra_dim_level_follow_min),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                AppSlider(
                                    state = sliderState,
                                    valueRange = ExtraDimUtils.MIN_LEVEL.toFloat()..ExtraDimUtils.MAX_LEVEL.toFloat(),
                                    steps = (ExtraDimUtils.MAX_LEVEL - ExtraDimUtils.MIN_LEVEL - 1).coerceAtLeast(0),
                                    onValueChange = { sliderState.value = it },
                                    onValueChangeFinished = {
                                        val stepped = sliderState.value.roundToInt()
                                            .coerceIn(ExtraDimUtils.MIN_LEVEL, ExtraDimUtils.MAX_LEVEL)
                                        sliderState.value = stepped.toFloat()
                                        level = stepped
                                        scope.launch {
                                            writeAndReload { ExtraDimUtils.writeLevel(stepped) }
                                        }
                                    },
                                    enabled = supported,
                                )
                            }
                        }
                    }
                }

                if (!loading && !supported) {
                    val hint = stringResource(R.string.extra_dim_unsupported)
                    val hintModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    // 提示挂在卡片之外：COUI 线用说明字重与中性前景色，语气与 PrefRow 的 summary 一致
                    if (LocalUiMode.current == UiMode.Coui) {
                        CouiText(
                            text = hint,
                            modifier = hintModifier,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = hint,
                            modifier = hintModifier,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (systemPageAvailable) {
                    PrefGroup {
                        item {
                            PrefRow(
                                title = stringResource(R.string.extra_dim_open_system_page),
                                onClick = { IntentUtils(c).jumpVeryDarkMode() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 极暗强度卡片（COUI 线）：标题行给「强度 + 当前值」，滑条独占下一行。
 *
 * COUI 滑条按像素连续取值、不吸附刻度，因此手势期间的值只留在页内，松手才按
 * [ExtraDimUtils.MIN_LEVEL]..[ExtraDimUtils.MAX_LEVEL] 取整并交给 [onCommit] 写键。
 * 键被其他入口改过时回读的 level 会变，[LaunchedEffect] 再把页内值拉回真实值；
 * 拖动期间 level 不变，因此同步不会覆盖用户手势。
 */
@Composable
private fun CouiLevelCard(
    level: Int?,
    supported: Boolean,
    onCommit: (Int) -> Unit,
) {
    val dark = CouiTheme.isDark
    val colors = CouiPageDefaults.themeColors()
    val min = ExtraDimUtils.MIN_LEVEL
    val max = ExtraDimUtils.MAX_LEVEL
    val steps = (max - min - 1).coerceAtLeast(0)
    var dragValue by remember { mutableFloatStateOf((level ?: min).toFloat()) }
    LaunchedEffect(level) { dragValue = (level ?: min).toFloat() }
    PrefGroup {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                PrefRow(
                    title = stringResource(R.string.extra_dim_level),
                    summary = if (level == null) {
                        stringResource(R.string.extra_dim_level_follow_min)
                    } else null,
                    enabled = supported,
                    trailing = {
                        CouiText(
                            text = dragValue.roundToInt().toString(),
                            kind = CouiTextKind.ItemValue,
                            color = CouiTheme.colorScheme.accent,
                        )
                    },
                )
                CouiSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = CouiPageDefaults.rowPadding,
                            end = CouiPageDefaults.rowPadding,
                            bottom = 8.dp,
                        ),
                    value = dragValue,
                    onValueChange = { dragValue = it },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps,
                    enabled = supported,
                    colors = CouiSliderDefaults.colors(colors.accent, dark, supported),
                    dark = dark,
                    onValueChangeFinished = {
                        val stepped = dragValue.roundToInt().coerceIn(min, max)
                        dragValue = stepped.toFloat()
                        onCommit(stepped)
                    },
                )
            }
        }
    }
}
