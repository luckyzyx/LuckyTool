package com.luckyzyx.luckytool.hook.scopes.uiengine

import android.graphics.Typeface
import android.text.TextPaint
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object SetAodTypefaceMode : YukiBaseHooker() {
    override fun onHook() {
        val typefaceMode = preferences(ModulePrefs).getString("set_aod_typeface_mode", "0")
        val applyClock = preferences(ModulePrefs).getBoolean("apply_aod_clock_typeface", false)

        //Source AodTextView
        "com.oplus.egview.widget.AodTextView".toClass().resolve().apply {
            firstConstructor { parameterCount = 3 }.hook {
                after {
                    val typeface = when (typefaceMode) {
                        "1" -> Typeface.DEFAULT
                        "2" -> Typeface.DEFAULT_BOLD
                        else -> return@after
                    }
                    firstMethod {
                        name = "setTypeface"
                        parameters(Typeface::class)
                        superclass()
                    }.of(instance).invoke(typeface)
                }
            }
        }

        //Source TimeView
        "com.oplus.egview.widget.TimeView".toClass().resolve().apply {
            firstMethod {
                name = "setTextWidget"
                parameters(String::class)
            }.hook {
                after {
                    if (applyClock.not()) return@after
                    val typeface = when (typefaceMode) {
                        "1" -> Typeface.DEFAULT
                        "2" -> Typeface.DEFAULT_BOLD
                        else -> return@after
                    }
                    val mTextPaint = firstField { name = "mTextPaint";superclass() }.of(instance)
                        .get<TextPaint>() ?: return@after
                    mTextPaint.typeface = typeface
                    instance<View>().requestLayout()
                }
            }
        }
    }
}