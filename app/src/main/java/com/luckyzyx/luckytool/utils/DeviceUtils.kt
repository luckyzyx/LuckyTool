package com.luckyzyx.luckytool.utils

import android.content.Context
import android.os.SystemProperties
import com.android.internal.os.PowerProfile
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.R
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import org.lsposed.lsparanoid.Obfuscate
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.math.roundToInt

@Obfuscate
object DeviceUtils {

    /**
     * 获取OTA参数
     * @return String
     */
    fun getOTACOnfigs(): String {
        return """
                ${SystemProperties.get("ro.product.name")} ${SystemProperties.get("ro.build.oplus_nv_id")}
                ${getMyManifesstVersion()}
                ${getPcbInfo()} ${getSnInfo()}
                ${getGuid()}
                ${getRecruitId()}
                ${getRegisterId()}
            """.trimIndent()
    }

    /**
     * 获取OS OTA版本号
     * @return String
     */
    fun getOtaVersion(): String {
        val command = "getprop ro.build.version.ota"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取GUID
     * /data/system/openid_config.xml
     */
    fun getGuid(): String {
        val command =
            "cat /data/system/openid_config.xml | egrep guid | egrep -o 'value=\"[^\"]+\"' | sed 's/\\\"//g,s/value=//g'"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取招募ID
     * /data/user/0/com.oplus.ota/shared_prefs/persistent_info.xml
     */
    fun getRecruitId(): String {
        val command =
            "cat /data/user/0/com.oplus.ota/shared_prefs/persistent_info.xml | egrep record_recruitId | egrep -o '>[^<]+<' | tr -d '><'"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取注册ID
     * /data/user/0/com.oplus.ota/shared_prefs/persistent_info.xml
     */
    fun getRegisterId(): String {
        val command =
            "cat /data/user/0/com.oplus.ota/shared_prefs/persistent_info.xml | egrep ota_register_trigger_id | egrep -o '>[^<]+<' | tr -d '><'"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取主板ID
     * @return String
     */
    fun getDeviceID(): String {
        val serialCommand = "cat /sys/devices/soc0/serial_number"
        val serialNumber = ShellUtils.fastCmd(serialCommand)
        if (serialNumber.isNotBlank()) return serialNumber
        val serialNoCommand = "cat /sys/firmware/devicetree/base/firmware/android/serialno"
        val serialNo = ShellUtils.fastCmd(serialNoCommand)
        if (serialNo.isNotBlank()) return serialNo
        return "null"
    }

    /**
     * 获取闪存信息
     * @return String
     */
    fun getFlashInfo(): String {
        val command = "cat /sys/class/block/sda/device/inquiry"
        return ShellUtils.fastCmd(command).let {
            if ((it.isNotBlank())) formatSpace(it.replaceSpace.uppercase()) else "null"
        }
    }

    /**
     * 获取LCD信息
     */
    fun getLcdInfo(): String {
        val command = "cat /proc/devinfo/lcd | sed 's/^.*\t//g; s/$/\n/g; s/\n/ /g;'"
        return ShellUtils.fastCmd(command).let {
            if ((it.isNotBlank())) it.replaceSpace.uppercase() else "null"
        }
    }

    /**
     * 获取PCB信息
     */
    fun getPcbInfo(): String {
        val command = "echo $(getprop gsm.serial)$(getprop vendor.gsm.serial)"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取SN信息
     */
    fun getSnInfo(): String {
        val command1 = "getprop ro.serialno"
        val sn = ShellUtils.fastCmd(command1)
        val command2 = "getprop ro.boot.chipid"
        val chipid = ShellUtils.fastCmd(command2)
        return listOf(sn, chipid).filter { it.isNotBlank() }  // 过滤掉空字符串
            .minByOrNull { it.length }  // 找出最短的
            ?: "null"  // 如果全部为空，则返回空字符串
    }

    /**
     * 获取PrjName信息
     */
    fun getPrjNameInfo(): String {
        val command = "getprop ro.boot.prjname"
        return ShellUtils.fastCmd(command).ifBlank { "null" }
    }

    /**
     * 获取Slot信息
     */
    fun getSlotInfo(): String {
        val command = "getprop ro.boot.slot_suffix"
        return ShellUtils.fastCmd(command).replace("_", "")
            .takeIf { e -> e.isNotBlank() }?.uppercase() ?: "NonAB"
    }

    /**
     * 获取Root状态
     * @return Boolean
     */
    fun getRootStatus(): Boolean {
        val isSu = Shell.getShell().isRoot
        return isSu && ShellUtils.fastCmd(CommandUtils.id).split(" ").let {
            it.contains(CommandUtils.rootUid) && it.contains(CommandUtils.rootGid)
                    && it.contains(CommandUtils.rootGroup)
        }
    }

    /**
     * 获取Root来源与版本
     * @param context Context
     * @return String
     */
    fun getRootVersion(context: Context): String {
        val magisk = Shell.cmd("magisk").exec()
        val magiskv = ShellUtils.fastCmd("magisk -v")
        val magiskV = ShellUtils.fastCmd("magisk -V")
        val suh = Shell.cmd("su -h").exec()
        val suv = Shell.cmd("su -v").exec()
        val suV = Shell.cmd("su -V").exec()

        val rootSource = when {
            magisk.isSuccess -> "$magiskv ($magiskV)"
            suh.isSuccess -> {
                if (suv.isSuccess || suV.isSuccess) {
                    "${suv.out.firstOrNull()} (${suV.out.firstOrNull()})"
                } else "SuperSu?"
            }

            else -> "Other Or Error"
        }
        return "${context.getString(R.string.root_source)} $rootSource"
    }

    /**
     * 获取LSP版本
     * @param context Context
     * @return String
     */
    fun getFrameWorkVersion(context: Context): String {
        val moduleProp = Shell.cmd("cat ${CommandUtils.lspProp}").exec().out
        val name = moduleProp.find { it.startsWith("name=") }?.substringAfter("=")
        val version = moduleProp.find { it.startsWith("version=") }?.substringAfter("=")
        return "${context.getString(R.string.framework_version)} $name $version"
    }

    /**
     * 计算本地电池健康度
     * @receiver Context
     * @param isDebug Boolean
     * @return Int
     */
    fun Context.calcLocalHealth(isDebug: Boolean = false): Int {
        val oplusChgSoh = "/sys/class/oplus_chg/battery/battery_soh"
        val oplusChgFcc = "/sys/class/oplus_chg/battery/battery_fcc"
        val powerSupplySoh = "/sys/class/power_supply/battery/batt_soh"
        val powerSupplyFcc = "/sys/class/power_supply/battery/batt_fcc"
        try {
            val sohFile = if (SDK >= A13) File(oplusChgSoh)
            else File(powerSupplySoh)
            LogUtils.d("calcLocalHealth", "sohFile", sohFile.path, isDebug)
            val sohValue = safeOfNull {
                BufferedReader(FileReader(sohFile)).readLine().replaceSpace.toIntOrNull()
            } ?: -1
            LogUtils.d("calcLocalHealth", "sohValue", "$sohValue", isDebug)
            if (sohValue in 1..100) return sohValue

            val fccFile = if (SDK >= A13) File(oplusChgFcc)
            else File(powerSupplyFcc)
            LogUtils.d("calcLocalHealth", "curFile", fccFile.path, isDebug)
            val fccValue = safeOfNull {
                BufferedReader(FileReader(fccFile)).readLine().replaceSpace.toIntOrNull()
            } ?: -1
            LogUtils.d("calcLocalHealth", "curValue", "$fccValue", isDebug)
            if (fccValue <= 0) return -1

            val designValue = PowerProfile(this).batteryCapacity
            LogUtils.d("calcLocalHealth", "designValue", "$designValue", isDebug)
            val calc = safeOfNull { (fccValue / designValue * 100.0).roundToInt() } ?: -1
            LogUtils.d("calcLocalHealth", "calc", "$calc", isDebug)
            return if (calc > 100) calc / 1000 else calc
        } catch (e: Exception) {
            YLog.error("Calc Local Health Error", e = e)
            return -1
        }
    }

    suspend fun getQSlist(): ArrayList<String> {
        return com.drake.net.utils.withDefault {
            val configList = ArrayList<String>()
            val indexList = ArrayList<String>()
            val command1 =
                "ls -f /data/data/com.tencent.mobileqq/databases/ | egrep 'config_db([0-9]+)$' | sed 's/config_db//g'"
            val command2 =
                "ls -f /data/data/com.tencent.mobileqq/databases/ | egrep '([0-9]+)-IndexQQMsg.db' | sed 's/-IndexQQMsg.db//g'"
            Shell.cmd(command1).to(configList).exec()
            Shell.cmd(command2).to(indexList).exec()
//        LogUtils.e("getQSlist", "cachelist", "${list.toList()}", true)
            ArrayList(configList.union(indexList))
        }
    }

    suspend fun getCSid(): ArrayList<String> {
        val command =
            "cat /data/data/com.coolapk.market/shared_prefs/coolapk_preferences_v7.xml | egrep -o 'USER_SPAM_([0-9]+)' | sed 's/USER_SPAM_//g'"
        return com.drake.net.utils.withDefault {
            ArrayList<String>().apply {
                Shell.cmd(command).to(this).exec()
            }
        }
    }
}