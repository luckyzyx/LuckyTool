package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.MethodData

@Obfuscate
class HookInstallAppProgress(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    val removeAds = preferences(ModulePrefs).getBoolean("remove_install_ads", false)
    val autoDone = preferences(ModulePrefs).getBoolean("auto_click_install_button", false)

    override fun onHook() {
        //Source InstallAppProgress
        dexKitBridge.findClass {
            matcher {
                className(
                    "com.android.packageinstaller.oplus.InstallAppProgress",
                    StringMatchType.StartsWith
                )
            }
        }.apply {
            checkDataList("InstallAppProgress", onlyOne = false)

            val initView = findMethod {
                matcher {
                    paramCount(0)
                    returnType(Void.TYPE)
                    addCaller { name("onCreate") }
                    usingStrings("source_info", "type_channel_title", "type_channel_tips")
                }
            }.checkDataList("initView").single()
            val handleMessage = findMethod {
                matcher {
                    name("handleMessage")
                    usingStrings(
                        "oplus.intent.action.VIRUS_APK_INSTALLED",
                        "oplus.permission.OPLUS_COMPONENT_SAFE"
                    )
                }
            }.checkDataList("handleMessage").single()

            val onPackageInstalled = findMethod {
                matcher {
                    paramTypes(classOf<Int>())
                    usingNumbers(1)
                    addUsingField { type(classOf<Handler>()) }
                    addCaller { name(initView.name) }
                    addCaller { name("onReceive") }
                }
            }.checkDataList("onPackageInstalled").single()

            if (removeAds) hookRemoveAds(initView, handleMessage)
            if (autoDone) hookAutoDone(onPackageInstalled)
        }
    }

    fun hookRemoveAds(initView: MethodData, handleMessage: MethodData) {
        var activity: Activity? = null

        initView.className.toClass().resolve().apply {
            firstMethod { name = initView.methodName }.hook {
                after {
                    activity = instance<Activity>()
                    activity.removeViews()
                }
            }
        }
        handleMessage.className.toClass().resolve().apply {
            firstMethod { name = handleMessage.methodName }.hook {
                after {
                    activity?.removeViews()
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    fun hookAutoDone(onPackageInstalled: MethodData) {
        onPackageInstalled.className.toClass().resolve().apply {
            firstMethod { name = onPackageInstalled.methodName }.hook {
                after {
                    val activity = instance<Activity>()
                    if ((firstArg().get<Int>() ?: 0) == 0) {
                        activity.findViewById<Button>(
                            activity.resources.getIdentifier(
                                "done_button", "id",
                                this@HookInstallAppProgress.packageName
                            )
                        )?.performClick()
                    }
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun Activity.removeViews() {
        findViewById<View>(
            resources.getIdentifier(
                "suggest_A_scroll_layout", "id",
                this@HookInstallAppProgress.packageName
            )
        )?.isVisible = false
        findViewById<View>(
            resources.getIdentifier(
                "install_done_suggest_B", "id",
                this@HookInstallAppProgress.packageName
            )
        )?.isVisible = false
    }
}