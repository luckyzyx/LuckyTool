<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README.md">简体中文</a> 丨
  <b>English</b> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_ZH_TW.md">繁體中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_JA.md">日本語</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_RU.md">Русский</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_VI.md">Tiếng Việt</a>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>Extended and optimized Xposed module for ColorOS</p>
<p>Free forever module — any paid channel is unrelated to the author</p>
<p>Redirecting traffic, reposting, reprinting, selling or re-uploading without permission is prohibited</p>
<p>Share the know-how rather than the finished result</p>
</div>

---

## Overview

LuckyTool is an Xposed module running on the [LSPosed](https://github.com/LSPosed/LSPosed) framework. It targets
  **ColorOS** devices and adds dozens of enhancements to the system framework, SystemUI, the launcher, Settings and more than 40 system apps. Switches are synced to the system processes and take effect immediately — no reboot required.

The full manual lives on the [documentation site](https://luckyzyx.gitlab.io/LuckyTool_Doc/).

## Features

| Area                                | Highlights                                                                                                                                                                                      |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| System framework                    | custom volume steps, forced split-screen, 32-bit app support, battery-optimization whitelist, app keep-alive, risky-app check bypass, USB install without confirmation, and more                |
| SystemUI / status bar / lock screen | clock and network-speed styles, icon visibility, Control Center tiles and transparency, notification styles, lock-screen clock and charging widget, fingerprint icon, and more                  |
| Launcher                            | Dock background and blur, grid rows and columns, folder name limit, Dock icon count, recents behaviour, badge removal, and more                                                                 |
| Settings & system apps              | DPI change without reboot, dark-mode app list, per-app language, disabling system apps, plus enhancements for Camera, Gallery, Game Assistant, Theme Store, Cloud Service and 40+ apps in total |
| Real-time                           | switch changes are synced to the system processes and applied without rebooting                                                                                                                 |
| Multi-version                       | implementations are matched automatically to your ColorOS version (12 – 16), tracking new releases                                                                                              |

The complete list is on the documentation site ([feature list](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)); the switches inside the app are authoritative.

## Requirements

| Item             | Requirement                                   |
|------------------|-----------------------------------------------|
| System           | ColorOS 12 or above (OPPO / OnePlus / realme) |
| Adapted versions | ColorOS 12 – 17, tracking new releases        |
| Framework        | LSPosed                                       |
| Permissions      | Root is required for some features            |
| Device ABI       | arm64-v8a                                     |
| Tested device    | OnePlus 15                                    |

## Download

Download the latest
  `LuckyTool_v*.apk` from [Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) and install it normally.

File names look like `LuckyTool_v<versionName>(<buildNumber>)_<buildType>.apk`. The number in parentheses is the build counter that identifies the exact build, so please include it when reporting an issue.

## Installation & activation

1. Download and install the module APK.
2. Open the LSPosed manager → **Modules** → enable LuckyTool.
3. Under **Scope**, tick the system apps to hook. Selecting all is recommended: the module skips the logic that does not apply to each package.
4. Reboot the device (or at least restart the affected scopes).
5. Open LuckyTool and turn on the master switch at the top right of the home screen.

## Troubleshooting & FAQ

- **Nothing works after a system update**: use **Re-optimize Dex** in the module's reboot menu, or long-press the scope app in LSPosed and
  re-optimize it. For example, if the developer-options notification cannot be removed, re-optimize System UI.
- **The module does not work at all**: make sure it is enabled in LSPosed, the target app is inside the scope, no `/sdcard/disable_lt` is left over, and the
  LSPosed log shows LuckyTool being loaded into the host process.
- **Some switches have no effect**: features are branched per ColorOS version, so after a major system upgrade you have to wait for the module to adapt. A switch that is not shown in the app is not supported on your version yet.
- More questions are answered in the [FAQ](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq);
  for feedback use [Telegram](https://t.me/LuckyTool).

## Links

| Item                                 | Link                                                          |
|--------------------------------------|---------------------------------------------------------------|
| Documentation                        | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| Changelog                            | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| Module repo (LSPosed Repo, releases) | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| Feedback                             | https://t.me/LuckyTool                                        |
| Xposed API                           | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| License                              | [GPL-3.0](./LICENSE.txt)                                      |

## Disclaimer

- The official repository is [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool)
  and keeps the complete commit history; anything released under this project's name elsewhere has nothing to do with the author.
- The author has a day job and does not live off this module. It is open-sourced under GPL-3.0 as an entry point for module development.

## Star History

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>
