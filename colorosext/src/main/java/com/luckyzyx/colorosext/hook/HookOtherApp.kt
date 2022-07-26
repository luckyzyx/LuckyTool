package com.luckyzyx.colorosext.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.colorosext.hook.otherapp.HookMoreAnime
import com.luckyzyx.colorosext.hook.otherapp.HookTuLing

class HookOtherApp : YukiBaseHooker(){
    override fun onHook() {
        //好多动漫
        loadApp("com.east2d.everyimage",HookMoreAnime())

        //图凌
        loadApp("com.chan.cwallpaper",HookTuLing())
    }
}
