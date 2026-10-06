package com.luckyzyx.luckytool.service

import android.app.IActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.UserInfo
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IActivityServiceController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ActivityManagerService : BaseControllerService<IActivityServiceController>() {
    override val TAG: String = "ActivityManagerService"
    override var controllerService: Class<*> = classOf<ActivityControllerService>()

    private var am: IActivityManager? = null
    private var binder: IBinder? = null

    private val recipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            LogUtils.w(TAG, "DeathRecipient", "is dead", true)
            binder?.unlinkToDeath(this, 0)
            binder = null
            am = null
        }
    }

    override fun getController(iBinder: IBinder?): IActivityServiceController? {
        return IActivityServiceController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    class ActivityControllerService : RootService() {
        override fun onBind(intent: Intent) = object : IActivityServiceController.Stub() {
            override fun forceStopPackage(packageName: String?, userId: Int) {
                ActivityManagerService.forceStopPackage(packageName, userId)
            }

            override fun getCurrentUser(): UserInfo? {
                return ActivityManagerService.getCurrentUser()
            }
        }
    }

    private fun getActivityManager(): IActivityManager? {
        if (binder == null || am == null) {
            binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            if (binder == null) return null
            try {
                binder!!.linkToDeath(recipient, 0)
                am = IActivityManager.Stub.asInterface(binder)
                // For oddo Android 9 we cannot set activity controller here...
                // am.setActivityController(null, false);
            } catch (e: RemoteException) {
                LogUtils.e(TAG, "getActivityManager", e.toString(), true)
            }
        }
        return am
    }

    fun forceStopPackage(packageName: String?, userId: Int) {
        val am = getActivityManager() ?: return
        am.forceStopPackage(packageName, userId)
    }

    fun getCurrentUser(): UserInfo? {
        val am: IActivityManager = getActivityManager() ?: return null
        return am.currentUser
    }

}