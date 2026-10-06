package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.scopes.launcher.AllowLockingUnLockingOfExcludedActivity
import com.luckyzyx.luckytool.hook.scopes.launcher.CustomDesktopDefaultHomePage
import com.luckyzyx.luckytool.hook.scopes.launcher.DisableLongPressAppIconSecondaryMenu
import com.luckyzyx.luckytool.hook.scopes.launcher.EnableAutoCloseFolder
import com.luckyzyx.luckytool.hook.scopes.launcher.EnableDockerBackground
import com.luckyzyx.luckytool.hook.scopes.launcher.EnableLauncherIndicatorEntry
import com.luckyzyx.luckytool.hook.scopes.launcher.ForceEnableDockerBackgroundBlur
import com.luckyzyx.luckytool.hook.scopes.launcher.ForceEnableRecentTaskMemoryDisplay
import com.luckyzyx.luckytool.hook.scopes.launcher.HookAppBadge
import com.luckyzyx.luckytool.hook.scopes.launcher.HookDeviceProfileOption
import com.luckyzyx.luckytool.hook.scopes.launcher.HookLauncherFeature
import com.luckyzyx.luckytool.hook.scopes.launcher.HookOplusBubbleTextView
import com.luckyzyx.luckytool.hook.scopes.launcher.LauncherLayoutRowColume
import com.luckyzyx.luckytool.hook.scopes.launcher.LongPressAppIconOpenAppDetails
import com.luckyzyx.luckytool.hook.scopes.launcher.PageIndicator
import com.luckyzyx.luckytool.hook.scopes.launcher.RecentTaskListClearButton
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveAppUpdateGreenDot
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveBottomAppIconOfRecentTaskList
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveDockerMaxNumberLimit
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveFolderNameInputLimit
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveFolderPreviewBackground
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveLauncherCardName
import com.luckyzyx.luckytool.hook.scopes.launcher.RemoveWidgetsAddRequestWhitelist
import com.luckyzyx.luckytool.hook.scopes.launcher.UnlockTaskLocks
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookLauncher : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        loadHooker(HookGlobalFeatureConfig)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->

            loadHooker(HookGlobalFeatureProvider(dexKitBridge))

            //HookLauncherFeature
            loadHooker(HookLauncherFeature(dexKitBridge))

            //强制启用最近任务内存显示
            if (preferences(ModulePrefs).getBoolean("force_enable_recent_task_memory_display", false)) {
                if (osCode >= 30) loadHooker(ForceEnableRecentTaskMemoryDisplay(dexKitBridge))
            }

            //应用徽章
            if (SDK >= A13) loadHooker(HookAppBadge(dexKitBridge))

            //解锁后台任务锁定限制
            if (preferences(ModulePrefs).getBoolean("unlock_task_locks", false)) {
                loadHooker(UnlockTaskLocks(dexKitBridge))
            }
        }

        //HookLauncherFeatureFlags
//        if (osCode >= 27) loadHooker(HookLauncherFeatureFlags)

        //HookDeviceProfileOption
        loadHooker(HookDeviceProfileOption)

        //分页组件
        loadHooker(PageIndicator)

        //堆叠布局
