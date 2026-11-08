package com.luckyzyx.luckytool.ui.theme

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 界面风格及其持久化值。
 *
 * - [Coui]：COUI 外观线，应用默认；其材质与效果组件要求 Android 14（API 34），低版本设备不可选用
 * - [Material]：Material 3 Expressive 外观线，COUI 不可用时的备用线
 *
 * 未设置偏好时，Android 14+ 取 [Coui]，更低版本取 [Material]。
 * 偏好里残留的 COUI（例如从高版本设备备份恢复）在低版本设备上读取时同样回落 [Material]，
 * 因为各条分派分支都按「不是 COUI 就是 Material」的两线前提实现。
 */
enum class UiMode(val value: String) {
    Coui("coui"),
    Material("material");

    /** 当前系统版本能否选用该外观线。 */
    val isAvailable: Boolean
        get() = this != Coui || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** 把不可用的外观线回落到 [Material]，调用方拿到的一定是能渲染的值。 */
    fun coerceAvailable(): UiMode = if (isAvailable) this else Material

    companion object {
        /** 两线之前的旧偏好值，读取时按当前设备能力映射到新的一线。 */
        private const val LEGACY_MIUIX_VALUE = "miuix"

        fun fromValue(value: String): UiMode = when (value) {
            Coui.value -> Coui
            Material.value, LEGACY_MIUIX_VALUE -> Material
            else -> if (Coui.isAvailable) Coui else Material
        }

        /** 未设置偏好时的取值：Android 14+ 用 [Coui]，更低版本用 [Material]。 */
        val DEFAULT_VALUE: String
            get() = if (Coui.isAvailable) Coui.value else Material.value
    }
}

/** 当前界面风格 */
val LocalUiMode = staticCompositionLocalOf { UiMode.fromValue(UiMode.DEFAULT_VALUE).coerceAvailable() }
