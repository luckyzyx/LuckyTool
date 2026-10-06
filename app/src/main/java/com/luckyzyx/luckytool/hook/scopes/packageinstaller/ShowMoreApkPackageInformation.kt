package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.view.Gravity
import android.widget.LinearLayout
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.highcapable.betterandroid.ui.extension.view.child
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.appdetail.ApkDetailsView
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.safeOf
import com.oplus.util.OplusUnitConversionUtils
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.io.File

@Obfuscate
class ShowMoreApkPackageInformation(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    lateinit var loadApkInfo: MethodData

    private val cacheApkInfoMap = java.util.WeakHashMap<Any, ArrayMap<String, Any>>()
    private val cacheSourceInfoMap = java.util.WeakHashMap<Any, ArrayMap<String, Any>>()

    override fun onHook() {
        val apkInfoViewClazz = "com.android.packageinstaller.oplus.view.ApkInfoView"
        val apkInfoClazz = "com.android.packageinstaller.oplus.common.ApkInfo"
        val sourceInfoClazz = "com.android.packageinstaller.oplus.common.SourceInfo"

        //Source ApkInfoView
        dexKitBridge.findClass {
            matcher {
                className(apkInfoViewClazz)
            }
        }.apply {
            checkDataList("ApkInfoView")
            loadApkInfo = findMethod {
                matcher {
                    usingStrings("loadApkInfo")
                }
            }.apply {
                checkDataList("loadApkInfo")
            }.singleOrNull() ?: return
        }.singleOrNull() ?: return

        //Source ApkInfo
        apkInfoClazz.toClass().resolve().apply {
            firstConstructor { parameterCount = 7 }.hook {
                after {
                    cacheApkInfoMap[instance] = arrayMapOf(
                        "icon" to (arg(0).get<Int>() ?: 0),
                        "apkPath" to (arg(1).get<String>() ?: ""),
                        "label" to (arg(2).get<String>() ?: ""),
                        "versionName" to (arg(3).get<String>() ?: ""),
                        "versionCode" to (arg(4).get<Int>() ?: 0),
                        "packageName" to (arg(5).get<String>() ?: ""),
                        "size" to (arg(6).get<Long>() ?: 0L),
                    )
                }
            }
        }

        //Source SourceInfo
        sourceInfoClazz.toClass().resolve().apply {
            firstConstructor { parameterCount = 4 }.hook {
                after {
                    cacheSourceInfoMap[instance] = arrayMapOf(
                        "sourcePackage" to (arg(0).get<String>() ?: ""),
                        "sourceName" to (arg(1).get<String>() ?: ""),
                        "bUnknownSource" to (arg(2).get<Boolean>() ?: false),
                        "actionType" to (arg(3).get<Int>() ?: 0),
                    )
                }
            }
        }

        //Source ApkInfoView
        apkInfoViewClazz.toClass().resolve().apply {
            firstMethod {
                name = loadApkInfo.name
                parameterCount = loadApkInfo.paramCount
            }.hook {
                after {
                    val apkInfoView = instance<LinearLayout>()
                    val context = apkInfoView.context
                    val pm = context.packageManager

                    val apkInfo =
                        arg(args.indexOfFirst { it?.javaClass?.name == apkInfoClazz }).get()
                    val sourceInfo =
                        arg(args.indexOfFirst { it?.javaClass?.name == sourceInfoClazz }).get()

                    val cacheApkInfo = cacheApkInfoMap[apkInfo] ?: return@after
                    val cacheSourceInfo = cacheSourceInfoMap[sourceInfo] ?: return@after

                    val actionType = cacheSourceInfo["actionType"] as? Int ?: -1
                    val installSource = cacheSourceInfo["sourceName"] as? String
                        ?: cacheSourceInfo["sourcePackage"] as? String ?: ""

                    val appName = cacheApkInfo["label"] as? String ?: ""
                    val packName = cacheApkInfo["packageName"] as? String ?: ""
                    val versionName = cacheApkInfo["versionName"] as? String ?: ""
                    val versionCode = cacheApkInfo["versionCode"] as? Int ?: -1
                    val apkFilePath = cacheApkInfo["apkPath"] as? String ?: ""
                    val apkSize = cacheApkInfo["size"] as? Long ?: -1

                    val packInfo = PackageUtils(pm).getPackageArchiveInfo(apkFilePath, 1)
                    val newIcon = packInfo?.applicationInfo?.apply {
                        sourceDir = apkFilePath
                        publicSourceDir = apkFilePath
                    }?.loadIcon(pm)

                    val curPackInfo = PackageUtils(pm).getPackageInfo(packName, 0)
                    val curIcon = curPackInfo?.applicationInfo?.loadIcon(pm)
                    val curVersionName = curPackInfo?.versionName

                    val isInstall = actionType == 0
                    val isUninstall = actionType == 1

                    // Reuse the same information cards as AppDetail for install, uninstall and
                    // every other action, but use LinearLayout's normal measurement here instead
                    // of its ConstraintLayout integration. The incoming/current comparison only
                    // makes sense on install, so pass incoming as null otherwise.
                    val tag = "LuckyTool.ClassicApkDetails"
                    val panel = apkInfoView.findViewWithTag(tag) ?: ApkDetailsView(context).also {
                        it.tag = tag
                        apkInfoView.addView(
                            it, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        )
                    }
                    for (i in 0 until apkInfoView.childCount) {
                        val child = apkInfoView.child(i)
                        child.visibility =
                            if (child === panel) android.view.View.VISIBLE else android.view.View.GONE
                    }
                    apkInfoView.orientation = LinearLayout.VERTICAL
                    apkInfoView.gravity = Gravity.TOP or Gravity.START
                    val padding = (16 * context.resources.displayMetrics.density).toInt()
                    apkInfoView.setPadding(padding, padding, padding, padding)
                    apkInfoView.layoutParams = apkInfoView.layoutParams.apply {
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                    }
                    panel.bind(
                        packName,
                        versionName,
                        versionCode.toString(),
                        apkFilePath,
                        if (isInstall) packInfo else null,
                        curPackInfo
                    )
                    panel.addAppHeader(
                        appName,
                        if (isInstall) newIcon ?: curIcon ?: pm.defaultActivityIcon
                        else curIcon ?: pm.defaultActivityIcon,
                        if (isInstall) getInstallSourceText(context, installSource) else "",
                        if (isInstall) versionName else curVersionName ?: versionName,
                        getApkSizeFormat(context, curPackInfo, apkSize, isUninstall)
                    )

                    cacheApkInfoMap.clear()
                    cacheSourceInfoMap.clear()
                }
            }
        }
    }

    private fun getApkSizeFormat(
        context: Context, packInfo: PackageInfo?, size: Long, isUninstall: Boolean
    ): String {
        val apkSize = if (isUninstall) {
            val sourceDir = packInfo?.applicationInfo?.sourceDir
            if (sourceDir.isNullOrBlank()) size else File(sourceDir).length()
        } else size
        return OplusUnitConversionUtils(context).getUnitValue(apkSize)
    }

    @SuppressLint("DiscouragedApi")
    private fun getInstallSourceText(context: Context, source: String): String {
        return safeOf("From: $source") {
            context.resources.getString(
                context.resources.getIdentifier(
                    "from_source", "string", context.packageName
                ), source
            )
        }
    }

}