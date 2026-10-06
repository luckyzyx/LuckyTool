package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.View
import android.widget.Button
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData

@Obfuscate
class HookOplusPackageInstallerActivity(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    val disableScan = preferences(ModulePrefs).getBoolean("skip_apk_scan", false)
    val allowReplace = preferences(ModulePrefs).getBoolean("allow_downgrade_install", false)
    val autoInstall = preferences(ModulePrefs).getBoolean("auto_click_install_button", false)

    override fun onHook() {
        //Source OPlusPackageInstallerActivity
        dexKitBridge.findClass {
            matcher {
                className("com.android.packageinstaller.oplus.OPlusPackageInstallerActivity")
            }
        }.apply {
            checkDataList("OPlusPackageInstallerActivity")

            val checkToScanRisk = findMethod {
                matcher {
                    paramCount(0)
                    usingFields {
                    add { type(classOf<Long>()) }
                        add { type(classOf<Boolean>()) }
                        add { type(classOf<View>()) }
                    }
                    addCaller { name("onClick") }
                    addInvoke { paramCount(0);returnType(Void.TYPE) }
                    usingNumbers(8)
                }
            }.checkDataList("checkToScanRisk").single()
            val initiateInstall = findMethod {
                matcher {
                    paramCount(0)
                    usingFields {
                    add { type(classOf<PackageManager>()) }
                        add { type(classOf<PackageInfo>()) }
                        add { type(classOf<ApplicationInfo>()) }
                        add { type(classOf<Boolean>()) }
                    }
                    addCaller { name("onClick") }
                    addInvoke { paramCount(0);returnType(Void.TYPE) }
                    usingNumbers(3, 8388608, 8192)
                }
            }.checkDataList("initiateInstall").single()

            val parseReplaceInstall = findMethod {
                matcher {
                    paramCount(0)
                    usingStrings("currentVersionCode", "apkVersioncode")
                }
            }.checkDataList("parseReplaceInstall").single()
            val preSafeInstall = findMethod {
                matcher {
                    paramCount(0)
                    usingStrings("startAppdetail", "reason")
                }
            }.checkDataList("parseReplaceInstall").single()

            val startInstallConfirm = findMethod {
                matcher {
                    paramCount(0)
                    usingNumbers(0)
                    addCaller { name(initiateInstall.name);paramCount(0) }
                    addInvoke { paramCount(0);returnType(Void.TYPE) }
                    addUsingField { type(classOf<View>()) }
                    addUsingField { type(classOf<Boolean>()) }
                    addUsingField { type(classOf<ArrayList<*>>()) }
                }
            }.checkDataList("startInstallConfirm").single()

            if (disableScan) hookSkipScanRisk(checkToScanRisk, initiateInstall)
            if (allowReplace) hookAllowReplace(parseReplaceInstall, preSafeInstall)
            if (autoInstall) hookAutoClickOk(startInstallConfirm)
        }
    }

    fun hookSkipScanRisk(checkToScanRisk: MethodData, initiateInstall: MethodData) {
        checkToScanRisk.className.toClass().resolve().apply {
            firstMethod { name = checkToScanRisk.methodName }.hook {
                before {
                    firstMethod { name = initiateInstall.methodName }.of(instance).invoke()
                    result = null
                }
            }
        }
    }

    fun hookAllowReplace(parseReplaceInstall: MethodData, preSafeInstall: MethodData) {
        parseReplaceInstall.className.toClass().resolve().apply {
            firstMethod { name = parseReplaceInstall.methodName }.hook {
                before {
                    firstMethod { name = preSafeInstall.methodName }.of(instance).invoke()
                    result = null
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    fun hookAutoClickOk(startInstallConfirm: MethodData) {
        startInstallConfirm.className.toClass().resolve().apply {
            firstMethod { name = startInstallConfirm.methodName }.hook {
                after {
                    val activity = instance<Activity>()
                    activity.findViewById<Button>(
                        activity.resources.getIdentifier(
                            "ok_button", "id",
                            this@HookOplusPackageInstallerActivity.packageName
                        )
                    ).performClick()
                }
            }
        }
    }
}