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

                else -> if (isWireless) "AirSVOOC" else "SUPERVOOC"
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
            2 -> if (isWireless) "AirSVOOC" else "SUPERVOOC"
            20 -> if (isWireless) "AirSVOOC2" else "SUPERVOOC2.0"
            30 -> if (isWireless) "AirSVOOC" else "SUPERVOOC Athena Foreign Pro"
            25 -> if (isWireless) "AirVOOC" else "VOOC Beta Pro"
            3 -> "PD"
            4 -> "QC"
            5 -> "PPS" //null
            6 -> "UFCS" //null
            else -> "[${technology}]"
        }
    }

    companion object {
        private val VOOC_ADAPTER_ID_LIST =
            listOf(1, 19, 21, 22, 23, 24, 25, 41, 52, 65, 66, 67, 68, 69, 70)
        private val SVOOC_ADAPTER_ID_LIST = listOf(
            17, 18, 20, 26, 27, 28, 29, 30, 2, 33, 34, 35, 36, 37, 38, 39, 40, 42, 43, 44,
            45, 46, 49, 50, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 71, 72, 73, 74, 75,
            76, 77, 78, 81, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110
        )
        private val UFCS_ADAPTER_ID_LIST = listOf(529, 16913, 33297, 49681)

        fun transferChargerType(i: Int): Int {
            if (VOOC_ADAPTER_ID_LIST.contains(i)) {
                return 1
            }
            if (SVOOC_ADAPTER_ID_LIST.contains(i)) {
                return 2
            }
            if (UFCS_ADAPTER_ID_LIST.contains(i)) {
                return 6
            }
            return i
        }
    }
}