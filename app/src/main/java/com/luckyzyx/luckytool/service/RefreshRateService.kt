package com.luckyzyx.luckytool.service

import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.view.DisplayInfo
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IRefreshRateController
import com.luckyzyx.luckytool.data.DisplayMode
import com.luckyzyx.luckytool.hook.utils.DisplayManagerUtils
import com.luckyzyx.luckytool.hook.utils.DynamicDisplayInfoUtils
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RefreshRateService : BaseControllerService<IRefreshRateController>() {
    override val TAG = "RefreshRateService"
    override var controllerService: Class<*> = classOf<RefreshRateControllerService>()

    override fun getController(iBinder: IBinder?): IRefreshRateController? {
        return IRefreshRateController.Stub.asInterface(iBinder)
    }

    @Obfuscate
    @Suppress("PrivatePropertyName", "ConstPropertyName")
    class RefreshRateControllerService : RootService() {
        private val TAG = "RefreshRateControllerService"
        val isDebug = false

        companion object {
            private const val serviceName = "SurfaceFlinger"
            private const val interfaceName = "android.ui.ISurfaceComposer"

            /** HDR/SDR 比率覆盖层 开启(1) / 关闭(0) / 查询(2)，与开发者选项里的同名开关共用 */
            private const val codeHdrSdrRatio = 1043

            private val surfaceFlinger by lazy {
                ServiceManager.getService(serviceName)
            }
        }

        override fun onBind(intent: Intent) = object : IRefreshRateController.Stub() {
            override fun getRefreshRateDisplay(): Boolean {
                try {
                    if (surfaceFlinger == null) surfaceFlinger
                    if (surfaceFlinger != null) {
                        val obtain = Parcel.obtain()
                        val obtain2 = Parcel.obtain()
                        obtain.writeInterfaceToken(interfaceName)
                        obtain.writeInt(2)
                        surfaceFlinger?.transact(1034, obtain, obtain2, 0)
                        val status = obtain2.readBoolean()
                        obtain2.recycle()
                        obtain.recycle()
                        return status
                    }
                    LogUtils.d(TAG, "getRefreshRateDisplay", "surfaceFlinger is null", isDebug)
                    return false
                } catch (e: RemoteException) {
                    LogUtils.e(TAG, "getRefreshRateDisplay ", "$e", true)
                    return false
                }
            }

            override fun setRefreshRateDisplay(status: Boolean) {
                try {
                    if (surfaceFlinger == null) surfaceFlinger
                    if (surfaceFlinger != null) {
                        val obtain = Parcel.obtain()
                        obtain.writeInterfaceToken(interfaceName)
                        obtain.writeInt(if (status) 1 else 0)
                        surfaceFlinger?.transact(1034, obtain, null, 0)
                        obtain.recycle()
                        return
                    }
                    LogUtils.d(TAG, "setRefreshRateDisplay", "surfaceFlinger is null", isDebug)
                } catch (e: RemoteException) {
                    LogUtils.e(TAG, "setRefreshRateDisplay ", "$e", true)
                }
            }

            override fun getSupportModes(): ArrayList<DisplayMode> {
                val list = ArrayList<DisplayMode>()
                val context = this@RefreshRateControllerService
                return try {
                    DisplayManagerUtils(null).apply {
                        val displayManager = getDisplayManagerService(context)
                        val display = displayManager.getDisplay(0)
                        val displayInfo = DisplayInfo()
                        if (!display.getDisplayInfo(displayInfo)) {
                            LogUtils.e(TAG, "getSupportModes", "getDisplayInfo -> false", true)
                            return@apply
                        }
                        val dynamicInfo = getDynamicDisplayInfo(displayInfo)
                        if (dynamicInfo == null) {
                            LogUtils.e(
                                TAG,
                                "getSupportModes",
                                "dynamicInfo is null, address = ${displayInfo.address}",
                                true
                            )
                            return@apply
                        }
                        DynamicDisplayInfoUtils(dynamicInfo).apply {
                            val allDisplayModes = getSupportedDisplayModes()
//                            LogUtils.d(
//                                TAG,
//                                "getSupportModes",
//                                "supportedDisplayModes size = ${allDisplayModes.size}",
//                                true
//                            )
                            allDisplayModes.forEach {
                                val mode = getDisplayMode(it)
                                if (mode == null) {
                                    LogUtils.e(
                                        TAG, "getSupportModes", "parse mode failed -> $it", true
                                    )
                                } else {
                                    list.add(mode.second)
                                    LogUtils.d(TAG, "getSupportModes", "Mode is add", isDebug)
                                }
                            }
                        }
                    }
                    LogUtils.e(TAG, "getSupportModes", "Final size ${list.size}", true)
                    list
                } catch (e: Exception) {
                    LogUtils.e(TAG, "getSupportModes", "$e", true)
                    list
                }
            }

            override fun setRefreshRateMode(modeId: Int) {
                try {
                    if (surfaceFlinger == null) surfaceFlinger
                    if (surfaceFlinger != null) {
                        val obtain = Parcel.obtain()
                        obtain.writeInterfaceToken(interfaceName)
                        obtain.writeInt(modeId)
                        surfaceFlinger?.transact(1035, obtain, null, 0)
                        obtain.recycle()
                        return
                    }
                    LogUtils.d(TAG, "setRefreshRateMode", "surfaceFlinger is null", isDebug)
                } catch (e: Exception) {
                    if (modeId >= 0) LogUtils.e(TAG, "setRefreshRateMode", "$e", true)
                }
            }

            override fun resetRefreshRateMode() {
                try {
                    setRefreshRateMode(-1)
                } catch (_: Exception) {
//                LogUtils.e(tag, "resetRefreshRateMode", "$e", true)
                }
            }

            override fun getHdrSdrRatioOverlay(): Boolean {
                val obtain = Parcel.obtain()
                val obtain2 = Parcel.obtain()
                return try {
                    obtain.writeInterfaceToken(interfaceName)
                    obtain.writeInt(2)
                    // 更早的 SurfaceFlinger 没有 code 1043，transact 返回 false 且 reply 为空
                    if (surfaceFlinger?.transact(codeHdrSdrRatio, obtain, obtain2, 0) != true
                        || obtain2.dataSize() < 4
                    ) {
                        LogUtils.d(TAG, "getHdrSdrRatioOverlay", "not supported", isDebug)
                        false
                    } else obtain2.readBoolean()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "getHdrSdrRatioOverlay", "$e", true)
                    false
                } finally {
                    obtain2.recycle()
                    obtain.recycle()
                }
            }

            override fun setHdrSdrRatioOverlay(enabled: Boolean) {
                val obtain = Parcel.obtain()
                try {
                    obtain.writeInterfaceToken(interfaceName)
                    obtain.writeInt(if (enabled) 1 else 0)
                    surfaceFlinger?.transact(codeHdrSdrRatio, obtain, null, 0)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "setHdrSdrRatioOverlay", "$e", true)
                } finally {
                    obtain.recycle()
                }
            }
        }
    }
}