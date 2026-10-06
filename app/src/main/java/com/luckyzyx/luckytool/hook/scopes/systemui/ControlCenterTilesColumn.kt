package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import android.view.ViewGroup
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.MediaPlayerDataUtils
import com.luckyzyx.luckytool.hook.utils.sysui.QSFeatureOptionUtils
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getScreenOrientation
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControlCenterTiles : YukiBaseHooker() {
    var callback: ((key: String, value: String) -> Unit)? = null

    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("control_center_tile_enable", false)
        if (!isEnable) return
        if (SDK >= A13) loadHooker(ControlCenterTilesLayout)
        else loadHooker(ControlCenterTilesLayoutC12)
    }

    @Obfuscate
    object ControlCenterTilesLayout : YukiBaseHooker() {
        override fun onHook() {
            val osCode = getOSVersionCode

            val columnUnexpandedVerticalC13 =
                preferences(ModulePrefs).getInt("tile_unexpanded_columns_vertical_c13", 5)
            val rowExpandedVerticalC13 =
                preferences(ModulePrefs).getInt("tile_expanded_rows_vertical_c13", 3)
            val columnExpandedVerticalC13 =
                preferences(ModulePrefs).getInt("tile_expanded_columns_vertical_c13", 4)
            val columnHorizontal = preferences(ModulePrefs).getInt("tile_columns_horizontal_c13", 4)
            //媒体播放器模式
            var mediaMode = preferences(ModulePrefs).getString("set_media_player_display_mode", "0")
            //自动扩展
            var autoExpandTile = osCode >= 30 && preferences(ModulePrefs).getBoolean(
                "auto_expand_tile_rows_horizontal", false
            )
            dataChannel.wait<Boolean>("auto_expand_tile_rows_horizontal") { autoExpandTile = it }

            callback = { key: String, value: String ->
                when (key) {
                    "set_media_player_display_mode" -> mediaMode = value
                }
            }

            //Source QuickQSPanel
            "com.android.systemui.qs.QuickQSPanel".toClass().resolve().apply {
                firstMethod { name = "getNumQuickTiles" }.hook {
                    intercept(columnUnexpandedVerticalC13)
                }
            }

            //Source TileLayout
            "com.android.systemui.qs.TileLayout".toClass().resolve().apply {
                firstMethod { name = "updateMaxRows" }.hook {
                    before {
                        getScreenOrientation(instance<ViewGroup>()) {
                            val mRows = firstField { name = "mRows" }.of(instance).get<Int>()
                            val newRows = if (it) rowExpandedVerticalC13
                            else if (autoExpandTile &&
                                QSFeatureOptionUtils(hostClassLoader!!).isSupportVolumeSeekBar().not()
                            ) when (mediaMode) {
                                "2" -> 2
                                "3" -> {
                                    if (MediaPlayerDataUtils(hostClassLoader!!).getMediaDataStatus() == null) 2
                                    else return@getScreenOrientation
                                }

                                else -> return@getScreenOrientation
                            }
                            else return@getScreenOrientation
                            firstField { name = "mRows" }.of(instance).set(newRows)
                            result = mRows != newRows
                        }
                    }
                }

                (firstMethodOrNull { name = "updateColumns" }
                    ?: firstMethod { name = "setMaxColumns" }).hook {
                    before {
                        instance<ViewGroup>().apply {
                            getScreenOrientation(this) {
                                val mColumns =
                                    firstField { name = "mColumns" }.of(instance).get<Int>()
                                val newColumns = if (it) columnExpandedVerticalC13
                                else columnHorizontal
                                firstField { name = "mColumns" }.of(instance).set(newColumns)
                                result = mColumns != newColumns
                            }
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object ControlCenterTilesLayoutC12 : YukiBaseHooker() {
        override fun onHook() {
            val columnUnexpandedVertical =
                preferences(ModulePrefs).getInt("tile_unexpanded_columns_vertical", 6)
            val columnUnexpandedHorizontal =
                preferences(ModulePrefs).getInt("tile_unexpanded_columns_horizontal", 6)
            val columnExpandedVertical =
                preferences(ModulePrefs).getInt("tile_expanded_columns_vertical", 4)
            val columnExpandedHorizontal =
                preferences(ModulePrefs).getInt("tile_expanded_columns_horizontal", 6)

            //Source QuickQSPanel
            "com.android.systemui.qs.QuickQSPanel".toClass().resolve().apply {
                firstMethod { name = "getNumQuickTiles" }.hook {
                    before {
                        getScreenOrientation(instance<View>()) {
                            result = if (it) columnUnexpandedVertical
                            else columnUnexpandedHorizontal
                        }
                    }
                }
            }

            //Source TileLayout
            "com.android.systemui.qs.TileLayout".toClass().resolve().apply {
                firstMethod { name = "updateColumns" }.hook {
                    before {
                        instance<ViewGroup>().apply {
                            getScreenOrientation(this) {
                                val mColumns =
                                    firstField { name = "mColumns" }.of(instance).get<Int>()
                                val newColumns = if (it) columnExpandedVertical
                                else columnExpandedHorizontal
                                firstField { name = "mColumns" }.of(instance).set(newColumns)
                                result = mColumns != newColumns
                            }
                        }
                    }
                }
            }
        }
    }
}

