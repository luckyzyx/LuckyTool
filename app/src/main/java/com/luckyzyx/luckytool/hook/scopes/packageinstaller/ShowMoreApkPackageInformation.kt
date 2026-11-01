package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.highcapable.betterandroid.ui.extension.view.child
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
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
                    val tag = TAG_PANEL
                    val panel = apkInfoView.findViewWithTag(tag) ?: ApkDetailsView(context, module.resources).also {
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
                        child.visibility = if (child === panel) View.VISIBLE else View.GONE
                    }
                    apkInfoView.orientation = LinearLayout.VERTICAL
                    apkInfoView.gravity = Gravity.TOP or Gravity.START
                    val padding = (16 * context.resources.displayMetrics.density).toInt()
                    apkInfoView.setPadding(padding, padding, padding, padding)
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
                    // 卡片高度随包名等长文本换行变高，宿主容器自己不滚动，超出一屏后安装按钮
                    // 会被挤出可见区域；这里给面板补上滚动能力，按钮仍按自身高度测量。
                    ensureScrollable(apkInfoView)

                    cacheApkInfoMap.clear()
                    cacheSourceInfoMap.clear()
                }
            }
        }
    }

    /**
     * 让面板在宿主容器里能滚动。
     *
     * 宿主把 ApkInfoView 放在不可滚动的容器里，卡片一高就把下方的安装按钮挤出屏幕；
     * 这里在 ApkInfoView 外补一层 [ScrollView]，并沿用宿主原本的布局参数占位，
     * 使滚动区域只吃掉内容区的高度，按钮仍按自身高度测量。
     * 宿主自己已经提供滚动容器时不再套第二层，避免嵌套滚动抢手势。
     */
    private fun ensureScrollable(apkInfoView: LinearLayout) {
        val hostParams = apkInfoView.layoutParams ?: return
        if (apkInfoView.hasScrollableAncestor()) {
            hostParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            apkInfoView.layoutParams = hostParams
            return
        }
        val parent = apkInfoView.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(apkInfoView)
        if (index < 0) return
        val scrollView = ScrollView(apkInfoView.context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        parent.removeViewAt(index)
        scrollView.addView(
            apkInfoView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        parent.addView(scrollView, index)
        // 纵向 LinearLayout 里原本 wrap_content 的内容区改成占满剩余空间，
        // 其余容器（ConstraintLayout 等）直接沿用宿主参数，约束关系保持不变。
        scrollView.layoutParams = if (hostParams is LinearLayout.LayoutParams && parent is LinearLayout
            && parent.orientation == LinearLayout.VERTICAL && hostParams.weight == 0f
            && hostParams.height == LinearLayout.LayoutParams.WRAP_CONTENT
        ) LinearLayout.LayoutParams(hostParams).apply { height = 0; weight = 1f } else hostParams
    }

    private companion object {
        const val TAG_PANEL = "LuckyTool.ClassicApkDetails"
    }

    // 宿主可能用自己的 ScrollView/NestedScrollView 子类，按类名匹配即可，
    // 命中就说明滚动已经不缺，再多套一层只会抢走手势。
    private fun View.hasScrollableAncestor(): Boolean {
        var current = parent
        while (current is View) {
            if (current.javaClass.name.contains("ScrollView")) return true
            current = current.parent
        }
        return false
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