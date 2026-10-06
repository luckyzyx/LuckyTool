package com.luckyzyx.luckytool.hook.scopes.systemui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.toColorInt
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableVolumeBarPercentDisplay : YukiBaseHooker() {
    override fun onHook() {
        var color = preferences(ModulePrefs).getString("custom_volume_bar_percent_color", "#FFFFFFFF")
        dataChannel.wait<String>("custom_volume_bar_percent_color") { color = it }

        //Source OplusVolumeSeekBar
        "com.oplus.systemui.volume.OplusVolumeSeekBar".toClass().resolve().apply {
            firstMethod {
                name = "drawActiveTrack"
                parameters(Canvas::class)
            }.hook {
                after {
                    val view = instance<View>()
                    val canvas = firstArg().get<Canvas>() ?: return@after
                    val progress = firstMethod { name = "getProgress"; superclass() }
                        .of(instance).invoke<Int>() ?: return@after
                    val max = firstMethod { name = "getMax"; superclass() }.of(instance)
                        .invoke<Int>() ?: return@after
                    if (max <= 0) return@after
                    val width = firstMethod { name = "getWidth"; superclass() }
                        .of(instance).invoke<Int>() ?: return@after
                    val height = firstMethod { name = "getHeight"; superclass() }.of(instance)
                        .invoke<Int>() ?: return@after
                    if (width <= 0 || height <= 0) return@after
                    val percentage = (progress * 100) / max
                    val textPaint = TextPaint().apply {
                        isAntiAlias = true
                        setColor(color.toColorInt())
                        textSize = 12F.dp
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val x = width / 2.0F
                    val y = (height * 0.25F) - (view.resources.displayMetrics.density * 10)
                    val safeY = if (y < textPaint.textSize) textPaint.textSize else y
                    canvas.drawText("$percentage%", x, safeY, textPaint)
                }
            }
        }
    }
}