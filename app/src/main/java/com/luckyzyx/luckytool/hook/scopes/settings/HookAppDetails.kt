package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isSystem
import com.luckyzyx.luckytool.utils.safeOf
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookAppDetails : YukiBaseHooker() {

    override fun onHook() {
        loadHooker(HookAppInfos)

        loadHooker(HookAppInfoDashboard)
    }

    @Obfuscate
    object HookAppInfos : YukiBaseHooker() {

        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            val isPackName =
                preferences(ModulePrefs).getBoolean("show_package_name_in_app_details", false)
            val isSdk = preferences(ModulePrefs).getBoolean("show_sdk_in_app_details", false)
            val isFirstInstallTime =
                preferences(ModulePrefs).getBoolean("show_first_install_time_in_app_details", false)
            val isLastUpdateTime =
                preferences(ModulePrefs).getBoolean("show_last_update_time_in_app_details", false)
            val isInstallSource =
                preferences(ModulePrefs).getBoolean("show_install_source_in_app_details", false)
            val isEnableCopy =
                preferences(ModulePrefs).getBoolean("enable_long_press_to_copy_in_app_details", false)

            //Source AppInfoFeature
            "com.oplus.settings.feature.appmanager.AppInfoFeature".toClass().resolve().apply {
                firstMethod {
                    name = "setAppLabelAndIcon"
                    parameterCount = 1
                }.hook {
                    after {
                        val mRootView = firstField { name = "mRootView" }.of(instance).get<View>()
                            ?: return@after
                        val appButtonsPreferenceController = firstArg().get() ?: return@after
                        val instrumentedPreferenceFragment = appButtonsPreferenceController
                            .asResolver().firstField { name = "mFragment" }.get() ?: return@after
                        val packageInfo = instrumentedPreferenceFragment.asResolver().firstField {
                            type = PackageInfo::class
                        }.get<PackageInfo>() ?: return@after
                        val appInfo = packageInfo.applicationInfo

                        val context = mRootView.context
                        context.injectModuleResources()

                        val appSize = mRootView.findViewById<TextView>(
                            context.resources.getIdentifier(
                                "app_size", "id", this@HookAppInfos.packageName
                            )
                        )
                        val packName = packageInfo.packageName
                        val appVerInfo = AppUtils(context).getAppVerInfo(packName, false)
                            ?: return@after
                        var version = "${appVerInfo.versionName}(${appVerInfo.versionCode})"
                        if (appVerInfo.versionCommit.isNotBlank()) version += "_${appVerInfo.versionCommit}"
                        val versionText = context.getString(
                            context.resources.getIdentifier(
                                "version_text", "string", this@HookAppInfos.packageName
                            ), version
                        )

                        val list = ArrayList<String>()

                        if (isPackName) list.add(packName)

                        if (isSdk && appInfo != null) {
                            val min = appInfo.minSdkVersion
                            val target = appInfo.targetSdkVersion
                            list.add("Min $min Target $target")
                        }

                        list.add(versionText)

                        if (isFirstInstallTime) {
                            val firstInstallTimeStr = safeOf("First Install Time") {
                                context.getString(R.string.first_install_time)
                            }
                            val firstInstallTime =
                                formatDate("yyyy/MM/dd HH:mm:ss", packageInfo.firstInstallTime)
                            list.add("$firstInstallTimeStr $firstInstallTime")
                        }
                        if (isLastUpdateTime) {
                            val lastUpdateTimeStr = safeOf("Last Update Time") {
                                context.getString(R.string.last_update_time)
                            }
                            val lastUpdateTime =
                                formatDate("yyyy/MM/dd HH:mm:ss", packageInfo.lastUpdateTime)
                            list.add("$lastUpdateTimeStr $lastUpdateTime")
                        }

                        if (isInstallSource) {
                            val installSourceStr = safeOf("Install Source") {
                                context.getString(R.string.install_source)
                            }
                            val sourceInfo =
                                PackageUtils(context.packageManager).getInstallSourceInfo(packName)
                            val sourcePackName = sourceInfo?.installingPackageName ?: ""
                            val sourceAppName = AppUtils(context).getAppLabel(sourcePackName)
                            if (sourceAppName.isNotBlank()) list.add("$installSourceStr $sourceAppName")
                        }

                        appSize?.apply {
                            if (isEnableCopy) setTextIsSelectable(true)
                            text = formatStringAuto(list, "\n")
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object HookAppInfoDashboard : YukiBaseHooker() {
        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            val osCode = getOSVersionCode
            val quickMarket = preferences(ModulePrefs).getBoolean("enable_quick_open_market_page", false)
            val quickClone = preferences(ModulePrefs).getBoolean("enable_app_clone_quick_jump", false)

            //Source AppInfoDashboardFragment
            "com.android.settings.applications.appinfo.AppInfoDashboardFragment".toClass().resolve()
                .apply {
                    firstMethod { name = "onCreateOptionsMenu" }.hook {
                        after {
                            val menu = firstArg().get<Menu>() ?: return@after
//                        val menuInflater = lastArg().get<MenuInflater>() ?: return@after
                            val context = firstMethod {
                                name = "getContext"
                                superclass()
                            }.of(instance).invoke<Context>() ?: return@after

                            val packageInfo = firstField { type = PackageInfo::class }.of(instance)
                                .get<PackageInfo>() ?: return@after

                            if (quickMarket) {
                                context.injectModuleResources()
                                val openMarketLabel = safeOf("Open Market") {
                                    context.getString(R.string.open_market)
                                }
                                menu.add(0, 900, 0, openMarketLabel)
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                            }

                            if (osCode >= 27 && quickClone && !packageInfo.isSystem()) {
                                val multiAppLabel =
                                    AppUtils(context).getAppLabel("com.oplus.multiapp")
                                menu.add(0, 999, 0, multiAppLabel)
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                            }
                        }
                    }
                    firstMethod { name = "onOptionsItemSelected" }.hook {
                        before {
                            val menuItem = firstArg().get<MenuItem>() ?: return@before
                            val context = firstMethod {
                                name = "getContext"
                                superclass()
                            }.of(instance).invoke<Context>() ?: return@before
                            val packageInfo = firstField { type = PackageInfo::class }.of(instance)
                                .get<PackageInfo>() ?: return@before
                            val packName = packageInfo.packageName

                            when (menuItem.itemId) {
                                900 -> if (quickMarket) AppUtils(context).openMarketIntent(packName)
                                999 -> if (osCode >= 27 && quickClone && !packageInfo.isSystem()) {
                                    val appLabel = AppUtils(context).getAppLabel(packName)
                                    try {
                                        AppUtils(context).openMultiAppIntent(appLabel, packName)
                                    } catch (e: Throwable) {
                                        YLog.debug("EnableAppCloneQuickJump startActivity error", e = e)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}