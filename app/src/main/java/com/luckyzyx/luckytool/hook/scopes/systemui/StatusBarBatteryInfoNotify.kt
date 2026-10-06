package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.SystemProperties
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.graphics.drawable.IconCompat
import com.highcapable.betterandroid.ui.component.notification.factory.Notification
import com.highcapable.betterandroid.ui.component.notification.factory.NotificationChannel
import com.highcapable.betterandroid.ui.component.notification.type.NotificationImportance
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.hook.utils.IChargerUtils
import com.luckyzyx.luckytool.hook.utils.sysui.BatteryControllerUtils
import com.luckyzyx.luckytool.utils.DeviceUtils.calcLocalHealth
import com.luckyzyx.luckytool.utils.DevicesConfigUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.NotifyUtils
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.formatDecimals
import com.luckyzyx.luckytool.utils.getBooleanProperty
import com.luckyzyx.luckytool.utils.getIntProperty
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getStringProperty
import com.luckyzyx.luckytool.utils.safeOf
import org.lsposed.lsparanoid.Obfuscate
import java.io.StringReader
import java.util.Properties
import kotlin.math.abs

@Obfuscate
object StatusBarBatteryInfoNotify : YukiBaseHooker() {
    //battery
    private var status: String = ""
    private var statusValue: Int = 0
    private var plugged: String = ""
    private var level: Int = 0
    private var level_sub: Int = 0
    private var temperature: Double = 0.0
    private var temperature_noplug: Double = 0.0
    private var voltage: Double = 0.0
    private var voltage2: Double = 0.0
    private var electricCurrent: Int = 0
    private var wirelessVol: Double = 0.0
    private var wirelessCur: Int = 0

    private var isCharging: Boolean = false
    private var isWireless: Boolean = false

    //oplus battery
    private var chargerVoltage: Int = 0
    private var chargerTechnology: Int = 0
    private var chargeWattage: Int = 0
    private var ppsMode: Int = 0
    private var chargerWattageCpa: Int = -1
    private var usbFastChgType: Int = 0

    private var isSeriesDual = false
    private var isParallelDual = false
    private var chargerType = ""

    private var oplusCharger: Any? = null
    private lateinit var chargeInfo: Properties
    private var isMTKPlatform: Boolean? = null

    private var curHealth: Int? = null

    //params
    private lateinit var displayMode: String
    private var showChargerInfo: Boolean = false
    private var showUpdateTime: Boolean = false
    private var showVolMode: String = "0"
    private var isHealth: Boolean = false
    private var isPositive: Boolean = false
    private var isSimple: Boolean = false
    private var fontSize: Int = 11

    private const val NOTIFY_ID = 112233
    private const val CHANNEL_ID = "luckytool_notify"
    private const val CHANNEL_NAME = "LuckyTool"

    private val channel = NotificationChannel(
        channelId = CHANNEL_ID, importance = NotificationImportance.DEFAULT
    ) {
        name = CHANNEL_NAME
        sound(null)
    }

    override fun onHook() {
        displayMode = preferences(ModulePrefs).getString("battery_information_display_mode", "0")
        dataChannel.wait<String>("battery_information_display_mode") {
            displayMode = it
            initSend(hostApplication)
        }
        showChargerInfo =
            preferences(ModulePrefs).getBoolean("battery_information_show_charge_info", false)
        dataChannel.wait<Boolean>("battery_information_show_charge_info") {
            showChargerInfo = it
            initSend(hostApplication)
        }
        showUpdateTime =
            preferences(ModulePrefs).getBoolean("battery_information_show_update_time", false)
        dataChannel.wait<Boolean>("battery_information_show_update_time") {
            showUpdateTime = it
            initSend(hostApplication)
        }
        showVolMode =
            preferences(ModulePrefs).getString("battery_information_voltage_display_mode", "0")
        dataChannel.wait<String>("battery_information_voltage_display_mode") {
            showVolMode = it
            initSend(hostApplication)
        }
        isHealth =
            preferences(ModulePrefs).getBoolean("battery_information_show_battery_health", false)
        dataChannel.wait<Boolean>("battery_information_show_battery_health") {
            isHealth = it
            initSend(hostApplication)
        }
        isPositive = preferences(ModulePrefs).getBoolean(
            "battery_information_always_show_positive_current", false
        )
        dataChannel.wait<Boolean>("battery_information_always_show_positive_current") {
            isPositive = it
            initSend(hostApplication)
        }
        isSimple =
            preferences(ModulePrefs).getBoolean("battery_information_show_simple_mode", false)
        dataChannel.wait<Boolean>("battery_information_show_simple_mode") {
            isSimple = it
            initSend(hostApplication)
        }
        fontSize = preferences(ModulePrefs).getInt("battery_information_custom_font_size", 11)
        dataChannel.wait<Int>("battery_information_custom_font_size") {
            fontSize = it
            initSend(hostApplication)
        }

        registerAppLifecycle {
            onCreate {
                injectModuleResources()
            }
            //BatteryService
            registerReceiver(Intent.ACTION_BATTERY_CHANGED) { context: Context, _: Intent ->
                context.injectModuleResources()

                initInfo(context)
                initSend(context)
            }
            //OplusBatteryService
            registerReceiver("android.intent.action.ADDITIONAL_BATTERY_CHANGED") { context: Context, intent: Intent ->
                context.injectModuleResources()
                chargerTechnology = intent.getIntExtra("chargertechnology", 0)
                chargeWattage = intent.getIntExtra("chargewattage", 0)
                ppsMode = intent.getIntExtra("pps_chg_mode", 0)
                chargerWattageCpa = intent.getIntExtra("cpa_charge_wattage", 0)

                initInfo(context)
                initSend(context)
            }
        }
    }

