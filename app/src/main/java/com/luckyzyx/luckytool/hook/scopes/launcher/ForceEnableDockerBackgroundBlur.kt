package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.interceptAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class ForceEnableDockerBackgroundBlur(private val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        private const val SCREEN_UTILS = "com.android.common.util.ScreenUtils"
        private const val HOTSEAT = "com.android.launcher3.OplusHotseat"
        private const val BLUR_PROPERTIES =
            "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties"
        private const val BLUR_PROPERTIES_COMPANION =
            "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties\$Companion"

        /** C17 混淆后的 ScreenUtils 里 setCurrentDisplayRotation 的日志字符串，DexKit 用它定位类 */
        private const val SCREEN_UTILS_ROTATION_LOG =
            "ScreenUtils#setCurrentDisplayRotation rotation="
    }

    override fun onHook() {
        val blur = preferences(ModulePrefs)
            .getBoolean("force_enable_docker_background_blur", false)
        if (!blur) return

        // 在宿主生成背景的调用栈内把模糊分支条件强制为 true
        val forcing = ThreadLocal<Boolean>()
        HOTSEAT.toClass().resolve()
            .firstMethod { name = "setDockerBackground"; emptyParameters() }.hook {
                before { forcing.set(true) }
                after { forcing.remove() }
            }
        // 旧版本：闸门一是 OplusBlurProperties 的实例方法，闸门二是 ScreenUtils.hasLargeDisplayFeatures()
        val screenUtils = SCREEN_UTILS.toClassOrNull()?.resolve()
        if (screenUtils != null) {
            BLUR_PROPERTIES.toClass().resolve()
                .firstMethod { name = "isSupportNewBlur" }.hook {
                    before { if (forcing.get() == true) result = true }
                }
            screenUtils.firstMethod { name = "hasLargeDisplayFeatures"; emptyParameters() }.hook {
                before { if (forcing.get() == true) result = true }
            }
            return
        }

        // C17：闸门一是 OplusBlurProperties$Companion 的静态 isSupportNewBlur(Context)，
        // 闸门二是混淆后 ScreenUtils 的无参 boolean 方法（setDockerBackground 里的 ho9.d()）
        BLUR_PROPERTIES_COMPANION.toClass().resolve()
            .firstMethod { name = "isSupportNewBlur" }.hook {
                before { if (forcing.get() == true) result = true }
            }
        // C17 的 large display 判定在混淆后的 ScreenUtils 里；类名与方法名都被混淆，
        // 按日志字符串用 DexKit 定位类，再按结构挂所有无参 boolean 方法
        runCatching {
            val screenUtilsClass = dexKitBridge.findMethod {
                matcher { usingStrings(SCREEN_UTILS_ROTATION_LOG) }
            }.checkDataList("ForceEnableDockerBackgroundBlur ScreenUtils").single().className
            screenUtilsClass.toClass().resolve().apply {
                method { emptyParameters(); returnType = Boolean::class }.interceptAll {
                    if (forcing.get() == true) true else proceed()
                }
            }
        }.onFailure { YLog.warn("dock 背景：强制模糊分支闸门失败 $it") }
    }
}
