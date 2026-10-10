package com.luckyzyx.luckytool.service.base

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.bindRootService
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("PropertyName")
abstract class BaseControllerService<T : IInterface> {
    abstract val TAG: String
    abstract var controllerService: Class<*>
    open var controller: T? = null

    open fun init(context: Context) {
        get(context) {}
    }

    abstract fun getController(iBinder: IBinder?): T?

    open fun get(context: Context?, result: (T?) -> Unit) {
        // 已绑定且 binder 存活：直接复用缓存，避免每次进入页面重复绑定 RootService
        val cached = controller
        if (cached != null && cached.asBinder().isBinderAlive) {
            result(cached)
            return
        }
        // 缓存已死（root 守护进程重启）或尚未绑定：清空缓存后重新绑定
        if (cached != null) controller = null
        if (context == null) {
            result(null)
            return
        }
        context.bindRootService(
            controllerService,
            onConnected = { _: ComponentName?, iBinder: IBinder? ->
                controller = getController(iBinder)

                val uid = Binder.getCallingUid()
                val userid = UserHandle::class.resolve().firstMethod {
                    name = "getUserId"
                    parameters(Int::class)
                }.invoke(Binder.getCallingUid())
                LogUtils.d(
                    TAG, "get ($uid : $userid)", "${controller != null}", true
                )
                result(controller)
            },
            onDisconnected = { _ ->
                // 服务断开：清空缓存，下次请求时自动重绑
                controller = null
                LogUtils.w(TAG, "onServiceDisconnected", "root service died", true)
            },
        )
    }

    /** 同步返回「已绑定且存活」的缓存控制器；未绑定或 binder 已死时返回 null（不触发重绑）。 */
    fun getCachedController(): T? = controller?.takeIf { it.asBinder().isBinderAlive }
}