package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.DependencyUtils
import com.luckyzyx.luckytool.hook.utils.sysui.MediaPlayerDataUtils
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object MediaPlayerPanel : YukiBaseHooker() {
    override fun onHook() {
        //自动显示媒体播放器
        val isAutoDisplay = VariousClass(
            "com.oplusos.systemui.qs.OplusQSTileMediaContainer", //C13.1
            "com.oplus.systemui.qs.OplusQSTileMediaContainer" //C14
        ).toClassOrNull(hostClassLoader)?.let {
            it.resolve().firstMethodOrNull { name = "setMediaMode" } != null
        } ?: true

        if (isAutoDisplay) loadHooker(MediaPlayerDisplayMode)
        else loadHooker(MediaPlayerDisplayModePermanent)

        //强制开启媒体切换按钮
        if (preferences(ModulePrefs).getBoolean("force_enable_media_toggle_button", false)) {
            if (SDK == A13) loadHooker(ForceEnableMediaToggleButton)
        }
    }

    @Obfuscate
    object MediaPlayerDisplayMode : YukiBaseHooker() {
        override fun onHook() {
            var mode = preferences(ModulePrefs).getString("set_media_player_display_mode", "0")
            dataChannel.wait<String>("set_media_player_display_mode") { mode = it }

            //Source OplusQsMediaCarouselController
            val controller =
                "com.oplus.systemui.qs.media.OplusQsMediaCarouselController".toClass().resolve()
                    .apply {
                        firstMethodOrNull { name = "setCurrentMediaData" }?.hook {
                            after {
                                val status = when (mode) {
                                    "1" -> true
                                    "2" -> false
                                    "3" -> getMediaData() != null
                                    else -> return@after
                                }
                                val mediaModeChangeListener =
                                    firstField { name = "mediaModeChangeListener" }.of(instance)
                                        .get() ?: return@after
                                mediaModeChangeListener.asResolver()
                                    .firstMethod { name = "onChanged" }
                                    .invoke(status)
                            }
                            firstMethodOrNull { name = "setMediaModeChangeListener" }?.hook {
                                after {
                                    val status = when (mode) {
                                        "1" -> true
                                        "2" -> false
                                        "3" -> getMediaData() != null
                                        else -> return@after
                                    }
                                    val mediaModeChangeListener =
                                        firstArg().get() ?: return@after
                                    mediaModeChangeListener.asResolver()
                                        .firstMethod { name = "onChanged" }.invoke(status)
                                }
                            }
                        }
                    }

            if (controller.firstMethodOrNull { name = "setCurrentMediaData" } != null) return

            //Source OplusQSContainerImpl
            "com.oplus.systemui.qs.OplusQSContainerImpl".toClass().resolve().apply {
                firstMethod { name = "setQsMediaPanelShown" }.hook {
                    before {
                        val status = when (mode) {
                            "1" -> true
                            "2" -> false
                            "3" -> getMediaData() != null
                            else -> return@before
                        }
                        firstArg().set(status)
                    }
                }
            }
            //Source OplusQSTileMediaContainerController
            "com.oplus.systemui.qs.OplusQSTileMediaContainerController".toClass().resolve().apply {
                firstMethod { name = "setQsMediaPanelShown" }.hook {
                    before {
                        val status = when (mode) {
                            "1" -> true
                            "2" -> false
                            "3" -> getMediaData() != null
                            else -> return@before
                        }
                        firstArg().set(status)
                    }
                }
            }
        }
    }

    @Obfuscate
    object MediaPlayerDisplayModePermanent : YukiBaseHooker() {
        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            var mode = preferences(ModulePrefs).getString("set_media_player_display_mode", "0")
            dataChannel.wait<String>("set_media_player_display_mode") {
                mode = it
                ControlCenterTiles.callback?.invoke("set_media_player_display_mode", it)
            }

            //Source updateQsMediaPanelView
            VariousClass(
                "com.oplusos.systemui.qs.OplusQSTileMediaContainer", //C13.1
                "com.oplus.systemui.qs.OplusQSTileMediaContainer" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "setListening" }.hook {
                    after {
                        firstMethod { name = "updateResources" }.of(instance).invoke()
                    }
                }
                firstMethod { name = "updateQsMediaPanelView" }.hook {
                    before {
                        val status = when (mode) {
                            "1" -> 0
                            "2" -> 8
                            "3" -> if (getMediaData() == null) 8 else 0
                            else -> return@before
                        }
                        val res = firstArg().get<Resources>() ?: return@before
                        val bool = lastArg().get<Boolean>() ?: return@before
                        val linear = firstField { name = "mQsMediaPanelContainer" }.of(instance)
                            .get<LinearLayout>() ?: return@before
                        val mTmpConstraintSet =
                            firstField { name = "mTmpConstraintSet" }.of(instance).get()
                                ?: return@before
                        val smallHeight = res.getIdentifier(
                            "oplus_qs_media_panel_height_smallspace", "dimen", packageName
                        ).takeIf { it != 0 } ?: return@before
                        val height = res.getIdentifier(
                            "oplus_qs_media_panel_height", "dimen", packageName
                        ).takeIf { it != 0 } ?: return@before
                        val heightSize = safeOfNull {
                            res.getDimensionPixelSize(if (bool) smallHeight else height)
                        } ?: return@before
                        mTmpConstraintSet.setVisibilitySet(linear.id, status)
                        if (status == 0) mTmpConstraintSet.constrainHeightSet(
                            linear.id, heightSize
                        )
                        result = null
                    }
                }
                firstMethod { name = "updateQsSecondTileContainer" }.hook {
                    before {
                        val isShow = when (mode) {
                            "1" -> true
                            "2" -> false
                            "3" -> getMediaData() != null
                            else -> return@before
                        }
                        val res = firstArg().get<Resources>() ?: return@before
                        val bool = lastArg().get<Boolean>() ?: return@before
                        val linear = firstField { name = "mSecondTileContainer" }.of(instance)
                            .get<LinearLayout>() ?: return@before
                        val mTmpConstraintSet =
                            firstField { name = "mTmpConstraintSet" }.of(instance).get()
                                ?: return@before
                        val smallSideMargin = res.getIdentifier(
                            "qs_footer_hl_tile_side_margin_smallspace", "dimen", packageName
                        ).takeIf { it != 0 } ?: return@before
                        val sideMargin = res.getIdentifier(
                            "qs_footer_hl_tile_side_margin", "dimen", packageName
                        ).takeIf { it != 0 } ?: return@before
                        val sideSize = safeOfNull {
                            res.getDimensionPixelSize(if (bool) smallSideMargin else sideMargin)
                        } ?: return@before
                        val guideLine = res.getIdentifier(
                            "guide_line", "id", packageName
                        ).takeIf { it != 0 } ?: return@before
                        if (isShow) {
                            val firstTile = firstField { name = "mFirstTileContainer" }.of(instance)
                                .get<LinearLayout>() ?: return@before
                            val smallContainerMargin = res.getIdentifier(
                                "qs_footer_hl_tile_two_container_margin_top_smallspace",
                                "dimen",
                                packageName
                            ).takeIf { it != 0 } ?: return@before
                            val containerMargin = res.getIdentifier(
                                "qs_footer_hl_tile_two_container_margin_top", "dimen", packageName
                            ).takeIf { it != 0 } ?: return@before
                            val containerSize = safeOfNull {
                                res.getDimensionPixelSize(if (bool) smallContainerMargin else containerMargin)
                            } ?: return@before
                            mTmpConstraintSet.connectSet(linear.id, 6, 0, 6, 0)
                            mTmpConstraintSet.connectSet(linear.id, 7, guideLine, 6, sideSize)
                            mTmpConstraintSet.connectSet(
                                linear.id, 3, firstTile.id, 4, containerSize
                            )
                        } else {
                            mTmpConstraintSet.connectSet(linear.id, 6, guideLine, 7, sideSize)
                            mTmpConstraintSet.connectSet(linear.id, 7, 0, 7, 0)
                            mTmpConstraintSet.connectSet(linear.id, 3, 0, 3, 0)
                        }
                        result = null
                    }
                }
            }
        }
    }

    fun getMediaData(): Any? {
        return MediaPlayerDataUtils(hostClassLoader!!).getMediaDataStatus()
    }

    fun Any.connectSet(startId: Int, startSide: Int, endId: Int, endSide: Int, margin: Int) {
        asResolver().firstMethod {
            name = "connect"
            parameterCount = 5
        }.invoke(startId, startSide, endId, endSide, margin)
    }

    fun Any.constrainHeightSet(viewId: Int, height: Int) {
        asResolver().firstMethod {
            name = "constrainHeight"
            parameterCount = 2
        }.invoke(viewId, height)
    }

    fun Any.setVisibilitySet(viewId: Int, visibility: Int) {
        asResolver().firstMethod {
            name = "setVisibility"
            parameterCount = 2
        }.invoke(viewId, visibility)
    }

    @Obfuscate
    object ForceEnableMediaToggleButton : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusQsMediaPanelView
            "com.oplus.systemui.qs.media.OplusQsMediaPanelView".toClass().resolve().apply {
                firstMethod { name = "bindMediaData" }.hook {
                    after {
                        firstArg().get() ?: firstField { name = "mMediaOutputBtn" }.of(instance)
                            .get<ImageButton>()?.setMediaOutputBtn()
                    }
                }
            }
            //Source OplusQsMediaOutputDialog
            "com.oplus.systemui.qs.media.OplusQsMediaOutputDialog".toClass().resolve().apply {
                firstMethod { name = "bindMediaView" }.hook {
                    after {
                        firstArg().get() ?: firstField { name = "mMediaOutputBtn" }.of(instance)
                            .get<ImageButton>()?.setMediaOutputBtn()
                    }
                }
            }
        }
    }

    private fun ImageButton.setMediaOutputBtn() {
        isVisible = true
        isEnabled = true
        setOnClickListener {
            val clazz = "com.android.systemui.media.dialog.MediaOutputDialogFactory".toClass()
            val mMediaOutputDialogFactory = DependencyUtils(hostClassLoader!!).getDependency(clazz)
            mMediaOutputDialogFactory?.asResolver()
                ?.firstMethod { name = "create"; parameterCount = 3 }
                ?.invoke("", true, null)
        }
    }
}