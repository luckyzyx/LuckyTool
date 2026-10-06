package com.luckyzyx.luckytool.hook.scopes.audiomonitor

import android.annotation.SuppressLint
import android.content.Context
import androidx.collection.arrayMapOf
import androidx.core.content.edit
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.ArrayClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.data.VoipRecorder
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.GlobalKeyValue.dyPackName
import com.luckyzyx.luckytool.utils.GlobalKeyValue.fsPackName
import com.luckyzyx.luckytool.utils.GlobalKeyValue.qqPackName
import com.luckyzyx.luckytool.utils.GlobalKeyValue.qywxPackName
import com.luckyzyx.luckytool.utils.GlobalKeyValue.timPackName
import com.luckyzyx.luckytool.utils.GlobalKeyValue.wxPackName
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class VoipRecorderWhitelist(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    private val audioApplication = "com.oplus.audiomonitor.AudioApplication"
    private val oplusVoipRecorderService =
        "com.oplus.audiomonitor.voiprecord.OplusVoipRecorderService"

    private var switchAppClazz = ""
    private var appNameField = ""
    private var appStatusField = ""

    private val filePrefix = "未知应用录音"

    private val apps = arrayOf(
        VoipRecorder(qqPackName, "QQ"),
        VoipRecorder(wxPackName, "微信"),
        VoipRecorder(qywxPackName, "企业微信"),
        VoipRecorder(timPackName, "Tim"),
        VoipRecorder(fsPackName, "飞书"),
        VoipRecorder(dyPackName, "抖音", "com.bytedance.android.xr.fusion.XrAvCallActivity"),
//        VoipRecorder(tgPackName, "Telegram", "org.telegram.ui.CallsActivity"),
    )

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        val appList = arrayListOf<String>()
        val appMap = arrayMapOf<String, String>()
        apps.onEachIndexed { _, it ->
            appList.add(it.packName)
            if (it.activity.isNotBlank()) appMap[it.packName] = it.activity
        }

        //Source OplusVoipRecorderService
        oplusVoipRecorderService.toClass(initialize = true).resolve().apply {
            firstMethod { name = "onCreate" }.hook {
                before {
                    field { type = ArrayList::class }.forEachIndexed { _, field ->
                        val list =
                            field.get<java.util.ArrayList<String>>() ?: return@forEachIndexed
                        if (list.contains(qqPackName) && list.contains(wxPackName)) {
                            list.clear()
                            list.addAll(appList)
                        }
                        if (list.contains("com.tencent.av.ui.AVActivity")) {
                            list.addAll(appMap.values)
                        }
                    }
                }
            }
            firstMethod { emptyParameters();returnType = Boolean::class }.hook {
                after {
                    val packName = firstField { type = String::class }.get<String>() ?: ""
                    if (appList.contains(packName)) result = true
                }
            }
        }

        //Source OplusRecordWrapper
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<Context>())
                    addForType(classOf<String>())
                    addForType(classOf<Boolean>())
                }
                methods {
                    add { paramCount(0);returnType(Void.TYPE) }
                    add { paramCount(0);returnType(classOf<String>()) }
                    add { paramTypes(classOf<String>());returnType(classOf<Boolean>()) }
                }
                usingStrings("OplusRecordWrapper")
            }
        }.apply {
            checkDataList("HookVoipRecorder RecordingFilePrefix")
            single().name.toClass().resolve().apply {
                method { emptyParameters();returnType = String::class }.hookAll {
                    after {
                        val fileName = result<String>() ?: return@after
                        val packName = oplusVoipRecorderService.toClass().resolve().firstField {
                            type = String::class
                        }.get<String>() ?: ""
                        val appName = apps.find { it.packName == packName }?.appName ?: return@after
                        result = fileName.replace(filePrefix, appName)
                    }
                }
            }
        }

        //Source SwitchApp
        dexKitBridge.findClass {
            matcher {
            addFieldForType(classOf<String>())
                addFieldForType(classOf<Boolean>())
                addMethod { name("equals") }
                addMethod { name("hashCode") }
                addMethod {
                    name("toString")
                    usingStrings("appName", "packageName", "check")
                }
                usingStrings("appName", "packageName", "check")
            }
        }.apply {
            checkDataList("HookVoipRecorder SwitchApp Instance")
            switchAppClazz = single().name
            if (switchAppClazz.isBlank()) return

            appNameField = findField {
                matcher {
                    type(classOf<String>())
                    addReadMethod { name("toString") }
                    addWriteMethod { paramTypes(classOf<List<*>>());returnType(classOf<List<*>>()) }
                }
            }.checkDataList("HookVoipRecorder Util AppName").single().fieldName
            if (appNameField.isBlank()) return

            appStatusField = findField {
                matcher {
                    type(classOf<Boolean>())
                    addReadMethod { name("onPostExecute") }
                    addWriteMethod { paramTypes(classOf<List<*>>());returnType(classOf<List<*>>()) }
                }
            }.checkDataList("HookVoipRecorder Util AppStatus").single().fieldName
            if (appStatusField.isBlank()) return
        }

        //Source Util
        dexKitBridge.findClass {
            matcher {
                addFieldForType(ArrayClass(String::class))
                addMethod {
                    paramTypes(classOf<List<*>>())
                    returnType(classOf<List<*>>())
                    usingStrings(qqPackName, wxPackName, "enable_record_app")
                }
                addMethod {
                    paramTypes(classOf<String>())
                    returnType(classOf<Boolean>(primitiveType = false))
                }
                usingStrings(qqPackName, wxPackName)
            }
        }.apply {
            checkDataList("HookVoipRecorder Util")

            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(List::class)
                    returnType = List::class
                }.hook {
                    before {
                        val list = ArrayList<Any>()
                        var prefsValue = ""

                        val context = audioApplication.toClass().resolve().firstField {
                            type = Context::class
                        }.get<Context>() ?: return@before
                        val prefs = context.getSharedPreferences(
                            context.packageName + "_preferences", Context.MODE_PRIVATE
                        )

                        val enabledApp =
                            prefs.getString("enable_record_app", "")?.split("#") ?: arrayListOf()

                        apps.forEachIndexed { index, it ->
                            val existApp = enabledApp.contains(it.packName)

                            val switchApp = switchAppClazz.toClass().resolve().firstConstructor {
                                parameters(String::class, Boolean::class)
                            }.create(it.packName, existApp).apply {
                                asResolver().firstField {
                                    name = appNameField;type = String::class
                                }.set(it.appName)
                                val wxIcon = safeOfNull {
                                    context.resources.getIdentifier(
                                        "icon_wechat",
                                        "mipmap",
                                        this@VoipRecorderWhitelist.packageName
                                    )
                                } ?: return@forEachIndexed
                                asResolver().firstField { type = Int::class }.set(wxIcon)
//                                    val isInstalled = PackageUtils(context.packageManager)
//                                        .getPackageInfo(it.packName, 0) != null
                                asResolver().firstField {
                                    name = appStatusField;type = Boolean::class
                                }.set(true)
                            }

                            list.add(switchApp)

                            if (existApp) {
                                if (index > 0) prefsValue += "#"
                                prefsValue += it.packName
                            }
                        }

                        if (prefsValue.isBlank()) return@before
                        prefs.edit { putString("enable_record_app", prefsValue) }
                        result = list
                    }
                }
            }
        }
    }
}