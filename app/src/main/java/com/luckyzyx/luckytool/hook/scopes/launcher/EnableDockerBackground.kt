package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableDockerBackground : YukiBaseHooker() {

    private const val SCREEN_UTILS = "com.android.common.util.ScreenUtils"
    private const val HOTSEAT = "com.android.launcher3.OplusHotseat"

    override fun onHook() {
        val background =
            preferences(ModulePrefs).getBoolean("enable_docker_background", false)

        // 旧版本由 ScreenUtils 的判定决定能否显示 dock 背景，打开背景开关就强制放开
        val screenUtils = SCREEN_UTILS.toClassOrNull()?.resolve()
        if (screenUtils != null) {
            if (background) {
                screenUtils.firstMethod { name = "isSupportDockerExpandScreen" }.hook {
                    intercept(true)
                }
            }
            return
        }

        // 新版把判定移出桌面，官方生成入口对直板机不可达；这里不再手动拼 drawable，
        // 只在宿主绘制前发现缺背景时调用官方 setDockerBackground 补建
        val hotseat = HOTSEAT.toClass().resolve()
        val drawableField = hotseat.firstField { name = "mDockerBackgroundDrawable" }
        // 装 dock 图标的容器声明在父类 CellLayout 上
        val containerField = hotseat.firstField { name = "mShortcutsAndWidgets"; superclass() }
        val setBackground = hotseat.firstMethod { name = "setDockerBackground"; emptyParameters() }

        // KavaRef 的成员引用同一时间只能绑定一个实例，热路径上每次都要取副本
        val drawableOf = { any: Any -> drawableField.copy().of(any).get<Drawable>() }
        val containerOf = { any: Any -> containerField.copy().of(any).get<ViewGroup>() }
        val clear = { any: Any ->
            drawableOf(any)?.callback = null
            drawableField.copy().of(any).set(null)
            containerOf(any)?.background = null
        }

        // 宿主切换折叠状态时会跑这个入口，判定关掉时把背景清空；这里接管它，
        // 让它按 dock 上有没有图标补建或清空，补建同样走官方方法
        runCatching {
            hotseat.firstMethod { name = "updateDockerBgIfNecessary"; emptyParameters() }.hook {
                intercept {
                    val container = containerOf(instance)
                    if (container != null) {
                        if (container.childCount == 0) {
                            clear(instance)
                        } else if (drawableOf(instance) == null) {
                            setBackground.copy().of(instance).invoke()
                        }
                    }
                    null
                }
            }
        }.onFailure { YLog.warn("dock 背景：接管刷新入口失败 $it") }

        // 官方入口对直板机不可达，onDraw 检测到还没有背景时调官方方法补建
        hotseat.firstMethod { name = "onDraw"; parameterCount = 1 }.hook {
            before {
                val container = containerOf(instance) ?: return@before
                if (container.childCount == 0) {
                    if (drawableOf(instance) != null) clear(instance)
                    return@before
                }
                if (drawableOf(instance) == null) {
                    runCatching { setBackground.copy().of(instance).invoke() }
                        .onFailure { YLog.error("生成 dock 背景失败", it) }
                }
            }
        }
    }
}
