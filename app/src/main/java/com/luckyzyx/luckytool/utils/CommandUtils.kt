package com.luckyzyx.luckytool.utils

import android.annotation.SuppressLint
import org.lsposed.lsparanoid.Obfuscate

@Suppress("MayBeConstant")
@Obfuscate
object CommandUtils {

    val aesCryptKey = "luckyzyxluckyzyx"
    val otaCryptKey = "otatoolsotatools"
    val sunshineTool = "com.Sunshine.ToolBox"

    val id = "id"
    val uid = "uid"
    val gid = "gid"
    val groups = "groups"
    val rootUid = "uid=0(root)"
    val rootGid = "gid=0(root)"
    val rootGroup = "groups=0(root)"

    val lspProp = "/data/adb/modules/zygisk_lsposed/module.prop"

    @SuppressLint("SdCardPath")
    val otaDatabasePath = "/data/user/0/com.oplus.ota/databases/ota.db"

    val getRefreshRateStatus = "service call SurfaceFlinger 1034 i32 2"
    val showRefreshRate = "service call SurfaceFlinger 1034 i32 "
    val setRefreshRate = "service call SurfaceFlinger 1035 i32 "

    val memcConfigHelp = "/odm/bin/irisConfig -help 2 (MEMC)"
    val memcHdrConfigHelp = "/odm/bin/irisConfig -help 3 (SDR2HDR)"

    val touchPanel = "echo > /proc/touchpanel/game_switch_enable "
    val touchHidl = "touchHidlTest -c wo 0 26 "
    val highBrightness = "echo > /sys/kernel/oplus_display/hbm 1"
    val globalDCModeOppo = "echo > /sys/kernel/oppo_display/dimlayer_hbm 1"
    val globalDCModeOplus = "echo > /sys/kernel/oplus_display/dimlayer_hbm 1"

    val getUsers = "ls /data/user/ -m"
    val getFpsMode2 = "dumpsys display | grep -A 24 'mSfDisplayModes=' | grep 'DisplayMode{id='"

    val otaVerityMode = "ro.boot.veritymode"
    val otaVbmetaState = "ro.boot.vbmeta.device_state"
    val otaLockedState = "ro.boot.flash.locked"
    val otaVerifyResult = "persist.vendor.oplus.verify_result"

    val killSysui = "kill -9 `pgrep systemui`"
    val pkill9 = "pkill -9"
    val killzygote = "killall zygote"
    val reboot = "reboot"
    val logcat = "logcat"
    val afs = "am force-stop"
    val killall = "killall"
    val grep = "grep"
    val getprop = "getprop"
    val resetprop = "resetprop"
    val chattr = "chattr"
    val rmrf = "rm -rf"
    val pm = "pm"
    val cp = "cp"
    val pmuninstall = "pm uninstall"
    val pmlist = "pm list packages"
    val findapp = "find /data/app/"
}