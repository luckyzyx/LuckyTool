package com.luckyzyx.luckytool.service

import android.content.Intent
import android.os.IBinder
import android.os.SystemProperties
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate
import java.net.Inet4Address
import java.net.NetworkInterface

@Obfuscate
object AdbService : BaseControllerService<IAdbDebugController>() {
    override val TAG = "AdbService"
    override var controllerService: Class<*> = classOf<AdbControllerService>()

    override fun getController(iBinder: IBinder?): IAdbDebugController? {
        return IAdbDebugController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    @Suppress("PrivatePropertyName")
    class AdbControllerService : RootService() {
        private val TAG = "AdbControllerService"

        override fun onBind(intent: Intent) = object : IAdbDebugController.Stub() {
            override fun getAdbPort(): Int {
                val port = SystemProperties.get("service.adb.tcp.port")
                return if (port.isNullOrBlank()) 0 else port.toIntOrNull() ?: 0
            }

            override fun setAdbPort(port: Int) {
                SystemProperties.set(
                    "service.adb.tcp.port", (if (port == 0) "" else port).toString()
                )
            }

            override fun getWifiIP(): String {
                return getIpAddress("wlan0") ?: "IP"
            }

            override fun restartAdb() {
                val commands = arrayOf("stop adbd", "killall -9 adbd 2>/dev/null", "start adbd")
                ShellUtils.fastCmd(*commands)
            }

            fun getIpAddress(networkName: String): String? {
                try {
                    val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                    while (networkInterfaces.hasMoreElements()) {
                        val element = networkInterfaces.nextElement()
                        if (!networkName.equals(element.name, true)) continue
                        val addresses = element.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val address = addresses.nextElement()
                            if (address is Inet4Address && !address.isLoopbackAddress()) {
                                return address.hostAddress
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "getIpAddress", "$e", true)
                    return null
                }
                return null
            }
        }
    }
}