//        loadHooker(StackedTaskLayout)

        //桌面图标相关
        loadHooker(HookOplusBubbleTextView)


        //设置桌面布局行列数
        if (preferences(ModulePrefs).getBoolean("launcher_layout_enable", false)) {
            loadHooker(LauncherLayoutRowColume)
        }
        //移除文件夹名称输入限制
        if (preferences(ModulePrefs).getBoolean("remove_folder_name_input_limit", false)) {
            loadHooker(RemoveFolderNameInputLimit)
        }
        //移除文件夹预览背景
        if (preferences(ModulePrefs).getBoolean("remove_folder_preview_background", false)) {
            loadHooker(RemoveFolderPreviewBackground)
        }
        //最近任务列表清除按钮
        if (preferences(ModulePrefs).getBoolean("remove_recent_task_list_clear_button", false)) {
            loadHooker(RecentTaskListClearButton)
        }
        //最近任务列表长按APP图标打开应用详情
        if (preferences(ModulePrefs).getBoolean("long_press_app_icon_open_app_details", false)) {
            loadHooker(LongPressAppIconOpenAppDetails)
        }
        //移除最近任务列表底部APP图标
        if (preferences(ModulePrefs).getBoolean("remove_bottom_app_icon_of_recent_task_list", false)) {
            loadHooker(RemoveBottomAppIconOfRecentTaskList)
        }
        //启用Docker背景显示
        if (preferences(ModulePrefs).getBoolean("enable_docker_background", false)) {
            if (osCode >= 26) loadHooker(EnableDockerBackground)
        }
        //强制启用Docker背景模糊
        if (preferences(ModulePrefs).getBoolean("force_enable_docker_background_blur", false)) {
            if (osCode >= 37) loadHooker(ForceEnableDockerBackgroundBlur)
        }
        //允许锁定或解锁已排除活动
        if (preferences(ModulePrefs).getBoolean("allow_locking_unlocking_of_excluded_activity", false)) {
            loadHooker(AllowLockingUnLockingOfExcludedActivity)
        }
        //移除App更新圆点
        if (osCode >= 33) loadHooker(RemoveAppUpdateGreenDot)

        //启用自动关闭文件夹
        if (preferences(ModulePrefs).getBoolean("enable_auto_close_folder", false)) {
            if (osCode >= 34) loadHooker(EnableAutoCloseFolder)
        }
        //移除小组件添加请求白名单
        if (preferences(ModulePrefs).getBoolean("remove_widgets_add_request_whitelist", false)) {
            if (osCode >= 30) loadHooker(RemoveWidgetsAddRequestWhitelist)
        }
        //移除桌面卡片名称
        if (preferences(ModulePrefs).getBoolean("remove_launcher_card_name", false)) {
            if (osCode >= 26) loadHooker(RemoveLauncherCardName)
        }
        //禁用长按应用图标二级菜单
        if (preferences(ModulePrefs).getBoolean("disable_long_press_app_icon_secondary_menu", false)) {
            if (osCode >= 37) loadHooker(DisableLongPressAppIconSecondaryMenu)
        }
        //自定义桌面默认主页
        loadHooker(CustomDesktopDefaultHomePage)
        //启用桌面下方按钮选择项
        if (preferences(ModulePrefs).getBoolean("enable_launcher_indicator_entry", false)) {
            if (osCode >= 37) loadHooker(EnableLauncherIndicatorEntry)
        }
        //移除Docker最大数量限制
        if (preferences(ModulePrefs).getBoolean("remove_docker_max_number_limit", false)) {
            if (osCode >= 38) loadHooker(RemoveDockerMaxNumberLimit)
        }

        //com.android.quickstep.views.OplusTaskMenuViewImpl
        //res/layout/oplus_task_menu_option.xml

        //<string name="oplus_shortcut_lock_app">锁定</string>
        //<string name="oplus_shortcut_locked_app">解锁</string>
        //<string name="oplus_rapid_reach_float_window">浮窗</string>
        //<string name="recent_task_option_split_screen">分屏</string>
        //<string name="oplus_privacy_not_show_preview">隐藏内容</string>
        //<string name="oplus_privacy_show_preview">显示内容</string>
        //<string name="oplus_shortcut_lock_setting">管理</string>

        //com.android.launcher3.popup.OplusBaseSystemShortcut
        //OplusAppInfo etc. -> Click

        //AppEdit
        //com.oplus.uxicon.ui.ui.UxEditPanelFragment -> res/layout/edit_panel_layout.xml
        //<string name="no_icon_pack_toast">没有支持替换图标的图标包</string>
    }
}