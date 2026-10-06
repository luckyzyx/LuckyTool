package com.luckyzyx.luckytool.service

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.telephony.TelephonyManager
import com.android.internal.telephony.ITelephony
import com.android.internal.telephony.RILConstants
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.ITileServiceController
import com.luckyzyx.luckytool.hook.utils.IColorDisplayUtils
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A17
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.replaceSpace
import com.oplus.miragewindow.OplusMirageOptions
import com.oplus.miragewindow.OplusMirageWindowManager
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.ipc.RootService
import okhttp3.internal.toHexString
import org.lsposed.lsparanoid.Obfuscate
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@Obfuscate
object TilesService : BaseControllerService<ITileServiceController>() {
    override val TAG = "TileService"

    override var controllerService: Class<*> = classOf<TileControllerService>()

    override fun getController(iBinder: IBinder?): ITileServiceController? {
        return ITileServiceController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    @Suppress("ConstPropertyName", "PrivatePropertyName")
    class TileControllerService : RootService() {

        private val TAG = "TileControllerService"

        companion object {
            //DarkMode
            private val iColorDisplayManagerInternal by lazy {
                IColorDisplayUtils(null).getInstance()
            }

            //FiveG
            private val telephonyService by lazy {
                ServiceManager.getService(TELEPHONY_SERVICE)
            }

            private val iTelephony by lazy {
                ITelephony.Stub.asInterface(telephonyService)
            }

            /**
             * 指示用户请求允许的网络类型更改
             */
            @SuppressLint("InlinedApi")
            val reasonUser = TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER

            /**
             * 网络类型位掩码，指示无线电技术 NR（新无线电）5G 的支持
             */
            @SuppressLint("InlinedApi")
            val bitMaskNR = TelephonyManager.NETWORK_TYPE_BITMASK_NR

            /**
             * 首选网络模式为 TD-SCDMA/LTE/GSM/WCDMA、CDMA 和 EvDo
             */
            val modeLTE = RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA

            /**
             * 首选网络模式是 NR 5G、LTE、TD-SCDMA、CDMA、EVDO、GSM 和 WCDMA
             */
            val modeNR = RILConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA

            //GlobalDC
            private const val oppoFileDir = "/sys/kernel/oppo_display/dimlayer_hbm"
            private const val oplusFileDir = "/sys/kernel/oplus_display/dimlayer_hbm"
            private val oppoFile = File(oppoFileDir)
            private val oplusFile = File(oplusFileDir)

            //GoogleService
            //Source Settings OplusGoogleSettingsFragment
            private const val key = "customize_control_cn_gms"

            //HighBrightness
            private const val highBrightnessPath = "/sys/kernel/oplus_display/hbm"
            val highBrightnessFile = File(highBrightnessPath)

            //TouchPanel
            private const val touchPanelDir = "/proc/touchpanel/game_switch_enable"
            private const val touchHidlDir = "/odm/bin/touchHidlTest"
            private val touchPanel = File(touchPanelDir)
            private val touchHidl = File(touchHidlDir)
            private val touchProc = if (touchPanel.exists()) 1 else if (touchHidl.exists()) 2 else 0

            //            private const val askTouch = "/odm/bin/touchHidlTest -c ao 0 26"
            private const val readTouch = "/odm/bin/touchHidlTest -c ro 0 26"
            private const val writeTouch = "/odm/bin/touchHidlTest -c wo 0 26"

            //BypassPower
            private const val bypassPowerPath =
                "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable"
            val bypassPowerFile = File(bypassPowerPath)

        }

        override fun onBind(intent: Intent) = object : ITileServiceController.Stub() {
            override fun checkDarkMode(): Boolean {
                return try {
                    iColorDisplayManagerInternal != null
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "checkDarkMode", "$t", true)
                    false
                }
            }

            override fun getDarkMode(): Boolean {
                return try {
                    iColorDisplayManagerInternal?.asResolver()?.firstMethod {
                        name = "isReduceBrightColorsActivated"
                        emptyParameters()
                    }?.invoke<Boolean>() ?: false
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "getDarkMode", "$t", true)
                    false
                }
            }

            override fun setDarkMode(status: Boolean) {
                try {
                    iColorDisplayManagerInternal?.asResolver()?.firstMethod {
                        name = "setReduceBrightColorsActivated"
                        parameters(Boolean::class)
                    }?.invoke(status)
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "setDarkMode", "$t", true)
                }
            }