    private fun initInfo(context: Context) {
        try {
            chargeInfo = getChargeInfo()
            statusValue = chargeInfo.getIntProperty("battery_status")
            status = when (statusValue) {
                2 -> safeOf("Charging") { context.getString(R.string.battery_status_charging) }
                3 -> safeOf("Discharging") { context.getString(R.string.battery_status_discharging) }
                4 -> safeOf("Not Charging") { context.getString(R.string.battery_status_not_charging) }
                5 -> safeOf("Full") { context.getString(R.string.battery_status_full) }
                else -> safeOf("Unknown") { context.getString(R.string.battery_status_unknown) }
            }
            isCharging = statusValue == 2 || statusValue == 5
            plugged = when (getPlugType(chargeInfo)) {
                0 -> "Battery"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                BatteryManager.BATTERY_PLUGGED_DOCK -> "DOCK"
                else -> "Null"
            }
            isWireless = plugged == "WIRELESS"
            level = chargeInfo.getIntProperty("battery_capacity")
            level_sub = chargeInfo.getIntProperty("sub_soc")
            temperature = chargeInfo.getIntProperty("battery_temp") / 10.0
            temperature_noplug = chargeInfo.getIntProperty("battery_temp_not_plug") / 10.0
            isSeriesDual = DevicesConfigUtils.isSeriesDualBattery == true
            isParallelDual = DevicesConfigUtils.isParallelDualBattery == true
            chargerType = chargeInfo.getStringProperty("charger_type", "")
//            ppsMode = chargeInfo.getIntProperty("battery_ppschg_ing", 0)
            usbFastChgType = chargeInfo.getIntProperty("usb_fast_chg_type", 0)
            voltage = chargeInfo.getIntProperty("battery_voltage_now") / 1000.0
            voltage2 = if (isSeriesDual) chargeInfo.getIntProperty("battery_voltage_min") / 1000.0
            else if (isParallelDual) chargeInfo.getIntProperty("sub_voltage") / 1000.0
            else 0.0
            chargerVoltage = chargeInfo.getIntProperty("battery_charge_now")
            if (isMTKPlatform == null) isMTKPlatform = SystemProperties.get(
                "ro.board.platform", "unknown"
            ).lowercase().startsWith("mt")
            if (isMTKPlatform == false) {
                voltage /= 1000.0
                voltage2 /= 1000.0
            }
            electricCurrent = chargeInfo.getIntProperty("battery_current_now")
            if (isWireless) {
                val isAirSVOOC = DevicesConfigUtils.isAirSVOOCSupport
                val mBatteryReverse = chargeInfo.getIntProperty("wireless_enable_tx")
                wirelessCur = chargeInfo.getIntProperty("wireless_current_now")
                wirelessVol = if (isAirSVOOC == true) {
                    if (isWireless || mBatteryReverse == 2 || mBatteryReverse == 1) {
                        chargeInfo.getIntProperty("wireless_voltage_now") / 1000.0
                    } else chargerVoltage * 1.0
                } else chargeInfo.getIntProperty("wireless_voltage_now") / 1000.0
            }
        } catch (e: Exception) {
            YLog.error("StatusBarBatteryInfoNotify -> InitInfo", e)
        }
    }

    private fun initSend(context: Context?) {
        //hostApplication 可能先于首次电池广播可用，未初始化数据前不发送
        if (context == null || !::chargeInfo.isInitialized) return
        when (displayMode) {
            "1" -> sendNotification(
                context, showChargerInfo && isCharging, showUpdateTime, isSimple, showVolMode
            )

            "2" -> if (isCharging) sendNotification(
                context, showChargerInfo, showUpdateTime, isSimple, showVolMode
            ) else clearNotification(context)

            else -> clearNotification(context)
        }
    }

