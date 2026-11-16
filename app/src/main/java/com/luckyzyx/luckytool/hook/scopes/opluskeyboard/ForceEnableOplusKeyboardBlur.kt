package com.luckyzyx.luckytool.hook.scopes.opluskeyboard

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.interceptAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

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
 * 只改宿主自己的判断还不够：模糊 drawable 由框架 `ViewRootManager` 在宿主进程内另外按系统设置决定，
 * 系统「质感模糊」关着或全局禁用窗口模糊时 `getBackgroundBlurDrawable()` 直接返回 null，
 * 宿主拿不到 drawable 就静默不画（无异常、无日志）。所以这里再把进程内对这两项系统设置的读取一起改掉。
 *
 * 模糊强度由 `ViewRootManager.setBlurRadius()` 决定（宿主固定请求 150，0 表示关闭），
 * 模块设置里的滑条只改写这里的正数入参。`ViewRootManager` 属于框架而不是宿主 dex，
 * 名字不受宿主混淆影响，仍按类名定位。
 *
 * 刻意不碰 `utils.h.j()`（折叠判断被分屏键盘、悬浮工具栏复用，`b()` 拦截后已无需它）与
 * `reflect.a.v()`（版本号还喂给 appcompat、应用更新等处，改它风险大于收益）。
 */
@Obfuscate
class ForceEnableOplusKeyboardBlur(private val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        /** 宿主内置的模糊半径，作为自定义项的默认值 */
        private const val DEFAULT_BLUR_RADIUS = 150

        /** 系统「质感模糊」总开关 */
        private const val SYSTEM_MATERIAL_BLUR = "system_material_blur_enable"

        /** 全局禁用窗口模糊 */
        private const val DISABLE_WINDOW_BLURS = "disable_window_blurs"
    }

    /** 诊断日志用：总闸是否真的被调用过 */
    private var gateHit = false

    /** 诊断日志用：上一次记录的请求半径 */
    private var lastLoggedRadius = Int.MIN_VALUE

    override fun onHook() {
        //自定义模糊强度
        val blurRadius =
            preferences(ModulePrefs).getInt("oplus_keyboard_blur_radius", DEFAULT_BLUR_RADIUS)

        //Source f
        // 模糊总闸类的定位特征：
        // - 旧版本把「跟随系统深浅色」这个条件写在自己身上，只有 c() 是无参、返回 boolean 且使用 key_day_night_mode 的方法；
        // - 键盘 1.8.35.17 起该条件被拆进主题工具的内部类（f$a），总闸类里只剩一次对它的调用。
        // 先按旧特征找，找不到再顺着「谁调用了主题读取方法」找总闸类。
        val oldGate = dexKitBridge.findMethod {
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
            val dayNightModeGetter = dexKitBridge.findMethod {
                matcher {
                    paramCount(0)
                    usingStrings("key_day_night_mode")
                }
            }.checkDataList("ForceEnableOplusKeyboardBlur DayNightMode").single()
            dexKitBridge.findMethod {
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
        val materialBlurSetting = dexKitBridge.findMethod {
            matcher {
                paramCount(0)
                returnType(classOf<Boolean>())
                usingStrings(SYSTEM_MATERIAL_BLUR, "SettingsPrefs")
            }
        }.checkDataList("ForceEnableOplusKeyboardBlur MaterialBlurSetting").single()
        YLog.info("ForceEnableOplusKeyboardBlur: materialBlurSetting = $materialBlurSetting")

        materialBlurSetting.className.toClass().resolve().apply {
            firstMethod { name = materialBlurSetting.methodName; emptyParameters() }
                .hook { intercept(true) }
        }

        //Source Settings
        // 框架侧在宿主进程里自己再读一遍系统设置，宿主被改后的判断挡不住它，这里按同一个键一起改
        hookSettingGetInt("android.provider.Settings\$System", SYSTEM_MATERIAL_BLUR, 1)
        hookSettingGetInt("android.provider.Settings\$Global", DISABLE_WINDOW_BLURS, 0)

        //Source ViewRootManager
        // 模糊半径的唯一入口：宿主请求正数时才替换，保留 0 的关闭语义
        "com.oplus.view.ViewRootManager".toClass().resolve().apply {
            firstMethod { name = "setBlurRadius" }.hook {
                before {
                    val requested = args[0] as? Int ?: return@before
                    if (requested != lastLoggedRadius) {
                        lastLoggedRadius = requested
                        YLog.info("ForceEnableOplusKeyboardBlur: setBlurRadius $requested")
                    }
                    if (requested > 0 && requested != blurRadius) args[0] = blurRadius
                }
            }
            //诊断日志：框架是否给了模糊 drawable（null 表示框架层拒绝出模糊，与宿主逻辑无关）
            runCatching {
                firstMethod { name = "getBackgroundBlurDrawable" }.hook {
                    after {
                        val drawable = result
                        if (!loggedDrawable) {
                            loggedDrawable = true
                            YLog.info("ForceEnableOplusKeyboardBlur: blur drawable = $drawable")
                        }
                    }
                }
            }.onFailure {
                YLog.warn("ForceEnableOplusKeyboardBlur: hook drawable failed: $it")
            }
        }
    }

    /** 诊断日志用：drawable 结果是否已记录 */
    private var loggedDrawable = false

    /**
     * 把进程内对 [key] 的系统设置读取固定成 [value]。
     *
     * 只按 key 精确匹配，其它键原样返回；方法名 `getInt`/`getIntForUser` 的各个重载都挂一遍。
     */
    private fun hookSettingGetInt(className: String, key: String, value: Int) {
        runCatching {
            className.toClass().resolve().apply {
                method { name = "getInt" }.hookAll {
                    before { if (args.size > 1 && args[1] == key) result = value }
                }
                method { name = "getIntForUser" }.hookAll {
                    before { if (args.size > 1 && args[1] == key) result = value }
                }
            }
        }.onFailure {
            YLog.warn("ForceEnableOplusKeyboardBlur: hook $className failed: $it")
        }
    }
}
