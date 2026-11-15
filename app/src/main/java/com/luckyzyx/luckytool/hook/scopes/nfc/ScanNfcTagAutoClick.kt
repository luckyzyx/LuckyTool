package com.luckyzyx.luckytool.hook.scopes.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ScanNfcTagAutoClick : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 40) loadHooker(NfcTagAutoClick)
        else loadHooker(NfcTagAutoClickV16)
    }

    @Obfuscate
    object NfcTagAutoClick : YukiBaseHooker() {
        override fun onHook() {
            var isEnable = preferences(ModulePrefs).getBoolean("scan_nfc_tag_auto_click", false)
            dataChannel.wait<Boolean>("scan_nfc_tag_auto_click") { isEnable = it }

            //Source OplusNotificationManager
            "com.oplus.nfc.dispatch.OplusNotificationManager".toClass().resolve().apply {
                //参数表会随版本增删（17 起在头部多了一个候选列表，Intent 不再是固定的第 4 个），
                //所以只按名字 + 参数里出现 Intent 定位，取参也按类型取
                firstMethod {
                    name = "showNotificationIfNeeded"
                    parameters { it.contains(Intent::class.java) }
                }.hook {
                    after {
                        if (!isEnable || result != true) return@after
                        val context = firstField { name = "mContext" }.of(instance).get<Context>()
                            ?: return@after
                        val intent = args.firstOrNull { it is Intent } as? Intent ?: return@after
                        sendProcessTagBroadcast(context, intent)
                    }
                }
            }
        }
    }

    @Obfuscate
    object NfcTagAutoClickV16 : YukiBaseHooker() {
        override fun onHook() {
            var isEnable = preferences(ModulePrefs).getBoolean("scan_nfc_tag_auto_click", false)
            dataChannel.wait<Boolean>("scan_nfc_tag_auto_click") { isEnable = it }

            //Source TagDetectedNotification
            "com.oplus.nfc.dispatch.TagDetectedNotification".toClass().resolve().apply {
                firstMethod { name = "show" }.hook {
                    before {
                        if (!isEnable) return@before
                        val context = firstArg().get<Context>() ?: return@before
                        val intent = arg(1).get<Intent>() ?: return@before
                        val type = arg(2).get<Int>() ?: 0
//                    val bundle = lastArg().get<Bundle>()

                        sendProcessTagBroadcast(context, intent, type)
                    }
                }
            }
        }
    }

    private fun sendProcessTagBroadcast(context: Context, intent: Intent, type: Int? = null) {
        val pendingIntent = Intent().apply {
            action = "com.oplus.nfc.dispatch.TagDetectedNotification.ACTION_PROCESS_TAG"
            putExtra("dispatcherIntent", intent)
            if (type != null) putExtra("componentType", type)
            setPackage("com.android.nfc")
        }
        PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(), pendingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).send()
    }
}
