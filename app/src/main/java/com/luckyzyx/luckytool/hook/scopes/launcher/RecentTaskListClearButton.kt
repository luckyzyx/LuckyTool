package com.luckyzyx.luckytool.hook.scopes.launcher

import android.widget.Button
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RecentTaskListClearButton : YukiBaseHooker() {

    val PressFeedbackButton = "com.android.launcher.views.PressFeedbackButton"

    override fun onHook() {
        //Source OplusClearAllPanelView
        "com.oplus.quickstep.views.OplusClearAllPanelView".toClass().resolve().apply {
            (firstMethodOrNull { name = "inflateIfNeeded" }
                ?: firstMethod { name = "onFinishInflate" }).hook {
                after {
                    //C17字段名被混淆(mClearAllBtn -> f30408a), 按类型获取:
                    //C12.1-C17该字段类型均为 PressFeedbackButton 且在本类中唯一
                    firstField { type = PressFeedbackButton }
                        .of(instance).get<Button>()?.isVisible = false
                }
            }
            //C17 setAlpha 在 alpha>0 时会重新 setVisibility(0) 撤销隐藏
            //(C15 C16的setAlpha仅super调用不碰按钮, 此处重复隐藏无害)
            firstMethodOrNull {
                name = "setAlpha"
                parameters(Float::class)
            }?.hook {
                after {
                    firstField { type = PressFeedbackButton }
                        .of(instance).get<Button>()?.isVisible = false
                }
            }
        }
    }
}