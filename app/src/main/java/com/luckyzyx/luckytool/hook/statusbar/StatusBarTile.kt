package com.luckyzyx.luckytool.hook.statusbar

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.ControlCenterTiles
import com.luckyzyx.luckytool.hook.scopes.systemui.CustomTileBackgroundTransparency
import com.luckyzyx.luckytool.hook.scopes.systemui.FixTileAlignBothSides
import com.luckyzyx.luckytool.hook.scopes.systemui.ForceDisplayOfDeviceControlsTiles
import com.luckyzyx.luckytool.hook.scopes.systemui.ForceDisplayOfRingingStatusToggleTiles
import com.luckyzyx.luckytool.hook.scopes.systemui.LongPressTileOpenThePage
import com.luckyzyx.luckytool.hook.scopes.systemui.MediaPlayerPanel
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveControlCenterTileCountLimit
import com.luckyzyx.luckytool.hook.scopes.systemui.RestorePageLayoutRowCountForEditTiles
import com.luckyzyx.luckytool.hook.scopes.systemui.SpecialTileTopGap
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class StatusBarTile(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //强制显示响铃状态切换磁贴
        if (preferences(ModulePrefs).getBoolean("force_display_of_ringing_status_toggle_tiles", false)) {
            loadHooker(ForceDisplayOfRingingStatusToggleTiles)
        }

        //强制启用设备控制器磁贴
        if (preferences(ModulePrefs).getBoolean("force_display_of_device_controls_tiles", false)) {
            loadHooker(ForceDisplayOfDeviceControlsTiles)
        }

        //磁贴长按跳转事件
        if (preferences(ModulePrefs).getBoolean("restore_some_tile_long_press_event", false)) {
            if (SDK >= A13) loadHooker(LongPressTileOpenThePage)
        }

        //特殊磁贴间隙
        if (preferences(ModulePrefs).getBoolean("control_center_custom_gaps_for_special_tile", false)) {
            if (osCode >= 27) loadHooker(SpecialTileTopGap)
        }

        //媒体播放器
        if (osCode in 26..37) loadHooker(MediaPlayerPanel)

        //磁贴布局
        if (osCode < 40) loadHooker(ControlCenterTiles)

        //经典控制中心 横屏磁贴两侧对齐
        if (preferences(ModulePrefs).getBoolean("fix_tile_align_both_sides", false)) {
            if (osCode >= 26) loadHooker(FixTileAlignBothSides)
        }

        //恢复磁贴编辑页面布局行数
        if (preferences(ModulePrefs).getBoolean("restore_page_layout_row_count_for_edit_tiles", false)) {
            if (SDK >= A13) loadHooker(RestorePageLayoutRowCountForEditTiles)
        }

        //自定义磁贴背景透明度
        if (osCode in 27..33) loadHooker(CustomTileBackgroundTransparency)

        //移除控制中心磁贴数量限制
        if (preferences(ModulePrefs).getBoolean("remove_control_center_tile_count_limit", false)) {
            if (osCode < 37) loadHooker(RemoveControlCenterTileCountLimit(dexKitBridge))
        }
    }
}