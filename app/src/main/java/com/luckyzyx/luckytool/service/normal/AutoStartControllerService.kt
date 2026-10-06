package com.luckyzyx.luckytool.service.normal

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.drake.net.utils.scope
import com.highcapable.betterandroid.ui.component.notification.factory.Notification
import com.highcapable.betterandroid.ui.component.notification.factory.NotificationChannel
import com.highcapable.betterandroid.ui.component.notification.factory.startForeground
import com.highcapable.betterandroid.ui.component.notification.type.NotificationImportance
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsAutoStart
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsCur
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyGlobalDCMode
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyHighBrightness
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyTileAutoStart
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyTouchSamplingRate
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyTouchSamplingRateLevel
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getInt
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.showToast
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import okhttp3.internal.toHexString
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class AutoStartControllerService : Service() {

    private val CHANNEL_ID = "auto_start_channel"
    private val NOTIFY_ID = 1001

    override fun onCreate() {

    }

    @SuppressLint("WrongConstant", "InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope(Dispatchers.Default) {
            try {
                val channel = NotificationChannel(
                    channelId = CHANNEL_ID, importance = NotificationImportance.LOW
                ) {
                    name = getString(R.string.auto_start_service_channel_name)
                }
                val notify = Notification(
                    context = this@AutoStartControllerService, channel = channel
                ) {
                    smallIconResId = R.mipmap.ic_launcher_round
                    contentTitle = getString(R.string.auto_start_service_channel_title)
                    autoCancel(false)
                    ongoing(true)
                }
                if (SDK >= A14) startForeground(
                    NOTIFY_ID, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                ) else startForeground(NOTIFY_ID, notify)
            } catch (@SuppressLint("NewApi") _: ForegroundServiceStartNotAllowedException) {
                showToast(getString(R.string.service_auto_start_controller_not_allow_tips))
                return@scope
            } catch (_: Exception) {
                showToast("AutoStartControllerService cannot be started!")
                return@scope
            }

            val command = ArrayList<String>()
            //FPS自启
            if (getBoolean(SettingsPrefs, keyFpsAutoStart, false)) {
                val fpsCur = getInt(SettingsPrefs, keyFpsCur, -1)
                if (fpsCur != -1) command.add(CommandUtils.setRefreshRate + fpsCur)
            }
            //磁贴自启
            if (getBoolean(SettingsPrefs, keyTileAutoStart, false)) {
                //触控采样率相关
                if (getBoolean(SettingsPrefs, keyTouchSamplingRate, false)) {
                    val level = getString(SettingsPrefs, keyTouchSamplingRateLevel, "240")
                    val int16 = level.toInt().toHexString()
                    command.add(CommandUtils.touchPanel + int16)
//                    command.add("start touchDaemon && ps -A | grep touchDaemon")
                    command.add(CommandUtils.touchHidl + int16)
                }
                //高亮度模式
                if (getBoolean(SettingsPrefs, keyHighBrightness, false)) {
                    command.add(CommandUtils.highBrightness)
                }
                //全局DC模式
                if (getBoolean(SettingsPrefs, keyGlobalDCMode, false)) {
                    command.add(CommandUtils.globalDCModeOppo)
                    command.add(CommandUtils.globalDCModeOplus)
                }
            }
            if (command.isNotEmpty()) ShellUtils.fastCmd(*command.toTypedArray())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}