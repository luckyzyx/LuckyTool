<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README.md">简体中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_EN.md">English</a> 丨
  <b>繁體中文</b> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_JA.md">日本語</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_RU.md">Русский</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_VI.md">Tiếng Việt</a>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>對 ColorOS 系統進行擴展與最佳化的 Xposed 模組</p>
<p>永久免費模組，任何付費管道都與作者無關</p>
<p>未經授權禁止導流、轉載、轉貼、販售、分享或重新上傳</p>
<p>請分享做法，而不是現成的成果</p>
</div>

---

## 概觀

LuckyTool 是執行於 [LSPosed](https://github.com/LSPosed/LSPosed) 框架上的 Xposed 模組。它針對
  **ColorOS** 裝置，為系統框架、SystemUI、啟動器、設定以及超過 40 個系統應用程式加入數十項增強功能。開關會同步至系統行程並立即生效 — 不需要重新開機。

完整的使用手冊位於[說明文件網站](https://luckyzyx.gitlab.io/LuckyTool_Doc/)。

## 功能

| 領域                         | 重點功能                                                                                                                                                 |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 系統框架                     | 自訂音量級數、強制分割畫面、支援 32 位元應用程式、電池最佳化白名單、應用程式常駐、略過風險應用程式檢查、USB 安裝免確認等                                 |
| SystemUI / 狀態列 / 鎖定畫面 | 時鐘與網路速度樣式、圖示顯示與否、控制中心磚與透明度、通知樣式、鎖定畫面時鐘與充電小工具、指紋圖示等                                                     |
| 啟動器                       | Dock 背景與模糊效果、網格列數與欄數、資料夾名稱長度限制、Dock 圖示數量、最近使用應用程式行為、移除標記等                                                 |
| 設定與系統應用程式           | 免重新開機變更 DPI、深色模式應用程式清單、個別應用程式語言、停用系統應用程式，以及相機、相簿、遊戲助手、主題商店、雲端服務等共 40 多個應用程式的增強功能 |
| 即時生效                     | 開關變更會同步至系統行程並套用，不需要重新開機                                                                                                           |
| 多版本支援                   | 會依您的 ColorOS 版本（12 – 16）自動比對實作，並持續跟進新版本                                                                                           |

完整清單請見說明文件網站的[功能清單](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)；實際以應用程式內的開關為準。

## 需求

| 項目       | 需求                                       |
|------------|--------------------------------------------|
| 系統       | ColorOS 12 以上（OPPO / OnePlus / realme） |
| 已適配版本 | ColorOS 12 – 17，並持續跟進新版本          |
| 框架       | LSPosed                                    |
| 權限       | 部分功能需要 Root                          |
| 裝置 ABI   | arm64-v8a                                  |
| 測試機型   | OnePlus 15                                 |

## 下載

從 [Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) 下載最新的
  `LuckyTool_v*.apk` 並直接安裝即可。

檔案名稱格式為 `LuckyTool_v<versionName>(<buildNumber>)_<buildType>.apk`。括號中的數字是建置次數計數器，可用來識別確切的建置版本，因此回報問題時請一併附上。

## 安裝與啟用

1. 下載並安裝模組 APK。
2. 開啟 LSPosed 管理器 → **模組** → 啟用 LuckyTool。
3. 在**作用域**中勾選要 Hook 的系統應用程式。建議全選：模組會略過不適用於各套件的邏輯。
4. 重新啟動裝置（或至少重新啟動受影響的作用域）。
5. 開啟 LuckyTool，並打開首頁右上角的主開關。

## 疑難排解與常見問題

- **系統更新後完全失效**：使用模組重新開機選單中的 **重新最佳化 Dex**，或在 LSPosed 中長按作用域應用程式並
  重新最佳化。例如開發人員選項的通知無法移除時，請重新最佳化 System UI。
- **模組完全沒有作用**：請確認已在 LSPosed 中啟用、目標應用程式位於作用域內、沒有殘留 `/sdcard/disable_lt`，且
  LSPosed 記錄顯示 LuckyTool 已載入至宿主行程。
- **部分開關沒有作用**：功能會依 ColorOS 版本分支，因此系統大版本升級後必須等待模組適配。應用程式中未顯示的開關，代表您的版本尚未支援。
- 更多問題的解答請參閱 [FAQ](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq)；
  意見回饋請使用 [Telegram](https://t.me/LuckyTool)。

## 連結

| 項目                                 | 連結                                                          |
|--------------------------------------|---------------------------------------------------------------|
| 說明文件                             | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| 更新記錄                             | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| 模組儲存庫（LSPosed Repo、發行版本） | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| 意見回饋                             | https://t.me/LuckyTool                                        |
| Xposed API                           | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| 授權條款                             | [GPL-3.0](./LICENSE.txt)                                      |

## 免責聲明

- 官方儲存庫為 [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool)，
  並保留完整的提交記錄；其他地方以本專案名義發布的任何內容都與作者無關。
- 作者有正職工作，並不靠這個模組維生。本專案以 GPL-3.0 開源，作為模組開發的入門參考。

## Star History

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>
