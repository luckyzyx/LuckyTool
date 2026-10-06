package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookAIUnit : YukiBaseHooker() {
    override fun onHook() {
        //Source Router
        "com.oplus.aiunit.router.Router".toClass().resolve().apply {
            firstMethod { name = "getDefaultConfiguration" }.hook {
                after {
                    val list = result<List<Any>>()?.takeIf { it.isNotEmpty() } ?: return@after
                    list.forEachIndexed { _, it ->
//                            YLog.info("$index -> ${it.toString()}")
                        val unitName =
                            it.asResolver().firstMethod { name = "getUnitName" }.invoke<String>()
                        when (unitName) {
                            "cloud_aigc_segmentation" -> {
                                it.asResolver().firstMethod { name = "setDisabled" }.invoke(false)
                                it.asResolver().firstMethod { name = "setWhiteModels" }.invoke("")
                            }

                            "cloud_aigc_sdinpainting" -> {
                                it.asResolver().firstMethod { name = "setDisabled" }.invoke(false)
                                it.asResolver().firstMethod { name = "setWhiteModels" }.invoke("")
                            }
                        }
                    }
                }
            }
        }
    }
}