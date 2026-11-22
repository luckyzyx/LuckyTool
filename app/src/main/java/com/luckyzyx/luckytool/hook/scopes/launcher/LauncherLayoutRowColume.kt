package com.luckyzyx.luckytool.hook.scopes.launcher

import android.util.Pair
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.ArrayClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class LauncherLayoutRowColume(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 40) loadHooker(LayoutRowColume(dexKitBridge))
        else if (osCode >= 37) loadHooker(LayoutRowColumeV16)
        else loadHooker(LayoutRowColumeV15)
    }

    @Obfuscate
    class LayoutRowColume(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val maxRows = preferences(ModulePrefs).getInt("launcher_layout_max_rows", 6)
            val maxColumns = preferences(ModulePrefs).getInt("launcher_layout_max_columns", 4)

            //Source UiConfig (混淆类名逐版本变化): 优先用源文件名定位, 新版本已剥离 .source 信息, 回落到打印布局日志的那个方法
            //isSupportLayout(II)Z 校验行列数组合, getSupportLayout() 返回支持的组合列表
            var uiConfigList = dexKitBridge.findClass {
                matcher { source("UiConfig.java") }
            }
            if (uiConfigList.isNullOrEmpty()) {
                uiConfigList = dexKitBridge.findClass {
                    matcher { usingStrings("isUseSmallLayout") }
                }
            }
            val uiConfig = uiConfigList.apply {
                checkDataList("LauncherLayoutRowColume UiConfig")
            }.singleOrNull() ?: return
            //同类中 static + 无参返回 ArrayList 的方法有多个, 只有被行列校验方法 (II)Z 调用的那个是 getSupportLayout()
            //调用方钉死到本类而不是弹窗适配器: 适配器那类调用方同样依赖 .source, 新版本一样会断
            val supportLayout = uiConfig.findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<ArrayList<*>>())
                    addCaller {
                        declaredClass { descriptor(uiConfig.descriptor) }
                        paramTypes(classOf<Int>(), classOf<Int>())
                        returnType(classOf<Boolean>())
                    }
                }
            }.apply { checkDataList("LauncherLayoutRowColume getSupportLayout") }.singleOrNull() ?: return
            //同类中 static + (II)Z 的方法有两个, 只有内部调用 getSupportLayout() 的那个是 isSupportLayout()
            val isSupportLayout = uiConfig.findMethod {
                matcher {
                    paramTypes(classOf<Int>(), classOf<Int>())
                    returnType(classOf<Boolean>())
                    //被调方钉死到已解析的 UiConfig 类与形状: 这里同为 0 参同名的还有 x0.h()Z, 只按名字匹配会被误接受
                    addInvoke {
                        declaredClass { descriptor(uiConfig.descriptor) }
                        name(supportLayout.methodName)
                        paramCount(0)
                        returnType(classOf<ArrayList<*>>())
                    }
                }
            }.apply { checkDataList("LauncherLayoutRowColume isSupportLayout") }.singleOrNull() ?: return

            //返回自定义的行列数组合
            supportLayout.className.toClass().resolve().firstMethod {
                name = supportLayout.methodName
                emptyParameters()
            }.hook {
                before {
                    result = ArrayList<Pair<Int, Pair<Int, Int>>>().apply {
                        for (col in 4..maxColumns) {
                            for (row in 6..maxRows) {
                                add(Pair(col, Pair(row, row + 1)))
                            }
                        }
                    }
                }
            }

            //放行所有行列数组合
            isSupportLayout.className.toClass().resolve().firstMethod {
                name = isSupportLayout.methodName
                parameters(Int::class, Int::class)
                returnType = Boolean::class
            }.hook {
                intercept(true)
            }

            //Source InvariantDeviceProfile
            //getPredefinedDeviceProfiles 按 idp_grid_name 过滤网格选项, 官方列表里没有的组合会被过滤空
            //然后回落到 canBeDefault 的默认网格, 于是点应用后行列数又变回去; 这里把回落出来的网格改写成用户选择的组合
            "com.android.launcher3.InvariantDeviceProfile".toClass().resolve().firstMethod {
                name = "getPredefinedDeviceProfiles"
                parameterCount = 4
                returnType = ArrayList::class
            }.hook {
                after {
                    val gridName = args[1] as? String ?: return@after
                    val gridShape = gridName.split("_by_")
                    if (gridShape.size != 2) return@after
                    val column = gridShape[0].toIntOrNull() ?: return@after
                    val row = gridShape[1].toIntOrNull() ?: return@after
                    if (column !in 4..maxColumns || row !in 6..maxRows) return@after
                    val options = result<ArrayList<*>>() ?: return@after
                    val option = options.firstOrNull() ?: return@after
                    //选项实际是 OplusDisplayOption, grid 字段声明在父类 DisplayOption 上
                    val gridOption = option.asResolver()
                        .firstFieldOrNull { name = "grid"; superclass() }?.get<Any>()
                        ?: return@after
                    //官方已存在该组合时不处理, 只改写回落出来的那个选项
                    val currentName = gridOption.asResolver()
                        .firstFieldOrNull { name = "name"; type = String::class }?.get<String>()
                    if (currentName == gridName) return@after
                    gridOption.asResolver().apply {
                        firstFieldOrNull { name = "name"; type = String::class }?.set(gridName)
                        firstFieldOrNull { name = "numColumns"; type = Int::class }?.set(column)
                        firstFieldOrNull { name = "numRows"; type = Int::class }?.set(row)
                        firstFieldOrNull { name = "isEnabled"; type = Boolean::class }?.set(true)
                    }
                    result = arrayListOf(option)
                }
            }

            //Source IconFallenUtils (混淆类名逐版本变化, 新版本已剥离 .source, 改用调用点锚定)
            //getLogicCellX(ItemInfo)I 计算桌面图标所在列数, 只被图标掉落动画管理器调用(同类同参的 g(ItemInfo)Z 靠返回类型排除)
            val getLogicCellX = dexKitBridge.findClass {
                matcher {
                    methods {
                        add {
                            paramTypes("com.android.launcher3.model.data.ItemInfo".toClass())
                            returnType(classOf<Int>())
                            addCaller {
                                declaredClass {
                                    className("com.android.launcher3.util.OplusIconFallenAnimationManager")
                                }
                            }
                        }
                    }
                }
            }.apply {
                checkDataList("LauncherLayoutRowColume getLogicCellX class")
            }.findMethod {
                matcher {
                    paramTypes("com.android.launcher3.model.data.ItemInfo".toClass())
                    returnType(classOf<Int>())
                    addCaller {
                        declaredClass {
                            className("com.android.launcher3.util.OplusIconFallenAnimationManager")
                        }
                    }
                }
            }.apply { checkDataList("LauncherLayoutRowColume getLogicCellX") }.singleOrNull() ?: return

            //列数超过 4 时按 4 处理
            getLogicCellX.className.toClass().resolve().firstMethod {
                name = getLogicCellX.methodName
                parameterCount = 1
                returnType = Int::class
            }.hook {
                after {
                    val res = result<Int>() ?: return@after
                    if (res > 4) result = 4
                }
            }
        }
    }

    @Obfuscate
    object LayoutRowColumeV16 : YukiBaseHooker() {
        override fun onHook() {
            val maxRows = preferences(ModulePrefs).getInt("launcher_layout_max_rows", 6)
            val maxColumns = preferences(ModulePrefs).getInt("launcher_layout_max_columns", 4)

            //Source UiConfig
            "com.android.launcher.UiConfig".toClass().resolve().apply {
                firstMethod { name = "isSupportLayout" }.hook {
                    intercept(true)
                }
                firstMethod { name = "getSupportLayout" }.hook {
                    before {
                        result = ArrayList<Pair<Int, Pair<Int, Int>>>().apply {
                            for (col in 4..maxColumns) {
                                for (row in 6..maxRows) {
                                    add(Pair(col, Pair(row, row + 1)))
                                }
                            }
                        }
                    }
                }
            }

            //Source IconFallenUtils
            "com.android.launcher.iconfallen.IconFallenUtils".toClass().resolve().apply {
                firstMethod {
                    name = "getLogicCellX"
                    returnType = Int::class
                }.hook {
                    after {
                        val res = result<Int>() ?: return@after
                        if (res > 4) result = 4
                    }
                }
            }
        }
    }

    @Obfuscate
    object LayoutRowColumeV15 : YukiBaseHooker() {
        override fun onHook() {
            //Source UiConfig
            "com.android.launcher.UiConfig".toClass().resolve().apply {
                firstMethod { name = "isSupportLayout" }.hook {
                    intercept(true)
                }
            }
            val maxRows = preferences(ModulePrefs).getInt("launcher_layout_max_rows", 6)
            val maxColumns = preferences(ModulePrefs).getInt("launcher_layout_max_columns", 4)
            //Source ToggleBarLayoutAdapter
            "com.android.launcher.togglebar.adapter.ToggleBarLayoutAdapter".toClass().resolve()
                .apply {
                    firstMethod { name = "initToggleBarLayoutConfigs" }.hook {
                        before {
                            firstField {
                                name = "MIN_MAX_COLUMN"
                                type = ArrayClass(Int::class)
                            }.get<IntArray>()?.set(1, maxColumns)
                            firstField {
                                name = "MIN_MAX_ROW"
                                type = ArrayClass(Int::class)
                            }.get<IntArray>()?.set(1, maxRows)
                        }
                    }
                }
        }
    }
}
