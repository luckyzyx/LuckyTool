package com.luckyzyx.luckytool.ui.compose

import kotlinx.serialization.Serializable

/**
 * 页面路由通过 Activity Intent 传递，系统重建时保留目的地及搜索定位参数。
 */
@Serializable
sealed interface AppRoute

/** 主题设置页。 */
@Serializable
data object ThemeRoute : AppRoute

/**
 * 任意已注册的功能页。
 *
 * [scrollKey] / [scrollPosition] 是搜索命中普通偏好时的页内定位参数：
 * 进页面后滚到该条目（[scrollPosition] 为条目序号），不为空时才生效。
 */
@Serializable
data class ScopeRoute(
    val pageKey: String,
    val title: String = "",
    val scrollKey: String = "",
    val scrollPosition: Int = -1,
) : AppRoute
