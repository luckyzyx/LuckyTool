package com.luckyzyx.luckytool.hook.hookers

import android.app.StatusBarManager
import android.content.Intent
import android.nfc.NfcAdapter
import com.drake.net.utils.scope
import com.highcapable.betterandroid.ui.extension.component.lifecycleOwner
import com.highcapable.betterandroid.ui.extension.component.runDelayed
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyGlobalDCMode
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyHighBrightness
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.convertToMillis
import kotlinx.coroutines.delay
import org.lsposed.lsparanoid.Obfuscate
import kotlin.time.Duration.Companion.milliseconds

@Obfuscate
object HookSystemUIAutoStart : YukiBaseHooker() {
    override fun onHook() {
        var nfcEnable = preferences(ModulePrefs).getBoolean("enable_nfc_delay_shutdown", false)
        dataChannel.wait<Boolean>("enable_nfc_delay_shutdown") { nfcEnable = it }
        var nfcDelay = preferences(ModulePrefs).getString("custom_nfc_delay_shutdown_time", "10M")
        dataChannel.wait<String>("custom_nfc_delay_shutdown_time") { nfcDelay = it }

        //磁贴全局DC/高亮度模式：模块磁贴写入开关后，宿主侧即时唤起自启控制器执行
        dataChannel.wait<Boolean>(keyGlobalDCMode) { startAutoStartController() }
        dataChannel.wait<Boolean>(keyHighBrightness) { startAutoStartController() }

        registerAppLifecycle {
            //监听锁屏解锁
            registerReceiver(Intent.ACTION_USER_PRESENT) { _, _ ->
                startAutoStartController()
            }
            //监听模块磁贴关闭控制中心
            registerReceiver("LuckyTool_CloseCollapse") { context, _ ->
                val service = context.getSystemService(classOf<StatusBarManager>())
                service.asResolver().firstMethod { name = "collapsePanels" }.invoke()
            }
            //监听NFC启用状态
            registerReceiver(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) { context, intent ->
                if (!nfcEnable) return@registerReceiver
                val delay = convertToMillis(nfcDelay)
                if (delay < 0) {
                    nfcEnable = false
                    YLog.debug("NFC Delay Error -> $nfcDelay | $delay")
                    return@registerReceiver
                }
                val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                val intExtra = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1)
                if (nfcAdapter.isEnabled) {
                    try {
                        context.lifecycleOwner?.runDelayed(delay) {
                            nfcAdapter.asResolver().firstMethod { name = "disable" }.invoke()
                        }
                    } catch (t: Throwable) {
                        YLog.debug("NFC [$intExtra] Handler Add Error", t)
                    }
                }
            }
        }
    }

    /** 唤起模块自启控制器（读当前磁贴开关执行系统命令），解锁与磁贴开关变化共用 */
    private fun startAutoStartController() {
        val context = hostApplication ?: return
        scope {
            delay(200.milliseconds)
            try {
                context.startForegroundService(Intent().apply {
                    action = "${BuildConfig.APPLICATION_ID}.AutoStartControllerService"
                    setPackage(BuildConfig.APPLICATION_ID)
                })
            } catch (t: Throwable) {
                YLog.debug("AutoStartService try sthrow", t)
            }
        }.catch {
            YLog.debug("AutoStartService scope throw", it)
        }
    }
}
