package com.luckyzyx.luckytool.hook.scopes.battery

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookChain
import com.luckyzyx.luckytool.utils.DexkitUtils
import org.lsposed.lsparanoid.Obfuscate
import java.lang.reflect.Modifier

/**
 * 阻止电池组件在 Google 连通性探测失败后给 Google 服务禁网。
 *
 * 控制器要禁网时会走两个入口，它们的第一个参数都是 restricted，true 表示禁网。把它改成 false 后
 * 控制器改为「清除禁网策略」，其余逻辑（包括用户手动改过的包）仍然照常执行，所以这里只改写参数、
 * 必须继续执行原方法，不能把原方法跳过。
 *
 * 方法名随电池版本混淆，只能按「参数类型 + 返回 void + 非静态」定位；两个入口各自独立安装，
 * 其中一个定位不到或挂载失败，不影响另一个继续生效。
 */
@Obfuscate
object KeepGoogleServicesNetwork : YukiBaseHooker() {

    /**
     * 禁网控制器，位于 com.oplus.battery 进程。
     *
     * 该进程与 com.oplus.athena 共用，电池不是进程里第一个加载的包，所以不能加首个包判断，
     * 否则可能永远等不到挂载时机。
     */
    private const val CONTROLLER_CLASS =
        "com.oplus.battery.restrictdynamicfeature.google.GoogleRestrictionController"

    override fun onHook() {
        val controller = runCatching { CONTROLLER_CLASS.toClass().resolve() }.onFailure {
            YLog.error(
                msg = "KeepGoogleServicesNetwork -> $CONTROLLER_CLASS not found",
                e = it,
                tag = DexkitUtils.tag
            )
        }.getOrNull() ?: return

        //禁网入口一：maybeInformListeners(boolean restricted, boolean listChanged, int reason)
        val inform = runCatching {
            controller.method {
                parameters(Boolean::class, Boolean::class, Int::class)
                returnType = Void.TYPE
            }
        }.onFailure {
            YLog.error(
                msg = "KeepGoogleServicesNetwork -> maybeInformListeners not found",
                e = it,
                tag = DexkitUtils.tag
            )
        }.getOrDefault(emptyList()).filterNot { Modifier.isStatic(it.self.modifiers) }
        if (inform.size == 1) inform.single().intercept { liftRestriction() } else YLog.error(
            msg = "KeepGoogleServicesNetwork -> maybeInformListeners not unique (${inform.size}) " +
                    "[${inform.joinToString { it.self.name }}]",
            tag = DexkitUtils.tag
        )

        //禁网入口二：changeGmsControlState(boolean restricted, Set userChangedPackages)
        val control = runCatching {
            controller.method {
                parameters(Boolean::class, Set::class)
                returnType = Void.TYPE
            }
        }.onFailure {
            YLog.error(
                msg = "KeepGoogleServicesNetwork -> changeGmsControlState not found",
                e = it,
                tag = DexkitUtils.tag
            )
        }.getOrDefault(emptyList()).filterNot { Modifier.isStatic(it.self.modifiers) }
        if (control.size == 1) control.single().intercept { liftRestriction() } else YLog.error(
            msg = "KeepGoogleServicesNetwork -> changeGmsControlState not unique (${control.size}) " +
                    "[${control.joinToString { it.self.name }}]",
            tag = DexkitUtils.tag
        )
    }

    /**
     * 把第一个参数 restricted 从 true 改成 false，然后继续执行原方法。
     *
     * 只有 true 需要改写，其他取值一律原样放行；改写方式是给 proceed 传一份新的参数数组，
     * 原方法的实例、其余参数和后续逻辑都保持不动。
     * 原方法返回 void，proceed 的返回值本身不需要处理，这里只是把它交还给拦截器。
     */
    private fun HookChain.liftRestriction(): Any? {
        if (arg<Boolean>(0) != true) return proceed()
        YLog.debug("KeepGoogleServicesNetwork -> ${method.name} restricted true -> false", tag = DexkitUtils.tag)
        return proceed(args.toTypedArray().also { it[0] = false })
    }
}
