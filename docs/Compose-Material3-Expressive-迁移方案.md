# LuckyTool → Compose Material3 Expressive 全量迁移适配方案

> 版本：v1.0（方案）
> 适用基线：LuckyTool 1.3.5_beta · Kotlin 2.4.20 · AGP 9.4.1 · compileSdk 37 / targetSdk 28 / minSdk 30
> 原则：**现代化优先**——弃用 View 体系 / XML 布局 / ViewBinding / PreferenceFragmentCompat / safe-args / Bundle 路由，全面转向 Compose + 类型安全路由 + 单向数据流；**兼容性兜底**——存储层、hook 层、服务层的既有约束是硬边界，方案不触碰。

---

## 1. 结论摘要（TL;DR）

| 维度 | 决策 |
|---|---|
| UI 框架 | Jetpack Compose + Material3 **1.4.x（Expressive 已并入 material3 稳定版）** |
| 主题 | `MaterialExpressiveTheme` + 动态取色（dynamic color）+ MotionScheme |
| 导航 | `navigation-compose` 类型安全路由（kotlinx-serialization `@Serializable`，插件已就绪） |
| 存储 | **保持 SharedPreferences 不变**（hook 跨进程读取是硬约束），新增响应式 `PrefState` 适配层 |
| 偏好页 | 自研声明式 `PrefScope` DSL，替代 `PreferenceFragmentCompat`（85 个作用域页机械化批量迁移） |
| 列表/图片 | LazyColumn / PullToRefreshBox / Coil3（Glide 当前零调用，直接删除） |
| 渐进策略 | DSL 先行 + 试点 + 主壳翻转 + 批量替换（绞杀者模式，全程可编译可回退） |

---

## 2. 现状盘点（勘查结论）

- 纯 View 体系：`~586` 个 kt 文件；`app/src/main/res/layout` 52 个 XML 布局；ViewBinding 全量启用；无任何 Compose 依赖。
- 导航：`res/navigation/nav_container.xml` 单图，5 个顶级 destination（Home/Other/Function/Log/Setting）+ **约 85 个作用域详情页**（全部带 `title_text` 字符串参数）。
- 偏好体系：`BaseScopePreferenceFeagment : PreferenceFragmentCompat`（注意类名拼写）程序化构建偏好，`RemotePreferenceDataStore` 包装 SharedPreferences，支持搜索索引（`PrefsItem`）+ 滚动高亮；子类遍布 `ui/fragment/scopes/{apps,others,related,statusbar}`。
- 特殊页面：列表型（MemcConfig 双 Tab+搜索+刷新、ZoomWindow/MultiApp/ForceFps/DarkMode/HideAppIntent 应用列表）、选择器对话框 ×3（App/Activity/Intent）、ColorPicker 自定义 View 偏好（colorpicker 模块，仅 2 处使用）、CropImageActivity（canhub cropper，View 库）。
- 依赖使用面：hikage 仅 UpdateUtils 1 处；Glide 在代码中 **0 处调用**；Markwon 仅 XposedFragment 版本信息表格；betterandroid 的 UI 组件用于 9 个列表页/对话框，但其 **ui/system 扩展与通知工厂被 hook 层大量使用**（必须整体保留）。
- `androidx.preference` 引用 201 处：其中约 5 处在 **hook 层**（如 `EnableCameraDebugUIOption.kt:83` 用 `PreferenceManager.getDefaultSharedPreferences`，运行于目标进程内）。

## 3. 硬约束（方案必须遵守）

1. **存储层保持 SharedPreferences**。hook 注入的目标进程（SystemUI、设置等）通过 `SharedPreferences`/`PreferenceManager` 跨进程读配置。DataStore（protobuf）目标进程读不了——**禁止迁移 DataStore**。Compose 侧只加"响应式读取适配层"，写路径不变。
2. **hook 层（`hook/**`，300+ 文件）一行不改的 UI 语义**。它们操作的是目标 App 的 View 树，与模块自身 UI 无关；但它们引用的 betterandroid 扩展 / `androidx.preference` 类必须留在模块 dex 中（或就地改写，见 §10）。
3. **服务 / AIDL / 15 个 QS Tile / XposedServiceBridge / 生物识别 / 重启逻辑**不迁移，仅从 Activity 生命周期搬运到 Compose 生命周期等价物。
4. **包体敏感**：Xposed 模块 APK 带全部依赖，resopt 已启用；Compose 引入需评估增量（预计 dex +2~4MB，可接受）。

