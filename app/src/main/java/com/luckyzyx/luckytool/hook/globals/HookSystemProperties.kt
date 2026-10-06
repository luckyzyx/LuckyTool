package com.luckyzyx.luckytool.hook.globals

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class HookSystemProperties(private val props: Map<String, Any>) : YukiBaseHooker() {
    override fun onHook() {
        if (props.isEmpty()) return
        //Source SystemProperties
        arrayOf(
            "android.os.SystemProperties",
            "com.oplus.wrapper.os.SystemProperties"
        ).forEach {
            it.toClass().resolve().apply {
                method { name = "get"; returnType = String::class }.hookAll {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Boolean -> result = value.toString()
                            is String -> result = value
                            is Int -> result = value
                        }
                    }
                }
                firstMethod { name = "getBoolean"; returnType = Boolean::class }.hook {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            "1" -> result = true
                            "0" -> result = false
                            "true" -> result = true
                            "false" -> result = false
                            is Boolean -> result = value
                        }
                    }
                }
                firstMethod { name = "getInt"; returnType = Int::class }.hook {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Long -> result = value.toInt()
                            is Int -> result = value
                        }
                    }
                }
                firstMethod { name = "getLong"; returnType = Long::class }.hook {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Int -> result = value.toLong()
                            is Long -> result = value
                        }
                    }
                }
            }
        }
    }
}