package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.os.SystemClock
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveUSBConnectDialog : YukiBaseHooker() {
    //同一轮插线里宿主会反复请求弹窗（服务重建、通知点击），静默期内只压弹窗、不重复切换 USB 模式
    private const val FORCE_INTERVAL = 3000L
    private var lastForceTime = 0L

    override fun onHook() {
        //Source UsbService
        val usbService = VariousClass(
            "com.coloros.systemui.notification.usb.UsbService", //A11
            "com.oplusos.systemui.notification.usb.UsbService",
            "com.oplus.systemui.usb.UsbService" //C14 C15
        ).toClass().resolve()

        val showDialog = usbService.firstMethodOrNull {
            name = "showUsbDialog"
            parameters(Boolean::class)
        }

        if (showDialog != null) {
            //选择 USB 用途的弹窗统一由 showUsbDialog 弹出（插线走 updateUsbNotification 时传 false），
            //新版本的 onUsbConnected 已不在插线路径上，所以改拦这个收口点
            showDialog.intercept {
                val service = instanceOrNull ?: return@intercept proceed()
                //置回标志位，避免后续通知回调再走一次弹窗
                usbService.firstFieldOrNull { name { it.contains("NeedShowUsbDialog", true) } }
                    ?.of(service)?.set(false)
                val now = SystemClock.elapsedRealtime()
                if (now - lastForceTime < FORCE_INTERVAL) return@intercept null
                lastForceTime = now
                //onUsbSelect(1) 会广播 com.oplus.test.file.transmit，由宿主自己落盘选择并切换模式
                usbService.firstMethodOrNull { name = "onUsbSelect" }?.of(service)?.invoke(1)
                val context = usbService.firstFieldOrNull { name = "mContext" }
                    ?.of(service)?.get<Context>()
                if (context != null) {
                    usbService.firstMethodOrNull {
                        name = "changeUsbConfig"
                        parameters(Int::class, Context::class)
                    }?.of(service)?.invoke(1, context)
                }
                null
            }
            return
        }

        //旧版本仍以 onUsbConnected 作为 USB 连接后的入口
        usbService.apply {
            (firstMethodOrNull { name = "onUsbConnected" }
                ?: firstMethod { name { it.contains("onUsbConnected") } }).hook {
                before {
                    val instance = instanceOrNull ?: firstArg().get()
                    val context = lastArg().get<Context>() ?: return@before
                    firstMethod { name = "onUsbSelect" }.of(instance).invoke(1)
                    firstMethod { name = "updateAdbNotification" }.of(instance).invoke(context)
                    firstMethod { name = "updateUsbNotification" }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    firstMethod { name = "changeUsbConfig" }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    result = null
                }
            }
            firstMethod { name = "updateUsbNotification" }.hook {
                before {
                    firstField {
                        name { it.contains("NeedShowUsbDialog", true) }
                    }.of(instance).set(false)
                }
            }
        }
    }
}
