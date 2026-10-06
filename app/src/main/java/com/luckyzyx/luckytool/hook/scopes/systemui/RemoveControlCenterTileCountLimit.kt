package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

@Obfuscate
class RemoveControlCenterTileCountLimit(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode < 26) {
            loadHooker(RemoveReceiveItemLimitV12(dexKitBridge))
        } else {
            if (osCode >= 34) loadHooker(RemoveLimitNumberHint)
            else loadHooker(RemoveLimitNumberHintV14)
            loadHooker(RemoveReceiveItemLimit(dexKitBridge))
        }
    }

    @Obfuscate
    object RemoveLimitNumberHint : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusSeparateQSCustomizer
            "com.oplus.systemui.plugins.qs.customize.OplusSeparateQSCustomizer".toClass().resolve()
                .apply {
                    (firstMethodOrNull { name = "handleCheckLimitCount" }
                        ?: firstMethod { name { it.contains("handleCheckLimitCount") } }).hook {
                        intercept(false)
                    }
                    firstMethod { name = "updateLimitCountTip" }.hook {
                        intercept()
                    }
                }
        }
    }

    @Obfuscate
    class RemoveReceiveItemLimit(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusQSCustomizer
            val clazz = VariousClass(
                "com.oplusos.systemui.qs.customize.OplusQSCustomizer", //C12 C13
                "com.oplus.systemui.qs.customize.OplusQSCustomizer" //C14
            ).toClassOrNull() ?: return

            dexKitBridge.findClass {
                matcher {
                    className(clazz.name, StringMatchType.Contains)
                    addMethod { name("canReceiveItem");returnType(classOf<Boolean>()) }
                    addMethod { name("checkHighLightTileSize");returnType(classOf<Boolean>()) }
                }
            }.apply {
                checkDataList("RemoveReceiveItemLimit clazz", onlyOne = false)
                forEachIndexed { _, classData ->
                    classData.name.toClass().resolve().apply {
                        firstMethod { name = "canReceiveItem" }.hook {
                            intercept(true)
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object RemoveLimitNumberHintV14 : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusQSCustomizer
            VariousClass(
                "com.oplusos.systemui.qs.customize.OplusQSCustomizer", //C12 C13
                "com.oplus.systemui.qs.customize.OplusQSCustomizer" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "handleCheckMinCount" }.hook {
                    intercept(false)
                }
                firstMethod { name = "showMinCountHint" }.hook {
                    intercept()
                }
            }
        }
    }

    @Obfuscate
    class RemoveReceiveItemLimitV12(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusQSCustomizer
            val clazz = VariousClass(
                "com.oplusos.systemui.qs.customize.OplusQSCustomizer", //C12 C13
                "com.oplus.systemui.qs.customize.OplusQSCustomizer" //C14
            ).toClassOrNull() ?: return

            dexKitBridge.findClass {
                matcher {
                    className(clazz.name, StringMatchType.Contains)
                    addMethod { name("canReceiveItem");returnType(classOf<Boolean>()) }
                    addMethod { name("onMinCountDrag");paramTypes(classOf<Boolean>()) }
                }
            }.apply {
                checkDataList("RemoveReceiveItemLimit clazz", onlyOne = false)
                forEachIndexed { _, classData ->
                    classData.name.toClass().resolve().apply {
                        firstMethod { name = "canReceiveItem" }.hook {
                            intercept(true)
                        }
                        firstMethod { name = "onMinCountDrag" }.hook {
                            intercept()
                        }
                    }
                }
            }
        }
    }
}