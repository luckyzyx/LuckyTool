package com.luckyzyx.luckytool.hook.scopes.opluskeyboard

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.interceptAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

/**
 * 强制启用小布输入法的液态玻璃模糊。
 *
 * 宿主原本要求同时满足：系统「质感模糊」开关已打开（`system_material_blur_enable` == 1）、
 * 当前输入应用命中白名单、非折叠屏展开态、OS 版本 >= 40，并且键盘使用内置默认主题 + 跟随系统深浅色。
 * 这些条件最终全部收敛到总闸 `f.b()`：键盘内所有模糊渲染分支（容器背景模糊 delegate、
 * 各 view 在 `*Blur` 资源之间的二选一、ImeService 的窗口标记）都以它为唯一总闸。
 *
 * 总闸类里的 `b()`（模糊总开关）与 `c()`（主题条件）都是「无参 + 返回 boolean」，`l.i()`
 * （系统「质感模糊」开关，`ImeApp` 与设置观察者据此驱动 `f11731a` 这个 LiveData）同样如此。
 * 这些类名/方法名都是混淆名，ROM 更新后可能变化，所以用 DexKit 按稳定特征定位：
 * - `key_day_night_mode` 是「跟随系统深浅色」偏好键：旧版本它出现在 `c()` 自己身上，
 *   1.8.35.17 起被挪进主题工具内部类，此时改为找「调用了该读取方法的无参 boolean 方法」（即 `c()`）；
 * - `l.i()` 是唯一同时使用 `system_material_blur_enable` 与日志标签 `SettingsPrefs` 的无参 boolean 方法。
 * 定位到类后按「无参 + 返回 boolean」结构 hook，不再依赖具体方法名。
 *
 * 模糊 drawable 由框架 `ViewRootManager` 在构造时直接取自
 * `ViewRootImpl.createBackgroundBlurDrawable()`（本 ROM 恒返回非空 drawable，仅 view 尚未 attach
 * 时才会拿到 null），该路径不读取系统设置；`system_material_blur_enable` 只由宿主读取（上面已 hook），
 * `disable_window_blurs` 的判断在 system_server 侧，不在键盘进程的 drawable 路径里。
 * 开关判断全部收敛在宿主侧，所以这里不 hook 任何框架方法，只 hook 宿主的总闸与 l.i()。
 * 模糊半径由宿主 `com.oplus.keyboard.input.view.blur.b` 构造器写死为 150（`const/16 v3, 0x96`），
 * 存进类内唯一的 int 实例字段，渲染方法 `b()` 原样把它交给框架 `setBlurRadius`。这里 hook 宿主渲染方法，
 * after 里用 KavaRef 自己调一次框架 `setBlurRadius`，把半径覆盖成用户设置的 `oplus_keyboard_blur_radius`
 * （50..500），滑条重新生效；`c()` 的清除路径（先 `setBlurRadius(0)` 再置空字段）不匹配渲染方法特征，不会误伤。
 *
 * 刻意不碰 `utils.h.j()`（折叠判断被分屏键盘、悬浮工具栏复用，`b()` 拦截后已无需它）与
 * `reflect.a.v()`（版本号还喂给 appcompat、应用更新等处，改它风险大于收益）。
 */
