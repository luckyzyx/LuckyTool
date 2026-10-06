package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.camera.HookCameraConfig
import com.luckyzyx.luckytool.hook.scopes.gallery.GalleryWaterMarkWordDialog
import com.luckyzyx.luckytool.hook.scopes.gallery.HookFunctionManager
import com.luckyzyx.luckytool.hook.scopes.gallery.HookSystemStorage
import com.luckyzyx.luckytool.hook.scopes.gallery.RemoveAIGCEliminationLimit
import com.luckyzyx.luckytool.hook.scopes.gallery.ReplaceOnePlusModelWatermark
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGallery : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        if (osCode < 27) return

        //HookCameraConfig
        if (SDK >= A13) loadHooker(HookCameraConfig)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //HookOtherSystemStorage
            loadHooker(HookSystemStorage(dexKitBridge))
            //HookFunctionManager
            if (osCode < 34) loadHooker(HookFunctionManager(dexKitBridge))

            //替换OnePlus机型水印
            if (preferences(ModulePrefs).getBoolean("replace_oneplus_model_watermark", false)) {
                if (osCode >= 34) loadHooker(ReplaceOnePlusModelWatermark(dexKitBridge))
            }
            //移除自定义水印字数限制
            if (preferences(ModulePrefs).getBoolean("remove_gallery_watermark_word_limit", false)) {
                if (osCode < 30) loadHooker(GalleryWaterMarkWordDialog(dexKitBridge))
            }
            //移除AIGC消除限制
            if (preferences(ModulePrefs).getBoolean("remove_aigc_elimination_limit", false)) {
                if (SDK < A15) loadHooker(RemoveAIGCEliminationLimit(dexKitBridge))
            }
        }
    }
}