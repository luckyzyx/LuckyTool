package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.content.Intent
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
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.hook.utils.IChargerUtils
import com.luckyzyx.luckytool.hook.utils.sysui.BatteryControllerUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.createTextDrawable
import com.luckyzyx.luckytool.utils.getIntProperty
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import java.io.StringReader
import java.lang.ref.WeakReference
import java.util.Properties

@Suppress("MayBeConstant")
@Obfuscate
object LockScreenChargingComponent : YukiBaseHooker() {
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
    private object ChargingComponent : YukiBaseHooker() {

        private var oplusCharger: Any? = null

        //OplusBatteryService 广播缓存: updateChargeAnim 有时拿不到 getChargeWattageOrigin, 用监听器兜底
        private var cachedCpaWattage = -1
        private var cachedWattage = -1

        //广播兜底刷新: 记录当前动画瓦数视图与最近一次实时数据, 广播更新时直接推送文本
        private var currentWattageView = WeakReference<TextView?>(null)
        private var lastCpaWattage = 0
        private var lastWattage: Int? = null

        override fun onHook() {
            var userTypeface =
                preferences(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var textLogo =
                preferences(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") { textLogo = it }
            var showRealTech = preferences(ModulePrefs).getBoolean(
                "lock_screen_show_real_charging_technology", false
            )
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage = preferences(ModulePrefs).getBoolean(
                "force_lock_screen_charging_show_wattage", false
            )
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
            }
            var drawTechnology = preferences(ModulePrefs).getBoolean(
                "replace_charging_technology_drawing_style", false
            )
            dataChannel.wait<Boolean>("replace_charging_technology_drawing_style") {
                drawTechnology = it
            }

            val ChargeUtilCLazz = ChargeUtil.toClass()
            val hasShowWattage = ChargeUtilCLazz.resolve().firstMethodOrNull {
                name = "getShowWattage"
            } != null
            val hasTechnologyStrForFrameCharge = ChargeUtilCLazz.resolve().firstMethodOrNull {
                name = "getTechnologyStrForFrameCharge"
            } != null
            val hasShowWattageForFrameCharge = ChargeUtilCLazz.resolve().firstMethodOrNull {
                name = "getShowWattageForFrameCharge"
            } != null

            val ChargeLevelAndLogoView = ChargeLevelAndLogoView.toClass()
            val hasUpdateChargeTechImage = ChargeLevelAndLogoView.resolve().firstMethodOrNull {
                name = "updateChargeTechImage"
            } != null

            //OplusBatteryService: 注册监听器缓存瓦数, 与 StatusBarBatteryInfoNotify 一致
            //该广播在 C15 C16 C17 框架中均携带 chargewattage/cpa_charge_wattage
            registerAppLifecycle {
                onCreate {
                    registerReceiver("android.intent.action.ADDITIONAL_BATTERY_CHANGED") { _: Context, intent: Intent ->
                        onWattageBroadcast(intent)
                    }
                }
            }

            //Source ChargingLevelAndLogoView
            ChargeLevelAndLogoView.resolve().apply {
                firstMethod { name = "showCNChargeTechLogo" }.hook {
                    before {
                        result = when (textLogo) {
                            "1" -> true
                            "2" -> false
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
                            val text = BatteryControllerUtils(hostClassLoader!!).getTechnologyName(
                                chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                            )
                            chargeTechLogo.setImageDrawable(
                                createTextDrawable(viewGroup.context, text)
                            )
                            result = null
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
                            }.invoke<Int>() ?: 0
                            val wattage = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargeWattage"
                            }.invoke<String>()?.toIntOrNull()

                            //缓存当前视图与实时数据, 供广播兜底直接推送
                            currentWattageView = WeakReference(chargeWattageView)
                            lastCpaWattage = cpaWattage
                            lastWattage = wattage
                            chargeWattageView?.text = resolveWattageText(cpaWattage, wattage)
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
                            val text = BatteryControllerUtils(hostClassLoader!!).getTechnologyName(
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
            FrameChargeLevelAndLogoView.toClass().resolve().apply {
                firstMethodOrNull { name = "shouldShowTextLogo" }?.hook {
                    before {
                        result = when (textLogo) {
                            "1" -> true
                            "2" -> false
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
                        val oplusChargeInfo = lastArg().get() ?: return@after

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
                                BatteryControllerUtils(hostClassLoader!!).getTechnologyName(
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
                            //缓存当前视图与实时数据, 供广播兜底直接推送
                            currentWattageView = WeakReference(chargeWattageView)
                            lastCpaWattage = cpaWattage
                            lastWattage = wattage
                            chargeWattageView?.isVisible = true
                            chargeWattageView?.text = resolveWattageText(cpaWattage, wattage)
                        }
                    }
                }
            }

            //Source OplusChargeAnimImpl -> ChargeUtil
            ChargeUtilCLazz.resolve().apply {
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
                            val cpaWattage = firstArg().get<Int>() ?: 0
                            val wattage = arg(1).get<String>()?.toIntOrNull()
                            result = resolveWattageText(cpaWattage, wattage)
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
                            val cpaWattage = firstArg().get<Int>() ?: 0
                            val wattage = arg(1).get<String>()?.toIntOrNull()
                            result = resolveWattageText(cpaWattage, wattage)
                        }
                    }
                }
                if (hasTechnologyStrForFrameCharge) {
                    firstMethod { name = "getTechnologyStrForFrameCharge" }.hook {
                        before {
                            if (!showRealTech) return@before
                            val oplusChargeInfo = lastArg().get() ?: return@before
                            val isWirelessCharge = oplusChargeInfo.asResolver().firstMethod {
                                name = "isWirelessCharge"
                            }.invoke<Boolean>() ?: false
                            val chargerTechnology = oplusChargeInfo.asResolver().firstMethod {
                                name = "getChargerTechnology"
                            }.invoke<Int>() ?: 0

                            val chargeInfo = getChargeInfo()
                            val usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
                            val ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
                            result = BatteryControllerUtils(hostClassLoader!!).getTechnologyName(
                                chargerTechnology, usbFastChgType, ppsMode, isWirelessCharge
                            )
                        }
                    }
                }
            }
        }

        /**
         * 广播瓦数更新: 刷新缓存并直接推送到当前动画视图
         * 系统不保证每次广播都会再次回调 updateChargeAnim, 只能自行刷新
         */
        private fun onWattageBroadcast(intent: Intent) {
            cachedCpaWattage = intent.getIntExtra("cpa_charge_wattage", -1)
            cachedWattage = intent.getIntExtra("chargewattage", -1)
//            YLog.debug(msg = "onWattageBroadcast -> cpa: $cachedCpaWattage | wattage: $cachedWattage")
            currentWattageView.get()?.takeIf { it.isAttachedToWindow }?.apply {
                val text = resolveWattageText(lastCpaWattage, lastWattage)
                if (text.isNotEmpty()) isVisible = true
                setText(text)
            }
        }

        /**
         * 瓦数文案: 优先取 updateChargeAnim 传入的实时值,
         * 为0/缺失时回退到 ADDITIONAL_BATTERY_CHANGED 广播缓存的 chargewattage/cpa_charge_wattage
         * @param cpaWattage Int getChargeWattageOrigin
         * @param wattage Int? getChargeWattage
         * @return String
         */
        private fun resolveWattageText(cpaWattage: Int, wattage: Int?): String {
            val watt =
                if (wattage != null && wattage > 0) wattage else cachedWattage.takeIf { it > 0 }
            if (watt != null) return "${watt}W"
            val cpa = if (cpaWattage > 0) cpaWattage else cachedCpaWattage.takeIf { it > 0 }
            return if (cpa != null) "${cpa}W" else ""
        }

        private fun getChargeInfo(): Properties {
            return try {
                val queryChargeInfo = IChargerUtils(hostClassLoader!!).let {
                    if (oplusCharger == null) oplusCharger = it.getInstance()
                    it.queryChargeInfo(oplusCharger)
                }
//        LogUtils.d("getChargeInfo", "queryChargeInfo", queryChargeInfo.toString(), true)
                Properties().apply {
                    if (queryChargeInfo.isNullOrBlank().not()) load(StringReader(queryChargeInfo))
                }
            } catch (e: Exception) {
                YLog.error("StatusBarBatteryInfoNotify -> getChargeInfo", e)
                Properties()
            }
        }

    }

    @Obfuscate
    private object ChargingComponentC14 : YukiBaseHooker() {
        override fun onHook() {
            var userTypeface =
                preferences(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
//            var warpCharge =
//                preferences(ModulePrefs).getString("set_lock_screen_warp_charging_style", "0")
//            dataChannel.wait<String>("set_lock_screen_warp_charging_style") { warpCharge = it }
            var textLogo =
                preferences(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") { textLogo = it }
            var showRealTech = preferences(ModulePrefs).getBoolean(
                "lock_screen_show_real_charging_technology", false
            )
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage = preferences(ModulePrefs).getBoolean(
                "force_lock_screen_charging_show_wattage", false
            )
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
                        result = when (textLogo) {
                            "1" -> true
                            "2" -> false
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
                        val chargeInfoObserver = firstArg().get() ?: return@before
                        val getChargeWattage = chargeInfoObserver.asResolver().firstMethod {
                            name = "getChargeWattage"; emptyParameters()
                        }.invoke<String>()?.toIntOrNull() ?: return@before
                        if (getChargeWattage != 0) result = true
                    }
                }
                firstMethod { name = "showTechnology" }.hook {
                    if (showRealTech) {
                        intercept(true)
                    }
                }
                firstMethodOrNull { name = "getTechnologyStr" }?.hook {
                    before {
                        if (!showRealTech) return@before
                        val chargeInfoObserver = lastArg().get() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(hostClassLoader!!).getTechnologyNameOld(
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
                        val chargeInfoObserver = lastArg().get() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(hostClassLoader!!).getTechnologyNameOld(
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
                        val chargeInfoObserver = firstArg().get() ?: return@before
                        val technology = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmChargerTechnology"
                        }.invoke<Int>() ?: return@before
                        val ppsMode = chargeInfoObserver.asResolver().firstMethod {
                            name = "getmPpsState"
                        }.invoke<Int>() ?: return@before
                        val ismIsWirelessCharge = chargeInfoObserver.asResolver().firstMethod {
                            name = "ismIsWirelessCharge"
                        }.invoke<Boolean>() ?: return@before
                        result = BatteryControllerUtils(hostClassLoader!!).getTechnologyNameOld(
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
                        result = when (textLogo) {
                            "1" -> true
                            "2" -> false
                            else -> return@before
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    private object ChargingComponentC13 : YukiBaseHooker() {
        override fun onHook() {
            var userTypeface =
                preferences(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var warpCharge =
                preferences(ModulePrefs).getString("set_lock_screen_warp_charging_style", "0")
            dataChannel.wait<String>("set_lock_screen_warp_charging_style") {
                warpCharge = it
            }
            var textLogo =
                preferences(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") {
                textLogo = it
            }
            var showRealTech = preferences(ModulePrefs).getBoolean(
                "lock_screen_show_real_charging_technology", false
            )
            dataChannel.wait<Boolean>("lock_screen_show_real_charging_technology") {
                showRealTech = it
            }
            var showWattage = preferences(ModulePrefs).getBoolean(
                "force_lock_screen_charging_show_wattage", false
            )
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
                            result = when (textLogo) {
                                "1" -> true
                                "2" -> false
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
                                BatteryControllerUtils(hostClassLoader!!).let {
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
                            if (showWattage && (mChargerWattage != 0)) result = true
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
                            result = when (textLogo) {
                                "1" -> true
                                "2" -> false
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
                                BatteryControllerUtils(hostClassLoader!!).let {
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
    private object ChargingComponentC12 : YukiBaseHooker() {
        override fun onHook() {
            var userTypeface =
                preferences(ModulePrefs).getBoolean("lock_screen_charging_use_user_typeface", false)
            dataChannel.wait<Boolean>("lock_screen_charging_use_user_typeface") {
                userTypeface = it
            }
            var textLogo =
                preferences(ModulePrefs).getString("set_lock_screen_charging_text_logo_style", "0")
            dataChannel.wait<String>("set_lock_screen_charging_text_logo_style") {
                textLogo = it
            }
            var showWattage = preferences(ModulePrefs).getBoolean(
                "force_lock_screen_charging_show_wattage", false
            )
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
                            result = when (textLogo) {
                                "1" -> true
                                "2" -> false
                                else -> return@before
                            }
                        }
                    }
                }

            //Source ChargingAnimationImpl
            "com.oplusos.systemui.keyguard.charginganim.ChargingAnimationImpl".toClass().resolve()
                .apply {
                    firstMethod { name = "isSupportShowWattage" }.hook {
                        if (showWattage) {
                            intercept(true)
                        }
                    }
                }
        }
    }
}