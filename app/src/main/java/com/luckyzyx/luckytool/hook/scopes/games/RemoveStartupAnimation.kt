package com.luckyzyx.luckytool.hook.scopes.games

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData

@Obfuscate
class RemoveStartupAnimation(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //下面这段处理的是游戏优化浮窗的滑入动画，和启动动画弹窗不是同一条链路，行为保持原样
        //Source GameFloatView.startAnimation(boolean)
        //不能直接拦截 startAnimation：setVisibility(0)、postDelayed(q06, 5000) 和曝光日志都在它里面，拦掉浮窗就不显示了
        //入场动画是 ObjectAnimator.ofFloat(GameOptimizedNewView, "translationX", f, 0f).setDuration(500L)
        //这里只把该动画时长压成 0：原流程全部保留，仅去掉滑动过程（出场动画 183L，不受影响）
        val setDuration = ObjectAnimator::class.resolve().firstMethodOrNull { name = "setDuration" }
            ?: ValueAnimator::class.resolve().firstMethodOrNull { name = "setDuration" }
        if (setDuration == null) YLog.error("RemoveStartupAnimation -> setDuration is null")
        else setDuration.hook {
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

        //启动动画弹窗是另一条链路：StartupAnimationFeature 在 initialize 的协程里创建弹窗，创建动作全在那段 lambda 里
        //它是弹窗唯一的上屏入口，跳过它的 invokeSuspend 就等于弹窗从不被创建（不 addView、也不发埋点）
        val lambdaClass = findStartupAnimationLambda()
        if (lambdaClass == null) {
            YLog.error("RemoveStartupAnimation -> StartupAnimationFeature lambda is null")
            return
        }
        val invokeSuspend = runCatching {
            lambdaClass.name.toClass().resolve().firstMethodOrNull {
                name = INVOKE_SUSPEND
                parameters(Any::class)
            }
        }.onFailure {
            YLog.error("RemoveStartupAnimation -> load ${lambdaClass.name} failed", e = it)
        }.getOrNull()
        if (invokeSuspend == null) {
            YLog.error("RemoveStartupAnimation -> ${lambdaClass.name}#$INVOKE_SUSPEND is null")
            return
        }
        invokeSuspend.hook {
            //该 lambda 内部没有挂起点，正常返回就是协程完成，所以直接返回 kotlin.Unit 而不调原实现
            //返回值必须是 kotlin.Unit：调用方把这段 lambda 的结果当协程结果解包，返回 null 会在解包处抛异常
            intercept(Unit)
        }
    }

    //按字符串常量定位 StartupAnimationFeature 里创建启动动画弹窗的那段协程 lambda
    //弹窗标题 "GameSpaceFloatWindow" 与创建日志 "hyperBoostStart" 只出现在这段 lambda 上，用它当锚点
    //先要求父类是 SuspendLambda 收紧范围，命中不到再放宽；全程不抛异常，找不到返回 null 由调用方记日志
    private fun findStartupAnimationLambda(): ClassData? {
        for (anchor in arrayOf("GameSpaceFloatWindow", "hyperBoostStart")) {
            for (requireSuspendLambda in arrayOf(true, false)) {
                val result = dexKitBridge.findClass {
                    matcher {
                        usingStrings(anchor)
                        if (requireSuspendLambda) superClass = SUSPEND_LAMBDA
                        methods {
                            add {
                                name = INVOKE_SUSPEND
                                paramCount(1)
                            }
                        }
                    }
                }
                result.checkDataList("RemoveStartupAnimation find StartupAnimationFeature ($anchor)")
                result.firstOrNull()?.let { return it }
            }
        }
        return null
    }

    private companion object {
        const val INVOKE_SUSPEND = "invokeSuspend"
        const val SUSPEND_LAMBDA = "kotlin.coroutines.jvm.internal.SuspendLambda"
    }
}
