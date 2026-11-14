package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveWidgetsAddRequestWhitelist : YukiBaseHooker() {
    override fun onHook() {
        //Source AddItemActivity
        val addItemActivity = "com.android.launcher3.dragndrop.AddItemActivity".toClass().resolve()
        //Source WidgetControlHelper
        // 白名单判据类在 17.x 已被移除，存不存在就是两条实现的分界
        if ("com.android.launcher3.widget.WidgetControlHelper".toClassOrNull() != null) {
            addItemActivity.firstMethod {
                name = "isAllowedAddWidget"
                parameterCount { it in 1..2 }
            }.hook {
                intercept(true)
            }
            return
        }
        // 17.x 把白名单判定搬进了 SecurityPermission，桌面只负责向
        // content://com.oplusos.provider.PermissionProvider 查询状态。
        // 宿主对不在白名单里的应用直接给 1（拒绝并显示拦截页），对白名单应用给 0 或 2，
        // 所以这里只把「直接拒绝」降级成「询问用户」，让所有应用都能发起添加请求，
        // 0 / 2 / 3 保持宿主原有表现，用户仍然会对每次添加做确认
        val requestType =
            addItemActivity.firstFieldOrNull { name = "mRequestType" }
                ?: error("AddItemActivity 没有 mRequestType，白名单放行已无法区分请求类型")
        // 拦截器模式拿不到宿主实例上的 mRequestType，这里用经典模式在原实现跑完后改写返回值
        addItemActivity.firstMethod {
            name = "getAddItemPermissionState"
            parameterCount = 1
        }.hook {
            after {
                if (requestType.of(instance<Any>()).get<Int>() == 2 && result == 1) result = 2
            }
        }
    }
}