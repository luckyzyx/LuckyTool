package com.luckyzyx.luckytool.hook.scopes.battery

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.DeviceUtils.calcLocalHealth
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.filterNumber
import com.luckyzyx.luckytool.utils.safeOf
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisplayModuleCalculatesBatteryHealthData : YukiBaseHooker() {
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    override fun onHook() {
        val customCalcData =
            preferences(ModulePrefs).getString("customize_battery_health_data_percentage", "None")
        val showCalcData =
            preferences(ModulePrefs).getBoolean("display_module_calculates_battery_health_data", false)

        //Source BatteryHealthDataPreference
        "com.oplus.powermanager.fuelgaue.BatteryHealthDataPreference".toClass().resolve().apply {
            firstMethod { parameters(View::class) }.hook {
                after {
                    val view = firstArg().get<View>() ?: return@after
                    val context = view.context
                    context.injectModuleResources()
                    val contentView = view.findViewById<TextView>(
                        view.resources.getIdentifier(
                            "max_capacity_content",
                            "id", this@DisplayModuleCalculatesBatteryHealthData.packageName
                        )
                    ) ?: return@after
                    val dataView = firstField { type = TextView::class }.of(instance).get<TextView>()
                        ?: return@after
                    if (customCalcData.filterNumber.isNotEmpty()) {
                        dataView.text = "${customCalcData.filterNumber}%"
                    }
                    if (showCalcData) {
                        val health = context.calcLocalHealth(true)
                        val tips = safeOf(" Calc") {
                            context.getString(R.string.display_module_calculates_battery_health_data_tips)
                        }
                        contentView.apply {
                            layoutParams?.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                            gravity = Gravity.START
                            if(text.lines().size == 1) {
                                text = "$text\n\nLuckyTool$tips"
                            }
                        }
                        dataView.apply {
                            layoutParams?.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                            gravity = Gravity.END
                            if(text.lines().size == 1) {
                                text = "$text\n\n${health}%"
                            }
                        }
                    }
                }
            }
        }
    }
}