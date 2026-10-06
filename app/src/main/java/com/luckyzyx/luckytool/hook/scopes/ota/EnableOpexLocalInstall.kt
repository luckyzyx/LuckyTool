package com.luckyzyx.luckytool.hook.scopes.ota

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import android.view.Menu
import androidx.core.content.edit
import com.highcapable.betterandroid.ui.extension.view.toast
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.showToast
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.io.File

@Obfuscate
class EnableOpexLocalInstall(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        val packageListInfo = "com.oplus.ota.db.PackageListInfo"

        val opexCopyResultCode = "com.oplus.ota.opex.OpexPackageHelper\$OpexCopyResultCode"

        val OpexMenuItemCode = 10000
    }

    override fun onHook() {
        //OpexPackageHelper
        val opexPackageHelper = dexKitBridge.findClass {
            matcher {
                methods {
                    add {
                        paramTypes(classOf<String>())
                        returnType(packageListInfo)
                    }
                    add {
                        paramCount(3..4)
                        returnType(opexCopyResultCode)
                    }
                }
                usingStrings("OpexPackageHelper")
            }
        }.apply {
            checkDataList("OpexPackageHelper")
        }.single().name.toClass()

        //Source EntryActivity
        "com.oplus.otaui.activity.EntryActivity".toClass().resolve().apply {
            firstMethod {
                name = "onCreateOptionsMenu"
                parameters(Menu::class)
                returnType = Boolean::class
            }.hook {
                after {
                    val activity = instance<Activity>()
                    val menu = firstArg().get<Menu>() ?: return@after
                    menu.add(0, OpexMenuItemCode, 0, "Opex")
                    menu.findItem(OpexMenuItemCode)?.setOnMenuItemClickListener {
                        val intent = Intent("android.intent.action.OPEN_DOCUMENT")
                        intent.addCategory("android.intent.category.OPENABLE")
                        intent.setType("*/*")
                        activity.startActivityForResult(intent, OpexMenuItemCode)
                        true
                    }

                }
            }
            firstMethod {
                name = "onActivityResult"
                parameters(Int::class, Int::class, Intent::class)
                returnType = Void.TYPE
            }.hook {
                before {
                    val activity = instance<Activity>()
                    val requestCode = firstArg().get<Int>() ?: 0
                    val resultCode = arg(1).get<Int>() ?: 0
                    val intent = lastArg().get<Intent>() ?: return@before
                    if (requestCode == OpexMenuItemCode && resultCode == Activity.RESULT_OK) {
                        try {
                            val sp =
                                activity.getSharedPreferences("state_info", Context.MODE_PRIVATE)
                            sp.edit(commit = true) {
                                putString(
                                    "realOtaVersion",
                                    SystemProperties.get("ro.build.version.ota", "")
                                )
                            }
                        } catch (t: Throwable) {
                            YLog.debug("prefs state_info error: ${t.message}")
                        }

                        val uri = intent.data ?: return@before
                        try {
                            activity.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (t: Throwable) {
                            YLog.debug("takePersistableUriPermission error: ${t.message}")
                        }

                        val name = uri.path?.substringAfterLast("/") ?: return@before
                        if (!name.startsWith("ovl_update")) {
                            activity.toast("not ovl_update")
                            return@before
                        }

                        val opexDir = File(activity.cacheDir, "opexs_cache")
                        if (opexDir.exists()) FileUtils.deleteFile(opexDir)
                        if (!opexDir.exists()) opexDir.mkdirs()

                        val opexFile = File(opexDir, name)
                        if (!opexFile.exists()) {
                            activity.showToast("$name file is not found")
                            return@before
                        }
                        FileUtils.copyUriToFile(activity, uri, opexFile)

                        val fileList = opexDir.listFiles {
                            it.name.startsWith("ovl_update")
                        } ?: arrayOf()

                        val info = opexPackageHelper.resolve().firstMethod {
                            parameters(String::class)
                            returnType = packageListInfo
                        }.invoke(opexDir.path) ?: return@before

                        fileList.forEachIndexed { index, file ->
                            val name = file.nameWithoutExtension.substringAfterLast("/")
                            val copy = opexPackageHelper.resolve().firstMethod {
                                parameterCount { it in 3..4 }
                                parameters {
                                    it[0] == classOf<Context>() && it[1].name == packageListInfo && it[2] == classOf<Int>()
                                }
                                returnType = opexCopyResultCode
                            }
                            val code = if (copy.self.parameterCount == 3) {
                                copy.invoke(activity, info, index)
                            } else {
                                copy.invoke(activity, info, index, false)
                            }
                            YLog.debug("$name -> $code")
                            activity.showToast("$name -> $code")
                        }

                        FileUtils.deleteFile(opexDir)
                        result = null
                    }
                }
            }
        }
    }
}
