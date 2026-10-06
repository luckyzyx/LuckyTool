package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getScreenOrientation
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object FixTileAlignBothSides : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode <= 26) loadHooker(HookTileAlignVertical)
        loadHooker(HookTileAlignHorizontal)
    }

    @Obfuscate
    private object HookTileAlignVertical : YukiBaseHooker() {
        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            //Sourcee QuickStatusBarHeader 竖屏溢出
            //Search quick_qs_panel -> qs_header_panel_side_padding 24dp
            "com.android.systemui.qs.QuickStatusBarHeader".toClass().resolve().apply {
                firstMethod { name = "updateHeadersPadding" }.hook {
                    after {
                        firstField { name = "mHeaderQsPanel" }.of(instance).get<LinearLayout>()
                            ?.apply {
                                val qsHeaderPanelSidePadding = safeOfNull {
                                    resources.getDimensionPixelSize(
                                        resources.getIdentifier(
                                            "qs_header_panel_side_padding", "dimen",
                                            packageName
                                        )
                                    )
                                } ?: return@after
                                setViewPadding(qsHeaderPanelSidePadding)
                            }
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookTileAlignHorizontal : YukiBaseHooker() {
        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            val isCustomTile = preferences(ModulePrefs).getBoolean("control_center_tile_enable", false)
            val columnHorizontal = preferences(ModulePrefs).getInt("tile_columns_horizontal_c13", 4)

            val QSFragmentHelperCls = VariousClass(
                "com.oplusos.systemui.qs.helper.QSFragmentHelper", //C13
                "com.oplus.systemui.qs.helper.QSFragmentHelper" //C14
            ).toClass()

            //Source QSFragmentHelper 横屏溢出
            //Search expanded_qs_scroll_view -> qs_brightness_mirror_side_padding / qs_bottom_side_padding 24dp
            VariousClass(
                "com.android.systemui.qs.QSFragment", //C13
                "com.oplus.systemui.qs.OplusQSFragment", //C13
                "com.oplus.systemui.qs.OplusQSImpl" //C14 C15
            ).toClass().resolve().apply {
                firstMethod { name = "updateQsState" }.hook {
                    after {
                        val qSFragmentHelper = QSFragmentHelperCls.resolve().firstMethod {
                            name = "getInstance"
                        }.invoke() ?: return@after
                        qSFragmentHelper.asResolver().firstField {
                            name = "mQSPanelScrollView"
                        }.get<ViewGroup>()?.apply {
                            getScreenOrientation(this) {
                                if (it) setViewPadding(0)
                                else {
                                    val qsBrightnessMirrorSidePadding = safeOfNull {
                                        resources.getDimensionPixelSize(
                                            resources.getIdentifier(
                                                "qs_brightness_mirror_side_padding",
                                                "dimen",
                                                packageName
                                            )
                                        )
                                    } ?: return@getScreenOrientation
                                    if (isCustomTile && columnHorizontal > 4) setViewPadding(
                                        qsBrightnessMirrorSidePadding
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun View.setViewPadding(leftAndRight: Int) {
        setPadding(
            leftAndRight, paddingTop,
            leftAndRight, paddingBottom
        )
    }
}