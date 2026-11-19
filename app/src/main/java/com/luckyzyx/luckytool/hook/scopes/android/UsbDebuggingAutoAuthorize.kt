package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * USB调试自动授权：adbd 收到未知公钥后等待系统确认时，直接走官方 allowDebugging 流程，
 * 不启动确认界面。仅处理 USB 公钥入口，Wi-Fi 调试网络确认保持原样。
 */
@Obfuscate
object UsbDebuggingAutoAuthorize : YukiBaseHooker() {
    private const val TAG = "UsbDebuggingAutoAuthorize"
    private const val PREF_KEY = "usb_debugging_auto_authorize"

    override fun onHook() {
        val enabled = AtomicBoolean(
            preferences(ModulePrefs).getBoolean(PREF_KEY, false)
        )
        dataChannel.wait<Boolean>(PREF_KEY) { enabled.set(it) }

        //Source AdbDebuggingManager -> startConfirmationForKey
        "com.android.server.adb.AdbDebuggingManager".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull {
                name = "startConfirmationForKey"
                parameters(String::class, String::class)
            }?.hook {
                before {
                    if (!enabled.get()) return@before
                    val key = firstArg().get<String>() ?: return@before
                    val allowDebugging = firstMethodOrNull {
                        name = "allowDebugging"
                        parameters(Boolean::class, String::class)
                    }
                    if (allowDebugging == null) {
                        YLog.warn("$TAG: allowDebugging not found")
                        return@before
                    }
                    allowDebugging.of(instance).invoke(true, key)
                    YLog.debug("$TAG: auto authorize new adb key")
                    result = null
                }
            } ?: YLog.warn("$TAG: startConfirmationForKey not found")
        } ?: YLog.warn("$TAG: AdbDebuggingManager not found")
    }
}
