package com.luckyzyx.luckytool.hook.hookers

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import kotlin.math.max

/**
 * 全景工作台容器（com.oplus.pscanvas）：开关打开时伪装平板形态，
 * 并平移小窗的 VisualBounds/LaunchBounds 让三个小窗错位摆放。
 */
@Obfuscate
object HookPscanvas : YukiBaseHooker() {

    override fun onHook() {
        loadHooker(HookGlobalFeatureConfig)
        if (!preferences(ModulePrefs).getBoolean("panorama_work_station", false)) return
        loadHooker(HookFeature())
        loadHooker(HookCanvasLayout())
        loadHooker(HookDeviceTypeHelper())
        loadHooker(HookFlexibleWindowUtil())
        loadHooker(HookSmallWindowGeometry())
    }

    /** pscanvas 进程内的设备形态 feature */
    @Obfuscate
    class HookFeature : YukiBaseHooker() {
        override fun onHook() {
            val features = mapOf(
                "oplus.hardware.type.tablet" to true,
                "oplus.software.wms.panorama_work_station" to true,
                "oplus.hardware.type.fold" to false,
                "oplus.software.fold_remap_display_disabled" to false,
            )
            "com.oplus.content.OplusFeatureConfigManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hasFeature"
                    parameters(String::class)
                    returnType = Boolean::class
                }?.hook {
                    before {
                        val key = firstArg().get<String>() ?: return@before
                        features[key]?.let { result = it }
                    }
                }
            }
            "com.oplus.content.OplusFeatureConfigManagerInternal".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "hasFeature"
                    parameters(String::class)
                    returnType = Boolean::class
                }?.hook {
                    before {
                        val key = firstArg().get<String>() ?: return@before
                        features[key]?.let { result = it }
                    }
                }
            }
        }
    }

    /** 布局策略判定：平板为真、手机为假 */
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
        }
    }

    /** 设备形态助手：isTablet/isFold 为真，中屏判定为假 */
    @Obfuscate
    class HookDeviceTypeHelper : YukiBaseHooker() {
        override fun onHook() {
            val helper = findDeviceTypeHelper() ?: return
            helper.resolve().apply {
                firstMethodOrNull {
                    name = "x"
                    parameters(Context::class)
                }?.hook { before { result = false } }
                firstMethodOrNull {
                    name = "I"
                    emptyParameters()
                }?.hook { before { result = true } }
                firstMethodOrNull {
                    name = "q"
                    emptyParameters()
                }?.hook { before { result = true } }
            }
        }

        private fun findDeviceTypeHelper(): Class<*>? {
            listOf("i2.s", "h2.s", "j2.s", "k2.s", "l2.s", "m2.s", "n2.s").forEach { candidate ->
                val clazz = candidate.toClassOrNull() ?: return@forEach
                if (hasDeviceTypeSignals(clazz)) return clazz
            }
            for (c in 'a'..'z') {
                for (i in 0..3) {
                    val clazz = ("$" + c + i + ".s").toClassOrNull() ?: continue
                    if (hasDeviceTypeSignals(clazz)) return clazz
                }
            }
            return null
        }

        private fun hasDeviceTypeSignals(clazz: Class<*>): Boolean = runCatching {
            clazz.resolve().let { resolver ->
                val hasTablet = resolver.firstMethodOrNull {
                    name = "I"
                    emptyParameters()
                } != null
                val hasMediumScreen = resolver.firstMethodOrNull {
                    name = "x"
                    parameters(Context::class)
                } != null
                hasTablet && hasMediumScreen
            }
        }.getOrDefault(false)
    }

    /** FlexibleWindowUtil：zoom 与平板能力为真，支持列表全开 */
    @Obfuscate
    class HookFlexibleWindowUtil : YukiBaseHooker() {
        override fun onHook() {
            "com.oplus.pscanvas.canvasmode.canvas.util.FlexibleWindowUtil".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "e1"
                    parameters(Context::class)
                }?.hook { before { result = true } }
                for (methodName in listOf("M0", "O0", "N0", "P0", "L0", "Q0")) {
                    firstMethodOrNull {
                        name = methodName
                        parameters(Context::class)
                    }?.hook { before { result = true } }
                }
                firstMethodOrNull {
                    name = "R0"
                    parameters(List::class, Int::class)
                }?.hook {
                    after {
                        val src = args.getOrNull(0)
                        if (src is List<*>) result = BooleanArray(src.size)
                    }
                }
            }
        }
    }

    /** 平移小窗 VisualBounds/LaunchBounds，三个窗错位摆放 */
    @Obfuscate
    class HookSmallWindowGeometry : YukiBaseHooker() {
        override fun onHook() {
            "f2.x".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "S"
                    parameters(List::class)
                }?.hook {
                    before {
                        val list = args.getOrNull(0) as? List<*> ?: return@before
                        list.forEachIndexed { index, item ->
                            val bundle = item as? Bundle ?: return@forEachIndexed
                            shift(bundle, "androidx.flexible.VisualBounds", index)
                            shift(bundle, "androidx.flexible.LaunchBounds", index)
                        }
                    }
                }
            }
        }

        private fun shift(bundle: Bundle, key: String, index: Int) {
            @Suppress("DEPRECATION")
            val p = bundle.getParcelable(key) as? Rect ?: return
            val r = Rect(p)
            val dm = Resources.getSystem().displayMetrics
            val sw = (dm.widthPixels * 0.46f).toInt()
            val sh = (sw * (r.height().toFloat() / max(1, r.width()))).toInt()
            val margin = (16 * dm.density).toInt()
            val top = (dm.heightPixels * 0.12f).toInt()
            val bottomTop = dm.heightPixels - sh - top
            when (index % 3) {
                0 -> r.set(margin, top, margin + sw, top + sh)
                1 -> r.set(dm.widthPixels - sw - margin, top, dm.widthPixels - margin, top + sh)
                else -> r.set(margin, bottomTop, margin + sw, bottomTop + sh)
            }
            bundle.putParcelable(key, r)
        }
    }
}
