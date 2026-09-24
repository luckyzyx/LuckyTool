package com.luckyzyx.luckytool.hook.scopes.keyguardclock

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class KeyGuardcLockRedMode(val dexKitBridge: DexKitBridge) : Hooker {
    override fun onHook() {
        var redMode = prefs(ModulePrefs).getString("lock_screen_clock_redone_mode", "0")
        dataChannel.wait<String>("lock_screen_clock_redone_mode") { redMode = it }

        if (packageName == "com.oplus.keyguard.personality.clocks") {
            // ColorOS 17 将经典/数字时钟移入 personality APK，并提供独立红色 1 开关。
            listOf(
                "com.oplus.keyguard.clock.base.domain.model.ClockViewRootModel",
                "com.oplus.keyguard.clock.digital.domain.model.ClockViewRootModel"
            ).forEach { className ->
                className.toClass(appClassLoader).resolve().firstMethod {
                    name = "setOnePlusRedOneSwitch"
                    parameters(Boolean::class)
                }.hook {
                    before {
                        when (redMode) {
                            "1" -> args(0).setTrue()
                            "2" -> args(0).setFalse()
                        }
                    }
                }
            }
            return
        }

        // 新版 base APK 只提供基础设施，渲染 Hook 由 personality 包加载。
        if ("com.oplus.keyguard.clock.base.BaseClockImpl".toClassOrNull(appClassLoader) == null &&
            android.os.Build.VERSION.SDK_INT >= 37
        ) return
        if (redMode == "0") return

        //Source CustomizedTextView -> BrandUtils
        dexKitBridge.findClass {
            matcher {
                addFieldForType(classOf<Boolean>())
                usingStrings("ro.oplus.image.system_ext.brand", "ro.oplus.image.system_ext.area")
            }
        }.apply {
            checkDataList("KeyGuardcLockRedMode Clazz")
            findField {
                matcher {
                    type(classOf<Boolean>())
                    addReadMethod {
                        paramCount(1)
                        returnType(Void.TYPE)
                        usingStrings("1")
                        usingNumbers(1)
                    }
                }
            }.apply {
                checkDataList("KeyGuardcLockRedMode Field")
                single().className.toClass(initialize = true).resolve().apply {
                    firstField {
                        name = single().fieldName
                        type = Boolean::class
                    }.set(
                        when (redMode) {
                            "1" -> true
                            "2" -> false
                            else -> return
                        }
                    )
                }
            }
        }
    }
}
