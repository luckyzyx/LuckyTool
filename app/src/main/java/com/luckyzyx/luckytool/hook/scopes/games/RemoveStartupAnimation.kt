package com.luckyzyx.luckytool.hook.scopes.games

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStartupAnimation : YukiBaseHooker() {
    override fun onHook() {
        //Source GameFloatView.startAnimation(boolean)
        //不能直接拦截 startAnimation：setVisibility(0)、postDelayed(q06, 5000) 和曝光日志都在它里面，拦掉浮窗就不显示了
        //入场动画是 ObjectAnimator.ofFloat(GameOptimizedNewView, "translationX", f, 0f).setDuration(500L)
        //这里只把该动画时长压成 0：原流程全部保留，仅去掉滑动过程（出场动画 183L，不受影响）
        val setDuration = ObjectAnimator::class.resolve().firstMethodOrNull { name = "setDuration" }
            ?: ValueAnimator::class.resolve().firstMethodOrNull { name = "setDuration" }
        if (setDuration == null) {
            YLog.error("RemoveStartupAnimation -> setDuration is null")
            return
        }
        setDuration.hook {
            before {
                val animator = instance as? ObjectAnimator ?: return@before
                if (firstArg().long() != 500L) return@before
                if (animator.propertyName != "translationX") return@before
                if (animator.target?.javaClass?.name != "business.secondarypanel.view.GameOptimizedNewView") {
                    return@before
                }
                firstArg().set(0L)
            }
        }
    }
}
