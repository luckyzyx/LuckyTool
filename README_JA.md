<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README.md">简体中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_EN.md">English</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_ZH_TW.md">繁體中文</a> 丨
  <b>日本語</b> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_RU.md">Русский</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_VI.md">Tiếng Việt</a>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>ColorOS システムを拡張・最適化する Xposed モジュール</p>
<p>永久無料のモジュールです。有料のチャンネルは作者とは無関係です</p>
<p>無断でのトラフィックの転送、転載、複製、販売、共有、再アップロードを禁止します</p>
<p>完成品ではなくノウハウを共有しましょう</p>
</div>

---

## 概要

LuckyTool は、[LSPosed](https://github.com/LSPosed/LSPosed) フレームワーク上で動作する Xposed モジュールです。
  **ColorOS** 搭載端末を対象とし、システムフレームワーク、SystemUI、ランチャー、設定、および 40 以上のシステムアプリに数多くの拡張機能を追加します。スイッチの変更はシステムプロセスに同期され、再起動なしで即座に反映されます。

詳細なマニュアルは[ドキュメントサイト](https://luckyzyx.gitlab.io/LuckyTool_Doc/)で公開しています。

## 機能

| 領域                                   | 主な内容                                                                                                                                                                                              |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| システムフレームワーク                 | 音量ステップのカスタマイズ、強制分割画面、32 ビットアプリのサポート、バッテリー最適化のホワイトリスト、アプリの常駐化、リスクアプリ検査の回避、確認なしの USB インストールなど                        |
| SystemUI / ステータスバー / ロック画面 | 時計とネットワーク速度のスタイル、アイコンの表示・非表示、コントロールセンターのタイルと透明度、通知スタイル、ロック画面の時計と充電ウィジェット、指紋アイコンなど                                    |
| ランチャー                             | Dock の背景とぼかし、グリッドの行数・列数、フォルダー名の文字数制限、Dock のアイコン数、最近使ったアプリの動作、バッジの非表示など                                                                    |
| 設定 & システムアプリ                  | 再起動なしの DPI 変更、ダークモードのアプリ一覧、アプリごとの言語、システムアプリの無効化、さらにカメラ、ギャラリー、ゲームアシスタント、テーマストア、クラウドサービスなど合計 40 以上のアプリの拡張 |
| リアルタイム                           | スイッチの変更はシステムプロセスに同期され、再起動なしで適用されます                                                                                                                                  |
| マルチバージョン                       | ColorOS のバージョン（12 – 16）に合わせて実装を自動的に切り替え、新しいリリースにも追従します                                                                                                         |

完全な一覧はドキュメントサイト（[機能一覧](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)）にあります。アプリ内のスイッチが正式な情報源です。

## 動作要件

| 項目           | 要件                                       |
|----------------|--------------------------------------------|
| システム       | ColorOS 12 以降（OPPO / OnePlus / realme） |
| 対応バージョン | ColorOS 12 – 17、新しいリリースにも追従    |
| フレームワーク | LSPosed                                    |
| 権限           | 一部の機能には Root が必要です             |
| 端末 ABI       | arm64-v8a                                  |
| 動作確認端末   | OnePlus 15                                 |

## ダウンロード

[Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) から最新の
  `LuckyTool_v*.apk` をダウンロードし、通常どおりインストールしてください。

ファイル名は `LuckyTool_v<versionName>(<buildNumber>)_<buildType>.apk` の形式です。括弧内の数字はビルドを正確に特定するためのビルドカウンターですので、不具合を報告する際は必ず添えてください。

## インストールと有効化

1. モジュールの APK をダウンロードしてインストールします。
2. LSPosed マネージャーを開き → **モジュール** → LuckyTool を有効にします。
3. **スコープ**でフックするシステムアプリにチェックを入れます。すべて選択することをおすすめします。各パッケージに該当しないロジックはモジュール側でスキップされます。
4. 端末を再起動します（少なくとも影響を受けるスコープを再起動してください）。
5. LuckyTool を開き、ホーム画面右上のマスタースイッチをオンにします。

## トラブルシューティング & FAQ

- **システムアップデート後に何も動作しない**: モジュールの再起動メニューにある **Dex の再最適化** を実行するか、LSPosed でスコープのアプリを長押しして
  再最適化してください。たとえば開発者向けオプションの通知を消せない場合は、SystemUI を再最適化します。
- **モジュールがまったく動作しない**: LSPosed で有効になっているか、対象アプリがスコープに含まれているか、`/sdcard/disable_lt` が残っていないか、
  LSPosed のログに LuckyTool がホストプロセスへ読み込まれた記録があるかを確認してください。
- **一部のスイッチが効果を発揮しない**: 機能は ColorOS のバージョンごとに分岐しているため、大きなシステムアップデートの後はモジュール側の対応を待つ必要があります。アプリに表示されないスイッチは、お使いのバージョンではまだサポートされていません。
- その他の疑問は [FAQ](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq) で回答しています。
  フィードバックには [Telegram](https://t.me/LuckyTool) をご利用ください。

## リンク

| 項目                                           | リンク                                                        |
|------------------------------------------------|---------------------------------------------------------------|
| ドキュメント                                   | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| 変更履歴                                       | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| モジュールリポジトリ（LSPosed Repo、リリース） | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| フィードバック                                 | https://t.me/LuckyTool                                        |
| Xposed API                                     | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| ライセンス                                     | [GPL-3.0](./LICENSE.txt)                                      |

## 免責事項

- 公式リポジトリは [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool) で、
  完全なコミット履歴を保持しています。本プロジェクトの名前で他の場所から配布されているものは、作者とは一切関係ありません。
- 作者には本業があり、このモジュールで生計を立てているわけではありません。モジュール開発の入門として GPL-3.0 のもとでオープンソース化されています。

## Star History

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>