    @SuppressLint("DiscouragedApi", "MissingPermission", "RestrictedApi")
    private fun sendNotification(
        context: Context,
        isCharging: Boolean,
        isUpdateTime: Boolean,
        isSimple: Boolean,
        showVolMode: String
    ) {
        //com.oplusos.systemui.keyguard.charginganim.ChargingTypeConstants C14.1-
        val technology = BatteryControllerUtils(hostClassLoader!!).let {
            if (getOSVersionCode >= 34) it.getTechnologyName(
                chargerTechnology, usbFastChgType, ppsMode, isWireless
            )
            else it.getTechnologyNameOld(chargerTechnology, ppsMode, isWireless)
        }
//        YLog.debug("tech: $chargerTechnology | usbFastChgType: $usbFastChgType | pps: $ppsMode -> $technology")

        val powerCalc = if (isSeriesDual || isParallelDual) {
            (voltage + voltage2) * electricCurrent / 1000.0
        } else voltage * electricCurrent / 1000.0

        val batteryIcon = safeOf(android.R.drawable.sym_def_app_icon) {
            if (isCharging) R.drawable.ic_round_battery_charging_full_24
            else when (level) {
                100 -> R.drawable.round_battery_full_24
                in 80..99 -> R.drawable.round_battery_6_bar_24
                in 65..79 -> R.drawable.round_battery_5_bar_24
                in 50..64 -> R.drawable.round_battery_4_bar_24
                in 35..49 -> R.drawable.round_battery_3_bar_24
                in 25..34 -> R.drawable.round_battery_2_bar_24
                in 10..24 -> R.drawable.round_battery_1_bar_24
                in 0..9 -> R.drawable.round_battery_0_bar_24
                else -> R.drawable.round_battery_unknown_24
            }
        }
        val tempStr = safeOf("Temp") { context.getString(R.string.battery_temperature) }
        val volStr = safeOf("Vol") { context.getString(R.string.battery_voltage) }
        val curStr = safeOf("Cur") { context.getString(R.string.battery_electric_current) }
        val hltStr = safeOf("Cur") { context.getString(R.string.battery_health) }
        val typeStr = safeOf("Type") { context.getString(R.string.battery_charger_type) }
        val pwrStr = safeOf("Pwr") { context.getString(R.string.battery_power) }
        val techStr = safeOf("Tech") { context.getString(R.string.battery_technology) }
        val updateTimeStr = safeOf("UpdateTime") { context.getString(R.string.battery_update_time) }

        val power = abs(powerCalc).formatDecimals(2) + "W"
        val wattage = if (chargeWattage == 0) {
            if (chargerWattageCpa == 0) "" else "${chargerWattageCpa}W"
        } else "${chargeWattage}W"

        val finalTemp = if (temperature < 0) "NaN" else "$temperature℃"
        val tem = if (isSimple) finalTemp else "${tempStr}: $finalTemp"
        val formatVol = voltage.formatDecimals(2)
        val formatVol2 = voltage2.formatDecimals(2)
        val vol = when (showVolMode) {
            "1" -> if (isSimple) "${formatVol}V"
            else "${volStr}: ${formatVol}V"

            "2" -> if (isSeriesDual || isParallelDual) {
                if (isSimple) "${formatVol}V ${formatVol2}V"
                else "${volStr}: ${formatVol}V ${formatVol2}V"
            } else {
                if (isSimple) "${formatVol}V"
                else "${volStr}: ${formatVol}V"
            }

            else -> ""
        }
        val formatCur = if (abs(electricCurrent) >= 1000) {
            (electricCurrent / 1000.0).formatDecimals(1).let {
                if (isPositive) it.replace("-", "") else it
            } + "A"
        } else if (isPositive) abs(electricCurrent).toString() + "mA"
        else "${electricCurrent}mA"
        val cur = if (isSimple) formatCur
        else "${curStr}: $formatCur"
        val health = if (isHealth) {
            if (curHealth == null) curHealth = context.calcLocalHealth()
            val value = "${curHealth}%"
            if (isSimple) value else "${hltStr}: $value"
        } else ""

        val sp = if (isSimple) "$level%" else "$status: $level%"

        val ct = if (isSimple) {
            if (isWireless) plugged
            else plugged + if (chargerType.isNotBlank()) " $chargerType" else ""
        } else {
            if (isWireless) "${typeStr}: $plugged"
            else "${typeStr}: $plugged" + if (chargerType.isNotBlank()) " $chargerType" else ""
        }
        val pwr = if (isSimple) power
        else "${pwrStr}: $power"
        val tech = if (isSimple) "$technology $wattage"
        else "${techStr}: $technology $wattage"

        val formatWireVol = wirelessVol.formatDecimals(2)
        val wireVol = if (isSimple) "${formatWireVol}V"
        else "${volStr}: ${formatWireVol}V"
        val formatwireCur = if (abs(wirelessCur) >= 1000) {
            (wirelessCur / 1000.0).formatDecimals(1).let {
                if (isPositive) it.replace("-", "") else it
            } + "A"
        } else if (isPositive) abs(wirelessCur).toString() + "mA"
        else "${wirelessCur}mA"
        val wireCur = if (isSimple) formatwireCur
        else "${curStr}: $formatwireCur"
        val wirePwrCalc = (wirelessVol * wirelessCur / 1000.0).formatDecimals(2)
        val wirePwr = if (isSimple) "${wirePwrCalc}W"
        else "${pwrStr}: ${wirePwrCalc}W"

        val batteryInfo = if (isSimple) formatStringInfoSpace(tem, vol, cur, power, health)
        else formatStringInfoSpace(tem, vol, cur, health)
        val chargeInfo = if (isCharging) {
            if (isSimple) {
                if (isWireless) formatStringInfoSpace(wireVol, wireCur, wirePwr, "\n", sp, tech)
                else formatStringInfoSpace(sp, ct, tech)
            } else {
                if (statusValue == 5) formatStringInfoSpace(sp, tech)
                else {
                    if (isWireless) formatStringInfoSpace(
                        sp, wireVol, wireCur, wirePwr, "\n", ct, tech
                    ) else formatStringInfoSpace(sp, ct, pwr, "\n", tech)
                }
            }
        } else ""
        val updateTime = if (isUpdateTime) {
            if (isSimple) formatDate("HH:mm:ss")
            else "${updateTimeStr}: " + formatDate("HH:mm:ss")
        } else ""

        val remoteViews =
            RemoteViews(BuildConfig.APPLICATION_ID, R.layout.layout_battery_notify_view)
        val info = formatStringInfoLine(batteryInfo, chargeInfo, updateTime)
        remoteViews.setTextViewText(R.id.battery_notify_tv, info)
        remoteViews.setTextViewTextSize(
            R.id.battery_notify_tv, TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat()
        )
        val notify = Notification(context = context, channel = channel) {
            //不能传 context 创建图标：IconCompat.createWithResource(context, id) 构造时会调用
            //Resources.getResourceName(id) 解析资源名，依赖 SystemUI 进程内的模块资源注入，
            //且 0x7f 段 ID 与 SystemUI 自身资源表易撞车；解析失败直接抛
            //IllegalArgumentException("Icon resource cannot be found")，notify 根本不会执行。
            //传 null Resources 只记录包名+ID、不做任何解析，加载时任何进程都经已安装模块 APK 获取
            smallIcon(IconCompat.createWithResource(null, BuildConfig.APPLICATION_ID, batteryIcon))
            contentTitle = "BatteryInfo"
            contentText = info
            customContentView = remoteViews
            customBigContentView = remoteViews
            autoCancel(false)
            ongoing(true)
        }
        NotifyUtils.sendNotification(context, NOTIFY_ID, notify.instance)
    }

