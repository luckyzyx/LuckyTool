package com.luckyzyx.luckytool.hook.scopes.launcher

import android.os.Process
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

@Obfuscate
class ForceEnableRecentTaskMemoryDisplay(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    private val readingPhysicalMemory = ThreadLocal<Boolean>()

    override fun onHook() {
        val osCode = getOSVersionCode
        //Source MemoryInfoManager
        //C16 及以下类名与方法名均未混淆，按名字定位；C17 起被混淆，只能经 DexKit 按调用点特征定位
        if (osCode >= 40) {
            dexKitBridge.findClass {
                matcher {
                    usingStrings("MemoryInfoManager")
                }
            }.apply {
                checkDataList("MemoryInfoManager")

                findMethod {
                    matcher {
                        paramCount(0)
                        returnType(classOf<Boolean>())
                        callerMethods {
                            add { name("getIsMem") }
                            add {
                                paramTypes(classOf<Boolean>())
                                returnType(Void.TYPE)
                                usingNumbers(8)
                            }
                        }
                    }
                }.apply {
                    checkDataList("needMemoryDetail")
                }.single().getMethodInstance(hostClassLoader!!).hook {
                    intercept(true)
                }

                findMethod {
                    matcher {
                        paramCount(0)
                        returnType(classOf<Boolean>())
                        callerMethods {
                            add {
                                paramTypes(classOf<Boolean>())
                                returnType(Void.TYPE)
                                usingStrings("OplusRecentsView")
                            }
                        }
                    }
                }.apply {
                    checkDataList("isAllowMemoryInfoDisplay")
                }.single().getMethodInstance(hostClassLoader!!).hook {
                    intercept(true)
                }

                findMethod {
                    matcher {
                        paramTypes(classOf<Boolean>())
                        usingStrings(
                            "persist.sys.oplus.nandswap.condition",
                            "get_rom2ram_pinned_bytes", "MemAvailable:"
                        )
                        addInvoke {
                            name("readProcLines")
                            paramTypes(classOf<String>(), classOf<Array<String>>(), classOf<LongArray>())
                        }
                    }
                }.checkDataList("getPhysicalMemoryInfo").singleOrNull()
                    ?.getMethodInstance(hostClassLoader!!)?.let { hookPhysicalMemory(it) }
            }
        } else {
            "com.oplus.quickstep.memory.MemoryInfoManager".toClass().resolve().apply {
                firstMethod { name = "isAllowMemoryInfoDisplay" }.hook { intercept(true) }
                firstMethod { name = "needMemoryDetail" }.hook { intercept(true) }
                firstMethodOrNull {
                    name = "getMemoryInfo"
                    parameters(Boolean::class)
                }?.self?.let { hookPhysicalMemory(it) }
            }
        }
    }

    /** 仅修正可用内存；总内存及扩展容量后缀仍由系统按原值格式化。 */
    private fun hookPhysicalMemory(memoryMethod: Method) {
        val loader = hostClassLoader ?: return
        "com.oplus.util.OplusUnitConversionUtils".toClassOrNull(loader)?.resolve()
            ?.firstMethodOrNull {
                name = "getUnitValue"
                parameters(Long::class)
                returnType = String::class
            }?.hook {
                before {
                    if (readingPhysicalMemory.get() != true) return@before
                    val callIndex = (readingPhysicalMemoryCallIndex.get() ?: 0) + 1
                    readingPhysicalMemoryCallIndex.set(callIndex)
                    if (callIndex != 2) return@before

                    //第二次换算才是可用内存，直接读取 MemAvailable，排除扩展内存和 rom2ram。
                    readPhysicalAvailableMemory()?.let { firstArg().set(it) }
                }
            } ?: return

        memoryMethod.hook {
            before {
                readingPhysicalMemory.set(true)
                readingPhysicalMemoryCallIndex.set(0)
            }
            after {
                readingPhysicalMemory.remove()
                readingPhysicalMemoryCallIndex.remove()
            }
        }
    }

    private fun readPhysicalAvailableMemory(): Long? {
        val values = LongArray(1)
        Process::class.java.resolve().firstMethod {
            name = "readProcLines"
            parameters(String::class, Array<String>::class, LongArray::class)
        }.invokeQuietly("/proc/meminfo", arrayOf("MemAvailable:"), values)
        return values.first().takeIf { it > 0L }?.times(1024L)
    }

    private companion object {
        val readingPhysicalMemoryCallIndex = ThreadLocal.withInitial { 0 }
    }
}
