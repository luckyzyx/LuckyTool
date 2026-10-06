package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.FlowUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSeparateControlCenterButton : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 37) loadHooker(SeparateControlCenterButton)
        else loadHooker(SeparateControlCenterButtonV15)

        if (preferences(ModulePrefs).getBoolean("remove_control_center_settings_button", false)) {
            loadHooker(RemoveSeparateControlCenterSettingsButton)
        }
    }

    @Obfuscate
    object RemoveSeparateControlCenterSettingsButton : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusSeparateSettingsEntranceInteractor
            "com.oplus.systemui.plugins.qs.quickentrance.domain.interactor.OplusSeparateSettingsEntranceInteractor".toClass()
                .resolve().apply {
                    firstMethod {
                        name = "isHideSettingsEntrance"
                        emptyParameters()
                        returnType = "kotlinx.coroutines.flow.StateFlow"
                    }.hook {
                        before {
                            result = FlowUtils(hostClassLoader!!).let {
                                it.asStateFlow(it.MutableStateFlow(true))
                            }
                        }
                    }
                }
        }
    }

    @Obfuscate
    object SeparateControlCenterButton : YukiBaseHooker() {
        override fun onHook() {
            val osCode = getOSVersionCode

            val hideEdit =
                preferences(ModulePrefs).getBoolean("remove_control_center_edit_button", false)
            val hideMore =
                preferences(ModulePrefs).getBoolean("remove_control_center_more_button", false)

            //Source OplusQSQuickEntranceComponent
            "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent".toClass()
                .resolve().apply {
                    firstMethod { name = "onInit" }.hook {
                        after {
                            if (hideEdit) {
                                firstField { name = "editBtn" }.of(instance).get<View>()
                                    ?.isVisible = false
                                firstField { name = "editBtnRedDot" }.of(instance).get<View>()
                                    ?.isVisible = false
                            }
                            if (osCode in 34..39 && hideMore) {
                                firstField { name = "moreBtn" }.of(instance).get<View>()
                                    ?.isVisible = false
                                firstField { name = "moreBtnRedDot" }.of(instance).get<View>()
                                    ?.isVisible = false
                            }
                        }
                    }
                    firstMethod { name = "updateResource" }.hook {
                        after {
                            if (hideEdit) {
                                firstField { name = "editBtn" }.of(instance).get<View>()
                                    ?.isVisible = false
                                firstField { name = "editBtnRedDot" }.of(instance).get<View>()
                                    ?.isVisible = false
                            }
                            if (osCode in 34..39 && hideMore) {
                                firstField { name = "moreBtn" }.of(instance).get<View>()
                                    ?.isVisible = false
                                firstField { name = "moreBtnRedDot" }.of(instance).get<View>()
                                    ?.isVisible = false
                            }
                        }
                    }
                }
        }
    }

    @Obfuscate
    object SeparateControlCenterButtonV15 : YukiBaseHooker() {
        override fun onHook() {
            val hideEdit =
                preferences(ModulePrefs).getBoolean("remove_control_center_edit_button", false)
            val hideMore =
                preferences(ModulePrefs).getBoolean("remove_control_center_more_button", false)

            //Source OplusQSBottomViewController
            "com.oplus.systemui.plugins.qs.bottom.OplusQSBottomViewController".toClass().resolve()
                .apply {
                    firstMethod { name = "init" }.hook {
                        after {
                            if (hideEdit) firstField { name = "editBtn" }.of(instance).get<View>()
                                ?.isVisible = false
                            if (hideMore) firstField { name = "moreBtn" }.of(instance).get<View>()
                                ?.isVisible = false
                        }
                    }
                }
        }
    }
}