package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.allViews
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.isSubclassOf
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.XLog
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.instance
import com.luckyzyx.luckytool.hook.utils.IChargerUtils
import com.luckyzyx.luckytool.hook.utils.sysui.BatteryControllerUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.createTextDrawable
import com.luckyzyx.luckytool.utils.getIntProperty
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import java.io.StringReader
import java.util.Properties

@Suppress("MayBeConstant")
@Obfuscate
object LockScreenChargingComponent : Hooker {
    override fun onHook() {
        when (getOSVersionCode) {
            in 34..Int.MAX_VALUE -> loadHooker(ChargingComponent)
            in 30..33 -> loadHooker(ChargingComponentC14)
            in 26..29 -> loadHooker(ChargingComponentC13)
            else -> loadHooker(ChargingComponentC12)
        }
    }

    val ChargeLevelAndLogoView = "com.oplus.charge.view.ChargeLevelAndLogoView"
    val FrameChargeLevelAndLogoView = "com.oplus.charge.view.FrameChargeLevelAndLogoView"
    val ChargeUtil = "com.oplus.charge.util.ChargeUtil"

    val OplusChargeAnimImpl = "com.oplus.charge.viewmodel.OplusChargeAnimImpl"
    val OplusChargeAnimFlavorOneImpl =
        "com.oplus.systemui.keyguard.charginganim.siphonanim.viewmodel.OplusChargeAnimFlavorOneImpl"
    val ChargeLevelAndLogoFlavorOneView =
        "com.oplus.systemui.keyguard.charginganim.siphonanim.view.ChargeLevelAndLogoFlavorOneView"

    @Obfuscate
    @Suppress("LocalVariableName")
    private object ChargingComponent : Hooker {

        private var oplusCharger: Any? = null

        override fun onHook() {
            var userTypeface =
                prefs(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var textLogo =
                prefs(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") { textLogo = it }
            var showRealTech =
                prefs(ModulePrefs).getBoolean("lock_screen_show_real_charging_technology", false)
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
            }
            var drawTechnology =
                prefs(ModulePrefs).getBoolean("replace_charging_technology_drawing_style", false)
            dataChannel.wait<Boolean>("replace_charging_technology_drawing_style") {
                drawTechnology = it
            }

            val ChargeUtilCLazz = ChargeUtil.toClass()
            val hasShowWattage = ChargeUtilCLazz.resolve().optional(true).firstMethodOrNull {
                name = "getShowWattage"
            } != null
            val hasTechnologyStrForFrameCharge = ChargeUtilCLazz.resolve().optional(true).firstMethodOrNull {
                name = "getTechnologyStrForFrameCharge"
            } != null
            val hasShowWattageForFrameCharge = ChargeUtilCLazz.resolve().optional(true).firstMethodOrNull {
                name = "getShowWattageForFrameCharge"
            } != null

            val ChargeLevelAndLogoView = ChargeLevelAndLogoView.toClass()
            val hasUpdateChargeTechImage = ChargeLevelAndLogoView.resolve().optional(true).firstMethodOrNull {
                name = "updateChargeTechImage"
            } != null

            //Source ChargingLevelAndLogoView
            ChargeLevelAndLogoView.resolve().optional(true).apply {
                firstMethod { name = "showCNChargeTechLogo" }.hook {
                    before {
                        when (textLogo) {
                            "1" -> resultTrue()
                            "2" -> resultFalse()
                            else -> return@before
                        }
                    }
                }
                if (hasUpdateChargeTechImage) {
                    firstMethod { name = "updateChargeTechImage" }.hook {
                        before {
                            if (!drawTechnology) return@before
                            val viewGroup = instance<ViewGroup>()
                            val chargeTechLogo =
                                firstField { name = "chargeTechLogo" }.of(instance).get<ImageView>()
                                    ?: return@before
                            val isWirelessCharge =
                                firstField { name = "isWirelessCharge" }.of(instance).get<Boolean>()
                                    ?: false
                            val chargerTechnology =
                                firstField { name = "chargerTechnology" }.of(instance).get<Int>()
                                    ?: -1
                            val chargeInfo = getChargeInfo()
                            val usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
                            val ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
                            val text = BatteryControllerUtils(appClassLoader).getTechnologyName(
                                chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                            )
                            chargeTechLogo.setImageDrawable(
                                createTextDrawable(viewGroup.context, text)
                            )
                            resultNull()
                        }
                    }
                }
                firstMethod { name = "updateChargeAnim" }.hook {
                    after {
                        val oplusChargeInfo = args.firstOrNull {
                            it?.javaClass?.simpleName == "OplusChargeInfo"
                        } ?: return@after

                        if (showWattage || drawTechnology) {
                            firstField { name = "techWattageLayout" }.of(instance)
                                .get<View>()?.isVisible = true
                        }

                        if (showWattage && !hasShowWattage) {
                            val chargeWattageView =
                                firstField { name = "chargeWattage" }.of(instance).get<TextView>()
                                    ?.apply {
                                        isVisible = true
                                        gravity = Gravity.CENTER
                                        setPadding(paddingLeft, paddingTop, paddingRight, 3)
                                    }
                            val cpaWattage = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargeWattageOrigin"
                            }.invoke<Int>()
                            val wattage = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargeWattage"
                            }.invoke<String>()?.toIntOrNull()
                            chargeWattageView?.text = when (wattage) {
                                0 if cpaWattage == 0 -> ""
                                0 if cpaWattage != 0 -> "${cpaWattage}W"
                                else -> "${wattage}W"
                            }

//                            YLog.debug("ChargeLevelAndLogoView chargeWattage -> ${chargeWattageView?.isVisible}")
//                            YLog.debug("ChargeLevelAndLogoView $cpaWattage | $wattage")
                        }

                        if (drawTechnology && !hasUpdateChargeTechImage) {
                            val viewGroup = instance<ViewGroup>()
                            val chargeTechLogo =
                                firstField { name = "chargeTechLogo" }.of(instance).get<ImageView>()
                            val isWirelessCharge =
                                firstField { name = "isWirelessCharge" }.of(instance).get<Boolean>()
                                    ?: false
                            val chargerTechnology =
                                firstField { name = "chargerTechnology" }.of(instance).get<Int>()
                                    ?: -1
                            val chargeInfo = getChargeInfo()
                            val usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
                            val ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
                            val text = BatteryControllerUtils(appClassLoader).getTechnologyName(
                                chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                            )
                            chargeTechLogo?.isVisible = true
                            chargeTechLogo?.setImageDrawable(
                                createTextDrawable(viewGroup.context, text)
                            )
                        }
                    }
                }
                firstMethod { name = "updateAllIconAndBg" }.hook {
                    before {
                        if (showWattage) firstField { name = "isShowWattage" }.of(instance)
                            .set(true)
                    }
                }
            }

