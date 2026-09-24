package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.instance
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object FingerPrintIconAnim : Hooker {

    private const val TAG = "FpIcon"

    private val fpIconType = VariousClass(
        "com.oplusos.systemui.keyguard.onscreenfingerprint.OnScreenFingerprintIcon", //C12
        "com.oplus.systemui.keyguard.finger.onscreenfingerprint.OnScreenFingerprintIcon",  //C13
        "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon" //C14 C15
    )

    override fun onHook() {
        val removeMode = prefs(ModulePrefs).getString("remove_fingerprint_icon_mode", "0")
        val isReplaceIcon = prefs(ModulePrefs).getBoolean("replace_fingerprint_icon_switch", false)
        val iconPath = prefs(ModulePrefs).getString("replace_fingerprint_icon_path", "")

        //Source OnScreenFingerprintUiMech
        VariousClass(
            "com.oplusos.systemui.keyguard.onscreenfingerprint.OnScreenFingerprintOpticalAnimCtrl", //C12
            "com.oplus.systemui.keyguard.finger.onscreenfingerprint.OnScreenFingerprintUiMech", //C13
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMach", //C14
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech"  //C15
        ).toClass().resolve().optional(true).apply {
            firstMethod { name = "loadAnimDrawables" }.hook {
                if (removeMode == "3") intercept()
                else after {
//                    XLog.d(
//                        "loadAnimDrawables after: mode=$removeMode replace=$isReplaceIcon path=$iconPath",
//                        tag = TAG
//                    )
                    //C16：fade 动画已无独立方法（内联为 updateOpticalUI 的 case21/22），
                    //替换图标或禁用淡入淡出时归零 fade 字段，使宿主无动画可播且不覆盖自定义图
                    if (removeMode == "1" || isReplaceIcon) instance<Any>().removeFadeAnim()
                    when (removeMode) {
                        "0" -> if (isReplaceIcon) instance<Any>().setCustomDrawable(iconPath, true)
                        "1" -> instance<Any>().setCustomDrawable(null, true)
                        "2" -> {
                            instance<Any>().removePressAnim()
                            if (isReplaceIcon) instance<Any>().setCustomDrawable(iconPath, true)
                        }
                    }
                }
            }
            //C13-C15 旧版的独立 fade 方法（C16 起不存在，仅作旧版本回退兼容）
            firstMethodOrNull { name = "startFadeInAnimation" }?.hook {
                if (isReplaceIcon) before {
                    instance<Any>().setCustomDrawable(iconPath, false)
                    resultNull()
                } else if (removeMode == "1" || removeMode == "3") intercept()
            }
            firstMethodOrNull { name = "startFadeOutAnimation" }?.hook {
                if (isReplaceIcon) intercept()
                else if (removeMode == "1" || removeMode == "3") intercept()
            }
            //C16：宿主恢复图标（RunnableC32041 case0/1 setImageDrawable(ImMobileDrawable)）后，
            //替换模式重设自定义图，模式1保持移除，防止恢复链抹掉我们的设置
            firstMethodOrNull { name = "restoreIconDrawable" }?.hook {
                after {
                    when {
                        isReplaceIcon -> instance<Any>().setCustomDrawable(iconPath, false)
                        removeMode == "1" -> instance<Any>().setCustomDrawable(null, false)
                    }
                }
            }
            firstMethodOrNull { name = "restoreIconDrawableDark" }?.hook {
                after {
                    when {
                        isReplaceIcon -> instance<Any>().setCustomDrawable(iconPath, false)
                        removeMode == "1" -> instance<Any>().setCustomDrawable(null, false)
                    }
                }
            }
            firstMethod {
                name = "updateFpColor"
                parameters(Int::class)
            }.hook {
                before {
                    // 新系统异步给 Drawable 着色；after 清除会被排队的任务覆盖。
                    if (isReplaceIcon) resultNull()
                }
            }
        }
    }

    /** 归零淡入淡出动画字段（含暗色变体与 AlphaAnimation 冻路） */
    private fun Any.removeFadeAnim() {
        asResolver().optional(true).apply {
            // 按名称区分 fade 与 pressed，模式 1 不能连按压动画一起清空。
            field {
                name { fieldName ->
                    listOf("fadeInAnimDrawable", "fadeOutAnimDrawable", "fadeInAlphaAnimation", "fadeOutAlphaAnimation")
                        .any { fieldName.endsWith(it, ignoreCase = true) }
                }
            }.forEach { it.set(null) }
        }
    }

    private fun Any.setCustomDrawable(iconPath: String?, update: Boolean) {
        asResolver().optional(true).apply {
            val context = firstField { type = Context::class }.get<Context>() ?: return
            val drawable = if (iconPath.isNullOrBlank()) null
            else (BitmapFactory.decodeFile(iconPath) ?: return).toDrawable(context.resources)
            if (drawable == null) {
                firstField { name { it.contains("fadeInAnimDrawable", true) } }.set(null)
                firstField { name { it.contains("adeOutAnimDrawable", true) } }.set(null)
            }
            // 同步更新宿主字段，避免排队中的恢复任务覆盖自定义图或重新显示已隐藏图标。
            firstField { name = "imMobileDrawable" }.set(drawable)
            firstFieldOrNull { name = "imMobileDrawableDark" }?.set(drawable)
            firstFieldOrNull { name = "imMobileDrawableHY" }?.set(drawable)
            firstField { type = fpIconType.toClass() }.get<ImageView>()?.setImageDrawable(drawable)
            if (update) firstMethod { name = "updateFpIconColor"; emptyParameters() }.invoke()
        }
    }

    private fun Any.removePressAnim() {
        asResolver().firstField { name { it.endsWith("PressedAnimDrawable", true) } }.set(null)
        asResolver().firstField { name { it.endsWith("PressedAnimDrawableTmp", true) } }.set(null)
    }
}