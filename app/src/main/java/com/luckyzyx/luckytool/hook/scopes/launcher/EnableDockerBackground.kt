package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.drawable.Drawable
import android.view.View
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
    private const val BLUR_PROPERTIES =
        "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties"
    private const val BLUR_FACTORY =
        "com.android.launcher3.uioverrides.states.blurdrawable.BlurBaseFactory"

    /** prepareBlurBase 的 type 实参，与宿主保持一致 */
    private const val BLUR_TYPE = 4

    override fun onHook() {
        val background =
            preferences(ModulePrefs).getBoolean("enable_docker_background", false)
        val blur = preferences(ModulePrefs)
            .getBoolean("force_enable_docker_background_blur", false)

        // 旧版本由 ScreenUtils 的判定决定能否显示 dock 背景，打开背景开关就强制放开
        val screenUtils = SCREEN_UTILS.toClassOrNull()?.resolve()
        if (screenUtils != null) {
            if (background) {
                screenUtils.firstMethod { name = "isSupportDockerExpandScreen" }.hook {
                    intercept(true)
                }
            }
            if (!blur) return
            // 在宿主生成背景的调用栈内把模糊分支条件强制为 true
            val forcing = ThreadLocal<Boolean>()
            HOTSEAT.toClass().resolve()
                .firstMethod { name = "setDockerBackground"; emptyParameters() }.hook {
                    before { forcing.set(true) }
                    after { forcing.remove() }
                }
            BLUR_PROPERTIES.toClass().resolve().firstMethod { name = "isSupportNewBlur" }.hook {
                before { if (forcing.get() == true) result = true }
            }
            screenUtils.firstMethod { name = "hasLargeDisplayFeatures"; emptyParameters() }.hook {
                before { if (forcing.get() == true) result = true }
            }
            return
        }

        // 新版把判定移出桌面，生成入口对直板机不可达；但 onDraw 只按背景字段绘制，
        // 所以绘制前让宿主自己建一次背景：模糊优先，模糊建不起来再用普通背景
        val hotseat = HOTSEAT.toClass().resolve()
        val drawableField = hotseat.firstField { name = "mDockerBackgroundDrawable" }
        // 装 dock 图标的容器声明在父类 CellLayout 上
        val containerField = hotseat.firstField { name = "mShortcutsAndWidgets"; superclass() }
        val blurPropField = hotseat.firstField { name = "mBlurProp" }
        val cornerFactor = hotseat.firstField { name = "BACKGROUND_SMOOTH_CORNER_RADIUS_FACTOR" }
        val setBackground = hotseat.firstMethod { name = "setDockerBackground"; emptyParameters() }
        val updateBlurParams = hotseat.firstMethod { name = "updateBlurParams"; parameterCount = 1 }
        val updateBounds =
            hotseat.firstMethod { name = "updateDockerBackgroundBounds"; emptyParameters() }
        val setAlpha = hotseat.firstMethod { name = "setBackgroundAlpha"; parameterCount = 1 }
        val prepareBlur = BLUR_FACTORY.toClass().resolve()
            .firstMethod { name = "prepareBlurBase"; parameterCount = 6 }

        // KavaRef 的成员引用同一时间只能绑定一个实例，热路径上每次都要取副本
        val drawableOf = { any: Any -> drawableField.copy().of(any).get<Drawable>() }
        val containerOf = { any: Any -> containerField.copy().of(any).get<ViewGroup>() }
        val clear = { any: Any ->
            drawableOf(any)?.callback = null
            drawableField.copy().of(any).set(null)
            containerOf(any)?.background = null
        }

        // 宿主每帧开头会跑这个入口，判定关掉时把已经建好的背景清空；背景由本 hooker 管理，
        // 这里接管它，只保留「dock 上没有图标就清掉」的行为
        runCatching {
            hotseat.firstMethod { name = "updateDockerBgIfNecessary"; emptyParameters() }.hook {
                intercept {
                    val container = containerOf(instance)
                    if (container != null && container.childCount == 0) clear(instance)
                    null
                }
            }
        }.onFailure { YLog.warn("dock 背景：接管刷新入口失败 $it") }

        hotseat.firstMethod { name = "onDraw"; parameterCount = 1 }.hook {
            before {
                val view = instanceOrNull<View>() ?: return@before
                val container = containerOf(instance) ?: return@before
                val current = drawableOf(instance)
                if (container.childCount == 0) {
                    if (current != null) clear(instance)
                    return@before
                }
                // 已经有背景（宿主自己建的或上一帧建的）就不重复建
                if (current != null) return@before
                if (view.measuredHeight <= 0) return@before

                val blurBuilt = blur && runCatching {
                    val prop = prepareBlur.copy().of(instance).invoke(
                        container, view.context, false, false, BLUR_TYPE, true
                    ) ?: return@runCatching
                    blurPropField.copy().of(instance).set(prop)
                    val radius = view.measuredHeight * (cornerFactor.get<Float>() ?: 0.26f)
                    val drawable = updateBlurParams.copy().of(instance).invoke<Drawable>(radius)
                        ?: return@runCatching
                    drawableField.copy().of(instance).set(drawable)
                    drawable.callback = container
                    container.background = null
                    updateBounds.copy().of(instance).invoke()
                    setAlpha.copy().of(instance).invoke(1.0f)
                }.isSuccess
                if (blurBuilt) return@before
                runCatching { setBackground.copy().of(instance).invoke() }
                    .onFailure { YLog.error("生成 dock 背景失败", it) }
            }
        }
    }
}