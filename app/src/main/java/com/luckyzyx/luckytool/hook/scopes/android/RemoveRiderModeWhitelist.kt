package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveRiderModeWhitelist : YukiBaseHooker() {

    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_rider_mode_whitelist", false)
        if (!isEnable) return

        //Source OplusRiderModeService
        //把 mRiderPackages 换成 contains 恒为 true 的集合：任何 App 进入前台都会命中白名单并激活高能户外模式；
        //clear/add 沿用 HashSet 原实现，RomUpdateHelper 更新 riderPackage.xml 的解析流程不受影响。
        "com.android.server.OplusRiderModeService".toClass().resolve().apply {
            fun patchService(service: Any?) {
                if (service == null) return
                firstField { name = "mRiderPackages" }.of(service).set(
                    object : HashSet<String>() {
                        override fun contains(element: String): Boolean = true
                    },
                )
            }

            firstMethod { name = "getInstance" }.hook {
                after {
                    patchService(result<Any>())
                }
            }

            //getInstance 在 hook 生效前已执行时单例已存在，直接从静态字段取到再补丁
            patchService(firstField { name = "sOplusRiderModeService" }.get())
        }

        //Source OplusFeatureRiderMode
        //亮度增强只对 provider 下发的支持列表生效，绕过 mApps.contains 白名单让任意 App 都能走增强分支。
        //显示后处理管线在部分机型可能被裁剪，这里不存在就静默跳过，不影响上面的激活路径。
        "com.android.server.display.feature.postprocess.OplusFeatureRiderMode".toClassOrNull()
            ?.resolve()?.apply {
                firstMethod {
                    name = "onAppEnter"
                    parameters(String::class)
                }.hook {
                    intercept {
                        if (!isEnable) {
                            return@intercept callOriginal()
                        }
                        val enterApp = firstArg().get<String>()
                        if (enterApp.isNullOrEmpty()) {
                            return@intercept callOriginal()
                        }
                        val riderEnabled = firstField { name = "mIsRiderModeEnabled" }
                            .of(instance).get<Boolean>() ?: false
                        if (!riderEnabled) {
                            return@intercept callOriginal()
                        }
                        firstField { name = "mRateType" }.of(instance).set(34)
                        firstField { name = "mIsInRiderModeApp" }.of(instance).set(true)
                        firstMethod { name = "updateBrightness" }.of(instance).invoke()
                    }
                }
            }
    }
}
