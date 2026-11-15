package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object PageIndicator : YukiBaseHooker() {

    // 指示器上按下后移动超过这个距离才算「滑动」，避免把点击当成拖动
    private const val TOUCH_SLOP = 24

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

        // 滑动指示器切换页面：旧版靠 OplusPageIndicator.getSwitchTargetPage（返回 -1 跳过切换），
        // 17.x 起该入口被去掉，改成让指示器自己吃掉触摸——它不消费时事件会落到底下的工作区，
        // 由工作区当成普通滑动翻页；点击打开小布助手走 OplusDragLayer 转发的手势 helper，不受影响
        val indicator = "com.android.launcher.pageindicators.OplusPageIndicator".toClass().resolve()
        val switchTargetPage = indicator.firstMethodOrNull {
            name = "getSwitchTargetPage"
            parameters(MotionEvent::class)
        }
        if (switchTargetPage != null) {
            switchTargetPage.hook { if (disableSliding) intercept(-1) }
        } else {
            indicator.firstMethodOrNull { name = "onTouchEvent"; parameters(MotionEvent::class) }
                ?.hook { before { if (disableSliding) result = true } }
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