            @SuppressLint("DeprecatedSinceApi", "NewApi")
            override fun checkCompatibility(subId: Int): Boolean {
                return try {
                    if (SDK >= A12) {
                        val types = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                        if (SDK >= A17) iTelephony.setAllowedNetworkTypesForReason(
                            subId, reasonUser, types, "com.android.phone"
                        )
                        else iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, types)
                    } else {
                        // For Q and R.
                        val types = iTelephony.getPreferredNetworkType(subId)
                        iTelephony.setPreferredNetworkType(subId, types)
                    }
                } catch (_: Throwable) {
                    false
                } catch (_: RemoteException) {
                    false
                }
            }

            @SuppressLint("DeprecatedSinceApi")
            override fun getFiveGStatus(subId: Int): Boolean {
                return try {
                    if (SDK >= A12) {
                        iTelephony.getAllowedNetworkTypesForReason(
                            subId, reasonUser
                        ) and bitMaskNR != 0L
                    } else {
                        // For Q and R.
                        iTelephony.getPreferredNetworkType(subId) == modeNR
                    }
                } catch (_: Throwable) {
                    false
                } catch (_: RemoteException) {
                    false
                }
            }

            @SuppressLint("DeprecatedSinceApi", "NewApi")
            override fun setFiveGStatus(subId: Int, enabled: Boolean) {
                try {
                    if (SDK >= A12) {
                        var curTypes = iTelephony.getAllowedNetworkTypesForReason(subId, reasonUser)
                        curTypes = if (enabled) curTypes or bitMaskNR
                        else curTypes and bitMaskNR.inv()
                        if (SDK >= A17) iTelephony.setAllowedNetworkTypesForReason(
                            subId, reasonUser, curTypes, "com.android.phone"
                        )
                        else iTelephony.setAllowedNetworkTypesForReason(subId, reasonUser, curTypes)
                    } else {
                        // For Q and R.
                        iTelephony.setPreferredNetworkType(subId, if (enabled) modeNR else modeLTE)
                    }
                } catch (_: Throwable) {

                } catch (_: RemoteException) {

                }
            }

            override fun checkGlobalDCMode(): Boolean {
                return try {
                    oppoFile.exists() || oplusFile.exists()
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "checkGlobalDCMode", "$t", true)
                    false
                }
            }

            override fun getGlobalDCMode(): Boolean {
                return try {
                    val file = (if (oppoFile.exists()) oppoFile
                    else if (oplusFile.exists()) oplusFile
                    else null) ?: return false
                    when (BufferedReader(FileReader(file)).readLine()?.replaceSpace?.substring(0, 1)
                        ?.toIntOrNull()) {
                        0 -> false
                        1 -> true
                        else -> false
                    }
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "getGlobalDCMode", "$t", true)
                    false
                }
            }

            override fun setGlobalDCMode(status: Boolean) {
                try {
                    val file = (if (oppoFile.exists()) oppoFile
                    else if (oplusFile.exists()) oplusFile
                    else null) ?: return
                    file.writeText(if (status) "1" else "0")
                } catch (t: Throwable) {
                    LogUtils.d(TAG, "setGlobalDCMode", "$t", true)
                }
            }

            override fun getGoogleStatus(): Boolean {
                return try {
                    val result = ShellUtils.fastCmd("settings get system $key")
                    LogUtils.d(TAG, "getGoogleStatus", "result -> $result")
                    result.toIntOrNull() == 1
                } catch (t: Exception) {
                    LogUtils.d(TAG, "getGoogleStatus", "$t", true)
                    false
                }
            }

            override fun setGoogleStatus(status: Boolean) {
                try {
                    val result = ShellUtils.fastCmdResult(
                        "settings put system $key ${if (status) 1 else 0}"
                    )
                    LogUtils.d(TAG, "setGoogleStatus", "$status -> $result")
                } catch (t: Exception) {
                    LogUtils.d(TAG, "setGoogleStatus", "$t", true)
                }
            }

            override fun checkHighBrightnessMode(): Boolean {
                return try {
                    highBrightnessFile.exists()
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "checkHighBrightnessMode", "$t", true)
                    false
                }
            }

            override fun getHighBrightnessMode(): Boolean {
                return try {
                    when (BufferedReader(FileReader(highBrightnessFile)).readLine()?.replaceSpace?.substring(
                        0, 1
                    )?.toIntOrNull()) {
                        0 -> false
                        1 -> true
                        else -> false
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "getHighBrightnessMode", "$t", true)
                    false
                }
            }

            override fun setHighBrightnessMode(status: Boolean) {
                try {
                    if (highBrightnessFile.exists()) highBrightnessFile.writeText(if (status) "1" else "0")
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "setHighBrightnessMode", "$t", true)
                }
            }

            override fun checkTouchMode(): Boolean {
                return try {
                    when (touchProc) {
                        1 -> touchPanel.readText().substringBefore(",").toIntOrNull() != null
                        2 -> ShellUtils.fastCmd(readTouch).substringBefore(",")
                            .toIntOrNull() is Number

                        else -> false
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "checkTouchMode", "$t", true)
                    false
                }
            }

            override fun getTouchMode(): Int {
                return try {
                    when (touchProc) {
                        1 -> touchPanel.readText().substringBefore(",").toIntOrNull() ?: 0
                        2 -> ShellUtils.fastCmd(readTouch).substringBefore(",").toIntOrNull() ?: 0
                        else -> 0
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "getTouchMode", "$t", true)
                    0
                }
            }

            override fun setTouchMode(value: Int) {
                try {
                    val int16 = value.toHexString()
                    when (touchProc) {
                        1 -> touchPanel.writeText(int16)
                        2 -> ShellUtils.fastCmd("$writeTouch $int16")
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "setTouchMode", "$t", true)
                }
            }

            override fun checkBypassMode(): Boolean {
                return try {
                    bypassPowerFile.exists()
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "checkBypassMode", "$t", true)
                    false
                }
            }

            override fun getBypassMode(): Boolean {
                return try {
                    when (BufferedReader(FileReader(bypassPowerFile)).readLine()?.replaceSpace?.substring(
                        0, 1
                    )?.toIntOrNull()) {
                        1 -> false
                        0 -> true
                        else -> false
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "getBypassMode", "$t", true)
                    false
                }
            }

            override fun setBypassMode(status: Boolean) {
                try {
                    if (bypassPowerFile.exists()) bypassPowerFile.writeText(if (status) "0" else "1")
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "setBypassMode", "$t", true)
                }
            }

            override fun startMirageWindowMode(): Int {
                return try {
                    val makeBasic = OplusMirageOptions.makeBackgroundStreamModeOptions()
                    OplusMirageWindowManager.getInstance().startMirageWindowMode(
                        null, makeBasic.toBundle()
                    )
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "startMirageWindowMode", "$t", true)
                    -1
                }
            }
        }
    }
}