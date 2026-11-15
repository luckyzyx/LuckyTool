package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

    companion object {
        private const val TAG_CONTENT = "LuckyTool.ApkDetailsContent"

        // 宿主自己在信息区下面排了「应用权限」行与按钮行，找不到兄弟视图时按这个高度给它们留位置
        private const val RESERVED_HEIGHT_DP = 120
    }

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

                    // 只有带上 GET_META_DATA，解析出来的清单才会带 meta-data（OS 版本区间靠它显示）
                    val packInfo = PackageUtils(pm).getPackageArchiveInfo(
                        apkFilePath, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
                    )
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
                    val scroller = apkInfoView.findViewWithTag<ContentScroller>(TAG_CONTENT)
                        ?: ContentScroller(context).apply {
                            tag = TAG_CONTENT
                            apkInfoView.addView(
                                this, LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            )
                        }
                    val panel = (scroller.getChildAt(0) as? ApkDetailsView)
                        ?: ApkDetailsView(context, module.resources).also {
                            scroller.addView(
                                it, FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                )
                            )
                        }
                    for (i in 0 until apkInfoView.childCount) {
                        val child = apkInfoView.child(i)
                        child.visibility = if (child === scroller) View.VISIBLE else View.GONE
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
                    limitContentHeight(apkInfoView, scroller, panel)

                    cacheApkInfoMap.clear()
                    cacheSourceInfoMap.clear()
                }
            }
        }
    }

    /**
     * 把信息区的高度限制在窗口剩余空间之内，超出部分改为在 [scroller] 里滚动。
     *
     * 宿主底部确认面板按内容高度排布，横屏时窗口更矮，信息区一旦超过一屏，
     * 排在下方的「应用权限」行和安装按钮就会被推到屏幕外，按钮只剩一条边。
     * 这里不改宿主容器，只把自己测量出来的高度压回可用高度；监听会一直保留，
     * 旋转后内容与可用高度都变了，下一次布局就能重新算。
     */
    private fun limitContentHeight(container: View, scroller: ContentScroller, content: View) {
        if (scroller.heightLimiterAttached) return
        scroller.heightLimiterAttached = true
        // 监听跟着视图树一起结束，不需要移除；每次布局都会重算，旋转后同样能修正
        container.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 窗口可见区域与自身位置都按屏幕坐标算，避开状态栏与导航栏
                val frame = Rect()
                container.rootView.getWindowVisibleDisplayFrame(frame)
                val location = IntArray(2)
                container.getLocationOnScreen(location)
                val padding = container.paddingTop + container.paddingBottom
                val reserved =
                    maxOf(siblingHeightBelow(container), container.context.dp(RESERVED_HEIGHT_DP))
                val limit = frame.bottom - location[1] - reserved - padding
                if (limit <= 0 || content.measuredHeight <= 0) return
                // 内容高度在内容区宽度不变时是稳定值，改完高度下一次布局就不会再变
                val params = scroller.layoutParams ?: return
                val target = minOf(content.measuredHeight, limit)
                if (params.height == target) return
                params.height = target
                scroller.requestLayout()
            }
        })
    }

    // 信息区下面还排着宿主自己的行，量出它们占掉的可见高度，用来给信息区留出位置
    private fun siblingHeightBelow(view: View): Int {
        val parent = view.parent as? ViewGroup ?: return 0
        var height = parent.paddingBottom
        for (i in parent.indexOfChild(view) + 1 until parent.childCount) {
            val sibling = parent.getChildAt(i)
            if (sibling.visibility == View.VISIBLE) height += sibling.measuredHeight
        }
        return height
    }

    private fun Context.dp(value: Int) =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    // 记录高度限制是否已经在当前视图树上登记过，避免重复绑定同一页面时叠加监听
    private class ContentScroller(context: Context) : ScrollView(context) {
        var heightLimiterAttached = false
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