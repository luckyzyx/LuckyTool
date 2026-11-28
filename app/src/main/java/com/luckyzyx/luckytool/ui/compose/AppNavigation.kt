package com.luckyzyx.luckytool.ui.compose

/**
 * 任意已注册的功能页。二级页迁移到独立 Activity 后不再是导航目的地，
 * 只是把打开一个功能页所需的参数从主界面带到 PageActivity。
 *
 * [scrollKey] / [scrollPosition] 是搜索命中普通偏好时的页内定位参数：
 * 进页面后滚到该条目（[scrollPosition] 为条目序号），不为空时才生效。
 */
data class PageRoute(
    val pageKey: String,
    val title: String = "",
    val scrollKey: String = "",
    val scrollPosition: Int = -1,
)
