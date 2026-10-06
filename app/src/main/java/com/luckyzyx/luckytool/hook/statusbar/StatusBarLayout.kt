package com.luckyzyx.luckytool.hook.statusbar

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.highcapable.betterandroid.ui.extension.view.parent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getScreenOrientation
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("UNUSED_VARIABLE", "DiscouragedApi")
object StatusBarLayout : YukiBaseHooker() {
    private var statusBarLeftMargin: Int = 0
    private var statusBarRightMargin: Int = 0
    private var statusBarTopMargin: Int = 0
    private var statusBarBottomMargin: Int = 0

    override fun onHook() {
        if (SDK != A13) return
        var mLeftLayout: LinearLayout? = null
        var mRightLayout: LinearLayout? = null
        var mCenterLayout: LinearLayout?
        var mStatusBar: ViewGroup? = null

        val layoutMode = preferences(ModulePrefs).getString("statusbar_layout_mode", "0")

        val isCompatibleMode =
            preferences(ModulePrefs).getBoolean("statusbar_layout_compatible_mode", false)
        val leftMargin =
            preferences(ModulePrefs).getInt("statusbar_layout_left_margin", 0)
        val rightMargin =
            preferences(ModulePrefs).getInt("statusbar_layout_right_margin", 0)

        fun updateCustomLayout(context: Context) {
            getScreenOrientation(context) {
                if (it) {
                    mLeftLayout?.setPadding(statusBarLeftMargin, 0, 0, 0)
                    mRightLayout?.setPadding(0, 0, statusBarRightMargin, 0)
                    mStatusBar?.setPadding(0, statusBarTopMargin, 0, statusBarBottomMargin)
                } else {
                    mLeftLayout?.setPadding(0, 0, 0, 0)
                    mRightLayout?.setPadding(0, 0, 0, 0)
                }
            }
        }

        fun updateDefaultLayout(context: Context, leftView: ViewGroup?, rightView: ViewGroup?) {
            if (!isCompatibleMode) return
            getScreenOrientation(context) {
                if (it) {
                    leftView?.setPadding(leftMargin, 0, 0, 0)
                    rightView?.setPadding(0, 0, rightMargin, 0)
                } else {
                    leftView?.setPadding(0, 0, 0, 0)
                    rightView?.setPadding(0, 0, 0, 0)
                }
            }
        }

        fun setCustomMargin() {
            if (!isCompatibleMode) return
            if (leftMargin != 0) statusBarLeftMargin = leftMargin
            if (rightMargin != 0) statusBarRightMargin = rightMargin
        }

        //Source ScreenDecorations
        "com.android.systemui.ScreenDecorations\$DisplayCutoutView".toClass().resolve().apply {
            firstMethod {
                name = "boundsFromDirection"
                parameterCount = 3
            }.hook {
                before {
                    if (isCompatibleMode) arg(1).set(0)
                }
            }
        }

        //Source CollapsedStatusBarFragment
        VariousClass(
            "com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", //A12
            "com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment" //C13
        ).toClass().resolve().apply {
            firstMethod {
                name = "onViewCreated"
                parameterCount = 2
            }.hook {
                after {
                    val phoneStatusBarView = arg(0).get<ViewGroup>()!!
                    val context = phoneStatusBarView.context
                    val res = phoneStatusBarView.resources
                    val statusBarId = res?.getIdentifier(
                        "status_bar", "id",
                        packageName
                    )
                    val statusBarContentsId =
                        res?.getIdentifier("status_bar_contents", "id", packageName)

                    val statusBarLeftSideId =
                        res?.getIdentifier(
                            "status_bar_left_side", "id",
                            packageName
                        )
                    val clockId = res?.getIdentifier("clock", "id", packageName)
                    val systemPromptViewId =
                        res?.getIdentifier("system_prompt_view", "id", packageName)
                    val notificationIconAreaInnerId =
                        res?.getIdentifier(
                            "notification_icon_area_inner", "id",
                            packageName
                        )

                    val systemIconAreaId = res?.getIdentifier(
                        "system_icon_area", "id",
                        packageName
                    )
                    val statusIconsId = res?.getIdentifier(
                        "statusIcons", "id",
                        packageName
                    )
                    val batteryId = res?.getIdentifier("battery", "id", packageName)

                    mStatusBar =
                        statusBarId?.let { phoneStatusBarView.findViewById(it) } ?: return@after
                    val statusBarContents: ViewGroup? =
                        statusBarContentsId?.let { phoneStatusBarView.findViewById(it) }
                    val statusBarLeftSide: ViewGroup? =
                        statusBarLeftSideId?.let { phoneStatusBarView.findViewById(it) }
                    val clock: TextView? = clockId?.let { phoneStatusBarView.findViewById(it) }
                    val systemPromptView: ImageView? =
                        systemPromptViewId?.let { phoneStatusBarView.findViewById(it) }
                    val notificationIconAreaInner: ViewGroup? =
                        notificationIconAreaInnerId?.let { phoneStatusBarView.findViewById(it) }
                    val systemIconArea: ViewGroup? =
                        systemIconAreaId?.let { phoneStatusBarView.findViewById(it) }
                    val statusIcons: ViewGroup? =
                        statusIconsId?.let { phoneStatusBarView.findViewById(it) }
                    val battery: ViewGroup? =
                        batteryId?.let { phoneStatusBarView.findViewById(it) }

                    mLeftLayout = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f
                        )
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }

                    mCenterLayout = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                    }

                    mRightLayout = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f
                        )
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    }

                    if (layoutMode == "0" && isCompatibleMode) {
                        setCustomMargin()
                        updateDefaultLayout(context, statusBarLeftSide, systemIconArea)
                        return@after
                    }
                    if (layoutMode.isBlank() || layoutMode == "0") return@after

                    clock?.parent()?.removeView(clock)
                    statusBarLeftSide?.parent()?.removeView(statusBarLeftSide)
                    systemIconArea?.parent()?.removeAllViews()

                    statusBarLeftSide?.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    mLeftLayout.addView(statusBarLeftSide)
                    mCenterLayout.addView(clock)
                    mRightLayout.addView(systemIconArea)

                    statusBarContents?.addView(mLeftLayout, 0)
                    statusBarContents?.addView(mCenterLayout)
                    statusBarContents?.addView(mRightLayout)

                    statusBarLeftMargin = mStatusBar.paddingLeft
                    statusBarRightMargin = mStatusBar.paddingRight
                    statusBarTopMargin = mStatusBar.paddingTop
                    statusBarBottomMargin = mStatusBar.paddingBottom

                    setCustomMargin()
                    updateCustomLayout(context)
                }
            }
            firstMethod { name = "onDestroyView" }.hook {
                if (layoutMode != "0" && getOSVersionCode == 26) intercept()
            }
        }

        //Source PhoneStatusBarView
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().resolve().apply {
            firstMethod { name = "updateLayoutForCutout" }.hook {
                after {
                    if (isCompatibleMode) updateCustomLayout(instance<ViewGroup>().context)
                }
            }
        }

        //Source KeyguardStatusBarViewExImpl
        "com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl".toClass().resolve().apply {
            firstMethod { name = "onFinishInflate" }.hook {
                after {
                    //keyguard_status_bar_contents
                    if (isCompatibleMode) firstField {
//                        keyguardStatusbarLeftContView / keyguardStatusBarLeftContView
                        name {
                            it.startsWith("keyguardStatus")
                            it.endsWith("LeftContView")
                        }
                    }.of(instance).get<ViewGroup>()?.setPadding(leftMargin, 0, 0, 0)
                }
            }
        }
    }
}