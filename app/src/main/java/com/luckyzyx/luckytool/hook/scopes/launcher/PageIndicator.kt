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

        // 滑动指示器切换页面：旧版走 OplusPageIndicator.getSwitchTargetPage（返回 -1 跳过切换），
        // 17.x 起该入口被去掉，改为在 OplusDragLayer 持有的手势 helper 上拦 onScroll / onFling，
        // 只挡滑动、点击与按压反馈保持原样
        val indicator = "com.android.launcher.pageindicators.OplusPageIndicator".toClass().resolve()
        val switchTargetPage = indicator.firstMethodOrNull {
            name = "getSwitchTargetPage"
            parameters(MotionEvent::class)
        }
        if (switchTargetPage != null) {
            switchTargetPage.hook {
                if (disableSliding) intercept(-1)
            }
        } else {
            // 手势 helper 类名是混淆的，按 OplusDragLayer 上那个固定字段的类型取；
            // 翻页判定写在它那个 void(MotionEvent) 入口里，同签名的另两个是 GestureDetector
            // 回调（onLongPress / onShowPress，名字不会被混淆），入口则只剩一两个字符
            val helper = "com.android.launcher3.OplusDragLayer".toClass().resolve()
                .firstFieldOrNull { name = "mPageIndicatorTouchHelper" }
                ?.self?.type
            helper?.resolve()?.apply {
                method { parameters(MotionEvent::class) }
                    .firstOrNull { it.self.returnType == Void.TYPE && it.self.name.length <= 3 }
                    ?.hook { if (disableSliding) intercept() }
            }
            // 指示器自己也要吃掉触摸：它不消费时事件会落到底下的工作区，由工作区完成翻页
            indicator.firstMethodOrNull { name = "onTouchEvent"; parameters(MotionEvent::class) }?.hook {
                if (disableSliding) intercept(true)
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