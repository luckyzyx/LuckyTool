**The beta test version is only released for testing
in [TG Channel’s associated groups](https://t.me/LuckyTool)**

**中文更新日志：[更新日志](../changelog)**

Entries for versions before `1.3.5_beta` are currently only available in the
[Chinese changelog](../changelog).

### 1.3.5_beta

#### Added

- Keep Google services on the network when the battery component blocks them C16+
- Force enable the Feiniu Cloud NAS option
- Custom system message region presets
- Disable malicious application intercept C16.1
- Disable the accessibility warning dialog C16.1
- Remove Control Center carriers C16.1(400)
- Remove the Control Center settings button C16.1
- Unlock GpsJoyStick Pro
- Disable audio focus
- Remove Breeno Next root detection C17
- Hide the status bar in panoramic AOD C16+ (@ShellWen)
- Optimize the application installation UI (@蛮吉)
- Custom notification side spacing C12+ (portrait and landscape separately)
- Custom action key event interception (no-op / ringtone cycle)
- Manual scope hot reload
- Expand the update list by default in the software store
- Miuix UI style (default, switchable back to Material)
- Cold-start animation
- Custom status bar icon order
- Custom status bar network speed refresh interval (0.1-5 s)
- Force enable the Xiaobu keyboard liquid glass blur (strength adjustable)
- App permission page (notifications / all files access / app list / install apps)
- MEMC configuration Activity list supports searching and shows activity names
- Launcher layout backup and restore
- Allow choosing a third-party APK installer (@蛮吉)
- Classic Control Center tile rows and columns on ColorOS 17
- Control Center HDR/SDR ratio tile
- Custom video on the OTA card C17
- OTA card background/video row thumbnail preview, long press to clear
- Custom rotate-screen button size (@RealmeOS)
- Fluid cloud card redirect (with a configurable target browser)
- Show the declared OS version range (minOsdkVersion / maxOsdkVersion) in the package information
- Very dark mode control page (a replacement when the system page renders empty)
- Remove forced automatic entry into super power saving mode (@RealmeOS)

#### Optimized

- Rework the red-one control on the home screen clock widget
- Redesigned the single-choice, multi-choice and action-list dialogs
- Redesigned the app picker dialog
- Redesigned the remote ADB debug dialog
- Clarified the descriptions of the "app clone limit" options
- Function page data is preloaded in the background at app start
- Colored badges for the "XX-related" category rows on the function page
- Settings backup/restore records where each storage lives
- Settings backup/restore pickers default to Download/LuckyTool
- Reworked keep-alive for docked floating windows, restored on ColorOS 15+ (ColorOS 14 uses the SystemUI path)

#### Fixed

- Fixed "Remove Breeno Next root detection" not taking effect, adapting to Breeno Next 17.0.75
- Fixed "Remove app clone badge" not taking effect, adapting to C17
- Fixed the launcher crashing with sinking icons at the maximum column count C16
- Fixed the wireless charging wattage on the lock screen C17
- Fixed "Remove recent task list clear button" not taking effect C17
- Fixed "Remove the lock icon at the top of the lock screen" not taking effect C17
- Fixed "Custom launcher layout rows/columns" not taking effect C17
- Fixed "Force enable device controls tile" only producing a grey tile on C17
- Fixed "Force show Wi-Fi standard" crash-looping System UI
- Fixed the status bar clock disappearing behind the fluid cloud with wrapped layouts
- Fixed "About phone" custom options requiring the processor click page first
- Fixed the custom OTA card background being overwritten after a card refresh
- Fixed backups right after a reboot or a scope reload always timing out
- Fixed automatic launcher backups failing when the media library rejects the insert
- Fixed data backup and restore for set values and legacy string formats
- Fixed "Use user typeface" doing nothing under the default network speed layout
- Fixed donate data loading (now uses the jsDelivr mirror)
- Fixed stale hang-up state in docked floating window keep-alive blocking later hang-ups of the same task
- Fixed docked floating window keep-alive moving the task before the dock transition finished, which pulled it back and paused the app
- Fixed the virtual display being released before the restore finished, which made the mini window disappear
- Fixed three broken com.oplus.games hooks V10.43
- Fixed "Remove startup animation" not removing the game start popup animation V10.43

#### Adapted

- Enable Wi-Fi details to display the gateway C16.1
- Wi-Fi address information shows the gateway C17
- Custom fingerprint icon replacement C16.1
- Disable automatic switching to the last task v16.6.17(8a73764)
- Remove the countdown for adding a virus app to the Phone Manager whitelist v17.3.10(c24156e)
- Remove the personal hotspot power consumption notification C17
- Force recent task memory usage display C17
- Remove the widget add-request whitelist C17
- Long press a recent task icon to open app details C17
- Force Docker background blur C17
- Launcher page indicator component sliding C17
- Custom launcher icon size C17
- Control Center clock seconds display C17
- Remove the Control Center clock C17
- Lock screen clock "red 1" style C17
- Remove the rotate-screen button in the lower-left corner C17
- Remove the accessibility warning dialog C17
- Status bar battery notification C17
- Unlock the background task lock limit C17
- 5G switch tile C17
- Force refresh rate C17
- Scan an NFC tag to click the button automatically C17
- Enable Opex local installation C17
- Enable the Smart Sidebar to keep running in the background C17
- Remove the battery restriction plugin C17
- Remove the word limit for saving and renaming files v17.9.16(064a5da)
- Remove the image save watermark of the Pictorial lock screen v40.10.15.2(4e15fb0)

#### Changed

- "Auto app archiving" is now disabled by default

#### Removed

- Removed "Notification Center two-side alignment"
- Removed the secure payment scope, its restrictions can be turned off in the system secure payment settings
