package com.luckyzyx.luckytool.hook.scopes.claw

import android.content.Context
import android.os.SystemProperties
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveRootDetection(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        var isEnable = preferences(ModulePrefs).getBoolean("remove_root_detection", false)
        dataChannel.wait<Boolean>("remove_root_detection") { isEnable = it }

        //Source RecruitmentUtilsKt DeviceScenario 检测
        //Root 检测: e7.b().d() || ro.boot.flash.locked==0 || vbmeta==unlocked || verifiedbootstate==orange
        dexKitBridge.findClass {
            matcher {
                addMethod {
                    paramTypes(classOf<Context>())
                    usingStrings(
                        "ro.boot.flash.locked",
                        "ro.boot.vbmeta.device_state",
                        "ro.boot.verifiedbootstate"
                    )
                }
                usingStrings("root_inspect", "device_eligibility")
            }
        }.apply {
            checkDataList("RemoveRootDetection Clazz")
            findMethod {
                matcher {
                    paramTypes(classOf<Context>())
                    usingStrings(
                        "ro.boot.flash.locked",
                        "ro.boot.vbmeta.device_state",
                        "ro.boot.verifiedbootstate"
                    )
                }
            }.apply {
                checkDataList("RemoveRootDetection Method")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        parameters(Context::class)
                    }.hook {
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
                }
            }
        }
    }
}