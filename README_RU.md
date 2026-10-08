<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README.md">简体中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_EN.md">English</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_ZH_TW.md">繁體中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_JA.md">日本語</a> 丨
  <b>Русский</b> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_VI.md">Tiếng Việt</a>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>Xposed-модуль для расширения и оптимизации системы ColorOS</p>
<p>Модуль бесплатен навсегда — любой платный канал не имеет отношения к автору</p>
<p>Перенаправление трафика, перепубликация, перепечатка, продажа, распространение и повторная загрузка без разрешения запрещены</p>
<p>Делитесь опытом, а не готовым результатом</p>
</div>

---

## Обзор

LuckyTool — это Xposed-модуль, работающий на фреймворке [LSPosed](https://github.com/LSPosed/LSPosed). Он предназначен для устройств с
  **ColorOS** и добавляет десятки улучшений в системный фреймворк, SystemUI, лаунчер, «Настройки» и более 40 системных приложений. Переключатели синхронизируются с системными процессами и применяются сразу — перезагрузка не требуется.

Полное руководство доступно на [сайте документации](https://luckyzyx.gitlab.io/LuckyTool_Doc/).

## Возможности

| Область                                        | Основное                                                                                                                                                                                                                                                                      |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Системный фреймворк                            | свои шаги громкости, принудительный разделённый экран, поддержка 32-битных приложений, белый список оптимизации батареи, удержание приложений в памяти, обход проверки рискованных приложений, установка по USB без подтверждения и другое                                    |
| SystemUI / строка состояния / экран блокировки | стили часов и индикатора скорости сети, видимость значков, плитки и прозрачность Центра управления, стили уведомлений, часы и виджет зарядки на экране блокировки, значок сканера отпечатков и другое                                                                         |
| Лаунчер                                        | фон и размытие Dock, количество строк и столбцов сетки, ограничение длины названий папок, число значков в Dock, поведение списка недавних, удаление бейджей и другое                                                                                                          |
| «Настройки» и системные приложения             | изменение DPI без перезагрузки, список приложений в тёмном режиме, язык для каждого приложения, отключение системных приложений, а также улучшения для «Камеры», «Галереи», «Игрового помощника», «Магазина тем», «Облачного сервиса» и более 40 приложений в общей сложности |
| В реальном времени                             | изменения переключателей синхронизируются с системными процессами и применяются без перезагрузки                                                                                                                                                                              |
| Мультиверсионность                             | реализации автоматически подбираются под вашу версию ColorOS (12 – 16), с отслеживанием новых выпусков                                                                                                                                                                        |

Полный список — на сайте документации ([список функций](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)); решающее значение имеют переключатели внутри приложения.

## Требования

| Пункт                  | Требование                                      |
|------------------------|-------------------------------------------------|
| Система                | ColorOS 12 или новее (OPPO / OnePlus / realme)  |
| Поддерживаемые версии  | ColorOS 12 – 17, с отслеживанием новых выпусков |
| Фреймворк              | LSPosed                                         |
| Разрешения             | Root требуется для некоторых функций            |
| ABI устройства         | arm64-v8a                                       |
| Проверенное устройство | OnePlus 15                                      |

## Загрузка

Скачайте последний файл
  `LuckyTool_v*.apk` из раздела [Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) и установите его обычным способом.

Имена файлов имеют вид `LuckyTool_v<versionName>(<buildNumber>)_<buildType>.apk`. Число в скобках — это счётчик сборок, по которому можно точно определить сборку, поэтому указывайте его при сообщении о проблеме.

## Установка и активация

1. Скачайте и установите APK модуля.
2. Откройте менеджер LSPosed → **Модули** → включите LuckyTool.
3. В разделе **Область применения** отметьте системные приложения, которые нужно перехватывать. Рекомендуется выбрать все: модуль сам пропускает логику, неприменимую к каждому пакету.
4. Перезагрузите устройство (или хотя бы перезапустите затронутые области применения).
5. Откройте LuckyTool и включите главный переключатель в правом верхнем углу главного экрана.

## Решение проблем и частые вопросы

- **После обновления системы ничего не работает**: воспользуйтесь пунктом **Повторно оптимизировать Dex** в меню перезагрузки модуля или нажмите и удерживайте приложение из области применения в LSPosed,
  чтобы повторно его оптимизировать. Например, если уведомление о режиме разработчика не убирается, повторно оптимизируйте System UI.
- **Модуль вообще не работает**: убедитесь, что он включён в LSPosed, целевое приложение входит в область применения, файл `/sdcard/disable_lt` не остался,
  а в журнале LSPosed видно загрузку LuckyTool в процесс-хост.
- **Некоторые переключатели не действуют**: функции различаются по версиям ColorOS, поэтому после крупного обновления системы нужно дождаться адаптации модуля. Переключатель, которого нет в приложении, на вашей версии пока не поддерживается.
- Ответы на другие вопросы есть в [FAQ](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq);
  для обратной связи используйте [Telegram](https://t.me/LuckyTool).

## Ссылки

| Пункт                                     | Ссылка                                                        |
|-------------------------------------------|---------------------------------------------------------------|
| Документация                              | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| Список изменений                          | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| Репозиторий модуля (LSPosed Repo, релизы) | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| Обратная связь                            | https://t.me/LuckyTool                                        |
| Xposed API                                | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| Лицензия                                  | [GPL-3.0](./LICENSE.txt)                                      |

## Отказ от ответственности

- Официальный репозиторий — [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool),
  в нём хранится полная история коммитов; всё, что выпускается под именем этого проекта в других местах, не имеет отношения к автору.
- У автора есть основная работа, и он не живёт за счёт этого модуля. Он открыт под GPL-3.0 как отправная точка для разработки модулей.

## Star History

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>
