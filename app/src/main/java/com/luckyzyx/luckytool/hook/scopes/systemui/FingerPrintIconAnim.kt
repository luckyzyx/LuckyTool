package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Handler
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object FingerPrintIconAnim : YukiBaseHooker() {

    private const val TAG = "FpIcon"

    private val fpIconType = VariousClass(
        "com.oplusos.systemui.keyguard.onscreenfingerprint.OnScreenFingerprintIcon", //C12
        "com.oplus.systemui.keyguard.finger.onscreenfingerprint.OnScreenFingerprintIcon",  //C13
        "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon" //C14 C15
    )

    private val animationDrawable = VariousClass(
        "com.oplus.systemui.keyguard.view.OplusAnimationDrawable" //C16
    )

    //C17：宿主滤镜经 KeyguardFingerprintUtils.updateOpticalUI 投递到专用 HandlerThread 异步应用，
    //懒加载到首次 updateFpColor 回调时再读，此时该类必已初始化、字段非空
    private val opticalUiUpdateHandler: Handler? by lazy {
        runCatching {
            VariousClass("com.oplus.systemui.biometrics.finger.KeyguardFingerprintUtils")
                .toClass().resolve().firstFieldOrNull { name = "opticalUiUpdateHandler" }
                ?.get<Handler>()
        }.getOrNull()
    }

    override fun onHook() {
        val removeMode = preferences(ModulePrefs).getString("remove_fingerprint_icon_mode", "0")
        val isReplaceIcon =
            preferences(ModulePrefs).getBoolean("replace_fingerprint_icon_switch", false)
        val iconPath = preferences(ModulePrefs).getString("replace_fingerprint_icon_path", "")

        //Source OnScreenFingerprintUiMech
        VariousClass(
            "com.oplusos.systemui.keyguard.onscreenfingerprint.OnScreenFingerprintOpticalAnimCtrl", //C12
            "com.oplus.systemui.keyguard.finger.onscreenfingerprint.OnScreenFingerprintUiMech", //C13
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMach", //C14
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech"  //C15
        ).toClass().resolve().apply {
            firstMethod { name = "loadAnimDrawables" }.hook {
                if (removeMode == "3") intercept()
                else after {
//                    YLog.d(
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
                    result = null
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
                after {
                    if (!isReplaceIcon) return@after
                    val mech = instance<Any>()
                    //C17：宿主滤镜经 updateOpticalUI 投递到专用 HandlerThread 异步应用，
                    //同步清除会被随后执行的 setColorFilter 覆盖，投递到同一队列保证在其之后执行
                    val handler = opticalUiUpdateHandler
                    if (handler != null) {
                        handler.post { mech.clearFpIconColorFilter() }
                    } else {
                        mech.clearFpIconColorFilter()
                    }
                }
            }
        }
    }

    /** 清除指纹图标 drawable 的颜色滤镜（C15 及之前为 Dark，C17 起改名 HY） */
    private fun Any.clearFpIconColorFilter() {
        asResolver().apply {
            firstFieldOrNull { name = "imMobileDrawable" }?.get<Drawable>()?.clearColorFilter()
            firstFieldOrNull { name = "imMobileDrawableDark" }?.get<Drawable>()?.clearColorFilter()
            firstFieldOrNull { name = "imMobileDrawableHY" }?.get<Drawable>()?.clearColorFilter()
        }
    }

    /** 归零淡入淡出动画字段（含暗色变体与 AlphaAnimation 冻路） */
    private fun Any.removeFadeAnim() {
        asResolver().apply {
            field { type = animationDrawable.toClass() }.forEach {
                it.set(null)
            }
            firstField { name = "fadeInAlphaAnimation" }.set(null)
            firstField { name = "fadeOutAlphaAnimation" }.set(null)
        }
    }

    private fun Any.setCustomDrawable(iconPath: String?, update: Boolean) {
        asResolver().apply {
            val context = firstField { type = Context::class }.get<Context>() ?: return
            val drawable = if (iconPath.isNullOrBlank()) null
            else BitmapFactory.decodeFile(iconPath).toDrawable(context.resources)
            if (drawable == null) {
                firstField { name { it.contains("fadeInAnimDrawable", true) } }.set(null)
                firstField { name { it.contains("adeOutAnimDrawable", true) } }.set(null)
            } else {
                //C16：宿主所有的恢复/静态写点（RunnableC32041 case0/1、case21 fadeIn 链等）
                //最终都 setImageDrawable(imMobileDrawable/Dark)，把自定义图注入这些原生字段，
                //让一切恢复链写回的都是自定义图，无需逐点对抗
                firstField { name = "imMobileDrawable" }.set(drawable)
                firstFieldOrNull { name = "imMobileDrawableDark" }?.set(drawable)
                firstFieldOrNull { name = "imMobileDrawableHY" }?.set(drawable)
            }
            firstField { type = fpIconType.toClass() }.get<ImageView>()?.setImageDrawable(drawable)
            if (update) firstMethod { name = "updateFpIconColor"; emptyParameters() }.invoke()
        }
    }

    private fun Any.removePressAnim() {
        asResolver().firstField { name { it.contains("PressedAnimDrawable", true) } }.set(null)
        asResolver().firstField { name { it.contains("PressedAnimDrawableTmp", true) } }.set(null)
    }
}