    private fun clearNotification(context: Context) {
        NotifyUtils.clearNotification(context, NOTIFY_ID)
    }

    private fun getChargeInfo(): Properties {
        return try {
            val queryChargeInfo = IChargerUtils(hostClassLoader!!).let {
                if (oplusCharger == null) oplusCharger = it.getInstance()
                it.queryChargeInfo(oplusCharger)
            } ?: ""
//            YLog.d("getChargeInfo -> queryChargeInfo : $queryChargeInfo")
            Properties().apply {
                if (queryChargeInfo.isNotBlank()) load(StringReader(queryChargeInfo))
            }
        } catch (e: Exception) {
            YLog.error("StatusBarBatteryInfoNotify -> getChargeInfo", e)
            Properties()
        }
    }

    private fun getPlugType(properties: Properties): Int {
        if (properties.getBooleanProperty("chargerAcOnline")) {
            return 1
        }
        if (properties.getBooleanProperty("chargerUSBOnline")) {
            return 2
        }
        if (properties.getBooleanProperty("chargerWirelessOnline")) {
            return 4
        }
        return 0
    }

    private fun formatStringInfoSpace(vararg info: String) = formatStringInfo(info.toList(), " ")
    private fun formatStringInfoLine(vararg info: String) = formatStringInfo(info.toList(), "\n")
    private fun formatStringInfo(infos: List<String>, text: String): String {
        var finalText = ""
        infos.forEachIndexed { index, it ->
            if (it != "\n") {
                if (it.isBlank()) return@forEachIndexed
                if (index > 0 && infos[index - 1] != "\n") finalText += text
            }
            finalText += it
        }
        return finalText
    }
}