package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.resolver.MethodResolver
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

/**
 * 允许锁定或解锁已排除活动
 *
 * 判断入口是 quickstep 的 AppLock 管理器，判断时会把 Intent 的 EXCLUDE_FROM_RECENTS 标志去掉，
 * 让本该排除的界面也能参与锁定/解锁判断。
 *
 * C17 起管理器的类名与方法名都被混淆（com.oplus.quickstep.applock.b#j），名字不再可依赖，
 * 只有「锁定包内 (String, String, Intent) -> Boolean」这个签名保持稳定，所以优先按签名定位；
 * 签名落空时再回退到旧版本的类名 + 方法名。
 * 两条路都落空时只记日志：这里抛出的异常会中断 HookLauncher 后续 hooker 的装载。
 */
@Obfuscate
class AllowLockingUnLockingOfExcludedActivity(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        //锁定判断所在的包，按签名定位时用它排除包外的同签名方法
        private const val APP_LOCK_PACKAGE = "com.oplus.quickstep.applock"

        //锁定管理器类名，C13 及以下为 ColorLockManager，C14 起换成 Oplus 前缀
        private val LOCK_MANAGER_CLASS_NAMES = arrayOf(
            "com.coloros.quickstep.applock.ColorLockManager",
            "com.oplus.quickstep.applock.OplusLockManager",
        )

        //旧版本的锁定判断方法名：isAppLockable / isAppSupportLock 在不同版本间来回换，按顺序取第一个存在的
        private val LOCKABLE_METHOD_NAMES = arrayOf("isAppLockable", "isAppSupportLock")
    }

    override fun onHook() {
        //DexKit 查的是 dex 文件，只给类名与方法名；可 hook 的 Method 要在宿主类加载器里查，链式三步省不掉
        val entry = dexKitBridge.findClass {
            matcher {
                methods {
                    add {
                        paramTypes(classOf<String>(), classOf<String>(), classOf<Intent>())
                        returnType(classOf<Boolean>())
                    }
                }
            }
        }.apply {
            checkDataList("AllowLockingUnLockingOfExcludedActivity AppLockSupportLock class")
        }.findMethod {
            matcher {
                paramTypes(classOf<String>(), classOf<String>(), classOf<Intent>())
                returnType(classOf<Boolean>())
            }
        }.apply {
            checkDataList("AllowLockingUnLockingOfExcludedActivity AppLockSupportLock")
        }.singleOrNull { it.className.startsWith("$APP_LOCK_PACKAGE.") }
            ?: return hookByKnownClassNames()

        entry.className.toClass().resolve().firstMethodOrNull {
            name = entry.methodName
            parameters(String::class, String::class, Intent::class)
            returnType = Boolean::class
        }?.hookExcludeFromRecentsFlag() ?: hookByKnownClassNames()
    }

    /**
     * 按类名 + 方法名定位判断入口
     *
     * 用于类名未被混淆、但签名不是 (String, String, Intent) 的旧版本（如只带 Intent 的判断）。
     */
    private fun hookByKnownClassNames() {
        val lockManager =
            runCatching { VariousClass(*LOCK_MANAGER_CLASS_NAMES).toClass() }.getOrNull()
        if (lockManager != null) {
            val resolver = lockManager.resolve()
            for (methodName in LOCKABLE_METHOD_NAMES) {
                val method = resolver.firstMethodOrNull { name = methodName } ?: continue
                method.hookExcludeFromRecentsFlag()
                return
            }
        }
        YLog.error("AllowLockingUnLockingOfExcludedActivity -> 签名与类名均未命中，跳过")
    }

    /** 判断前去掉 EXCLUDE_FROM_RECENTS：已排除活动的判断因此与普通活动一致 */
    private fun MethodResolver<*>.hookExcludeFromRecentsFlag() = intercept {
        (args.lastOrNull() as? Intent)?.let { intent ->
            if (intent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS ==
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            ) {
                intent.removeFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        }
        proceed()
    }
}