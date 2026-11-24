package com.luckyzyx.luckytool.hook.utils.sysui

import android.content.Context
import android.os.Build
import androidx.annotation.DeprecatedSinceApi
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
class BatteryControllerUtils(val classLoader: ClassLoader?) {

    val clazz = VariousClass(
        "com.oplusos.systemui.keyguard.charginganim.ChargingAnimationImpl", //C12 C13
        "com.oplusos.systemui.common.battery.OplusBatteryController" //C14
    ).load(classLoader)

    fun getInstance(context: Context): Any? {
        return clazz.resolve().firstMethod { name = "getInstance" }.let {
            if (it.self.parameterCount == 0) it.invoke()
            else it.invoke(context)
        }
    }

    @DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU, "仅在A13中使用")
    fun getChargerTechnology(instance: Any): Int {
        return instance.asResolver().firstMethod { name = "getChargerTechnology" }.invoke<Int>()
            ?: 0
    }

    @DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU, "仅在A13中使用")
    fun getPPSMode(instance: Any): Int {
        return instance.asResolver().firstField {
            name = if (SDK >= A14) "chargeMode" else "mPPSState"
        }.get<Int>() ?: 0
    }

    @DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU, "仅在A13中使用")
    fun isWirelessCharging(instance: Any): Boolean {
        return instance.asResolver().firstMethod {
            name = if (SDK >= A14) "getWirelessCharging" else "isWirelessCharging"
        }.invoke<Boolean>() ?: false
    }

    fun getTechnologyName(
        technology: Int = 0, usbFastChgType: Int = 0, ppsMode: Int = 0, isWireless: Boolean = false
    ): String {
        /**
         * usb_fast_chg_type
         * 1 vooc
         * 2 svooc
         * 3 pd
         * 4 qc
         * 5 pps
         * 6 ufcs
         */
//        YLog.debug("getTechnologyName -> $technology | $usbFastChgType | $ppsMode")
        return when (technology) {
            1 -> when (usbFastChgType) {
                3 -> "PD"
                4 -> "QC"
                else -> "Normal"
            }

            2 -> if (isWireless) "AirVOOC" else "VOOC"

            3 -> when (usbFastChgType) {
                5 -> when (ppsMode) {
                    1 -> "PrivatePPS"
                    2 -> "PublicPPS"
                    else -> "Normal"
                }

                6 -> when (ppsMode) {
                    3 -> "PublicUFCS"
                    4 -> "PrivateUFCS"
                    else -> "Normal"
                }

                else -> if (isWireless) "AirVOOC" else "SUPERVOOC"
            }

            else -> "Normal"
        }
    }

    fun getTechnologyNameOld(
        technology: Int = 0, ppsMode: Int = 0, isWireless: Boolean = false
    ): String {
        return when (technology) {
            0 -> when (ppsMode) {
                1 -> "PrivatePPS"
                2 -> "PublicPPS"
                3 -> "PublicUFCS"
                4 -> "PrivateUFCS"
                else -> "Normal"
            }

            1 -> if (isWireless) "AirVOOC" else "VOOC"
            2 -> if (isWireless) "AirVOOC" else "SUPERVOOC"
            20 -> if (isWireless) "AirVOOC" else "SUPERVOOC2.0"
            30 -> if (isWireless) "AirVOOC" else "SUPERVOOC Athena Foreign Pro"
            25 -> if (isWireless) "AirVOOC" else "VOOC Beta Pro"
            3 -> "PD"
            4 -> "QC"
            5 -> "PPS" //null
            6 -> "UFCS" //null
            else -> "[${technology}]"
        }
    }
}