@Obfuscate
class ForceEnableOplusKeyboardBlur(private val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        /** 系统「质感模糊」总开关 */
        private const val SYSTEM_MATERIAL_BLUR = "system_material_blur_enable"

        /** 模块设置里的自定义模糊半径键，与 UI 滑条保持一致 */
        private const val BLUR_RADIUS_KEY = "oplus_keyboard_blur_radius"

        /** 与宿主构造器写死的 0x96（150）一致 */
        private const val DEFAULT_BLUR_RADIUS = 150
    }

    /** 诊断日志用：总闸是否真的被调用过 */
    private var gateHit = false

    override fun onHook() {
        //Source f
        // 模糊总闸类的定位特征：
        // - 旧版本把「跟随系统深浅色」这个条件写在自己身上，只有 c() 是无参、返回 boolean 且使用 key_day_night_mode 的方法；
        // - 键盘 1.8.35.17 起该条件被拆进主题工具的内部类（f$a），总闸类里只剩一次对它的调用。
        // 先按旧特征找，找不到再顺着「谁调用了主题读取方法」找总闸类。
        val oldGate = dexKitBridge.findClass {
            matcher {
                usingStrings("key_day_night_mode")
            }
        }.apply {
            checkDataList("ForceEnableOplusKeyboardBlur Gate class")
        }.findMethod {
            matcher {
                paramCount(0)
                returnType(classOf<Boolean>())
                usingStrings("key_day_night_mode")
            }
        }
        YLog.info("ForceEnableOplusKeyboardBlur: oldGate size = ${oldGate.size}")
        val gateClass = if (oldGate.isNotEmpty()) {
            oldGate.checkDataList("ForceEnableOplusKeyboardBlur Gate").single().className
        } else {
            val dayNightModeGetter = dexKitBridge.findClass {
                matcher {
                    usingStrings("key_day_night_mode")
                }
            }.apply {
                checkDataList("ForceEnableOplusKeyboardBlur DayNightMode class")
            }.findMethod {
                matcher {
                    paramCount(0)
                    usingStrings("key_day_night_mode")
                }
            }.checkDataList("ForceEnableOplusKeyboardBlur DayNightMode").single()
            dexKitBridge.findClass {
                matcher {
                    methods {
                        add {
                            paramCount(0)
                            returnType(classOf<Boolean>())
                            addInvoke {
                                paramCount(0)
                                name(dayNightModeGetter.methodName)
                                declaredClass(dayNightModeGetter.className)
                            }
                        }
                    }
                }
            }.apply {
                checkDataList("ForceEnableOplusKeyboardBlur Gate class")
            }.findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<Boolean>())
                    addInvoke {
                        paramCount(0)
                        name(dayNightModeGetter.methodName)
                        declaredClass(dayNightModeGetter.className)
                    }
                }
            }.checkDataList("ForceEnableOplusKeyboardBlur Gate").single().className
        }

        YLog.info("ForceEnableOplusKeyboardBlur: gateClass = $gateClass")

        gateClass.toClass().resolve().apply {
            // b() 与 c()：该类里只有这两个无参 boolean 方法，命中即全部返回 true
            method { emptyParameters(); returnType = Boolean::class }.interceptAll {
                if (!gateHit) {
                    gateHit = true
                    YLog.info("ForceEnableOplusKeyboardBlur: gate ${method.name} hit")
                }
                true
            }
        }

        //Source SettingsPrefs
        // 系统「质感模糊」开关，驱动 f11731a 这个 LiveData
        val materialBlurSetting = dexKitBridge.findClass {
            matcher {
                usingStrings("SettingsPrefs", SYSTEM_MATERIAL_BLUR)
            }
        }.apply {
            checkDataList("ForceEnableOplusKeyboardBlur MaterialBlurSetting class")
        }.findMethod {
            matcher {
                paramCount(0)
                returnType(classOf<Boolean>())
                usingStrings(SYSTEM_MATERIAL_BLUR, "SettingsPrefs")
            }
        }.checkDataList("ForceEnableOplusKeyboardBlur MaterialBlurSetting").single()
        YLog.info("ForceEnableOplusKeyboardBlur: materialBlurSetting = $materialBlurSetting")

        materialBlurSetting.className.toClass().resolve().apply {
            firstMethod { name = materialBlurSetting.methodName; emptyParameters() }
                .intercept { true }
        }

        //Source BlurBuilder
        // 宿主在 blur/b 的渲染方法里读 int 字段 h（构造器写死 0x96=150）后调用框架 setBlurRadius(h)；
        // c() 是清除路径：先 setBlurRadius(0) 再把 ViewRootManager 字段置 null。
        // 不 hook 框架方法：这里 hook 宿主渲染方法，after 里用 KavaRef 自己调一次 setBlurRadius(用户值) 覆盖半径。
        // 渲染方法的定位特征 = 同时调用 setBlurRadius 与 setBlurParams（框架方法名稳定，c() 只调前者）。
        val blurBuilder = dexKitBridge.findClass {
            matcher {
                className("com.oplus.keyboard.input.view.blur.", StringMatchType.StartsWith)
                methods {
                    add {
                        addInvoke {
                            name("setBlurRadius")
                            declaredClass("com.oplus.view.ViewRootManager")
                        }
                    }
                }
            }
        }.apply {
            checkDataList("ForceEnableOplusKeyboardBlur BlurBuilder class")
        }.single()
        YLog.info("ForceEnableOplusKeyboardBlur: blurBuilder = ${blurBuilder.name}")

        val renderMethod = blurBuilder.findMethod {
            matcher {
                addInvoke {
                    name("setBlurRadius")
                    declaredClass("com.oplus.view.ViewRootManager")
                }
                addInvoke {
                    name("setBlurParams")
                    declaredClass("com.oplus.view.ViewRootManager")
                }
            }
        }.apply {
            checkDataList("ForceEnableOplusKeyboardBlur BlurRender method")
        }.singleOrNull() ?: return
        YLog.info("ForceEnableOplusKeyboardBlur: renderMethod = ${renderMethod.methodName}")

        val vrmFieldName = blurBuilder.findField {
            matcher {
                declaredClass(blurBuilder.name)
                type("com.oplus.view.ViewRootManager".toClass())
            }
        }.apply {
            checkDataList("ForceEnableOplusKeyboardBlur ViewRootManager field")
        }.singleOrNull()?.fieldName ?: return
        YLog.info("ForceEnableOplusKeyboardBlur: vrmField = $vrmFieldName")

        blurBuilder.name.toClass().resolve().apply {
            val vrmField = firstField { name = vrmFieldName }
            val setBlurRadius = "com.oplus.view.ViewRootManager".toClass().resolve()
                .firstMethod { name = "setBlurRadius" }
            firstMethod { name = renderMethod.methodName; emptyParameters() }.hook {
                after {
                    runCatching {
                        val vrm = vrmField.copy().of(instance).get<Any?>() ?: return@after
                        setBlurRadius.copy().of(vrm).invoke<Unit>(
                            preferences(ModulePrefs).getInt(BLUR_RADIUS_KEY, DEFAULT_BLUR_RADIUS)
                        )
                    }.onFailure {
                        YLog.warn("ForceEnableOplusKeyboardBlur: override blur radius failed: $it")
                    }
                }
            }
        }
    }
}
