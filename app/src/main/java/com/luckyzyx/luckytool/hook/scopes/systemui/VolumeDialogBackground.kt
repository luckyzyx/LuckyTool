package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Message
import android.view.View
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.hookAll
import com.luckyzyx.luckytool.hook.core.instance
import com.luckyzyx.luckytool.hook.core.result
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.formatColorAlpha
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class VolumeDialogBackground(val dexKitBridge: DexKitBridge) : Hooker {

    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 37) loadHooker(VolumeDialog)
        else if (osCode >= 35) loadHooker(VolumeDialogV15(dexKitBridge))
        else loadHooker(VolumeDialogV14)
    }

    @Obfuscate
    object VolumeDialog : Hooker {
        override fun onHook() {
            var customAlpha =
                prefs(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            dataChannel.wait<Int>("custom_volume_dialog_background_transparency") {
                customAlpha = it
            }
            if (customAlpha < 0) return

            //Source OplusVolumeDialogView
            "com.oplus.systemui.volume.view.OplusVolumeDialogView".toClass().resolve().apply {
                firstMethod { name = "initDialog" }.hook {
                    after {
                        if (customAlpha < 0) return@after
                        val value = customAlpha * 25

                        firstField { name = "mMoreRowStreamLl" }.of(instance).get<View>()
                            ?.background?.alpha = 255 - value
                        firstField { name = "mAppVolumeAdjustFl" }.of(instance).get<View>()
                            ?.background?.alpha = 255 - value
                    }
                }
            }

            //Source OplusVolumeSeekBar
            "com.oplus.systemui.volume.OplusVolumeSeekBar".toClassOrNull()?.resolve()?.optional(true)?.apply {
                constructor {}.hookAll {
                    after {
                        if (customAlpha < 0) return@after
                        val seekBar = instance<Any>()
                        seekBar.asResolver().firstMethod {
                            name = "setProgressColor"
                            parameters(ColorStateList::class)
                            superclass()
                        }.invoke(
                            ColorStateList.valueOf(
                                formatColorAlpha(Color.WHITE, 0.5F)
                            )
                        )
                    }
                }
            }

            //Source VolumeBlurManager
            "com.oplus.systemui.volume.utils.VolumeBlurManager".toClassOrNull()?.resolve()?.optional(true)?.apply {
                firstMethodOrNull { name = "getBackgroundBlurDrawable" }?.hook {
                    after {
                        if (customAlpha < 0) return@after
                        val value = customAlpha * 25
                        val drawable = result<Drawable>()
                        if (drawable is BackgroundBlurDrawable) {
                            drawable.setBlurRadius(value.dp)
                        }
                    }
                } ?: run {
                    firstMethod { name = "getVolumeBarBackground" }.hook {
                        after {
                            if (customAlpha < 0) return@after
                            val value = customAlpha * 25
                            val drawable = result<Drawable>()
                            if (drawable is LayerDrawable) {
                                val blurDrawable = drawable.getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                drawable.getDrawable(1)?.alpha = value
                            } else drawable?.alpha = value
                        }
                    }
                    firstMethod { name = "getVolumePanelBackground" }.hook {
                        after {
                            if (customAlpha < 0) return@after
                            val value = customAlpha * 25
                            val drawable = result<Drawable>()
                            if (drawable is BackgroundBlurDrawable) {
                                drawable.setBlurRadius(value.dp)
                            } else {
                                drawable?.alpha = value
                            }
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    class VolumeDialogV15(val dexKitBridge: DexKitBridge) : Hooker {
        override fun onHook() {
            var customAlpha =
                prefs(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            dataChannel.wait<Int>("custom_volume_dialog_background_transparency") {
                customAlpha = it
            }
            if (customAlpha < 0) return

            //Source VolumeDialogImplEx / OplusVolumeDialogImpl
            val volumnDialogClazz = VariousClass(
                "com.oplusos.systemui.volume.VolumeDialogImplEx", //C13
                "com.oplus.systemui.volume.OplusVolumeDialogImpl" //C14 C15
            ).toClass()

            volumnDialogClazz.resolve().apply {
                firstMethodOrNull { name = "isSurrealQualityOn" }?.hook {
                    replaceToFalse()
                }

                firstMethod { name { it.startsWith("initDialog") } }.hook {
                    after {
                        val dialog = firstField { name = "mDialog" }.of(instance).get<Dialog>()
                            ?: return@after
                        val message = dialog.asResolver().firstField {
                            name = "mShowMessage";superclass()
                        }.get<Message>() ?: return@after
                        hookShowListener(volumnDialogClazz, message.obj, customAlpha)
                    }
                }

                firstFieldOrNull { name = "mVerticalRowsLayerDrawableMap" }?.let {
                    firstMethodOrNull { name = "updateRowsH" }?.hook {
                        before {
                            if (customAlpha < 0) return@before
                            val value = customAlpha * 25
                            it.copy().of(instance).get<HashMap<Int, LayerDrawable>>()?.apply {
                                forEach { (key, layer) ->
                                    val blurDrawable = layer.getDrawable(0)
                                    if (blurDrawable is BackgroundBlurDrawable) {
                                        blurDrawable.setBlurRadius(value.dp)
                                    }
                                    layer.getDrawable(1)?.alpha = value
                                    put(key, layer)
                                }
                            }
                        }
                    }
                }
                firstMethodOrNull { name = "expandPanel" }?.hook {
                    before {
                        if (customAlpha < 0) return@before
                        val value = customAlpha * 25
                        firstFieldOrNull {
                            name = "mVolumeBackgroundLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        } ?: run {
                            firstField {
                                name = "mVolumeBackgroundBlurDrawable"
                            }.of(instance).get<Drawable>()?.alpha = value
                        }
                    }
                }
            }

            //Source OplusVolumeSeekBar
            "com.oplus.systemui.volume.OplusVolumeSeekBar".toClassOrNull()?.resolve()?.apply {
                constructor {}.hookAll {
                    after {
                        if (customAlpha < 0) return@after
                        val seekBar = instance<Any>()
                        seekBar.asResolver().firstMethod {
                            name = "setProgressColor"
                            parameters(ColorStateList::class)
                            superclass()
                        }.invoke(
                            ColorStateList.valueOf(
                                formatColorAlpha(Color.WHITE, 0.5F)
                            )
                        )
                    }
                }
            }

            //Source VolumeBlurManager
            "com.oplus.systemui.volume.utils.VolumeBlurManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "getBackgroundBlurDrawable" }?.hook {
                    after {
                        if (customAlpha < 0) return@after
                        val value = customAlpha * 25
                        val drawable = result<Drawable>()
                        if (drawable is BackgroundBlurDrawable) {
                            drawable.setBlurRadius(value.dp)
                        }
                    }
                } ?: run {
                    firstMethod { name = "getVolumeBarBackground" }.hook {
                        after {
                            if (customAlpha < 0) return@after
                            val value = customAlpha * 25
                            result<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            }
                        }
                    }
                    firstMethod { name = "getVolumePanelBackground" }.hook {
                        after {
                            if (customAlpha < 0) return@after
                            val value = customAlpha * 25
                            val drawable = result<Drawable>()
                            if (drawable is BackgroundBlurDrawable) {
                                drawable.setBlurRadius(value.dp)
                            } else {
                                drawable?.alpha = value
                            }
                        }
                    }
                }
            }
        }

        fun hookShowListener(dialog: Class<Any>, listener: Any, customAlpha: Int) {
            listener.javaClass.resolve().apply {
                firstMethod { name = "onShow" }.hook {
                    before {
                        if (customAlpha < 0) return@before
                        val value = customAlpha * 25

                        (firstFieldOrNull { type = dialog }?.let {
                            it.of(instance).get() ?: return@before
                        } ?: instance).asResolver().apply {

                            firstFieldOrNull {
                                name = "mVerticalRowsLayerDrawable"
                            }?.get<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            }
                            firstFieldOrNull {
                                name = "mVolumeMoreLayerDrawable"
                            }?.get<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            } ?: run {
                                firstField { name = "mMoreRowStreamLl" }.get<View>()
                                    ?.background?.alpha = 255 - value
                            }
                            firstFieldOrNull {
                                name = "mVolumeAppAdjustLayerDrawable"
                            }?.get<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            }
                            firstFieldOrNull {
                                name = "mVolumeCaptionLayerDrawable"
                            }?.get<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            }
                            firstFieldOrNull {
                                name = "mVolumeBackgroundLayerDrawable"
                            }?.get<LayerDrawable>()?.apply {
                                val blurDrawable = getDrawable(0)
                                if (blurDrawable is BackgroundBlurDrawable) {
                                    blurDrawable.setBlurRadius(value.dp)
                                }
                                getDrawable(1)?.alpha = value
                            } ?: run {
                                firstField {
                                    name = "mVolumeBackgroundBlurDrawable"
                                }.get<Drawable>()?.alpha = value
                            }
                            firstFieldOrNull {
                                name = "mVolumeBtnDrawable"
                            }?.get<Drawable>()?.apply {
                                alpha = 255 - value
                            } ?: run {
                                firstField { name = "mAppVolumeAdjustFl" }.get<View>()?.background
                                    ?.alpha = 255 - value
                                firstField { name = "mDoubleEarView" }.get<View>()?.background
                                    ?.alpha = 255 - value
                                firstField { name = "mODICaptionsView" }.get<View>()?.background
                                    ?.alpha = 255 - value
                            }
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object VolumeDialogV14 : Hooker {
        override fun onHook() {
            var customAlpha =
                prefs(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            dataChannel.wait<Int>("custom_volume_dialog_background_transparency") {
                customAlpha = it
            }
            if (customAlpha < 0) return

            //Source VolumeDialogImplEx
            val volumnDialogClazz = VariousClass(
                "com.oplusos.systemui.volume.VolumeDialogImplEx", //C13
                "com.oplus.systemui.volume.OplusVolumeDialogImpl" //C14 C15
            ).toClass()

            volumnDialogClazz.resolve().apply {
                firstMethodOrNull { name = "isSurrealQualityOn" }?.hook {
                    replaceToFalse()
                }

                firstMethod { parameters(DialogInterface::class) }.hook {
                    before {
                        if (customAlpha < 0) return@before
                        val value = customAlpha * 25
                        firstFieldOrNull {
                            name = "mVerticalRowsLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                        firstFieldOrNull {
                            name = "mVolumeMoreLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                        firstFieldOrNull {
                            name = "mVolumeAppAdjustLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                        firstFieldOrNull {
                            name = "mVolumeCaptionLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                        firstFieldOrNull {
                            name = "mVolumeBackgroundLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                        firstFieldOrNull {
                            name = "mVolumeBtnDrawable"
                        }?.of(instance)?.get<Drawable>()?.apply {
                            alpha = 255 - value
                        }
                    }
                }
                firstFieldOrNull { name = "mVerticalRowsLayerDrawableMap" }?.let {
                    (firstMethodOrNull { name = "addVerticalContainerBg" } ?: firstMethod {
                        name = "updateVolumeRowBgForSide"
                    }).hook {
                        before {
                            if (customAlpha < 0) return@before
                            val value = customAlpha * 25
                            it.copy().of(instance).get<HashMap<Int, LayerDrawable>>()?.apply {
                                forEach { (key, layer) ->
                                    val blurDrawable = layer.getDrawable(0)
                                    if (blurDrawable is BackgroundBlurDrawable) {
                                        blurDrawable.setBlurRadius(value.dp)
                                    }
                                    layer.getDrawable(1)?.alpha = value
                                    put(key, layer)
                                }
                            }
                        }
                    }
                }
                firstFieldOrNull { name = "mVerticalRowsLayerDrawableMap" }?.let {
                    firstMethodOrNull { name = "updateRowsH" }?.hook {
                        before {
                            if (customAlpha < 0) return@before
                            val value = customAlpha * 25
                            it.copy().of(instance).get<HashMap<Int, LayerDrawable>>()?.apply {
                                forEach { (key, layer) ->
                                    val blurDrawable = layer.getDrawable(0)
                                    if (blurDrawable is BackgroundBlurDrawable) {
                                        blurDrawable.setBlurRadius(value.dp)
                                    }
                                    layer.getDrawable(1)?.alpha = value
                                    put(key, layer)
                                }
                            }
                        }
                    }
                }
                firstMethodOrNull { name = "expandPanel" }?.hook {
                    before {
                        if (customAlpha < 0) return@before
                        val value = customAlpha * 25
                        firstFieldOrNull {
                            name = "mVolumeBackgroundLayerDrawable"
                        }?.of(instance)?.get<LayerDrawable>()?.apply {
                            val blurDrawable = getDrawable(0)
                            if (blurDrawable is BackgroundBlurDrawable) {
                                blurDrawable.setBlurRadius(value.dp)
                            }
                            getDrawable(1)?.alpha = value
                        }
                    }
                }
            }

            //Source OplusVolumeSeekBar
            "com.oplus.systemui.volume.OplusVolumeSeekBar".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "init" }.hook {
                    after {
                        if (customAlpha < 0) return@after
                        val seekBar = instance<Any>()
                        seekBar.asResolver().firstMethod {
                            name = "setProgressColor"
                            parameters(ColorStateList::class)
                            superclass()
                        }.invoke(
                            ColorStateList.valueOf(
                                formatColorAlpha(Color.WHITE, 0.5F)
                            )
                        )
                    }
                }
            }

            //Source VolumeBlurManager
            "com.oplus.systemui.volume.utils.VolumeBlurManager".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "getBackgroundBlurDrawable" }.hook {
                    after {
                        if (customAlpha < 0) return@after
                        val value = customAlpha * 25
                        val drawable = result<Drawable>()
                        if (drawable is BackgroundBlurDrawable) {
                            drawable.setBlurRadius(value.dp)
                        }
                    }
                }
            }
        }
    }
}