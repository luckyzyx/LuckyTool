package com.luckyzyx.luckytool.hook.scopes.market

import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveMarketDetailPageAppRecommend(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //com.heytap.cdo.client.detail.app.AppDetailActivity
        //com.heytap.cdo.client.detail.app.base.ScrollContentView

        "com.heytap.cdo.client.detail.app.AppDetailActivity".toClass().resolve().apply {
            firstMethod { parameters(Intent::class) }.hook {
                after {
                    val resourceDto = result<Any>()
                    if (resourceDto == null) {
                        YLog.debug("resourceDto is null")
                        return@after
                    }

                    //只遍历 public 字段：本类声明的字段优先，本类没有时再往父类/接口找；
                    //optional(true) 保证一个都找不到时返回空列表而不是抛异常
                    "com.heytap.cdo.common.domain.dto.ResourceDto".toClass().resolve()
                        .optional(silent = true)
                        .field {
                            modifiers(Modifiers.PUBLIC)
                            superclass(interfaces = true)
                        }
                        .forEachIndexed { index, field ->
                            val ff = field.of(resourceDto).get()
                            YLog.debug("$index | ${field.self.name} -> $ff")
                        }

                }
            }
        }
    }
}