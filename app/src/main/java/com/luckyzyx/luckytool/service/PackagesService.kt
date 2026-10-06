package com.luckyzyx.luckytool.service

import android.content.Context
import android.content.Intent
import android.content.pm.IPackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemProperties
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IPackageServiceController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate
import java.io.IOException

@Obfuscate
object PackagesService : BaseControllerService<IPackageServiceController>() {
    override val TAG = "PackageService"
    override var controllerService: Class<*> = classOf<PackageControllerService>()

    private var pm: IPackageManager? = null
    private var binder: IBinder? = null

    private val recipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            LogUtils.w(TAG, "DeathRecipient", "is dead", true)
            binder?.unlinkToDeath(this, 0)
            binder = null
            pm = null
        }
    }

    override fun getController(iBinder: IBinder?): IPackageServiceController? {
        return IPackageServiceController.Stub.asInterface(iBinder)
    }

    override fun get(context: Context?, result: (IPackageServiceController?) -> Unit) {
        if (controller?.asBinder()?.isBinderAlive == false) controller = null
        super.get(context, result)
    }

    @Obfuscate
    class PackageControllerService : RootService() {
        override fun onBind(intent: Intent) = object : IPackageServiceController.Stub() {
            override fun clearApplicationProfileData(packageName: String) {
                PackagesService.clearApplicationProfileData(packageName)
            }

            override fun performDexOptMode(packageName: String): Boolean {
                return PackagesService.performDexOptMode(packageName)
            }
        }
    }

    private fun getPackageManager(): IPackageManager? {
        if (binder == null || pm == null) {
            binder = ServiceManager.getService("package")
            if (binder == null) return null
            try {
                binder!!.linkToDeath(recipient, 0)
            } catch (e: RemoteException) {
                LogUtils.e(TAG, "getPackageManager", e.toString(), true)
            }
            pm = IPackageManager.Stub.asInterface(binder)
        }
        return pm
    }

    fun clearApplicationProfileData(packageName: String) {
        val pm = getPackageManager() ?: return
        pm.clearApplicationProfileData(packageName)
    }

    fun performDexOptMode(packageName: String): Boolean {
        // ColorOS 17 removes IPackageManager.performDexOptMode. Run the ART shell
        // entry point from this root process instead of calling the missing API.
        if (Build.VERSION.SDK_INT >= 37) return compileWithArtService(packageName)
        val pm = getPackageManager() ?: return false
        return pm.performDexOptMode(
            packageName, SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false),
            SystemProperties.get("pm.dexopt.install", "speed-profile"), true, true, null
        )
    }

    private fun compileWithArtService(packageName: String): Boolean {
        return try {
            val process = ProcessBuilder(
                "/system/bin/cmd", "package", "compile", "-f", "-m",
                SystemProperties.get("pm.dexopt.install", "speed-profile"), packageName
            ).redirectErrorStream(true).start()
            try {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                // ART may exit with 0 even when dexopt prints "Failure".
                val success = exitCode == 0 &&
                    output.lineSequence().lastOrNull { it.isNotBlank() }?.trim() == "Success"
                if (!success) {
                    LogUtils.e(TAG, "compileWithArtService", "$packageName: exit=$exitCode $output", true)
                }
                success
            } finally {
                process.destroy()
            }
        } catch (e: IOException) {
            LogUtils.e(TAG, "compileWithArtService", "$packageName: $e", true)
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            LogUtils.e(TAG, "compileWithArtService", "$packageName: $e", true)
            false
        }
    }
}
