package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.isSubclassOf
import com.highcapable.kavaref.extension.loadClassOrNull
import com.highcapable.kavaref.extension.makeAccessible
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.util.WeakHashMap
import kotlin.math.roundToInt

@Obfuscate
class CustomRotateScreenButtonSize(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    private val originalDiameters = WeakHashMap<View, Int>()
    private var hasLoggedScaledSize = false

    override fun onHook() {
        val percent = preferences(ModulePrefs).getInt("custom_rotate_screen_button_size", 100)
            .coerceIn(50, 100)
        if (percent == 100) return
        val loader = hostClassLoader ?: return

        //Source FloatingRotationButton C13+
        //通过窗口标题和公开 API 定位, 不依赖混淆后的类名、方法名和字段名。
        val layoutData = dexKitBridge.findMethod {
            matcher {
                paramCount(0)
                returnType(classOf<WindowManager.LayoutParams>())
                usingStrings("FloatingRotationButton")
                addInvoke {
                    name("setTitle")
                    paramTypes(classOf<CharSequence>())
                }
                addInvoke {
                    name("getRotation")
                    paramCount(0)
                    returnType(classOf<Int>())
                }
            }
        }.checkDataList("CustomRotateScreenButtonSize layoutMethod").singleOrNull() ?: return

        val controllerClass = dexKitBridge.getClassData(layoutData.className) ?: return
        val dimensionData = controllerClass.findMethod {
            matcher {
                paramCount(0)
                returnType(Void.TYPE)
                addInvoke {
                    name("getDimensionPixelSize")
                    paramTypes(classOf<Int>())
                    returnType(classOf<Int>())
                }
                addInvoke {
                    name("setLayoutParams")
                    paramTypes(classOf<ViewGroup.LayoutParams>())
                }
            }
        }.checkDataList("CustomRotateScreenButtonSize dimensionMethod").singleOrNull() ?: return

        //同时被尺寸初始化与窗口布局流程读取的 View 字段才是按钮, 外层 ViewGroup 不在此列。
        val buttonData = controllerClass.findField {
            matcher {
                addReadMethod { descriptor(layoutData.descriptor) }
                addReadMethod { descriptor(dimensionData.descriptor) }
            }
        }.checkDataList("CustomRotateScreenButtonSize buttonFields", onlyOne = false)
            .singleOrNull { field ->
                loader.loadClassOrNull(field.typeName)?.isSubclassOf(classOf<View>()) == true
            } ?: return
        val buttonClass = dexKitBridge.getClassData(buttonData.typeName) ?: return

        //直径设置方法由尺寸初始化方法调用, 并由它唯一定位测量直径字段。
        val setterData = buttonClass.findMethod {
            matcher {
                paramTypes(classOf<Int>())
                returnType(Void.TYPE)
                addCaller { descriptor(dimensionData.descriptor) }
            }
        }.checkDataList("CustomRotateScreenButtonSize diameterSetter").singleOrNull() ?: return
        val diameterData = buttonClass.findField {
            matcher {
                type(classOf<Int>())
                addWriteMethod { descriptor(setterData.descriptor) }
            }
        }.checkDataList("CustomRotateScreenButtonSize diameterField").singleOrNull() ?: return

        val layoutMethod = layoutData.getMethodInstance(loader)
        val dimensionMethod = dimensionData.getMethodInstance(loader)
        val diameterSetter = setterData.getMethodInstance(loader)
        val buttonField = buttonData.getFieldInstance(loader).apply { makeAccessible() }
        val diameterField = diameterData.getFieldInstance(loader).apply { makeAccessible() }

        dimensionMethod.hook {
            after {
                val button = buttonField.get(instance) as? View ?: return@after
                val diameter = diameterField.getInt(button)
                if (diameter > 0) originalDiameters[button] = diameter
            }
        }
        layoutMethod.hook {
            after {
                val layoutParams = result<WindowManager.LayoutParams>() ?: return@after
                val button = buttonField.get(instance) as? View ?: return@after
                val originalDiameter = originalDiameters[button]
                    ?: button.layoutParams?.width?.takeIf { it > 0 }
                    ?: return@after
                val buttonLayoutParams = button.layoutParams ?: return@after
                val scaledDiameter = scaledDiameter(originalDiameter, percent)
                if (scaledDiameter == originalDiameter) return@after

                diameterSetter.invoke(button, scaledDiameter)
                button.layoutParams = buttonLayoutParams.apply {
                    width = scaledDiameter
                    height = scaledDiameter
                }
                //保留原窗口边距, 让系统继续用最终 LayoutParams 更新触摸区域和位置。
                layoutParams.width += scaledDiameter - originalDiameter
                layoutParams.height += scaledDiameter - originalDiameter
                if (!hasLoggedScaledSize) {
                    hasLoggedScaledSize = true
                    YLog.debug(
                        "CustomRotateScreenButtonSize scaled: $originalDiameter -> " +
                            "$scaledDiameter ($percent%)", tag = "LuckyTool"
                    )
                }
            }
        }
        YLog.debug("CustomRotateScreenButtonSize hook success: $percent%", tag = "LuckyTool")
    }

    internal companion object {
        fun scaledDiameter(originalDiameter: Int, percent: Int): Int =
            (originalDiameter * percent.coerceIn(50, 100) / 100f).roundToInt().coerceAtLeast(1)
    }
}
