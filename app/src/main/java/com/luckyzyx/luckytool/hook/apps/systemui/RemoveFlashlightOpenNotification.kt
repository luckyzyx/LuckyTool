package com.luckyzyx.luckytool.hook.apps.systemui

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

class RemoveFlashlightOpenNotification : YukiBaseHooker() {
    override fun onHook() {
        //Source FlashlightNotification
        findClass("com.oplusos.systemui.flashlight.FlashlightNotification").hook {
            injectMember {
                method {
                    name = "sendNotification"
                    paramCount = 1
                }
                intercept()
            }
        }
    }
}