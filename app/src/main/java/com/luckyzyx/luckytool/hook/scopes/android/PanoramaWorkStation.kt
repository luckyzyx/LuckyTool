package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 全景工作台（平板三分屏）：开关打开时把 system_server 侧的分支判定伪装成平板，
 * 并把布局策略固定为 TabletCanvasLayoutPolicy、窗口数上限取用户配置。
 */
@Obfuscate
object PanoramaWorkStation : YukiBaseHooker() {

    override fun onHook() {
        if (!preferences(ModulePrefs).getBoolean("panorama_work_station", false)) return
        val maxNum = preferences(ModulePrefs).getInt("panorama_max_win_num", 3)
        loadHooker(HookPanoramaWorkBranch())
        loadHooker(HookFlexibleWindow(maxNum))
        loadHooker(HookMiniWindowFeature())
        loadHooker(HookZoomWindow())
        loadHooker(HookCanvasLayout())
    }

    /** 全景工作台分支判定：平板/全景为真，折叠/小屏为假 */
    @Obfuscate
    class HookPanoramaWorkBranch : YukiBaseHooker() {
        override fun onHook() {
            "com.android.server.wm.OplusPanoramaWorkBranchUtil".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hasTabletPanoramaWorkFeature"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "hasPanoramaWorkFeature"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "isPanoramaWorkBranchEnable"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "isPanoramaWorkBranchExcludeSmallDevice"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "hasFoldPanoramaWorkFeature"
                    emptyParameters()
                }?.hook { before { result = false } }
                firstMethodOrNull {
                    name = "isFoldPanoramaWorkEnable"
                    emptyParameters()
                }?.hook { before { result = false } }
                firstMethodOrNull {
                    name = "isPanoramaWorkBranchForSmallDevice"
                    emptyParameters()
                }?.hook { before { result = false } }
                firstMethodOrNull {
                    name = "isPanoramaForcedDisable"
                    emptyParameters()
                }?.hook { after { result = false } }
            }
            "com.android.server.wm.FlexibleWindowUtils".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "isSmallDeviceMultiFlexibleEnabled"
                    emptyParameters()
                }?.hook { before { result = false } }
            }
        }
    }

    /** 布局策略固定为平板 + 窗口数上限 + 收起 12 号 zoom 窗口 */
    @Obfuscate
    class HookFlexibleWindow(private val maxNum: Int) : YukiBaseHooker() {
        override fun onHook() {
            "com.android.server.wm.FlexibleWindowManagerService".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "getTaskLayoutPolicy"
                    emptyParameters()
                }?.hook {
                    after {
                        runCatching {
                            firstField {
                                name = "mTabletCanvasLayoutPolicy"
                                superclass()
                            }.of(instance).get<Any>()
                        }.getOrNull()?.let { result = it }
                    }
                }
                firstMethodOrNull {
                    name = "getMaxWinNum"
                    returnType = Int::class
                }?.hook {
                    after {
                        if (maxNum > 0) result = maxNum
                    }
                }
            }
            "com.android.server.wm.FlexibleTaskController".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hideZoomWindow"
                    parameters(Int::class)
                }?.hook {
                    before {
                        if (firstArg().get<Int>() == 12) result = null
                    }
                }
            }
        }
    }

    /** 关闭「小窗切换到全屏」feature，并清掉启动期缓存的静态开关 */
    @Obfuscate
    class HookMiniWindowFeature : YukiBaseHooker() {
        override fun onHook() {
            "com.oplus.content.OplusFeatureConfigManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hasFeature"
                    parameters(String::class)
                    returnType = Boolean::class
                }?.hook {
                    after {
                        val key = firstArg().get<String>() ?: return@after
                        if (key.contains("mini_window_switch_to_fullscreen")) result = false
                    }
                }
            }
            "com.oplus.content.OplusFeatureConfigManagerInternal".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hasFeature"
                    parameters(String::class)
                    returnType = Boolean::class
                }?.hook {
                    after {
                        val key = firstArg().get<String>() ?: return@after
                        if (key.contains("mini_window_switch_to_fullscreen")) result = false
                    }
                }
            }
            "com.android.server.wm.FlexibleTaskController".toClassOrNull()?.resolve()?.apply {
                runCatching {
                    firstField {
                        name = "sIsMiniSwitchToFullscreen"
                        superclass()
                    }.set(false)
                }
            }
        }
    }

    /** zoom 能力开关强制打开 */
    @Obfuscate
    class HookZoomWindow : YukiBaseHooker() {
        override fun onHook() {
            "com.android.server.wm.OplusZoomWindowConfig".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "isZoomWindowEnabled"
                    emptyParameters()
                }?.hook { before { result = true } }
            }
            "com.android.server.wm.OplusZoomWindowManagerService".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "isZoomSupportMultiWindow"
                    emptyParameters()
                }?.hook { after { result = true } }
            }
        }
    }

    /** 布局策略判定 + 竖屏三屏方向 */
    @Obfuscate
    class HookCanvasLayout : YukiBaseHooker() {
        override fun onHook() {
            "com.oplus.flexiblewindow.CanvasCalculateLayoutRect".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "isTabletDevice"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "isPhoneDisplay"
                    parameters(Context::class)
                }?.hook { before { result = false } }
            }
            "com.oplus.flexiblewindow.TabletCanvasCalculateLayoutRectPolicy".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "computeLayoutOrientation"
                    parameters(List::class, Int::class, Int::class, Context::class)
                    returnType = Int::class
                }?.hook {
                    after {
                        val context = args.getOrNull(3) as? Context ?: return@after
                        val panes = args.getOrNull(0) as? List<*> ?: return@after
                        if (panes.size == 3) {
                            val config = context.resources.configuration
                            if (config.screenHeightDp > config.screenWidthDp) result = 4
                        }
                    }
                }
            }
            "com.oplus.flexiblewindow.DefaultCanvasCalculateLayoutRectPolicy".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "computeLayoutOrientation"
                    parameters(List::class, Int::class, Int::class, Context::class)
                    returnType = Int::class
                }?.hook {
                    after {
                        val context = args.getOrNull(3) as? Context ?: return@after
                        val panes = args.getOrNull(0) as? List<*> ?: return@after
                        if (panes.size == 3) {
                            val config = context.resources.configuration
                            if (config.screenHeightDp > config.screenWidthDp) result = 4
                        }
                    }
                }
            }
        }
    }
}
