package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.HookSystemUIFeature
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveAutoEnterSuperPowerSave
import com.luckyzyx.luckytool.utils.DexkitUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSystemUI : YukiBaseHooker() {
    override fun onHook() {

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //系统界面Feature
            loadHooker(HookSystemUIFeature(dexKitBridge))

            //状态栏功能
            loadHooker(HookSystemUIStatusBar(dexKitBridge))

            //移除强制进入超级省电模式
            loadHooker(RemoveAutoEnterSuperPowerSave(dexKitBridge))
        }

        //锁屏
        loadHooker(HookSystemUILockScreen)

        //对话框相关
        loadHooker(HookSystemUIDialog)

        //全面屏手势相关
        loadHooker(HookSystemUIGesture)

        //指纹相关
        loadHooker(HookSystemUIFingerPrint)

        //杂项
        loadHooker(HookSystemUiMiscellaneous)

        //自启
        loadHooker(HookSystemUIAutoStart)

    }
}