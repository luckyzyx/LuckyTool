package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object BluetoothIconRelated : Hooker {

    private val BluetoothController = "com.android.systemui.statusbar.policy.BluetoothController"
    private val StatusBarIconController =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconController"

    override fun onHook() {
        var isHide = prefs(ModulePrefs).getBoolean("hide_icon_when_bluetooth_not_connected", false)
        dataChannel.wait<Boolean>("hide_icon_when_bluetooth_not_connected") { isHide = it }

        //Source PhoneStatusBarPolicyEx
        VariousClass(
            "com.oplusos.systemui.statusbar.phone.PhoneStatusBarPolicyEx", //C13
            "com.oplus.systemui.statusbar.phone.OplusPhoneStatusBarPolicyExImpl" //C14
        ).toClass().resolve().optional(true).apply {
            firstMethodOrNull { name = "updateBluetoothIcon";parameterCount = 4 }?.hook {
                before {
                    if (!isHide) return@before
                    val isBluetoothEnabled = args().last().boolean()
                    val controller = firstField {
                        type = BluetoothController
                        if (SDK < A14) superclass()
                    }.of(instance).get() ?: return@before
                    val isBluetoothConnected = controller.asResolver().firstMethod {
                        name = "isBluetoothConnected"
                    }.invoke<Boolean>() ?: return@before
                    args().last().set(isBluetoothEnabled && isBluetoothConnected)
                }
            } ?: run {
                (firstMethodOrNull {
                    name = "updateBluetooth"
                    emptyParameters()
                } ?: firstMethod {
                    name { it.contains("updateBluetooth") }
                    emptyParameters()
                }).hook {
                    before {
                        if (!isHide) return@before
                        val bluetoothController = (firstFieldOrNull { type = BluetoothController }
                            ?: firstField { type = BluetoothController }).of(instance).get()
                            ?: return@before
                        val statusBarIconController = firstField { type = StatusBarIconController }
                            .of(instance).get() ?: return@before
                        val slotBluetooth =
                            firstField { name = "slotBluetooth" }.of(instance).get<String>()
                        val isBluetoothEnabled = bluetoothController.asResolver().firstField {
                            name = "mEnabled"
                        }.get<Boolean>() ?: false
                        val bluetoothConnectionState = bluetoothController.asResolver().firstField {
                            name = "mConnectionState"
                        }.get<Int>()
                        if (isBluetoothEnabled && bluetoothConnectionState != 2) {
                            statusBarIconController.asResolver().firstMethod {
                                name = "setIconVisibility"
                            }.invoke(slotBluetooth, false)
                            resultNull()
                        }
                    }
                }
            }
        }
    }
}