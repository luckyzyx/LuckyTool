---
name: luckytool-coui
description: "LuckyTool 接入 COUI-Kit 的约定：JitPack 渠道与版本、主题接线（显式 CouiTheme + 双向色板翻译，禁用 Coui.initialize/CouiMaterial3Style）、SDK 门槛、背景源、浮层规则、已知红测基线。修改 COUI 相关代码或升级依赖前加载。"
---

> 同步：本文件与 DSH 本地技能 `luckytool-coui`（$DSH_HOME/skills/luckytool-coui/SKILL.md）双份保存，改动须同时更新两份。

# LuckyTool × COUI-Kit 接入约定

本技能记录 LuckyTool 项目接入 COUI-Kit 的既定方案与红线。修改 COUI 相关代码（主题、脚手架、导航栏、弹层、依赖升级）前先读本技能；与项目 `AGENTS.md` 的通用约定（hook 拦截器、KavaRef、DexKit、versionCode、提交风格）配合使用，不重复其内容。

## 依赖渠道与版本（硬事实）
- 坐标走 JitPack：`com.github.itoseo.coui-kit`（coui / coui-material3 / coui-icons 三件套同版本）。
- 当前版本：`gradle/libs.versions.toml` 中 `coui = "0.0.1-alpha21"`。
- 不要回退到 Maven Central 的 `io.github.itoseo:coui-*`（该渠道止步 alpha17）。
- 升级流程：先查 JitPack 构建状态与 GitHub tag diff（Release 未必有，以 tag 为准），再改 toml，然后一次 gradle 跑完验证（见「构建与红测」）。

## 主题接线：保留当前方案，禁止改成全局 M3 适配
- 禁用 `Coui.initialize(defaultStyle = CouiMaterial3Style())`：进程级一次性（重复调用抛「Coui 已初始化」），与 ui_mode 运行时可切换 COUI/Material 两条线的需求冲突。
- 禁用 `CouiMaterial3Style` 的同名字段直取映射：M3 基线里 background == surface == surfaceBright 同一档，映射后 COUI 页面底/卡片/菜单融成一片，分组卡片撞色看不见。
- 正确做法：每次组合显式 `CouiTheme(darkTheme, colorScheme, typography, materialTint)` 包局部子树；库发现 LocalCouiTheme 非空时不会读全局 defaultStyle。
- 色板统一从 `app/src/main/java/com/luckyzyx/luckytool/ui/theme/CouiPalette.kt` 取：
  - `couiPresetColorScheme`（:20-24）：取色开关关闭时用 COUI 预设；
  - `couiColorScheme`（:39-74）：M3→COUI 翻译，用容器阶拉开层次（浅色页 surfaceContainer / 卡 surfaceContainerLow / 弹层 surfaceContainerHigh；深色整体上移一档；AMOLED 纯黑底 + 半透明白卡）；
  - `couiMaterialColorScheme`（:81-111）：COUI→M3 反推，给 COUI 线内由 material3 渲染的部分（弹层/底部弹窗/输入框/滑块）同源强调色。
- `app/src/main/java/com/luckyzyx/luckytool/ui/theme/CouiAppTheme.kt:69` `materialTint = couiColors.card`：顶栏/底栏/弹层材质染色跟随卡片色，不要改回 surface。
- 改动主题接线前重读 `app/src/main/java/com/luckyzyx/luckytool/ui/theme/CouiAppTheme.kt:19-29` 的取舍注释，不要推翻已决策的结论。

## SDK 门槛（红线 1）
库 minSdk=31，调用前判 `Build.VERSION.SDK_INT`：
- 34+（UPSIDE_DOWN_CAKE）才可用：NavigationBar / Fab / TopAppBarButton / TopAppBarSearch / ChipTab / ChipTabRow / SegmentedControl / MessageInputBar / couiShadow / CouiFloatingRenderer / CouiShadowRenderer。
- 33+（TIRAMISU）才可用：RuntimeShader 模糊、轮廓光、CouiMetaballRenderer、弹窗面板材质。
- 项目 COUI 外观线整体门槛在 `app/src/main/java/com/luckyzyx/luckytool/ui/theme/UiMode.kt:22`：`isAvailable = this != Coui || SDK_INT >= UPSIDE_DOWN_CAKE`，新增 COUI 入口沿用同一判定。

## 背景源（红线 2）
- 正文源：COUI Scaffold 自动提供（项目用 `CouiPageScaffold`），或 `rememberCouiBackdrop()`；窗口源：根部 `ProvideCouiMenuBackdrop`。
- 禁止自采样背景；API33+ 弹层用窗口源时传 material + backdrop。

## 浮层与 Renderer（红线 5/6）
- 独立 Renderer 禁止跨节点共享；需要同一成员作用于多个实例时 `copy()` 重新绑，与 KavaRef 约定一致。
- 浮层传 `visible` 常驻，禁止外包 `if (visible)` 条件组合。

## 已知红测基线（不要当回归）
- `com.luckyzyx.luckytool.ui.compose.PageTopAppBarTest` 的 4 个 Coui 折叠标题栏用例在 Robolectric 下稳定失败（alpha16/18/19 均失败）：错误 `cannot swizzle value of type 'shader'` 或 `'color' is not a valid layout qualifier`，根因是 Robolectric 的 RuntimeShader 解析器不支持 COUI TopAppBarSurface 的 Compose 模糊 shader。
- 真机不受影响。全量测试预期 213 tests / 4 failed；出现新增失败才需要排查。

## 构建与提交
- 任何 gradle 任务都会让 `app/version.properties` 的 versionCode +1（`app/build.gradle.kts:28` 配置阶段自增，改写逻辑在 :191-201），验证时能一次跑完就别拆多次。
- 升级/修改 COUI 相关代码：跑 `./gradlew.bat :app:testDebugUnitTest --console=plain -q` 一次验证；静态检查用 `:app:compileDebugKotlin`。
- 提交遵循项目 `AGENTS.md`：conventional 前缀 + 双语 message，一次提交一个逻辑改动；versionCode 变更随代码提交、不单独提交。

## 关键文件地图
- `gradle/libs.versions.toml` — coui 版本与坐标。
- `app/src/main/java/com/luckyzyx/luckytool/ui/theme/CouiAppTheme.kt` — 主题入口与取舍注释。
- `app/src/main/java/com/luckyzyx/luckytool/ui/theme/CouiPalette.kt` — 三套色板翻译。
- `app/src/main/java/com/luckyzyx/luckytool/ui/theme/UiMode.kt` — 外观线开关与 SDK 门槛。
- `app/src/main/java/com/luckyzyx/luckytool/ui/compose/MainShell.kt:438` — NavigationBar 唯一调用点（只传 modifier/renderer/items/selectedIndex/dark/onSelection）。