package com.luckyzyx.luckytool.service

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.IPowerManager
import android.os.RemoteException
import android.os.ServiceManager
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IPowerServiceController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object PowerService : BaseControllerService<IPowerServiceController>() {
    override val TAG: String = "PowerService"
    override var controllerService: Class<*> = classOf<PowerControllerService>()

    private var pm: IPowerManager? = null
    private var binder: IBinder? = null

    private val recipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            LogUtils.w(TAG, "DeathRecipient", "is dead", true)
            binder?.unlinkToDeath(this, 0)
            binder = null
            pm = null
        }
    }

    override fun getController(iBinder: IBinder?): IPowerServiceController? {
        return IPowerServiceController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    class PowerControllerService : RootService() {
        override fun onBind(intent: Intent) = object : IPowerServiceController.Stub() {
            override fun reboot(confirm: Boolean, reason: String?, wait: Boolean) {
                PowerService.reboot(confirm, reason, wait)
            }

            override fun rebootSafeMode(confirm: Boolean, wait: Boolean) {
                PowerService.rebootSafeMode(confirm, wait)
            }
        }
    }

    private fun getPowerManager(): IPowerManager? {
        if (binder == null || pm == null) {
            binder = ServiceManager.getService(Context.POWER_SERVICE)
            if (binder == null) return null
            try {
                binder!!.linkToDeath(recipient, 0)
            } catch (e: RemoteException) {
                LogUtils.e(TAG, "getPowerManager", e.toString(), true)

            }
            pm = IPowerManager.Stub.asInterface(binder)
        }
        return pm
    }

    fun reboot(confirm: Boolean, reason: String?, wait: Boolean) {
        val pm = getPowerManager() ?: return
        pm.reboot(confirm, reason, wait)
    }

    fun rebootSafeMode(confirm: Boolean, wait: Boolean) {
        val pm = getPowerManager() ?: return
        pm.rebootSafeMode(confirm, wait)
    }

}