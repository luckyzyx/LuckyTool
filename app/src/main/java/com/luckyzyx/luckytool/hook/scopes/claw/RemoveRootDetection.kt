package com.luckyzyx.luckytool.hook.scopes.claw

import android.content.Context
import android.os.SystemProperties
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveRootDetection(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        var isEnable = preferences(ModulePrefs).getBoolean("remove_root_detection", false)
        dataChannel.wait<Boolean>("remove_root_detection") { isEnable = it }

        //各版本 Root 检测共用的三条 boot 属性
        val bootProps = arrayOf(
            "ro.boot.flash.locked",
            "ro.boot.vbmeta.device_state",
            "ro.boot.verifiedbootstate"
        )

        //Source Recruitment Welcome z3.i(Context)Z
        //Root 检测: anim.v.c() || flash.locked==0 || vbmeta==unlocked || verifiedbootstate==orange
        //返回值语义是「设备已失陷(compromised)」: true 会被 BetaDeviceAccessPolicy 记成 Compromised，
        //再由 AccessViewModel 映射成 AccessFailReason.Rooted 拦截功能，故放行时必须返回 false
        val modern = dexKitBridge.findClass {
            matcher {
                usingStrings(*bootProps)
            }
        }.findMethod {
            matcher {
                paramTypes(classOf<Context>())
                returnType(classOf<Boolean>())
                usingStrings(*bootProps)
            }
        }.singleOrNull()

        //Source RecruitmentUtilsKt DeviceScenario 检测（旧版结构，17.0.75 起已不存在）
        //Root 检测: e7.b().d() || ro.boot.flash.locked==0 || vbmeta==unlocked || verifiedbootstate==orange
        val legacy = dexKitBridge.findClass {
            matcher {
                usingStrings(*bootProps, "root_inspect", "device_eligibility")
            }
        }.findMethod {
            matcher {
                paramTypes(classOf<Context>())
                usingStrings(*bootProps)
                usingStrings("root_inspect", "device_eligibility")
            }
        }.singleOrNull()

        //按版本只有一条 query 会命中，空集本身是正常分支，所以两条都不加 checkDataList；
        //两条都落空说明宿主又改了检测结构，必须留痕，否则会静默不生效
        if (modern == null && legacy == null) YLog.error("RemoveRootDetection -> modern/legacy 均未命中")

        when {
            modern != null -> modern.className.toClass().resolve().firstMethodOrNull {
                name = modern.methodName
                parameters(Context::class)
                returnType = Boolean::class
            }?.intercept {
                if (!isEnable) return@intercept proceed()
                false
            }

            legacy != null -> legacy.className.toClass().resolve().firstMethodOrNull {
                name = legacy.methodName
                parameters(Context::class)
            }?.hook {
                after {
                    if (!isEnable) return@after
                    val scenario = result ?: return@after
                    val enumConstants = scenario.javaClass.enumConstants ?: return@after
                    //Root 设备 ordinal = 4
                    if (enumConstants.getOrNull(4) !== scenario) return@after
                    val oplusRom = SystemProperties.get("ro.build.version.oplusrom", "")
                    val major = Regex(
                        "^(?:ColorOS\\s*|V)?(\\d{1,2})(?:[._]|$)",
                        RegexOption.IGNORE_CASE
                    ).find(oplusRom)?.groupValues?.get(1)?.toIntOrNull()
                    //正常设备 ordinal = 2，系统版本低 ordinal = 3（保留版本门禁，仅绕过 Root 判定）
                    result = enumConstants.getOrNull(if (major != null && major < 16) 3 else 2)
                }
            }

            else -> YLog.error("RemoveRootDetection -> findMethod isNullOrEmpty")
        }
    }
}