---

## 4. 目标技术栈与版本清单

> ⚠️ 版本号基于撰写时知识，**落地前以 Google Maven 实际最新稳定版为准**（本会话无网络，未在线核实）。

`gradle/libs.versions.toml` 增删：

```toml
[versions]
compose-bom = "2026.XX.XX"        # ← 以 google maven 最新稳定 BOM 为准
# kotlin = "2.4.20" 已是最新，Compose 编译器插件与 Kotlin 同版本（现代做法，不再写 composeOptions）
navigation = "2.10.2"             # 已有，navigation-compose 复用同一版本
coil = "3.4.0"                    # 以最新稳定为准
lifecycle = "2.11.0"              # 已有

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }            # 含 Expressive
androidx-compose-material3-adaptive = { group = "androidx.compose.material3", name = "material3-adaptive" } # 可选
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`app/build.gradle.kts`：新增 `alias(libs.plugins.kotlin.compose)`，`buildFeatures { compose = true }`。**不要**写 `composeOptions.kotlinCompilerExtensionVersion`（Kotlin 2.x 已废弃该机制）。

**删除**（全部迁移完成后）：`material(Components) 1.14.0`、`constraintlayout`、`preference-ktx`、`swiperefreshlayout`、`hikage`、`glide`（零调用）、`markwon`、`me.zhanghai.android:fastscroll`、safe-args 插件、viewBinding 开关。

**保留**：betterandroid（hook 层 + 服务通知）、drake-net、libsu、biometric、dexkit、yukihook、kavaref、lsparanoid、resopt、kotlinx-serialization。

## 5. 主题与动效基建（Material3 Expressive）

新建 `ui/theme/LuckyTheme.kt`，替换 `ThemeUtils` 的 View 侧职责（`ThemeUtils` 的偏好读写与 hook 无关，保留函数签名，只删 View 专属逻辑）：

```kotlin
@Composable
fun LuckyTheme(
    darkTheme: Boolean = when (darkThemePref) {           // "dark_theme": 0 跟随/1 强制夜间/2 强制白天
        "1" -> true
        "2" -> false
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = dynamicColorPref,              // "use_dynamic_color" 默认 true
    motionScheme: MotionScheme = MaterialExpressiveMotionScheme.standard(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        else -> if (darkTheme) DarkColorScheme else LightColorScheme   // 沿用 md_theme_* 色值映射
    }
    MaterialExpressiveTheme(            // material3 1.4+：MaterialTheme + 动效 + 强调对比
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        emphasisContrast = EmphasisContrast.Medium,   // 0 / -1 / 0.5 / 1 / 2
        content = content,
    )
}
```

要点：

- **夜间模式**：`AppCompatDelegate.setDefaultNightMode` 是官方在 API<31 上控制日/夜模式的推荐做法，**保留**（targetSdk 28 无法用 `UiModeManager.setApplicationNightMode`）。Compose 侧以 `darkTheme` 状态驱动，两者同步。
- **动态取色**：`DynamicColors.applyToActivityIfAvailable` 从 BaseActivity 移除，等价能力上移到 `dynamicDarkColorScheme/dynamicLightColorScheme`；可用性判断沿用 `DynamicColors.isDynamicColorAvailable()`。
- **Expressive 落地**：全局 `MotionScheme`（标准/富有表现力两档可给设置页加开关）；`LoadingIndicator`（新 API，替代旧的 `CircularProgressIndicator` 进度用法，用于 XposedFragment 加载对话框、Home 状态卡）；`TooltipBox`（长按说明）；`VerticalScrollableTabRow` 备用于未来侧边 Tab 场景。
- `themes.xml` 仅保留窗口级最小主题（透明状态栏 + WindowAnimation），颜色全部由 Compose 接管；CropImageActivity 在过渡期继续引用旧主题（见 §11）。

## 6. 存储响应式适配层（PrefState）

新建 `utils/PrefState.kt`：SharedPreferences 的 Compose 友好封装，**写路径与现有一致**（hook 读到的值不变）：

```kotlin
class PrefState(private val prefs: SharedPreferences) : Closeable {
    private val listeners = CopyOnWriteArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    fun stringFlow(key: String, default: String = ""): StateFlow<String> { /* listener + distinctUntilChanged */ }
    fun boolFlow(key: String, default: Boolean = false): StateFlow<Boolean>
    fun intFlow(key: String, default: Int = 0): StateFlow<Int>
    fun stringSetFlow(key: String, default: Set<String> = emptySet()): StateFlow<Set<String>>

    fun set(key: String, value: Any) { /* 与 context.putXxx 等价写入 */ }
}
```

- 每个偏好文件一个实例：`ModulePrefState` / `SettingsPrefState` / `IntentPrefState` / `OtherPrefState`（对应现有 `ModulePrefs/SettingsPrefs/...` 常量），挂在 `remember { }` 或 ViewModel 中。
- `RemotePreferenceDataStore` 删除；`FuncUtils` 中依赖 `EditTextPreference/ListPreference` 的辅助函数（`FuncUtils.kt:53-55`）改写为 DSL 辅助函数。
- 这层解决了现有体系的痛点：**条件可见性、summary 联动全部变成响应式**（原来改一个值不会自动刷新同页其他项）。

## 7. 导航迁移（类型安全路由）

新建 `ui/navigation/`：

```kotlin
@Serializable data object HomeRoute
@Serializable data object OtherRoute
@Serializable data object FunctionRoute
@Serializable data object LogRoute
@Serializable data object SettingRoute
@Serializable data class ScopeRoute(val scopeId: String, val scrollKey: String? = null, val scrollPosition: Int = 0)
@Serializable data object MemcConfigRoute
@Serializable data object DonateRoute
// ... ZoomWindowRoute / MultiAppRoute / ForceFpsRoute / DarkModeRoute / HideAppIntentRoute
```

- `ScopeRegistry`（单例注册表）：`scopeId → ScopeSpec(titleRes, iconRes, @Composable () -> Unit)`。85 个作用域页统一注册，`title_text` 参数与 `Bundle` 彻底消失；搜索命中页通过 `ScopeRoute(scopeId, key, position)` 跳转。
- 顶级 5 Tab：`NavigationBar` + `NavHost`，`navController.navigate(route) { popUpTo(graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`（官方推荐模式）。
- 可选增强：`material3-adaptive` 的 `NavigationSuiteScaffold`，自动适配大屏（侧边 Rail/抽屉）——模块以手机为主，作为二期可选项。

## 8. 偏好页 DSL（本方案核心，决定 85 页迁移成本）

### 8.1 设计目标

把 `BaseScopePreferenceFeagment` 的"程序化构建 + 搜索索引 + 滚动高亮 + 条件可见性 + 变更回调"整合同化为一个可组合 DSL，使每个作用域页从 300+ 行 Fragment 变为 ~100 行声明式代码，且**搜索索引自动生成**。

### 8.2 API 草案

```kotlin
// ui/components/preference/PrefScope.kt
class PrefScope internal constructor(
    private val state: PrefState,
    private val index: MutableList<PrefIndexItem>,      // 搜索索引自动收集
    private val sendValue: (key: String, value: Any) -> Unit,  // sendPrefsValue 回调
) {
    fun category(title: String)
    fun switch(key: String, title: String, summary: String? = null,
               icon: Painter? = null, onChange: ((Boolean) -> Unit)? = null)
    fun list(key: String, title: String, entries: Array<String>, entryValues: Array<String>,
             summaryFormat: String = "%s", onChange: ((String) -> Unit)? = null)
    fun slider(key: String, title: String, valueRange: IntRange, step: Int = 1,
               summaryFormat: String = "%s", onChange: ((Int) -> Unit)? = null)
    fun editText(key: String, title: String, dialogTitle: String,
                 keyboardType: KeyboardType = KeyboardType.Text, onChange: ((String) -> Unit)? = null)
    fun page(title: String, summary: String?, icon: Painter?, scopeId: String)  // 分类入口
    fun custom(key: String, title: String) { /* 扩展点：ColorPicker 等 */ }
}

data class PrefIndexItem(          // 替代 PrefsItem.kt 的 Serializable 数据类
    val key: String?, val title: String?, val summary: String?,
    val isVisible: () -> Boolean, val scopeId: String, val position: Int,
)

@Composable
fun ScopeScreen(
    scopeId: String,
    title: String,
    prefs: PrefState,
    scrollTarget: ScrollTarget?,          // (key, position) 用于搜索跳转高亮
    actions: @Composable RowScope.() -> Unit = {},   // TopAppBar actions：重启作用域/打开菜单
    content: PrefScope.() -> Unit,
)
```

### 8.3 迁移对照（StatusBarClock 示例）

现有（View）：`class StatusBarClock : BaseScopePreferenceFeagment()`，`loadPreferences()` 里 `if (getString(...) == "...") { add(DropDownPreference(...).apply { setEntries/setSummaryProvider/setOnPreferenceChangeListener { sendPrefsValue(...) } }) }`。

迁移后（Compose）：

```kotlin
@Composable
fun StatusBarClockScreen(scrollTarget: ScrollTarget?, onRestart: () -> Unit) = ScopeScreen(
    scopeId = "status_bar_clock", title = stringResource(R.string.status_bar_clock),
    prefs = remember { ModulePrefState }, scrollTarget = scrollTarget,
    actions = { IconButton(onClick = onRestart) { Icon(Icons.Default.Refresh, null) } },
) {
    category("时钟")
    switch(keyClockEnabled, getString(R.string.clock_enable), onChange = { sendPrefsValue(SYSTEMUI, keyClockEnabled, it) })
    val style = state.stringFlow(keyClockStyle).collectAsStateWithLifecycle()
    if (style.value == "custom") {          // ← 条件可见性天然响应式
        list(keyClockFormat, ..., onChange = { sendPrefsValue(SYSTEMUI, keyClockFormat, it); onRestart() })
        slider(keyClockSize, 10..30, summaryFormat = "%s dp", ...)
        editText(keyClockText, ...)
    }
}
```

迁移规则（机械可执行，可脚本辅助）：

| 旧 API | 新 API |
|---|---|
| `DropDownPreference + setEntries/entryValues` | `PrefScope.list(...)` |
| `SwitchPreference` | `PrefScope.switch(...)` |
| `SeekBarPreference + min/max` | `PrefScope.slider(...)` |
| `EditTextPreference` | `PrefScope.editText(...)` + M3 `AlertDialog` + `OutlinedTextField` |
| `PreferenceCategory` | `PrefScope.category(...)` |
| `if (getXxx(...)==...) add(...)` 条件块 | 条件 `if` 包住 DSL 调用（响应式） |
| `setSummaryProvider` | `summaryFormat` 或 `summary` lambda |
| `setOnPreferenceChangeListener { sendPrefsValue(...) }` | `onChange` lambda（内容不变） |
| `setDefaultValue` | `PrefState` 默认值参数 |
| `getAllPrefsItem()` 搜索索引 | DSL 运行时自动收集 `PrefIndexItem` |
| `scrollKey/scrollPosition + PreferencePositionCallback + forceRippleAnimation` | `LazyListState.animateScrollToItem(position)` + 目标项背景色脉冲动画（`animateColorAsState`） |
| 菜单 1=重启作用域 / 2=打开 | TopAppBar `actions` 插槽 |

- **搜索**（XposedFragment 功能）：改为 Compose 全屏搜索页（`SearchBar` + `LazyColumn`），索引由 `ScopeRegistry` 惰性构建（首次在 IO 协程收集全部 DSL 页的 `PrefIndexItem`，缓存），过滤逻辑平移自 `SearchResultAdapter.getFilter`。
- 高亮：`ScopeScreen` 收到 `scrollTarget` 后 `LaunchedEffect { listState.animateScrollToItem(target.position) }` + 3 秒背景色渐隐。

## 9. 组件映射总表（View → Compose）

| 现状（View） | 迁移目标 |
|---|---|
| `BottomNavigationView` (LABEL_VISIBILITY_SELECTED) | M3 `NavigationBar`（或 `NavigationSuiteScaffold`） |
| `Toolbar + setupActionBarWithNavController` | `CenterAlignedTopAppBar`/`TopAppBar` + `currentBackStackEntryAsState` 取标题 |
| `PreferenceFragmentCompat` 全家 | `PrefScope` DSL（§8） |
| `RecyclerView + bindAdapter`（9 个列表页/选择器） | `LazyColumn`/`LazyVerticalGrid` + `items(key = ...)` |
| `ViewPager2 + TabLayoutMediator`（MemcConfig） | `HorizontalPager + rememberPagerState` + `PrimaryTabRow`（2 Tab 场景） |
| `SwipeRefreshLayout` | M3 `PullToRefreshBox`（material3 1.3+，官方新组件） |
| `FastScrollerBuilder(useMd2Style)` | 删除：`LazyColumn` + `stickyHeader` 分组即可；如确需拖动条，二期自研 `LazyListState` 拖动条 |
| `MaterialAlertDialogBuilder.setView` | M3 `AlertDialog`/`BasicAlertDialog` composable |
| `showBottomSheet` 版本信息 | `ModalBottomSheet` |
| `MenuProvider + MenuItem` | TopAppBar `actions` / `DropdownMenu` |
| `Markwon`（版本信息 Markdown 表格） | Compose 原生 `Column/Row` 渲染表格（数据是程序生成的，无需 Markdown 引擎），删除 markwon |
| `Glide`（零调用） | 删除；后续有图用 Coil3 `AsyncImage` |
| `EditText.addTextChangedListener` 过滤 | `TextField` + `remember` 派生 `filtered = remember(query, items) { ... }` |
| `ColorPickerPreference`（自定义 View） | Compose 色板选择器（§11.2），colorpicker 模块 View 类删除 |
| `CropImageActivity`（canhub cropper） | 见 §11.3：保留为 View 孤岛，ActivityResultContract 不变 |
| `ActivityLifecycleManager` | `LifecycleEventObserver` + `repeatOnLifecycle` / `LaunchedEffect` |
| `MainActivity.restart()` recreate | 保留 recreate 机制；状态恢复由 `rememberSaveable`/`SavedStateHandle` 承担（Navigation Compose 自动支持） |
| `AppInfoSelectDialog` 等 3 选择器 | 通用 `AppPickerDialog(onSelect: (AppInfo) -> Unit)`，三处复用 |
| `DonateUtils` 弹窗 | Compose `Dialog` 页面化（`DonateRoute`） |
| `ShortcutActivity`（QS_TILE_PREFERENCES 透传壳） | 独立 Compose Activity，逻辑不变 |

## 10. 依赖清理中的 hook 层改写（唯一允许触碰 hook 文件的地方）

`androidx.preference` 在 hook 层约 5 处调用（`EnableCameraDebugUIOption.kt:83`、`HookADM.kt:34` 等），本质只是拿默认 SharedPreferences：

```kotlin
// 改前
PreferenceManager.getDefaultSharedPreferences(activity)
// 改后（等价、无 androidx.preference 依赖）
activity.getSharedPreferences("${activity.packageName}_preferences", Context.MODE_PRIVATE)
```

完成后 `androidx.preference` 依赖可整体移除（colorpicker View 类同步删除后无残留引用）。**其余 hook 文件一律不动。**

## 11. 特殊页面专项

### 11.1 MemcConfigFragment（列表页标杆）
- 外层：`HorizontalPager` 2 页（Package/Activity）+ `PrimaryTabRow` 或 `TabRow`；`SwipeRefreshLayout`→`PullToRefreshBox`；`bindAdapter`→`LazyColumn`；Import Xml / Reset 菜单 → TopAppBar actions（`rememberLauncherForActivityResult(GetContent("text/xml"))` 替换 `ActivityResultContracts.GetContent` 的 register 回调）。
- 搜索：`TextField` + 派生过滤（原 `addTextChangedListener`）。
- 编辑对话框：`AlertDialog` 内嵌 `AppPickerDialog`；`MemcCallback` object 回调 → 对侧 tab 共享一个 `ViewModel`（或 `mutableStateOf` 提升到外层），替代全局 object 回调。
- 数据读写（`memcConfigPackageList`/`memcConfigActivityList` 的 StringSet+JSON）经 `PrefState.stringSetFlow`，写路径不变。

### 11.2 ColorPicker
仅 2 处使用（`StatusBarControlCenter.kt:293`、`SoundRelated.kt:143`）。方案：在 app 内实现 `ColorPickerDialog` composable（渐变面板 + HSV 滑杆 + 预设色），作为 `PrefScope.custom` 扩展点；删除 `colorpicker` 模块的 View 实现（`ColorPickerPreference`/`ColorPickerDialog`/`ColorGradientView`）与模块本身（或保留模块壳改造成 compose 库，二选一，推荐并入 app 删除模块）。

### 11.3 CropImageActivity
canhub cropper 是活跃维护的 View 库，**保留为 View 孤岛**（独立 Activity，不进 Compose 导航，`CropImageContract` 签名不变，`FingerPrintRelated`/`OplusSettings` 的调用零改动）。这是标准绞杀者策略：叶子组件不迁移。主题继续使用旧 Material3 主题（`themes.xml` 保留窗口样式）。待官方/社区出现成熟 Compose 裁剪库再行替换（非必要不引入）。

### 11.4 其他列表页
ZoomWindow / MultiApp / ForceFps / DarkMode / HideAppIntent / BatteryInfo / ExtractOTA：统一走「`LazyColumn` + 通用 AppPicker + `PrefState`」模板，`bindAdapter` 的 item 绑定函数直接翻译为 `@Composable AppRow(...)`。

### 11.5 HomeFragment / OtherFragment / SettingsFragment / QuickEntryFragment / LoggerFragment
- Home：状态卡 + 版本信息 → `Card`/`ListItem` 组合；菜单（重启/关于）→ TopAppBar actions；OTA 复制、捐赠入口 → 按钮。
- Other：快捷键多选对话框 → `AlertDialog` + `Checkbox` 列表；ADB 对话框（`DialogAdbLayoutBinding`）→ Compose Dialog；快捷入口点击 → `navigate(ScopeRoute(...))`。
- Settings/QuickEntry：直接走 `PrefScope` DSL（SettingsFragment 的 DropDown/Switch 照 §8 映射）。
- Logger：菜单 4 项 → TopAppBar actions；日志列表 → `LazyColumn`（`FragmentLogsBinding` 的实际内容组件在迁移时同步翻译）；保存/分享沿用 FileProvider。

## 12. 分阶段实施路线（绞杀者模式，每阶段可编译、可运行、可回退）

| 阶段 | 内容 | 产出/验收 |
|---|---|---|
| **P0 基建**（2-3 天） | §4 依赖接入；`LuckyTheme`；`PrefState` + 单测；`PrefScope` DSL + `ScopeScreen`；通用 `AppPickerDialog` | 新代码编译通过；`PrefState` 单测覆盖 listener 与写路径；UI 未切换，旧版功能零回归 |
| **P1 试点**（2-3 天） | 选 2 个作用域页（建议 `StatusBarClock` + `StatusBarRelated`）做成 Compose 页，以 `ComposeView` 壳 Fragment 挂进旧导航图 | 试点页功能与旧版逐项比对（含条件可见性/搜索索引/高亮跳转）；**release 包验证 lsparanoid + Compose 兼容性（关键风险，见 §14）** |
| **P2 主壳翻转**（3-5 天） | `MainActivity` → `ComponentActivity + setContent`（onCreate 的生物识别/Xposed 检测/版本检查逻辑保持在前，`onResume` 的 checkSu/initAllService 移入 `repeatOnLifecycle`）；`Scaffold + NavigationBar + NavHost`；Home/Other/Log/Setting 四页全量 Compose 化；功能页（Function 树）暂以 `AndroidView` 承载旧 `NavHostFragment` 子树过渡 | 主界面 5 Tab 可用；四页功能等价；Function 树旧版可用 |
| **P3 批量迁移作用域页**（核心，2-3 周） | 按 `apps → statusbar → related → others` 顺序，85 页按 §8.3 规则机械化迁移并注册进 `ScopeRegistry`；新 Compose 搜索页上线；删除旧导航图、`BaseScopePreferenceFeagment`、`PrefsItem`、`RemotePreferenceDataStore`、`FuncUtils` 偏好辅助 | 每批迁移后对每页执行核对清单（key 列表、默认值、条件可见性、summary、onChange 行为与旧版 diff）；搜索全量命中一致 |
| **P4 特殊页**（1 周） | §11：MemcConfig、4 个列表页、ColorPicker、捐赠、版本信息 ModalBottomSheet、QuickEntry、ShortcutActivity | 各页功能等价；colorpicker 模块删除 |
| **P5 清理**（2-3 天） | 删除 52 个 XML 布局、ViewBinding、safe-args 插件、material/constraintlayout/preference/swiperefresh/hikage/glide/markwon/fastscroll；hook 层 5 处 PreferenceManager 改写；`ThemeUtils` 精简；betterandroid 的 AppBindingActivity/Fragment 基类引用清除（库保留） | 依赖树无残留 View UI 依赖；APK 体积对比报告；lsparanoid 混淆正常 |
| **P6 打磨**（可选） | Expressive 动效调优、`NavigationSuiteScaffold` 自适应、性能剖析（首屏、搜索索引构建）、`@Preview` 覆盖 | 体验验收 |

**关键顺序说明**：P1 试点必须跑在 P2/P3 之前，因为 lsparanoid（仅 release 生效）与 Compose 的兼容性只有 release 包才能验证——若出问题，影响面仅为 2 页试点。

## 13. 风险与对策

| 风险 | 对策 |
|---|---|
| **lsparanoid 字符串加密 + Compose 合成类在 release 下异常**（lsparanoid 针对标注 `@Obfuscate` 的类做字符串加密，Compose 生成的 lambda/合成类不受注解覆盖，存在已知社区兼容问题） | P1 即用 release 试点验证；若异常：Compose UI 类移除 `@Obfuscate` 标注（UI 层无敏感字符串，hook 层保持混淆不变），或对 UI 包配置 lsparanoid 排除。**先试点后铺开，绝不在 P3 批量迁移前才首次打 release 包** |
| 85 页机械迁移引入回归 | 每页迁移前保存"旧版行为快照"（key/默认值/条件），迁移后 diff 核对；分 4 批提交，每批独立回归 |
| 包体积增长 | P5 输出 APK 对比；`resopt` 继续生效；图标先复用现有 `R.drawable` vector（`painterResource`），不引入 material-icons-extended |
| 动态取色在 ColorOS 上可用性差异 | 与现状行为一致（已用 `isDynamicColorAvailable()` 判断）；无新增风险 |
| 双导航共存期（P2）的 `findNavController` 调用 | Function 子树留在旧 `NavHostFragment` 内，调用点无需改；过渡期结束后随 P3 一并消除 |
| recreate 重启逻辑与状态 | Navigation Compose 自带 `rememberSaveable`/`SavedStateHandle` 恢复，`restart()` 保留 recreate 语义不变 |
| 性能（搜索索引 85 页收集） | 索引惰性构建于 IO 协程并缓存；LazyColumn 默认惰性渲染无压力 |

## 14. 工作量估算（单人）

- P0–P1：约 5 人日
- P2：约 4 人日
- P3：约 15 人日（85 页 × ~0.15 人日，机械转换 + 核对）
- P4：约 5 人日
- P5：约 2 人日
- **合计约 5~6 周**（含回归与 release 验证）；P6 可选。

## 15. 验收标准（迁移完成定义）

1. `app/src/main/res/layout` 目录仅剩 CropImageActivity 所需布局（或 0）；`viewBinding`、`safe-args` 移除。
2. 依赖树中不再出现 `androidx.preference`、`androidx.constraintlayout`、`com.google.android.material`（Components）、`hikage`、`markwon`、`glide`、`fastscroll`、`swiperefreshlayout`。
3. 全部 85 个作用域页 + 5 顶级页功能与旧版逐项等价（核对清单归档）。
4. hook 层除 §10 的 5 处存储读取改写外无其他变更；模块在目标 App 内的注入行为不变（回归注入测试）。
5. release 包通过 lsparanoid 混淆验证，无 Compose 相关崩溃。
6. 主题三态（跟随/夜间/白天）+ 动态取色开关行为与旧版一致；Material3 Expressive 主题（MotionScheme）生效。
