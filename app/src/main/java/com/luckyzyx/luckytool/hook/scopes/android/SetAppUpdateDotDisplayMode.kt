package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object SetAppUpdateDotDisplayMode : YukiBaseHooker() {

    private const val InstallSource = "com.android.server.pm.InstallSource"
    private const val OplusPMHelper = "com.android.server.pm.OplusOsPackageManagerHelper"

    override fun onHook() {
        val mode = preferences(ModulePrefs).getString("set_app_update_dot_display_mode", "0")
        if (mode == "0") return

        //Source PackageManagerServiceExtImpl
        "com.android.server.pm.PackageManagerServiceExtImpl".toClass().resolve().apply {
            firstMethod {
                name = "handleSuccessAtEndInHPPI"
                parameterCount = 6
            }.hook {
                after {
                    val packName = arg(2).get<String>() ?: ""
                    val installSource = arg(3).get()

                    val isUpdate = arg(4).get<Boolean>() ?: false
                    val marketList = firstField {
                        name = "DEFAULT_MARKET_LIST";type = List::class
                    }.get<List<String>>() ?: java.util.ArrayList()

                    val installerPackageName = installSource as? String
                        ?: installSource?.asResolver()
                            ?.firstField { name = "mInstallerPackageName" }
                            ?.get<String>() ?: ""

                    if (isUpdate || marketList.contains(installerPackageName)) {
                        if (mode == "1") {
                            OplusPMHelper.toClass().resolve().firstMethod {
                                name = "addPkgToNotLaunchedList";parameters(String::class)
                            }.invoke(packName)
                        }
                    }
                }
            }
        }
    }
}