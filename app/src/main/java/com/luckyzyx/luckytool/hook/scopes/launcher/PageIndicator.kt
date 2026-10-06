package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object PageIndicator : YukiBaseHooker() {
    override fun onHook() {
        val removeDesktop = preferences(ModulePrefs).getBoolean("remove_pagination_component", false)
        val removeFolder =
            preferences(ModulePrefs).getBoolean("remove_folder_pagination_component", false)
        val disableSliding =
            preferences(ModulePrefs).getBoolean("disable_pagination_component_sliding", false)

        //Source OplusPageIndicator
        "com.android.launcher.pageindicators.OplusPageIndicator".toClass().resolve().apply {
            (firstMethodOrNull { name = "dispatchDraw"; parameters(Canvas::class) }
                ?: firstMethod { name = "onDraw"; parameters(Canvas::class) }).hook {
                before {
                    val view = instance<View>()
                    val parentView = if (view.parent != null) view.parent as View else return@before
                    val entryName = safeOfNull {
                        view.resources.getResourceEntryName(parentView.id)
                    } ?: return@before
                    when (entryName) {
                        "drag_layer" -> if (removeDesktop) {
                            view.isVisible = false
                            result = null
                        }

                        "folder_content_root" -> if (removeFolder) {
                            view.isVisible = false
                            result = null
                        }
                    }
                }
            }
        }

        if (SDK < A13) return

        // C17 起 PageIndicatorTouchHelper 混淆为 pageindicators/p，onActionMove 内联进
        // 其 a(MotionEvent) 入口；两版共用的滑动切换点是
        // OplusPageIndicator.getSwitchTargetPage，返回 -1 时新旧版均跳过页面切换，
        // 且不影响按压反馈动画与统计。
        //Source OplusPageIndicator getSwitchTargetPage
        "com.android.launcher.pageindicators.OplusPageIndicator".toClass().resolve().apply {
            firstMethod {
                name = "getSwitchTargetPage"
                parameters(MotionEvent::class)
            }.hook {
                if (disableSliding) {
                    intercept(-1)
                }
            }
        }

        //Source BigFolderIcon
        "com.android.launcher3.folder.big.BigFolderIcon".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "onScrollPageStart" }.hook {
                after {
                    if (removeFolder) firstField { name = "indicator" }.of(instance)
                        .get<View>()?.isVisible = false
                }
            }
            firstMethod { name = "exposureForWorkspace" }.hook {
                after {
                    if (removeFolder) firstField { name = "indicator" }.of(instance)
                        .get<View>()?.isVisible = false
                }
            }
        }
    }
}