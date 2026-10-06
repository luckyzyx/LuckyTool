package com.luckyzyx.luckytool.hook.scopes.launcher

import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookOplusBubbleTextView : YukiBaseHooker() {

    override fun onHook() {
        val osCode = getOSVersionCode

        val multiLine =
            preferences(ModulePrefs).getBoolean("allow_app_names_display_multiple_lines", false)
        val textLineHeight = preferences(ModulePrefs).getInt("custom_app_icon_name_line_height", -1)
        val iconSize = preferences(ModulePrefs).getInt("custom_launcher_app_icon_size", 0)

        //Source OplusBubbleTextView
        "com.android.launcher3.OplusBubbleTextView".toClass().resolve().apply {
            if (osCode < 26 && multiLine) {
                firstMethod {
                    name = "setMaxLines"
                    parameters(Int::class)
                }.hook {
                    before {
                        instance<TextView>().maxLines = 2
                        result = null
                    }
                }
            }
            if (multiLine && textLineHeight > -1) {
                firstConstructor { parameterCount = 3 }.hook {
                    after {
                        instance<TextView>().apply {
                            lineHeight = textLineHeight.dp
                        }
                    }
                }
            }
        }

        // C17 起 IconParam 混淆为 j4.j、getIconSizePx 混淆为 d()，旧 hook 无法命中；
        // 两版图标尺寸的构造源头都是 LauncherIconConfig.calculateIconSizeByUxDesign()
        // 无参版本，hook 源头等价于替换 getIconSizePx 的读取结果。
        //（secondary 分支不经过该方法，C16 走 SecondaryLauncherUtils、C17 固定 72）
        //Source LauncherIconConfig
        "com.android.launcher.theme.LauncherIconConfig".toClass().resolve().apply {
            firstMethod {
                name = "calculateIconSizeByUxDesign"
                emptyParameters()
            }.hook {
                before {
                    if (iconSize > 0) result = iconSize.dp
                }
            }
        }
    }
}