package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveAdsAtDownloadPageBottom(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val recommendConfig = VariousClass(
            "com.heytap.browser.downloads.entity.RecommendConfig",  //v40.8.24.1
            "com.heytap.browser.download.ui.downloadlist.model.RecommendConfig"  //v40.8.25.1
        ).toClass()

        //Source AppRecommendManager -> LinearLayout setVisibility 0/8 500L
        dexKitBridge.findMethod {
            matcher {
                paramCount(0)
                returnType(Void.TYPE)
                usingNumbers(0, 8, 500L)
                usingFields {
                    add {
                        type(recommendConfig)
                        addWriteMethod {
                            paramTypes(recommendConfig)
                            returnType(Void.TYPE)
                        }
                    }
                    add {
                        type(classOf<LinearLayout>())
                        addWriteMethod {
                            paramCount(0)
                            returnType(Void.TYPE)
                        }
                    }
                }
            }
        }.apply {
            checkDataList("RemoveAdsAtDownloadPageBottom")
            single().className.toClass().resolve().apply {
                firstMethod {
                    name = single().methodName
                    emptyParameters()
                    returnType(Void.TYPE)
                }.hook {
                    before {
                        firstField { type(LinearLayout::class) }.of(instance).get<View>()
                            ?.isVisible = false
                        result = null
                    }
                }
            }
        }
    }
}