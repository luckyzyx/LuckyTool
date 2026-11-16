package com.luckyzyx.luckytool.hook.scopes.battery

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.KavaRef.MemberScope
import com.highcapable.kavaref.extension.isStatic
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate
import kotlin.reflect.KClass

/**
 * 阻止电池组件在 Google 连通性探测失败后给 Google 服务禁网。
 *
 * 控制器的两个禁网入口第一个参数都是 restricted，true 表示禁网。把它改成 false 后控制器改为「清除禁网策略」，
 * 其余逻辑（包括用户手动改过的包）仍然照常执行，所以这里只改写参数、必须继续执行原方法，不能把原方法跳过。
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
            YLog.error(msg = "KeepGoogleServicesNetwork -> $CONTROLLER_CLASS not found", e = it)
        }.getOrNull() ?: return

        //maybeInformListeners(boolean restricted, boolean listChanged, int reason)
        controller.hookRestrictionEntry(Boolean::class, Boolean::class, Int::class)
        //changeGmsControlState(boolean restricted, Set userChangedPackages)
        controller.hookRestrictionEntry(Boolean::class, Set::class)
    }

    /**
     * 挂载一个禁网入口：把第一个参数 restricted 从 true 改成 false，然后继续执行原方法。
     *
     * 方法名随电池版本混淆，只能按「参数类型 + 返回 void + 非静态」定位，必须唯一命中；
     * 两个入口各自独立挂载，其中一个定位不到或挂载失败，不影响另一个继续生效。
     */
    private fun <T : Any> MemberScope<T>.hookRestrictionEntry(vararg types: KClass<*>) {
        val signature = types.joinToString(", ") { it.simpleName ?: it.toString() }
        val entry = runCatching {
            method { parameters(*types); returnType = Void.TYPE }
        }.getOrElse {
            YLog.error(msg = "KeepGoogleServicesNetwork -> ($signature) query failed", e = it)
            emptyList()
        }.filterNot { it.self.isStatic }

        if (entry.size != 1) {
            YLog.error(
                msg = "KeepGoogleServicesNetwork -> ($signature) expected 1, found ${entry.size} " +
                        "[${entry.joinToString { it.self.name }}]"
            )
            return
        }

        //只有 true 需要改写，其他取值一律原样放行；原方法返回 void，proceed 的返回值不用处理
        entry.single().intercept {
            if (arg<Boolean>(0) != true) return@intercept proceed()
            YLog.debug("KeepGoogleServicesNetwork -> ${method.name} restricted true -> false")
            proceed(args.toTypedArray().also { it[0] = false })
        }
    }
}
