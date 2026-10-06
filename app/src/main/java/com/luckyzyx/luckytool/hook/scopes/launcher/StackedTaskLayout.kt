package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.dp
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object StackedTaskLayout : YukiBaseHooker() {

    override fun onHook() {

        val isEnable = preferences(ModulePrefs).getBoolean("enable_stacked_task_layout", false)
        val level = preferences(ModulePrefs).getInt("set_task_stacking_level", 7)
        val isFix = preferences(ModulePrefs).getBoolean("fix_current_task_to_the_top", false)
        if (!isEnable) return

        //Source DeviceProfile -> overview_page_spacing
        "com.android.launcher3.DeviceProfile".toClass().resolve().apply {
            firstConstructor { parameterCount { it in 8..11 } }.hook {
                after {
                    if (SDK >= A13) {
                        firstField { name = "overviewPageSpacing" }.of(instance)
                            .set(-(level * 10).dp)
                    }
                }
            }
        }
        var oldView: View? = null
        //Source RecentsView
        "com.android.quickstep.views.RecentsView".toClass().resolve().apply {
            if (SDK < A13) {
                firstMethod {
                    name = "setPageSpacing"
                    superclass()
                }.hook {
                    before {
                        firstArg().set(-(level * 10).dp)
                    }
                }
            }
//            method {
//                name = "applyLoadPlan"
//            }.hook {
//                before {
//                    firstArg().apply {
//                        set(java.util.ArrayList(list<Any>().reversed()))
//                    }
//                }
//            }
            firstMethod { name = "notifyPageSwitchListener" }.hook {
                before {
                    if (!isFix) return@before
                    val count = firstMethod { name = "getTaskViewCount" }.of(instance)
                        .invoke<Int>()
                    if (count == null || count == 0) return@before
                    val view = firstMethod { name = "getCurrentPageTaskView" }.of(instance)
                        .invoke<View>() ?: return@before
                    view.z = 999f
                    if (oldView != view) {
                        oldView?.z = 0f
                        oldView = view
                    }
                }
            }
            firstMethod { name = "resetTaskVisuals" }.hook {
                after {
                    if (!isFix) return@after
                    val count = firstMethod { name = "getTaskViewCount" }.of(instance)
                        .invoke<Int>()
                    if (count == null || count == 0) return@after
                    val view = firstMethod { name = "getCurrentPageTaskView" }.of(instance)
                        .invoke<View>() ?: return@after
                    view.z = 999f
                    if (oldView != view) {
                        oldView?.z = 0f
                        oldView = view
                    }
                }
            }
        }
    }
}