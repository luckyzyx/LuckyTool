package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveDeviceNameChangeLimit : YukiBaseHooker() {
    override fun onHook() {
        //Source PhoneNameSettingsActivity -> AlertDialog

        //Source PhoneNameVerifyUtil
        "com.oplus.settings.feature.deviceinfo.aboutphone.PhoneNameVerifyUtil".toClass().resolve()
            .apply {
                firstMethod { name = "activeVerifyPhoneName" }.hook {
                    before {
                        val callback = lastArg().get() ?: return@before
                        callback.asResolver().firstMethod { name = "onSuccess" }.invoke(null)
                        result = null
                    }
                }
                firstMethod { name = "timeScheduleVerifyPhoneName" }.hook {
                    intercept()
                }
            }

        //Source WirelessDeviceVerifyUtils
        "com.oplus.settings.utils.WirelessDeviceVerifyUtils".toClass().resolve().apply {
            firstMethod { name = "activeVerifyPhoneName" }.hook {
                before {
                    val callback = lastArg().get() ?: return@before
                    callback.asResolver().firstMethod { name = "onSuccess" }.invoke(null)
                    result = null
                }
            }
        }

        //Source OplusDeviceInfoUtils
        "com.oplus.settings.utils.OplusDeviceInfoUtils".toClass().resolve().apply {
            firstMethod { name = "getVerifyNameCondition" }.hook {
                intercept(false)
            }
        }
    }
}