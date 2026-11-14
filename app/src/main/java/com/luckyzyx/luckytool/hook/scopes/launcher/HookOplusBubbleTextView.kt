package com.luckyzyx.luckytool.hook.scopes.launcher

import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookOplusBubbleTextView(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

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

        if (iconSize <= 0) return
        //Source LauncherIconConfig
        // 图标尺寸的构造源头是 calculateIconSizeByUxDesign，17.0.1 起类名与方法名一起被混淆，
        // 类靠自己的日志标签定位，方法优先取 int[] 入参那个重载（17.x 只剩它），
        // secondary 分支不经过该方法（C16 走 SecondaryLauncherUtils、C17 固定 72）
        dexKitBridge.findClass {
            matcher {
                usingStrings("LauncherIconConfig")
            }
        }.apply {
            checkDataList("LauncherIconConfig")

            (findMethod {
                matcher {
                    paramTypes(classOf<IntArray>())
                    returnType(classOf<Int>())
                }
            }.firstOrNull() ?: findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<Int>())
                }
            }.apply {
                checkDataList("calculateIconSizeByUxDesign")
            }.single()).getMethodInstance(hostClassLoader!!).hook {
                before { result = iconSize.dp }
            }
        }
    }
}
