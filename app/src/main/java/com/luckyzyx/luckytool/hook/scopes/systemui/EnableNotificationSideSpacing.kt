package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getScreenOrientation
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableNotificationSideSpacing : YukiBaseHooker() {

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        var paddingVertical =
            preferences(ModulePrefs).getInt("custom_notification_side_spacing_vertical", 0)
        var paddingHorizontal =
            preferences(ModulePrefs).getInt("custom__notification_side_spacing_horizontal", 0)
        dataChannel.wait<Int>("custom_notification_side_spacing_vertical") { paddingVertical = it }
        dataChannel.wait<Int>("custom__notification_side_spacing_horizontal") {
            paddingHorizontal = it
        }

        //Source C12+: NotificationStackScrollLayout
        //通知卡片两侧留白由 mSidePaddings 原生控制,onMeasure 统一按 (size - mSidePaddings * 2) 测量子视图,onLayout 自动水平居中
        //锁屏媒体卡(hostView/MediaContainerView)同为 NSSL 子视图,一并覆盖
        "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout".toClass()
            .resolve().apply {
                firstMethod { name = "onMeasure" }.hook {
                    before {
                        val view = instance<View>()
                        getScreenOrientation(view) {
                            firstField { name = "mSidePaddings" }.of(instance)
                                .set(if (it) paddingVertical.dp else paddingHorizontal.dp)
                        }
                    }
                }
            }
    }
}
