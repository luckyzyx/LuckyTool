<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <b>简体中文</b> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_EN.md">English</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_ZH_TW.md">繁體中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_JA.md">日本語</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_RU.md">Русский</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_VI.md">Tiếng Việt</a>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-频道-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>对ColorOS系统进行的扩展优化的Xposed模块</p>
<p>永久免费模块，请勿上当受骗</p>
<p>禁止引流、搬运、转载、售卖、分享、分流</p>
<p>提倡授之以渔，切莫授之以鱼</p>
</div>

---

## 简介

LuckyTool 是一款运行在 [LSPosed](https://github.com/LSPosed/LSPosed) 框架上的 Xposed 模块，面向
**ColorOS** 设备，为系统框架、SystemUI、桌面、设置以及 40 多个系统应用提供数十个功能增强。开关改动会同步到系统进程实时生效，无需重启。

完整使用说明见[文档站](https://luckyzyx.gitlab.io/LuckyTool_Doc/)。

## 功能特性

| 模块                     | 主要功能                                                                                                            |
|--------------------------|---------------------------------------------------------------------------------------------------------------------|
| 系统框架                 | 音量阶数、强制分屏、32 位应用支持、电池优化白名单、应用保活、风险应用拦截、USB 安装免确认等                         |
| SystemUI / 状态栏 / 锁屏 | 时钟与网速样式、图标显隐、控制中心磁贴与透明度、通知样式、锁屏时钟与充电组件、指纹图标等                            |
| 桌面                     | Dock 背景与模糊、网格行列、文件夹命名限制、Dock 图标数量、最近任务、应用角标移除等                                  |
| 设置与系统应用           | DPI 免重启、暗色模式列表、应用专属语言、停用系统应用，以及相机 / 相册 / 游戏助手 / 主题商店 / 云服务等 40+ 应用增强 |
| 实时生效                 | 开关改动实时同步到系统进程，无需重启                                                                                |
| 多版本适配               | 按 ColorOS 版本（12 ~ 16）自动匹配实现，持续跟进新版本                                                              |

完整清单见文档站[功能列表](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)（以 App 内实际开关为准）。

## 支持环境

| 项目     | 要求                                      |
|----------|-------------------------------------------|
| 系统     | ColorOS 12 及以上（OPPO / 一加 / realme） |
| 适配版本 | ColorOS 12 ~ 17，持续跟进新版本           |
| 框架     | LSPosed                                   |
| 权限     | 部分功能需要 Root                         |
| 设备架构 | arm64-v8a                                 |
| 测试机型 | OnePlus 15                                |

## 下载

从 [Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) 下载最新版
`LuckyTool_v*.apk` 并直接安装。

文件名形如 `LuckyTool_v<版本名>(<编译号>)_<构建类型>.apk`，括号中的数字是编译序号，用于定位出问题的具体构建，反馈时可一并附上。

## 安装与激活

1. 下载并安装模块 APK。
2. 打开 LSPosed 管理器 → **模块** → 启用 LuckyTool。
3. 在「作用域」中勾选需要 Hook 的系统应用：推荐全选，模块会对每个包自适应跳过不适用的逻辑。
4. 重启设备（或至少重启对应作用域）。
5. 打开 LuckyTool，在主页右上角打开「模块开关」总开关。

## 排障与常见问题

- **升级系统后无报错但不生效**：使用模块重启菜单里的「优化 Dex」，或在 LSPosed 中长按对应作用域 App
  点击重新优化。例如开发者选项通知无法移除时，重新优化系统界面即可。
- **模块完全不生效**：确认 LSPosed 中已启用、目标 App 在作用域内、没有残留的 `/sdcard/disable_lt`，并在
  LSPosed 日志中确认 LuckyTool 已注入宿主进程。
- **部分开关无效**：功能按 ColorOS 版本分支实现，系统大版本升级后需等待模块适配；App 内没有出现的开关即当前版本尚未支持。
- 更多问题见文档站[常见问题](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq)
  ，反馈请前往 [Telegram](https://t.me/LuckyTool)。

## 相关链接

| 内容                                 | 链接                                                          |
|--------------------------------------|---------------------------------------------------------------|
| 使用文档                             | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| 更新日志                             | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| 模块仓库（LSPosed Repo，发布与下载） | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| 反馈交流                             | https://t.me/LuckyTool                                        |
| Xposed API                           | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| 开源协议                             | [GPL-3.0](./LICENSE.txt)                                      |

## 声明

- 官方仓库为 [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool)
  ，完整提交记录保留于此；以本项目名义出现的其它内容与作者无关。
- 作者有本职工作，不靠模块盈利；现以 GPL-3.0 开源，供模块开发入门参考。

## Star History

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>