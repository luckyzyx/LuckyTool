package com.luckyzyx.luckytool.hook.scopes.systemui

import android.graphics.drawable.ShapeDrawable
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.ThemeUtils.isNightMode
import com.luckyzyx.luckytool.utils.formatColorAlpha
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomTileBackgroundTransparency : YukiBaseHooker() {
    override fun onHook() {
        val customAlpha = preferences(ModulePrefs).getInt("custom_tile_background_transparency", -1)

        //Source OplusQsMediaPanelBgDrawable status_bar_qs_tile_bg_color_inactive
        "com.oplus.systemui.qs.media.OplusQsMediaPanelBgDrawable".toClass().resolve().apply {
            firstConstructor { parameterCount = 5 }.hook {
                before {
                    if (customAlpha < 0) return@before
                    val value = customAlpha / 10.0F
                    val view = firstArg().get<View>() ?: return@before
                    if (view.context.isNightMode) return@before
                    val color = arg(1).get<Int>() ?: 0
                    val newColor = formatColorAlpha(color, value)
                    arg(1).set(newColor)
                }
            }
        }

        //Source OplusQSTileBaseView status_bar_qs_tile_bg_color_inactive
        VariousClass(
            "com.oplusos.systemui.qs.qstileimpl.OplusQSTileBaseView",  //C13
            "com.oplus.systemui.qs.qstileimpl.OplusQSTileBaseView",  //C14
            "com.oplus.systemui.qs.base.tile.OplusQSTileBaseView"  //C15
        ).toClass().resolve().apply {
            firstMethod { name = "generateDrawable" }.hook {
                after {
                    if (customAlpha < 0) return@after
                    val value = customAlpha / 10.0F
                    val view = instance<View>()
                    if (view.context.isNightMode) return@after
                    val type = firstArg().get<Int>() ?: 0
                    val shapeDrawable = result<ShapeDrawable>() ?: return@after
                    if (type == 1) {
                        val color = shapeDrawable.paint.color
                        val newColor = formatColorAlpha(color, value)
                        shapeDrawable.paint.color = newColor
                    }
                }
            }
        }

        //Source OplusQSHighlightTileView status_bar_qs_tile_bg_color_inactive
        VariousClass(
            "com.oplusos.systemui.qs.qstileimpl.OplusQSHighlightTileView",  //C13
            "com.oplus.systemui.qs.qstileimpl.OplusQSHighlightTileView",  //C14
            "com.oplus.systemui.qs.base.tile.OplusQSHighlightTileView"  //C15
        ).toClass().resolve().apply {
            firstMethod { name = "generateDrawable" }.hook {
                after {
                    if (customAlpha < 0) return@after
                    val value = customAlpha / 10.0F
                    val view = instance<View>()
                    if (view.context.isNightMode) return@after
                    val type = firstArg().get<Int>() ?: 0
                    val shapeDrawable = result<ShapeDrawable>() ?: return@after
                    if (type == 1) {
                        val color = shapeDrawable.paint.color
                        val newColor = formatColorAlpha(color, value)
                        shapeDrawable.paint.color = newColor
                    }
                }
            }
        }

    }
}