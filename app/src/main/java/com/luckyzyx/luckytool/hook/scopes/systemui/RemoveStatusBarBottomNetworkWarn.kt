package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.toClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStatusBarBottomNetworkWarn : Hooker {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 34) loadHooker(StatusBarBottomNetworkWarn)
        else loadHooker(StatusBarBottomNetworkWarnOld)
    }

    @Obfuscate
    object StatusBarBottomNetworkWarn : Hooker {
        override fun onHook() {
            var removeMode = prefs(ModulePrefs).getString("remove_control_center_networkwarn", "0")
            dataChannel.wait<String>("remove_control_center_networkwarn") { removeMode = it }

            //Source OplusQSSecurityController
            "com.oplus.systemui.qs.policy.OplusQSSecurityController".toClass().resolve().optional(true).apply {
                firstMethod { name = "showDeviceMonitoringDialog" }.hook {
                    if (removeMode == "1" || removeMode == "2") intercept()
                }
                (firstMethodOrNull { name = "handleRefreshState" }
                    ?: firstMethod { name { it.contains("handleRefreshState") } }).hook {
                    if (removeMode == "2") intercept()
                }
            }
        }
    }

    @Obfuscate
    object StatusBarBottomNetworkWarnOld : Hooker {
        override fun onHook() {
            var removeMode = prefs(ModulePrefs).getString("remove_control_center_networkwarn", "0")
            dataChannel.wait<String>("remove_control_center_networkwarn") { removeMode = it }

            //Source OplusQSSecurityText
            VariousClass(
                "com.oplusos.systemui.qs.widget.OplusQSSecurityText", //C13
                "com.oplus.systemui.qs.widget.OplusQSSecurityText" //C14
            ).toClass().resolve().optional(true).apply {
                firstMethod { name = "handleClick" }.hook {
                    if (removeMode == "1" || removeMode == "2") intercept()
                }
                firstMethod { name = "handleRefreshState" }.hook {
                    if (removeMode == "2") intercept()
                }
            }
        }
    }
}