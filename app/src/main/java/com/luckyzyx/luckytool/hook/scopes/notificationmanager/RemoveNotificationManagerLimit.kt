package com.luckyzyx.luckytool.hook.scopes.notificationmanager

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationManagerLimit : YukiBaseHooker() {
    override fun onHook() {
        //Source ControllerChannelGroup$AppItemListener -> 通知渠道列表允许通知锁
        "com.oplus.notificationmanager.property.uicontroller.ControllerChannelGroup\$AppItemListener".toClass()
            .resolve().apply {
                firstMethod { name = "isSwitchEnabled" }.hook {
                    intercept(true)
                }
            }
        //Source ControllerAllowNotificationChannel -> 通知渠道内允许通知锁
        "com.oplus.notificationmanager.property.uicontroller.ControllerAllowNotificationChannel".toClass()
            .resolve().apply {
                firstMethod { name = "isNormAppEnabled" }.hook {
                    intercept(true)
                }
            }
        //Source ControllerUnimportantChannel -> 通知渠道内不重要通知锁
        "com.oplus.notificationmanager.property.uicontroller.ControllerUnimportantChannel".toClass()
            .resolve().apply {
                firstMethod { name = "isNormAppEnabled" }.hook {
                    intercept(true)
                }
            }
        //Source ControllerAllowNotificationPkg -> 应用内允许通知锁
        "com.oplus.notificationmanager.property.uicontroller.ControllerAllowNotificationPkg".toClass()
            .resolve().apply {
                firstMethod { name = "isNormAppEnabled" }.hook {
                    intercept(true)
                }
            }
//
//        //Source ControllerAppLockBannerBubble -> 应用内气泡锁
//        "com.oplus.notificationmanager.property.uicontroller.ControllerAppLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
//        //Source ControllerAppLockBannerBubble ->
//        "com.oplus.notificationmanager.property.uicontroller.ControllerChannelLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
//        //Source ControllerAppLockBannerBubble ->
//        "com.oplus.notificationmanager.property.uicontroller.ControllerConversationLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
    }
}