package com.luckyzyx.luckytool.hook.scopes.alarmclock

import android.content.Context
import android.os.Handler
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class AlarmClockWidget(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        private lateinit var redMode: String

        //红 layout 依赖的自定义 View，同时也是各版本红一开关方法的稳定锚点字符串
        private val OplusTextClock = "com.oplus.widget.OplusTextClock"

        //没有红 layout 的老版本（如 COS12.1）直接给组件文本里的 "1" 上色
        private val OnePlusWidget = "com.coloros.widget.smallweather.OnePlusWidget"

        fun setCharRedOne(format: CharSequence): CharSequence {
            val sp = SpannableStringBuilder(format)
            val length = if (format.contains(":")) format.toString().substringBefore(":").length
            else if (format.contains("\u2236")) format.toString().substringBefore("\u2236").length
            else format.length
            for (i in 0 until length) {
                if (format[i].toString() == "1") {
                    val colorRes = "#c41442".toColorInt()
                    sp.setSpan(ForegroundColorSpan(colorRes), i, i + 1, 34)
                }
            }
            return sp
        }

        /** 红一开关方法，类名与方法名每版都不同，一律取自 DexKit 定位结果 */
        private data class RedOneGate(val className: String, val methodName: String)

        /** 按红 layout 依赖的 OplusTextClock 字符串反查宿主选 layout 的开关方法 */
        private fun DexKitBridge.findRedOneGates(): List<RedOneGate> = runCatching {
            findMethod {
                matcher { usingStrings(OplusTextClock) }
            }.apply {
                checkDataList("AlarmClockRedOne", onlyOne = false)
            }.mapNotNull { data ->
                if (data.paramTypeNames.isNotEmpty() || data.returnTypeName != "boolean") null
                else RedOneGate(data.className, data.methodName)
            }.distinct()
        }.getOrNull().orEmpty()
    }

    override fun onHook() {
        redMode = preferences(ModulePrefs).getString("alarmclock_widget_redone_mode", "0")
        dataChannel.wait<String>("alarmclock_widget_redone_mode") { redMode = it }

        //先找宿主的红一开关：找到说明该版本有红 layout，拦开关即可，不需要替换 layout
        val gates = dexKitBridge.findRedOneGates()
        if (gates.isNotEmpty()) {
            loadHooker(RedOneSwitch(gates))
            return
        }
        //找不到开关说明该版本没有红 layout，退回文本上色
        hookTextRedOne()
    }

    /** 没有红 layout 的版本：拦截组件生成时间的 CharSequence，把 "1" 上色 */
    private fun hookTextRedOne() {
        val onePlusWidget = OnePlusWidget.toClassOrNull()?.resolve()
        if (onePlusWidget == null) {
            YLog.debug("AlarmClockTextRedOne -> 未找到 OnePlusWidget，跳过", tag = "AlarmClockWidget")
            return
        }
        when {
            onePlusWidget.firstMethodOrNull {
                parameters(String::class, String::class)
                returnType = CharSequence::class
            } != null -> loadHooker(AlarmClock12)

            onePlusWidget.firstMethodOrNull {
                returnType = RemoteViews::class
            } != null -> loadHooker(AlarmClock130(dexKitBridge))
        }
    }

    private class RedOneSwitch(private val gates: List<RedOneGate>) : YukiBaseHooker() {
        override fun onHook() {
            gates.forEach { gate ->
                //宿主布局选择点走的是 O() { if (k()) return false; return i() } 这类包装方法，k() 恒为 false，
                //最终都会落到这里拦的开关方法上，所以只拦一个方法就能覆盖全部布局选择点
                val clazz = gate.className.toClassOrNull() ?: return@forEach
                clazz.resolve().method {
                    name = gate.methodName
                    parameterCount = 0
                    returnType = classOf<Boolean>()
                    modifiers(Modifiers.STATIC, Modifiers.PUBLIC)
                }.forEach { method ->
                    method.intercept {
                        when (redMode) {
                            //跟随系统
                            "0" -> proceed()
                            //强制红一，宿主缺 OplusTextClock 时选中红 layout 会 inflate 崩溃，退回原逻辑
                            "1" -> if (OplusTextClock.toClassOrNull() != null) true else proceed()
                            //强制普通
                            "2" -> false
                            else -> proceed()
                        }
                    }
                }
            }
        }
    }

    private class AlarmClock130(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //OnePlusWidget setTextViewText -> local_hour_txt -> SpannableStringBuilder -> CharSequence
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Boolean>())
                        addForType(classOf<Handler>())
                    }
                    methods {
                        add { returnType(classOf<Boolean>()) }
                        add { returnType(classOf<Handler>()) }
                        add { paramTypes(classOf<Context>()) }
                        add { paramTypes(classOf<Context>(), classOf<String>()) }
                        add {
                            paramTypes(
                                classOf<Context>(),
                                classOf<String>(),
                                classOf<String>()
                            )
                        }
                    }
                }
            }.apply {
                checkDataList("AlarmClock13")
                single().name.toClass().apply {
                    resolve().method {
                        parameters { it[0] == classOf<Context>() && it[1] == classOf<String>() }
                        parameterCount { it in 2..3 }
                    }.forEach {
                        it.intercept {
                            if (redMode == "0") return@intercept proceed()
                            val raw = proceed()
                            val text = raw as? CharSequence ?: return@intercept raw
                            when (redMode) {
                                "1" -> setCharRedOne(text)
                                "2" -> text.toString()
                                else -> raw
                            }
                        }
                    }
                }
            }
        }
    }

    private object AlarmClock12 : YukiBaseHooker() {
        override fun onHook() {
            //Source OnePlusWidget
            OnePlusWidget.toClass().resolve().apply {
                firstMethod {
                    parameters(String::class, String::class)
                    returnType = CharSequence::class
                }.intercept {
                    if (redMode == "0") return@intercept proceed()
                    val raw = proceed()
                    val text = raw as? CharSequence ?: return@intercept raw
                    when (redMode) {
                        "1" -> setCharRedOne(text)
                        "2" -> text.toString()
                        else -> raw
                    }
                }
            }
        }
    }
}