            //Source FrameChargeLevelAndLogoView
            FrameChargeLevelAndLogoView.toClass().resolve().optional(true).apply {
                firstMethodOrNull { name = "shouldShowTextLogo" }?.hook {
                    before {
                        when (textLogo) {
                            "1" -> resultTrue()
                            "2" -> resultFalse()
                            else -> return@before
                        }
                    }
                } ?: run {
                    firstMethod { name = "updateTextLogo" }.hook {
                        before {
                            when (textLogo) {
                                "1" -> firstField { name = "currentLocale" }.of(instance)
                                    .set("zh-CN")

                                "2" -> firstField { name = "currentLocale" }.of(instance).set("")
                                else -> return@before
                            }
                        }
                    }
                }
                firstMethod { name = "updateChargeAnim" }.hook {
                    after {
                        val oplusChargeInfo = args().last().any() ?: return@after

                        if (showRealTech || showWattage) {
                            firstField { name = "chargeWattageLayout" }.of(instance)
                                .get<View>()?.isVisible = true
                        }

                        if (showRealTech && !hasTechnologyStrForFrameCharge) {
                            val textLogoView =
                                firstField { name = "textLogo" }.of(instance).get<TextView>()
                            val isWirelessCharge = oplusChargeInfo.asResolver().firstMethod {
                                name = "isWirelessCharge"
                            }.invoke<Boolean>() ?: false
                            val chargerTechnology = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargerTechnology"
                            }.invoke<Int>() ?: 0
                            val chargeInfo = getChargeInfo()
                            val usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
                            val ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
                            textLogoView?.isVisible = true
                            textLogoView?.text =
                                BatteryControllerUtils(appClassLoader).getTechnologyName(
                                    chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                                )
                        }

                        if (showWattage && !hasShowWattageForFrameCharge) {
                            val chargeWattageView =
                                firstField { name = "chargeWattage" }.of(instance).get<TextView>()
                            val cpaWattage = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargeWattageOrigin"
                            }.invoke<Int>() ?: 0
                            val wattage = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargeWattage"
                            }.invoke<String>()?.toIntOrNull()
                            chargeWattageView?.isVisible = true
                            chargeWattageView?.text = when (wattage) {
                                0 if cpaWattage == 0 -> ""
                                0 if true -> "${cpaWattage}W"
                                else -> "${wattage}W"
                            }
//                            YLog.debug("FrameChargeLevelAndLogoView chargeWattage -> ${chargeWattageView?.isVisible}")
//                            YLog.debug("FrameChargeLevelAndLogoView $cpaWattage | $wattage")

                        }
                    }
                }
            }

            //Source OplusChargeAnimImpl -> ChargeUtil
            ChargeUtilCLazz.resolve().optional(true).apply {
                firstMethod {
//                    name = "getChargeLevelTypeFace"
//                    name = "getSansTypeFace"
                    parameters(Context::class)
                    returnType = Typeface::class
                }.hook {
                    after {
                        if (!userTypeface) return@after
                        result = Typeface.DEFAULT_BOLD
                    }
                }
                if (hasShowWattage) {
                    firstMethod { name = "getShowWattage"; parameterCount = 3 }.hook {
                        before {
                            if (!showWattage) return@before
                            val cpaWattage = args().first().int()
                            val wattage = args(1).string().toIntOrNull() ?: return@before
//                        val wattage = args(1).string()
//                        val isWireless = args().last().boolean()
//                        YLog.debug("ChargeUtil getShowWattage -> $origin | $wattage | $isWireless")
                            result = when (wattage) {
                                0 if cpaWattage == 0 -> ""
                                0 if true -> "${cpaWattage}W"
                                else -> "${wattage}W"
                            }
                        }
                    }
                }
                if (hasShowWattageForFrameCharge) {
                    firstMethodOrNull {
                        name = "getShowWattageForFrameCharge"
                        parameterCount = 3
                    }?.hook {
                        before {
                            if (!showWattage) return@before
                            val cpaWattage = args().first().int()
                            val wattage = args(1).string().toIntOrNull() ?: return@before
                            result = when (wattage) {
                                0 if cpaWattage == 0 -> ""
                                0 if true -> "${cpaWattage}W"
                                else -> "${wattage}W"
                            }
                        }
                    }
                }
                if (hasTechnologyStrForFrameCharge) {
                    firstMethod { name = "getTechnologyStrForFrameCharge" }.hook {
                        before {
                            if (!showRealTech) return@before
                            val oplusChargeInfo = args().last().any() ?: return@before
                            val isWirelessCharge = oplusChargeInfo.asResolver().firstMethod {
                                name = "isWirelessCharge"
                            }.invoke<Boolean>() ?: false
                            val chargerTechnology = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargerTechnology"
                            }.invoke<Int>() ?: 0

                            val chargeInfo = getChargeInfo()
                            val usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
                            val ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
                            result = BatteryControllerUtils(appClassLoader).getTechnologyName(
                                chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                            )
                        }
                    }
                }
            }
        }

        private fun getChargeInfo(): Properties {
            return try {
                val queryChargeInfo = IChargerUtils(appClassLoader).let {
                    if (oplusCharger == null) oplusCharger = it.getInstance()
                    it.queryChargeInfo(oplusCharger)
                }
//        LogUtils.d("getChargeInfo", "queryChargeInfo", queryChargeInfo.toString(), true)
                Properties().apply {
                    if (queryChargeInfo.isNullOrBlank().not()) load(StringReader(queryChargeInfo))
                }
            } catch (e: Exception) {
                XLog.error("StatusBarBatteryInfoNotify -> getChargeInfo", e)
                Properties()
            }
        }

    }

    @Obfuscate
    private object ChargingComponentC14 : Hooker {
        override fun onHook() {
            var userTypeface =
                prefs(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
//            var warpCharge =
//                prefs(ModulePrefs).getString("set_lock_screen_warp_charging_style", "0")
//            dataChannel.wait<String>("set_lock_screen_warp_charging_style") { warpCharge = it }
            var textLogo =
                prefs(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") { textLogo = it }
            var showRealTech =
                prefs(ModulePrefs).getBoolean("lock_screen_show_real_charging_technology", false)
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
            }

            //Source ChargingLevelAndLogoView
            ChargeLevelAndLogoView.toClass().resolve().apply {
                firstMethod { parameters(Typeface::class) }.hook {
                    after {
                        if (!userTypeface) return@after
                        instance<LinearLayout>().allViews.forEach {
                            if (it::class isSubclassOf TextView::class) {
                                (it as TextView).typeface = Typeface.DEFAULT_BOLD
                            }
                        }
                    }
                }
                firstMethod { name = "showTextLogo" }.hook {
                    before {
                        when (textLogo) {
                            "1" -> resultTrue()
                            "2" -> resultFalse()
                            else -> return@before
                        }
                    }
                }
            }

            //Source OplusChargeAnimImpl -> ChargeUtil
            ChargeUtil.toClass().resolve().apply {
                firstMethod { name = "showWattage" }.hook {
                    before {
                        if (!showWattage) return@before
                        val chargeInfoObserver = args().first().any() ?: return@before
                        val getChargeWattage = chargeInfoObserver.asResolver().firstMethod {
                            name = "getChargeWattage"; emptyParameters()
                        }.invoke<String>()?.toIntOrNull() ?: return@before
                        if (getChargeWattage != 0) resultTrue()
                    }
                }
                firstMethod { name = "showTechnology" }.hook {
                    if (showRealTech) replaceToTrue()
                }
                firstMethodOrNull { name = "getTechnologyStr" }?.hook {
                    before {
                        if (!showRealTech) return@before
                        val chargeInfoObserver = args().last().any() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(appClassLoader).getTechnologyNameOld(
                            technology, ppsMode, ismIsWirelessCharge
                        )
                    }
                }
            }

            //Source OplusChargeAnimImpl
            OplusChargeAnimImpl.toClass().resolve().apply {
                firstMethodOrNull { name = "getTechnologyStr" }?.hook {
                    before {
                        if (!showRealTech) return@before
                        val chargeInfoObserver = args().last().any() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(appClassLoader).getTechnologyNameOld(
                            technology, ppsMode, ismIsWirelessCharge
                        )
                    }
                }
            }

            //Source OplusChargeAnimFlavorOneImpl
            OplusChargeAnimFlavorOneImpl.toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "getTechnologyStr" }.hook {
                    before {
                        if (!showRealTech) return@before
                        val chargeInfoObserver = args().first().any() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(appClassLoader).getTechnologyNameOld(
                            technology, ppsMode, ismIsWirelessCharge
                        )
                    }
                }
            }

            //Source ChargeLevelAndLogoFlavorOneView
            ChargeLevelAndLogoFlavorOneView.toClassOrNull()?.resolve()?.apply {
                firstMethod { parameters(Typeface::class) }.hook {
                    after {
                        if (!userTypeface) return@after
                        instance<LinearLayout>().allViews.forEach {
                            if (it::class isSubclassOf TextView::class) {
                                (it as TextView).typeface = Typeface.DEFAULT_BOLD
                            }
                        }
                    }
                }
                firstMethod { name = "showTextLogo" }.hook {
                    before {
                        when (textLogo) {
                            "1" -> resultTrue()
                            "2" -> resultFalse()
                            else -> return@before
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    private object ChargingComponentC13 : Hooker {
        override fun onHook() {
            var userTypeface =
                prefs(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var warpCharge =
                prefs(ModulePrefs).getString("set_lock_screen_warp_charging_style", "0")
            dataChannel.wait<String>("set_lock_screen_warp_charging_style") {
                warpCharge = it
            }
            var textLogo =
                prefs(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") {
                textLogo = it
            }
            var showRealTech =
                prefs(ModulePrefs).getBoolean("lock_screen_show_real_charging_technology", false)
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
            }

            //Source ChargingLevelAndLogoView
            "com.oplusos.systemui.keyguard.charginganim.siphonanim.ChargingLevelAndLogoView".toClass()
                .resolve().apply {
                    firstMethod { name = "updatePowerFormat" }.hook {
                        after {
                            if (!userTypeface) return@after
                            instance<LinearLayout>().allViews.forEach {
                                if (it::class isSubclassOf TextView::class) {
                                    (it as TextView).typeface = Typeface.DEFAULT_BOLD
                                }
                            }
                        }
                    }
                    firstMethod { name = "showTextLogo" }.hook {
                        before {
                            if (warpCharge != "2") return@before
                            when (textLogo) {
                                "1" -> resultTrue()
                                "2" -> resultFalse()
                                else -> return@before
                            }
                        }
                    }
                    firstMethod { name = "updateLogoResource" }.hook {
                        after {
                            if (warpCharge != "2" || !showRealTech) return@after
                            val context = instance<View>().context
                            val showText =
                                firstMethod { name = "showTextLogo" }.of(instance).invoke<Boolean>()
                                    ?: return@after
                            val mTextLogo =
                                firstField { name = "mTextLogo" }.of(instance).get<TextView>()
                                    ?: return@after
                            if (showText) mTextLogo.text =
                                BatteryControllerUtils(appClassLoader).let {
                                    val ins = it.getInstance(context) ?: return@after
                                    val tech = it.getChargerTechnology(ins)
                                    val pps = it.getPPSMode(ins)
                                    val isWireless = it.isWirelessCharging(ins)
                                    it.getTechnologyNameOld(tech, pps, isWireless)
                                }
                        }
                    }
                }

            //Source ChargingAnimationImpl
            "com.oplusos.systemui.keyguard.charginganim.ChargingAnimationImpl".toClass().resolve()
                .apply {
                    firstMethod { name = "isMaxWattageMatchs" }.hook {
                        before {
                            if (warpCharge != "2") return@before
                            val mChargerWattage =
                                firstField { name = "mChargerWattage" }.of(instance).get<Int>()
                            if (showWattage && (mChargerWattage != 0)) resultTrue()
                        }
                    }
                }

            //Source ChargingLevelAndLogoViewForFlavorOneVfx
            "com.oplusos.systemui.keyguard.charginganim.siphonanim.flavorone.ChargingLevelAndLogoViewForFlavorOneVfx".toClassOrNull()
                ?.resolve()?.apply {
                    firstMethod { name = "setTypeface" }.hook {
                        after {
                            if (!userTypeface) return@after
                            instance<LinearLayout>().allViews.forEach {
                                if (it::class isSubclassOf TextView::class) {
                                    (it as TextView).typeface = Typeface.DEFAULT_BOLD
                                }
                            }
                        }
                    }
                    firstMethod { name = "showTextLogo" }.hook {
                        before {
                            if (warpCharge != "2") return@before
                            when (textLogo) {
                                "1" -> resultTrue()
                                "2" -> resultFalse()
                                else -> return@before
                            }
                        }
                    }
                    firstMethod { name = "updateLogoResource" }.hook {
                        after {
                            if (warpCharge != "2" || !showRealTech) return@after
                            val context = instance<View>().context
                            val showText =
                                firstMethod { name = "showTextLogo" }.of(instance).invoke<Boolean>()
                                    ?: return@after
                            val mTextLogo =
                                firstField { name = "mTextLogo" }.of(instance).get<TextView>()
                                    ?: return@after
                            if (showText) mTextLogo.text =
                                BatteryControllerUtils(appClassLoader).let {
                                    val ins = it.getInstance(context) ?: return@after
                                    val tech = it.getChargerTechnology(ins)
                                    val pps = it.getPPSMode(ins)
                                    val isWireless = it.isWirelessCharging(ins)
                                    it.getTechnologyNameOld(tech, pps, isWireless)
                                }
                        }
                    }
                }
        }
    }

    @Obfuscate
    private object ChargingComponentC12 : Hooker {
        override fun onHook() {
            var userTypeface =
                prefs(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var textLogo =
                prefs(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") {
                textLogo = it
            }
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
            }

            //Source ChargingLevelAndLogoView
            "com.oplusos.systemui.keyguard.charginganim.siphonanim.ChargingLevelAndLogoView".toClass()
                .resolve().apply {
                    firstMethod { name = "updatePowerFormat" }.hook {
                        after {
                            if (!userTypeface) return@after
                            instance<LinearLayout>().allViews.forEach {
                                if (it::class isSubclassOf TextView::class) {
                                    (it as TextView).typeface = Typeface.DEFAULT_BOLD
                                }
                            }
                        }
                    }
                    firstMethodOrNull { name = "isLocaleZhCN" }?.hook {
                        before {
                            when (textLogo) {
                                "1" -> resultTrue()
                                "2" -> resultFalse()
                                else -> return@before
                            }
                        }
                    }
                }

            //Source ChargingAnimationImpl
            "com.oplusos.systemui.keyguard.charginganim.ChargingAnimationImpl".toClass().resolve()
                .apply {
                    firstMethod { name = "isSupportShowWattage" }.hook {
                        if (showWattage) replaceToTrue()
                    }
                }
        }
    }
}