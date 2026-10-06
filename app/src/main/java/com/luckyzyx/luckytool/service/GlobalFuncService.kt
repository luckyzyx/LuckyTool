package com.luckyzyx.luckytool.service

import android.content.Intent
import android.os.IBinder
import android.os.SystemProperties
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IGlobalFuncController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.formatSpace
import com.luckyzyx.luckytool.utils.replaceSpace
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
object GlobalFuncService : BaseControllerService<IGlobalFuncController>() {
    override val TAG = "GlobalFuncService"
    override var controllerService: Class<*> = classOf<GlobalFuncControllerService>()

    override fun getController(iBinder: IBinder?): IGlobalFuncController? {
        return IGlobalFuncController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    class GlobalFuncControllerService : RootService() {
        override fun onBind(intent: Intent) = object : IGlobalFuncController.Stub() {

            override fun getFileText(dir: String): String {
                val file = File(dir)
                return if (file.exists() && file.isFile) file.readText() else ""
            }

            override fun getOtaVersion(): String {
                return SystemProperties.get("ro.build.version.ota", "null")
            }

            override fun getManifestVersion(): String {
                val str = SystemProperties.get("ro.oplus.image.my_manifest.version", "")
                val str2 = SystemProperties.get("ro.oplus.version.my_manifest", "")
//            return SystemProperties.get("ro.build.version.ota", "null")
                return str.ifBlank { str2 }
            }

            override fun getMarketName(): String {
                return SystemProperties.get("ro.vendor.oplus.market.name", "null")

            }

            override fun getFlashInfo(): String {
                val text = getFileText("/sys/class/block/sda/device/inquiry")
                return if (text.isBlank()) "null" else formatSpace(text)
            }

            override fun getPcbInfo(): String {
                val gms = SystemProperties.get("gsm.serial", "")
                val vendor = SystemProperties.get("vendor.gsm.serial", "null")
                return (gms + vendor).replaceSpace

            }

            override fun getSnInfo(): String {
                return SystemProperties.get("ro.serialno", "null")
            }

            override fun getChipInfo(): String {
                val chipid = SystemProperties.get("ro.boot.chipid", "null")
                val chipecid = SystemProperties.get("ro.boot.chipecid", "null")
                return chipid
            }

            override fun getPrjNameInfo(): String {
                return SystemProperties.get("ro.boot.prjname", "null")
            }

            override fun getSlotInfo(): String {
                return SystemProperties.get("ro.boot.slot_suffix", "").replace("_", "")
                    .takeIf { e -> e.isNotBlank() }?.uppercase() ?: "NonAB"
            }

            override fun getCpuInfo(): String {
                return SystemProperties.get("ro.product.oplus.cpuinfo", "null")
            }
        }
    }
}