package com.luckyzyx.luckytool.hook.scopes.launcher

import android.util.Pair
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.ArrayClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object LauncherLayoutRowColume : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 37) loadHooker(LayoutRowColume)
        else loadHooker(LayoutRowColumeV15)
    }

    @Obfuscate
    object LayoutRowColume : YukiBaseHooker() {
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